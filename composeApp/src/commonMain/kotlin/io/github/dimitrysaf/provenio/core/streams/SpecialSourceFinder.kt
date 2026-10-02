package io.github.dimitrysaf.provenio.core.streams

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.buildAddonResourceUrl
import io.github.dimitrysaf.provenio.core.addons.fetchAddonResponseText
import io.github.dimitrysaf.provenio.core.addons.httpGetText
import io.github.dimitrysaf.provenio.core.addons.httpRequestRaw
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.build.TrailerPlaybackMode
import io.github.dimitrysaf.provenio.core.debrid.encodePathSegment
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.trailer.youTubeWatchUrl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal data class SpecialContext(
    val parentMetaId: String,
    val seriesName: String,
    val specialTitle: String,
)

internal data class FoundVideo(
    val videoId: String,
    val title: String,
    val detail: String?,
)

internal data class ImdbSuggestion(
    val id: String,
    val title: String,
)

internal object SpecialSourceFinder {
    private val log = Logger.withTag("SpecialSources")
    private val json = Json { ignoreUnknownKeys = true }
    private val imdbIds = mutableMapOf<String, String>()
    private val foundStreams = mutableMapOf<String, List<StreamItem>>()
    private val cacheMutex = Mutex()

    suspend fun context(type: String, parentMetaId: String, episode: Int): SpecialContext? {
        val meta = MetaDetailsRepository.peek(type, parentMetaId) ?: MetaDetailsRepository.fetch(type, parentMetaId)
            ?: return null
        val specialTitle = meta.videos
            .firstOrNull { it.season == 0 && it.episode == episode }
            ?.title
            ?.trim()
            ?.takeUnless(::isGenericEpisodeTitle)
            ?: return null
        val seriesName = meta.name.trim().takeIf(String::isNotBlank) ?: return null
        return SpecialContext(parentMetaId = parentMetaId, seriesName = seriesName, specialTitle = specialTitle)
    }

    suspend fun imdbId(context: SpecialContext): String? {
        val cacheKey = "${context.parentMetaId}:${context.specialTitle}"
        cacheMutex.withLock { imdbIds[cacheKey]?.let { return it } }
        val query = youTubeSearchQuery(context.seriesName, context.specialTitle)
        val body = runCatching {
            httpGetText("https://v3.sg.media-imdb.com/suggestion/x/${encodePathSegment(query)}.json")
        }.onFailure { log.w(it) { "IMDb search failed" } }.getOrNull() ?: return null
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
        val imdbId = matchingImdbSuggestion(
            suggestions = parseImdbSuggestions(root),
            seriesId = context.parentMetaId,
            seriesName = context.seriesName,
            specialTitle = context.specialTitle,
        )?.id ?: return null
        cacheMutex.withLock { imdbIds[cacheKey] = imdbId }
        return imdbId
    }

    suspend fun streams(context: SpecialContext): List<StreamItem> {
        val cacheKey = "${context.parentMetaId}:${context.specialTitle}"
        cacheMutex.withLock { foundStreams[cacheKey]?.let { return it } }
        val found = searchYouTube(context.seriesName, context.specialTitle).map { it.toStreamItem() }
        if (found.isNotEmpty()) {
            cacheMutex.withLock { foundStreams[cacheKey] = found }
        }
        return found
    }

    private suspend fun searchYouTube(seriesName: String, specialTitle: String): List<FoundVideo> {
        val body = buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB")
                    put("clientVersion", YouTubeWebClientVersion)
                    put("hl", "en")
                }
            }
            put("query", youTubeSearchQuery(seriesName, specialTitle))
            put("params", YouTubeVideoOnlyParams)
        }
        val response = runCatching {
            httpRequestRaw(
                method = "POST",
                url = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false",
                headers = mapOf(
                    "content-type" to "application/json",
                    "origin" to "https://www.youtube.com",
                    "user-agent" to YouTubeUserAgent,
                ),
                body = body.toString(),
                maxResponseBodyBytes = YouTubeSearchMaxBytes,
            )
        }.onFailure { log.w(it) { "YouTube search failed" } }.getOrNull() ?: return emptyList()
        if (response.status !in 200..299) return emptyList()
        val root = runCatching { json.parseToJsonElement(response.body) }.getOrNull() ?: return emptyList()
        return matchingSearchResults(parseYouTubeSearch(root), seriesName, specialTitle)
    }

    private fun FoundVideo.toStreamItem(): StreamItem {
        val watchUrl = youTubeWatchUrl(videoId)
        val playsInApp = AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
        return StreamItem(
            name = title,
            description = detail,
            url = watchUrl.takeIf { playsInApp },
            externalUrl = watchUrl.takeUnless { playsInApp },
            addonName = "YouTube",
            addonId = EmbeddedSourceAddonId,
            discovered = true,
        )
    }

    private const val YouTubeSearchMaxBytes = 8 * 1024 * 1024
    private const val YouTubeWebClientVersion = "2.20250101.00.00"
    private const val YouTubeVideoOnlyParams = "EgIQAQ=="
    private const val YouTubeUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
}

private val GenericEpisodeTitle = Regex("^(episode|special|ep\\.?|e)\\s*\\d*$", RegexOption.IGNORE_CASE)
private val StopWords = setOf("the", "a", "an", "of", "and", "in", "on", "to", "at", "for", "with")
private const val MaxSearchResults = 5

internal fun isGenericEpisodeTitle(title: String): Boolean =
    title.isBlank() || GenericEpisodeTitle.matches(title.trim())

internal fun normalizeTitle(value: String): String =
    value.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

private fun significantWords(value: String): Set<String> =
    normalizeTitle(value).split(' ').filter { it.isNotBlank() && it !in StopWords }.toSet()

internal fun youTubeSearchQuery(seriesName: String, specialTitle: String): String =
    if (normalizeTitle(specialTitle).contains(normalizeTitle(seriesName))) specialTitle else "$seriesName $specialTitle"

internal fun matchingSearchResults(
    results: List<FoundVideo>,
    seriesName: String,
    specialTitle: String,
): List<FoundVideo> {
    val required = significantWords(seriesName) + significantWords(specialTitle)
    if (required.isEmpty()) return emptyList()
    return results
        .filter { video -> significantWords(video.title).containsAll(required) }
        .take(MaxSearchResults)
}

internal fun parseImdbSuggestions(root: JsonElement): List<ImdbSuggestion> =
    ((root as? JsonObject)?.get("d") as? JsonArray)
        .orEmpty()
        .mapNotNull { entry ->
            val item = entry as? JsonObject ?: return@mapNotNull null
            val id = (item["id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val title = (item["l"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            ImdbSuggestion(id = id, title = title)
        }

internal fun matchingImdbSuggestion(
    suggestions: List<ImdbSuggestion>,
    seriesId: String,
    seriesName: String,
    specialTitle: String,
): ImdbSuggestion? {
    val seriesImdbId = seriesId.substringBefore(':')
    val wanted = setOf(
        normalizeTitle(specialTitle),
        normalizeTitle(youTubeSearchQuery(seriesName, specialTitle)),
    )
    return suggestions.firstOrNull { suggestion ->
        suggestion.id.startsWith("tt") &&
            suggestion.id != seriesImdbId &&
            normalizeTitle(suggestion.title) in wanted
    }
}

internal fun parseYouTubeSearch(root: JsonElement): List<FoundVideo> {
    val found = mutableListOf<FoundVideo>()
    fun visit(element: JsonElement) {
        when (element) {
            is JsonObject -> {
                (element["videoRenderer"] as? JsonObject)?.let { renderer ->
                    val videoId = (renderer["videoId"] as? JsonPrimitive)?.contentOrNull
                    val title = renderer.text("title")
                    if (!videoId.isNullOrBlank() && !title.isNullOrBlank()) {
                        val detail = listOfNotNull(renderer.text("ownerText"), renderer.text("lengthText"))
                            .joinToString(" · ")
                            .ifBlank { null }
                        found += FoundVideo(videoId = videoId, title = title, detail = detail)
                    }
                }
                element.values.forEach { visit(it) }
            }
            is JsonArray -> element.forEach { visit(it) }
            else -> Unit
        }
    }
    visit(root)
    return found.distinctBy { it.videoId }
}

private fun JsonObject.text(name: String): String? {
    val node = this[name] as? JsonObject ?: return null
    (node["simpleText"] as? JsonPrimitive)?.contentOrNull?.let { return it }
    return (node["runs"] as? JsonArray)
        ?.mapNotNull { ((it as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull }
        ?.joinToString("")
        ?.ifBlank { null }
}

internal suspend fun InstalledStreamAddonTarget.fetchStreamItems(
    type: String,
    id: String,
    forceRefresh: Boolean,
): List<StreamItem> {
    val payload = fetchAddonResponseText(
        url = buildAddonResourceUrl(
            manifestUrl = manifest.transportUrl,
            resource = "stream",
            type = type,
            id = id,
        ),
        forceRefresh = forceRefresh,
    )
    return StreamParser.parse(
        payload = payload,
        addonName = addonName,
        addonId = addonId,
        addonLogo = manifest.logoUrl,
    )
}

internal suspend fun InstalledStreamAddonTarget.fetchSpecialStreams(
    specialImdbId: String?,
    forceRefresh: Boolean,
): List<StreamItem> {
    if (specialImdbId == null || !manifest.supportsStream(SpecialStreamType, specialImdbId)) return emptyList()
    return runCatchingUnlessCancelled {
        fetchStreamItems(type = SpecialStreamType, id = specialImdbId, forceRefresh = forceRefresh)
    }.getOrDefault(emptyList())
}

internal fun AddonStreamGroup.withSpecialStreams(specialStreams: List<StreamItem>): AddonStreamGroup {
    if (specialStreams.isEmpty()) return this
    return copy(
        streams = (streams + specialStreams).distinctBy { it.sourceKey() },
        error = null,
    )
}

internal fun List<StreamItem>.distinctSources(): List<StreamItem> = distinctBy { it.sourceKey() }

private fun StreamItem.sourceKey(): String =
    url ?: infoHash?.let { "$it:${fileIdx ?: ""}" } ?: externalUrl ?: "$addonId:$name:$description"

private const val SpecialStreamType = "movie"
