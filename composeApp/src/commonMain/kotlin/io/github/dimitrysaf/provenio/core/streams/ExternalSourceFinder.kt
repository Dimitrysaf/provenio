package io.github.dimitrysaf.provenio.core.streams

import androidx.compose.ui.text.intl.Locale
import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.httpGetText
import io.github.dimitrysaf.provenio.core.debrid.encodePathSegment
import io.github.dimitrysaf.provenio.core.debrid.queryString
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbConfig
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbService
import io.github.dimitrysaf.provenio.core.metadata.tmdb.buildTmdbUrl
import io.github.dimitrysaf.provenio.core.settings.ThemeMode
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository
import io.github.dimitrysaf.provenio.core.streams.availability.AvailabilityOptionKind
import io.github.dimitrysaf.provenio.core.streams.availability.AvailabilityScope
import io.github.dimitrysaf.provenio.core.streams.availability.StreamingAvailabilityService
import io.github.dimitrysaf.provenio.core.streams.availability.StreamingAvailabilitySettingsRepository
import io.github.dimitrysaf.provenio.core.streams.availability.availabilityShowId
import io.github.dimitrysaf.provenio.core.streams.availability.matchAvailability
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.*

internal data class ExternalSourceRequest(
    val type: String,
    val metaId: String,
    val season: Int?,
    val episode: Int?,
) {
    val isMovie: Boolean
        get() = type == "movie"
}

internal fun externalSourceRequest(
    type: String,
    videoId: String,
    parentMetaId: String?,
    season: Int?,
    episode: Int?,
): ExternalSourceRequest? =
    when (type.trim().lowercase()) {
        "movie" -> ExternalSourceRequest(type = "movie", metaId = videoId, season = null, episode = null)
        "series" -> if (season != null && episode != null) {
            ExternalSourceRequest(type = "series", metaId = parentMetaId ?: videoId, season = season, episode = episode)
        } else {
            null
        }
        else -> null
    }

internal enum class WatchOfferKind(val key: String) {
    Free("free"),
    Ads("ads"),
    Subscription("flatrate"),
    Rent("rent"),
    Buy("buy"),
}

internal data class WatchOffer(
    val providerName: String,
    val logoPath: String?,
    val kind: WatchOfferKind,
    val priority: Int,
)

internal data class WatchAvailability(
    val link: String?,
    val offers: List<WatchOffer>,
)

internal data class TmdbSeasonEpisode(
    val episodeNumber: Int,
    val name: String,
)

internal data class TmdbVideo(
    val key: String,
    val site: String,
    val name: String,
    val type: String?,
)

internal data class ArchiveItem(
    val identifier: String,
    val title: String,
    val year: Int?,
)

internal data class ArchiveFile(
    val name: String,
    val format: String?,
    val size: Long?,
)

internal object ExternalSourceFinder {
    private val log = Logger.withTag("ExternalSources")
    private val json = Json { ignoreUnknownKeys = true }
    private val foundStreams = mutableMapOf<String, List<StreamItem>>()
    private val cacheMutex = Mutex()

    suspend fun streams(request: ExternalSourceRequest, special: SpecialContext?): List<StreamItem> {
        val region = watchRegion()
        val availability = StreamingAvailabilitySettingsRepository.snapshot()
        val cacheKey = "$region:${availability.isActive}:${availability.apiKey.hashCode()}:${request.type}:${request.metaId}:${request.season}:${request.episode}"
        cacheMutex.withLock { foundStreams[cacheKey]?.let { return it } }
        val found = coroutineScope {
            val videos = async {
                special?.let { context ->
                    runCatchingUnlessCancelled { specialVideoStreams(request, context) }
                        .onFailure { log.w(it) { "TMDB special videos failed" } }
                        .getOrNull()
                }.orEmpty()
            }
            val archive = async {
                runCatchingUnlessCancelled { archiveStreams(request, special) }
                    .onFailure { log.w(it) { "Internet Archive search failed" } }
                    .getOrDefault(emptyList())
            }
            val providers = async {
                runCatchingUnlessCancelled { availabilityStreams(request, special, region) }
                    .onFailure { log.w(it) { "Streaming Availability failed" } }
                    .getOrNull()
                    ?: runCatchingUnlessCancelled { watchProviderStreams(request, region) }
                        .onFailure { log.w(it) { "TMDB watch providers failed" } }
                        .getOrDefault(emptyList())
            }
            videos.await() + archive.await() + providers.await()
        }
        if (found.isNotEmpty()) {
            cacheMutex.withLock { foundStreams[cacheKey] = found }
        }
        return found
    }

    private suspend fun watchProviderStreams(request: ExternalSourceRequest, region: String): List<StreamItem> {
        val tmdbId = tmdbId(request) ?: return emptyList()
        val season = request.season
        val availability: WatchAvailability?
        val scope: String?
        if (request.isMovie || season == null) {
            availability = tmdbJson("movie/$tmdbId/watch/providers")?.let { parseWatchProviders(it, region) }
            scope = null
        } else {
            val seasonAvailability = tmdbJson("tv/$tmdbId/season/$season/watch/providers")
                ?.let { parseWatchProviders(it, region) }
            when {
                seasonAvailability != null -> {
                    availability = seasonAvailability
                    scope = if (season == 0) {
                        getString(Res.string.source_offer_specials)
                    } else {
                        getString(Res.string.source_offer_season, season)
                    }
                }
                season == 0 -> {
                    availability = null
                    scope = null
                }
                else -> {
                    availability = tmdbJson("tv/$tmdbId/watch/providers")?.let { parseWatchProviders(it, region) }
                    scope = null
                }
            }
        }
        availability ?: return emptyList()
        val link = availability.link
            ?: "https://www.themoviedb.org/${if (request.isMovie) "movie" else "tv"}/$tmdbId/watch?locale=$region"
        val description = listOfNotNull(scope, getString(Res.string.source_offer_attribution)).joinToString(" · ")
        return availability.offers.map { offer ->
            StreamItem(
                name = offer.kind.label(),
                description = description,
                externalUrl = link,
                addonName = offer.providerName,
                addonId = EmbeddedSourceAddonId,
                addonLogo = offer.logoPath?.let { "$TmdbLogoBaseUrl$it" },
                discovered = true,
            )
        }
    }

    private suspend fun availabilityStreams(
        request: ExternalSourceRequest,
        special: SpecialContext?,
        region: String,
    ): List<StreamItem>? {
        val settings = StreamingAvailabilitySettingsRepository.snapshot()
        if (!settings.isActive) return null
        val isSeries = !request.isMovie
        val showId = availabilityShowId(request.metaId, null, isSeries)
            ?: availabilityShowId(request.metaId, tmdbId(request), isSeries)
            ?: return null
        val show = StreamingAvailabilityService.show(
            apiKey = settings.apiKey,
            showId = showId,
            isSeries = isSeries,
            country = region,
        ) ?: return null
        val match = matchAvailability(
            show = show,
            country = region,
            season = request.season,
            episode = request.episode,
            specialTitle = special?.specialTitle,
            normalize = ::normalizeTitle,
        ) ?: return null
        val scope = when (match.scope) {
            AvailabilityScope.Season -> request.season?.let { getString(Res.string.source_offer_season, it) }
            AvailabilityScope.Title, AvailabilityScope.Episode -> null
        }
        val attribution = getString(Res.string.source_offer_streaming_availability)
        val darkLogos = ThemeSettingsRepository.themeMode.value != ThemeMode.LIGHT
        return match.options.map { option ->
            StreamItem(
                name = listOfNotNull(option.kind.label(), option.price).joinToString(" · "),
                description = listOfNotNull(
                    option.serviceName.takeIf { option.addonName != null },
                    option.quality,
                    scope,
                    attribution,
                ).joinToString(" · "),
                externalUrl = option.link,
                addonName = option.addonName ?: option.serviceName,
                addonId = EmbeddedSourceAddonId,
                addonLogo = if (darkLogos) option.darkLogo ?: option.lightLogo else option.lightLogo ?: option.darkLogo,
                discovered = true,
            )
        }
    }

    private suspend fun specialVideoStreams(request: ExternalSourceRequest, special: SpecialContext): List<StreamItem> {
        val tmdbId = tmdbId(request) ?: return emptyList()
        val episodes = tmdbJson("tv/$tmdbId/season/0")?.let(::parseTmdbSeasonEpisodes).orEmpty()
        val episodeNumber = matchingSpecialEpisode(episodes, special.specialTitle) ?: return emptyList()
        return tmdbJson(
            endpoint = "tv/$tmdbId/season/0/episode/$episodeNumber/videos",
            query = mapOf("include_video_language" to "en,null"),
        )
            ?.let(::parseTmdbVideos)
            .orEmpty()
            .mapNotNull { it.toStreamItem() }
    }

    private suspend fun archiveStreams(request: ExternalSourceRequest, special: SpecialContext?): List<StreamItem> {
        val query: String
        val year: Int?
        if (special != null) {
            query = youTubeSearchQuery(special.seriesName, special.specialTitle)
            year = null
        } else if (request.isMovie) {
            val meta = MetaDetailsRepository.peek(request.type, request.metaId)
                ?: MetaDetailsRepository.fetch(request.type, request.metaId)
                ?: return emptyList()
            query = meta.name.trim().takeIf(String::isNotBlank) ?: return emptyList()
            year = meta.releaseInfo?.let { ReleaseYear.find(it)?.value?.toIntOrNull() }
        } else {
            return emptyList()
        }
        val words = significantWords(query)
        if (words.isEmpty()) return emptyList()
        val search = "title:(${words.joinToString(" AND ")}) AND mediatype:(movies) AND " +
            "collection:(${ArchiveCollections.joinToString(" OR ")})"
        val url = "https://archive.org/advancedsearch.php?" + queryString(
            "q" to search,
            "fl[]" to "identifier",
            "fl[]" to "title",
            "fl[]" to "year",
            "rows" to ArchiveSearchRows.toString(),
            "output" to "json",
        )
        val items = json.parseToJsonElement(httpGetText(url)).let(::parseArchiveSearch)
        val matches = matchingArchiveItems(items, query, year, exactTitle = special == null)
        return coroutineScope {
            matches.map { item ->
                async {
                    runCatchingUnlessCancelled {
                        json.parseToJsonElement(httpGetText("https://archive.org/metadata/${encodePathSegment(item.identifier)}"))
                    }.getOrNull()?.let(::pickArchiveVideo)?.let { file -> item.toStreamItem(file) }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun tmdbId(request: ExternalSourceRequest): String? {
        if (TmdbConfig.API_KEY.isBlank()) return null
        return TmdbService.ensureTmdbId(request.metaId, if (request.isMovie) "movie" else "tv")
    }

    private suspend fun tmdbJson(endpoint: String, query: Map<String, String> = emptyMap()): JsonElement? =
        runCatchingUnlessCancelled {
            json.parseToJsonElement(httpGetText(buildTmdbUrl(endpoint = endpoint, apiKey = TmdbConfig.API_KEY, query = query)))
        }.onFailure { log.w { "TMDB request failed for $endpoint: ${it.message}" } }.getOrNull()

    private fun watchRegion(): String =
        runCatching { Locale.current.region }
            .getOrNull()
            ?.uppercase()
            ?.takeIf { it.length == 2 }
            ?: "US"

    private suspend fun WatchOfferKind.label(): String =
        when (this) {
            WatchOfferKind.Free -> getString(Res.string.source_offer_free)
            WatchOfferKind.Ads -> getString(Res.string.source_offer_ads)
            WatchOfferKind.Subscription -> getString(Res.string.source_offer_subscription)
            WatchOfferKind.Rent -> getString(Res.string.source_offer_rent)
            WatchOfferKind.Buy -> getString(Res.string.source_offer_buy)
        }

    private suspend fun AvailabilityOptionKind.label(): String =
        when (this) {
            AvailabilityOptionKind.Free -> getString(Res.string.source_offer_free)
            AvailabilityOptionKind.Subscription -> getString(Res.string.source_offer_subscription)
            AvailabilityOptionKind.Addon -> getString(Res.string.source_offer_addon)
            AvailabilityOptionKind.Rent -> getString(Res.string.source_offer_rent)
            AvailabilityOptionKind.Buy -> getString(Res.string.source_offer_buy)
        }

    private fun TmdbVideo.toStreamItem(): StreamItem? =
        when (site.lowercase()) {
            "youtube" -> youTubeStreamItem(videoId = key, title = name, detail = type)
            "vimeo" -> StreamItem(
                name = name,
                description = type,
                externalUrl = "https://vimeo.com/$key",
                addonName = "Vimeo",
                addonId = EmbeddedSourceAddonId,
                discovered = true,
            )
            else -> null
        }

    private fun ArchiveItem.toStreamItem(file: ArchiveFile): StreamItem =
        StreamItem(
            name = title,
            description = listOfNotNull(year?.toString(), file.format).joinToString(" · ").ifBlank { null },
            url = "https://archive.org/download/${encodePathSegment(identifier)}/" +
                file.name.split('/').joinToString("/") { encodePathSegment(it) },
            addonName = "Internet Archive",
            addonId = EmbeddedSourceAddonId,
            behaviorHints = StreamBehaviorHints(videoSize = file.size, filename = file.name.substringAfterLast('/')),
            discovered = true,
        )

    private const val TmdbLogoBaseUrl = "https://image.tmdb.org/t/p/w92"
    private const val ArchiveSearchRows = 25
    private val ArchiveCollections = listOf("feature_films", "classic_tv", "classic_cartoons", "silent_films")
    private val ReleaseYear = Regex("\\d{4}")
}

private const val MaxArchiveMatches = 3
private val TrailingYear = Regex("\\s(19|20)\\d{2}$")

internal fun parseWatchProviders(root: JsonElement, region: String): WatchAvailability? {
    val country = ((root as? JsonObject)?.get("results") as? JsonObject)
        ?.get(region.uppercase()) as? JsonObject
        ?: return null
    val offers = WatchOfferKind.entries.flatMap { kind ->
        (country[kind.key] as? JsonArray)
            .orEmpty()
            .mapNotNull { entry ->
                val provider = entry as? JsonObject ?: return@mapNotNull null
                val name = provider.stringValue("provider_name")?.trim()?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                WatchOffer(
                    providerName = name,
                    logoPath = provider.stringValue("logo_path")?.takeIf(String::isNotBlank),
                    kind = kind,
                    priority = provider.stringValue("display_priority")?.toIntOrNull() ?: Int.MAX_VALUE,
                )
            }
            .sortedBy { it.priority }
    }
    if (offers.isEmpty()) return null
    return WatchAvailability(link = country.stringValue("link")?.takeIf(String::isNotBlank), offers = offers)
}

internal fun parseTmdbSeasonEpisodes(root: JsonElement): List<TmdbSeasonEpisode> =
    ((root as? JsonObject)?.get("episodes") as? JsonArray)
        .orEmpty()
        .mapNotNull { entry ->
            val episode = entry as? JsonObject ?: return@mapNotNull null
            val number = episode.stringValue("episode_number")?.toIntOrNull() ?: return@mapNotNull null
            val name = episode.stringValue("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            TmdbSeasonEpisode(episodeNumber = number, name = name)
        }

internal fun matchingSpecialEpisode(episodes: List<TmdbSeasonEpisode>, specialTitle: String): Int? {
    val wanted = normalizeTitle(specialTitle)
    if (wanted.isBlank()) return null
    return episodes.firstOrNull { normalizeTitle(it.name) == wanted }?.episodeNumber
}

internal fun parseTmdbVideos(root: JsonElement): List<TmdbVideo> =
    ((root as? JsonObject)?.get("results") as? JsonArray)
        .orEmpty()
        .mapNotNull { entry ->
            val video = entry as? JsonObject ?: return@mapNotNull null
            val key = video.stringValue("key")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val site = video.stringValue("site")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = video.stringValue("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            TmdbVideo(key = key, site = site, name = name, type = video.stringValue("type"))
        }

internal fun parseArchiveSearch(root: JsonElement): List<ArchiveItem> =
    (((root as? JsonObject)?.get("response") as? JsonObject)?.get("docs") as? JsonArray)
        .orEmpty()
        .mapNotNull { entry ->
            val doc = entry as? JsonObject ?: return@mapNotNull null
            val identifier = doc.stringValue("identifier")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = doc.stringValue("title")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            ArchiveItem(identifier = identifier, title = title, year = doc.stringValue("year")?.take(4)?.toIntOrNull())
        }

internal fun matchingArchiveItems(
    items: List<ArchiveItem>,
    title: String,
    year: Int?,
    exactTitle: Boolean,
): List<ArchiveItem> {
    val required = significantWords(title)
    if (required.isEmpty()) return emptyList()
    val wanted = normalizeTitle(title)
    return items
        .filter { item ->
            if (exactTitle) {
                val itemTitle = normalizeTitle(item.title)
                (itemTitle == wanted || itemTitle.replace(TrailingYear, "") == wanted) &&
                    (year == null || item.year == null || kotlin.math.abs(item.year - year) <= 1)
            } else {
                significantWords(item.title).containsAll(required)
            }
        }
        .distinctBy { it.identifier }
        .take(MaxArchiveMatches)
}

internal fun pickArchiveVideo(root: JsonElement): ArchiveFile? {
    val item = root as? JsonObject ?: return null
    if ((item["is_dark"] as? JsonPrimitive)?.booleanOrNull == true) return null
    val metadata = item["metadata"] as? JsonObject
    if (metadata?.stringValue("access-restricted-item") == "true") return null
    return (item["files"] as? JsonArray)
        .orEmpty()
        .mapNotNull { entry ->
            val file = entry as? JsonObject ?: return@mapNotNull null
            val name = file.stringValue("name")?.takeIf { it.endsWith(".mp4", ignoreCase = true) }
                ?: return@mapNotNull null
            ArchiveFile(name = name, format = file.stringValue("format"), size = file.stringValue("size")?.toLongOrNull())
        }
        .sortedWith(
            compareBy<ArchiveFile> { it.format?.contains("512Kb", ignoreCase = true) == true }
                .thenByDescending { it.size ?: 0L },
        )
        .firstOrNull()
}

private fun JsonObject.stringValue(name: String): String? =
    (this[name] as? JsonPrimitive)?.contentOrNull
