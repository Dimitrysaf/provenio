package io.github.dimitrysaf.provenio.core.metadata

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.httpGetText
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbApiKey
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbService
import io.github.dimitrysaf.provenio.core.metadata.tmdb.buildTmdbUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

internal data class TmdbCastEntry(
    val id: Int,
    val name: String,
    val originalName: String?,
)

object CastResolver {
    private val log = Logger.withTag("CastResolver")
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheMutex = Mutex()
    private val deceasedById = mutableMapOf<Int, Boolean>()
    private val creditsByTitle = mutableMapOf<String, List<TmdbCastEntry>>()
    private val tmdbIdByImdbId = mutableMapOf<String, Int>()
    private val requests = Semaphore(MaxParallelRequests)

    suspend fun resolve(cast: List<MetaPerson>, metaId: String?, metaType: String?): List<MetaPerson> =
        withContext(Dispatchers.Default) {
            if (cast.isEmpty() || TmdbApiKey.current().isBlank()) return@withContext cast
            try {
                resolveIds(cast, metaId, metaType).withDeathStatus()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                log.w(error) { "Could not resolve the cast" }
                cast
            }
        }

    private suspend fun resolveIds(cast: List<MetaPerson>, metaId: String?, metaType: String?): List<MetaPerson> {
        val needsTitleCredits = cast.any { it.tmdbId == null && it.imdbId == null }
        val creditIdsByName = if (needsTitleCredits && metaId != null && metaType != null) {
            matchableNames(titleCredits(metaId, metaType))
        } else {
            emptyMap()
        }
        return coroutineScope {
            cast.map { person ->
                async {
                    if (person.tmdbId != null) return@async person
                    val id = person.imdbId?.let { tmdbIdForImdb(it) }
                        ?: creditIdsByName[normalizePersonName(person.name)]
                    person.copy(tmdbId = id)
                }
            }.awaitAll()
        }
    }

    private suspend fun List<MetaPerson>.withDeathStatus(): List<MetaPerson> {
        val ids = mapNotNull { it.tmdbId }.distinct().take(MaxPeopleChecked)
        val deceased = coroutineScope {
            ids.map { id -> async { id to isDeceased(id) } }.awaitAll().toMap()
        }
        return map { person -> person.copy(deceased = person.tmdbId?.let { deceased[it] } == true) }
    }

    private suspend fun titleCredits(metaId: String, metaType: String): List<TmdbCastEntry> {
        val isMovie = when (metaType.trim().lowercase()) {
            "movie" -> true
            "series", "tv", "show" -> false
            else -> return emptyList()
        }
        val titleId = TmdbService.ensureTmdbId(metaId, if (isMovie) "movie" else "tv") ?: return emptyList()
        val cacheKey = "${if (isMovie) "movie" else "tv"}:$titleId"
        cacheMutex.withLock { creditsByTitle[cacheKey]?.let { return it } }
        val endpoint = if (isMovie) "movie/$titleId/credits" else "tv/$titleId/aggregate_credits"
        val credits = tmdbJson(endpoint)?.let(::parseTmdbCastEntries) ?: return emptyList()
        cacheMutex.withLock { creditsByTitle[cacheKey] = credits }
        return credits
    }

    private suspend fun tmdbIdForImdb(imdbId: String): Int? {
        cacheMutex.withLock { tmdbIdByImdbId[imdbId]?.let { return it } }
        val root = tmdbJson("find/$imdbId", mapOf("external_source" to "imdb_id")) as? JsonObject ?: return null
        val id = ((root["person_results"] as? JsonArray)?.firstOrNull() as? JsonObject)
            ?.let { (it["id"] as? JsonPrimitive)?.intOrNull }
            ?.takeIf { it > 0 }
            ?: return null
        cacheMutex.withLock { tmdbIdByImdbId[imdbId] = id }
        return id
    }

    private suspend fun isDeceased(personId: Int): Boolean {
        cacheMutex.withLock { deceasedById[personId]?.let { return it } }
        val person = tmdbJson("person/$personId") as? JsonObject ?: return false
        val deceased = !(person["deathday"] as? JsonPrimitive)?.contentOrNull.isNullOrBlank()
        cacheMutex.withLock { deceasedById[personId] = deceased }
        return deceased
    }

    private suspend fun tmdbJson(endpoint: String, query: Map<String, String> = emptyMap()): JsonElement? =
        requests.withPermit {
            try {
                json.parseToJsonElement(
                    httpGetText(buildTmdbUrl(endpoint = endpoint, apiKey = TmdbApiKey.current(), query = query)),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                log.w { "TMDB request failed for $endpoint: ${error.message}" }
                null
            }
        }

    private const val MaxParallelRequests = 6
    private const val MaxPeopleChecked = 40
}

internal fun parseTmdbCastEntries(root: JsonElement): List<TmdbCastEntry> =
    ((root as? JsonObject)?.get("cast") as? JsonArray)
        .orEmpty()
        .mapNotNull { element ->
            val entry = element as? JsonObject ?: return@mapNotNull null
            val id = (entry["id"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: return@mapNotNull null
            val name = (entry["name"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            TmdbCastEntry(
                id = id,
                name = name,
                originalName = (entry["original_name"] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank),
            )
        }

internal fun matchableNames(entries: List<TmdbCastEntry>): Map<String, Int> {
    val byName = linkedMapOf<String, Int>()
    entries.forEach { entry ->
        listOfNotNull(entry.name, entry.originalName)
            .map(::normalizePersonName)
            .filter(String::isNotBlank)
            .forEach { key -> if (key !in byName) byName[key] = entry.id }
    }
    return byName
}

internal fun normalizePersonName(name: String): String =
    name.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
