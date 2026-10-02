package io.github.dimitrysaf.provenio.core.streams.availability

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.httpRequestRaw
import io.github.dimitrysaf.provenio.core.debrid.encodePathSegment
import io.github.dimitrysaf.provenio.core.debrid.queryString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal enum class AvailabilityOptionKind(val key: String) {
    Free("free"),
    Subscription("subscription"),
    Addon("addon"),
    Rent("rent"),
    Buy("buy"),
}

internal data class AvailabilityOption(
    val serviceName: String,
    val addonName: String?,
    val lightLogo: String?,
    val darkLogo: String?,
    val kind: AvailabilityOptionKind,
    val link: String,
    val quality: String?,
    val price: String?,
)

internal enum class AvailabilityScope {
    Title,
    Season,
    Episode,
}

internal data class AvailabilityMatch(
    val scope: AvailabilityScope,
    val options: List<AvailabilityOption>,
)

object StreamingAvailabilityService {
    private val log = Logger.withTag("StreamingAvailability")
    private val json = Json { ignoreUnknownKeys = true }
    private val shows = mutableMapOf<String, JsonObject>()
    private val cacheMutex = Mutex()

    internal suspend fun show(apiKey: String, showId: String, isSeries: Boolean, country: String): JsonObject? {
        val cacheKey = "${apiKey.hashCode()}:$country:$showId:$isSeries"
        cacheMutex.withLock { shows[cacheKey]?.let { return it } }
        val isNativeKey = apiKey.startsWith(NativeKeyPrefix)
        val baseUrl = if (isNativeKey) NativeBaseUrl else RapidApiBaseUrl
        val headers = if (isNativeKey) {
            mapOf("X-API-Key" to apiKey)
        } else {
            mapOf("X-RapidAPI-Key" to apiKey, "X-RapidAPI-Host" to RapidApiHost)
        }
        val url = "$baseUrl/shows/${encodePathSegment(showId)}?" + queryString(
            "country" to country.lowercase(),
            "series_granularity" to if (isSeries) "episode" else "show",
            "output_language" to "en",
        )
        val response = httpRequestRaw(
            method = "GET",
            url = url,
            headers = headers + ("accept" to "application/json"),
            body = "",
            maxResponseBodyBytes = MaxResponseBytes,
        )
        if (response.status !in 200..299) {
            log.w { "Streaming Availability returned ${response.status} for $showId" }
            return null
        }
        val show = json.parseToJsonElement(response.body) as? JsonObject ?: return null
        cacheMutex.withLock { shows[cacheKey] = show }
        return show
    }

    private const val NativeKeyPrefix = "motn-key-"
    private const val NativeBaseUrl = "https://api.movieofthenight.com/v4"
    private const val RapidApiHost = "streaming-availability.p.rapidapi.com"
    private const val RapidApiBaseUrl = "https://$RapidApiHost"
    private const val MaxResponseBytes = 16 * 1024 * 1024
}

internal fun availabilityShowId(metaId: String, tmdbId: String?, isSeries: Boolean): String? {
    val imdbId = metaId
        .removePrefix("series:")
        .removePrefix("movie:")
        .substringBefore(':')
        .trim()
        .takeIf { it.startsWith("tt", ignoreCase = true) }
    return imdbId ?: tmdbId?.let { "${if (isSeries) "tv" else "movie"}/$it" }
}

internal fun matchAvailability(
    show: JsonObject,
    country: String,
    season: Int?,
    episode: Int?,
    specialTitle: String?,
    normalize: (String) -> String,
): AvailabilityMatch? {
    if (season == null) {
        return show.options(country).takeIf { it.isNotEmpty() }?.let { AvailabilityMatch(AvailabilityScope.Title, it) }
    }
    val seasons = (show["seasons"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
    if (season == 0) {
        val wanted = specialTitle?.let(normalize)?.takeIf(String::isNotBlank) ?: return null
        val special = seasons
            .flatMap { (it["episodes"] as? JsonArray).orEmpty().mapNotNull { entry -> entry as? JsonObject } }
            .firstOrNull { it.stringValue("title")?.let(normalize) == wanted }
            ?: return null
        return special.options(country).takeIf { it.isNotEmpty() }?.let { AvailabilityMatch(AvailabilityScope.Episode, it) }
    }
    val seasonObject = seasons.firstOrNull { seasonNumber(it.stringValue("title")) == season }
        ?: seasons.getOrNull(season - 1)?.takeIf { seasonNumber(it.stringValue("title")) == null }
        ?: return null
    val episodeObject = episode?.let { number ->
        (seasonObject["episodes"] as? JsonArray)?.getOrNull(number - 1) as? JsonObject
    }
    episodeObject?.options(country)?.takeIf { it.isNotEmpty() }?.let {
        return AvailabilityMatch(AvailabilityScope.Episode, it)
    }
    return seasonObject.options(country).takeIf { it.isNotEmpty() }?.let { AvailabilityMatch(AvailabilityScope.Season, it) }
}

internal fun parseAvailabilityOptions(options: JsonArray?): List<AvailabilityOption> =
    options.orEmpty().mapNotNull { entry ->
        val option = entry as? JsonObject ?: return@mapNotNull null
        val service = option["service"] as? JsonObject ?: return@mapNotNull null
        val serviceName = service.stringValue("name")?.trim()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val kind = option.stringValue("type")
            ?.let { type -> AvailabilityOptionKind.entries.firstOrNull { it.key == type } }
            ?: return@mapNotNull null
        val link = (option.stringValue("videoLink") ?: option.stringValue("link"))
            ?.takeIf(String::isNotBlank)
            ?: return@mapNotNull null
        val addon = option["addon"] as? JsonObject
        val images = (addon?.get("imageSet") ?: service["imageSet"]) as? JsonObject
        AvailabilityOption(
            serviceName = serviceName,
            addonName = addon?.stringValue("name")?.trim()?.takeIf(String::isNotBlank),
            lightLogo = images?.stringValue("lightThemeImage"),
            darkLogo = images?.stringValue("darkThemeImage"),
            kind = kind,
            link = link,
            quality = option.stringValue("quality")?.uppercase(),
            price = (option["price"] as? JsonObject)?.stringValue("formatted"),
        )
    }.sortedBy { it.kind.ordinal }

private fun JsonObject.options(country: String): List<AvailabilityOption> =
    parseAvailabilityOptions((this["streamingOptions"] as? JsonObject)?.get(country.lowercase()) as? JsonArray)

private val SeasonNumber = Regex("\\d+")

private fun seasonNumber(title: String?): Int? =
    title?.let { SeasonNumber.find(it)?.value?.toIntOrNull() }

private fun JsonObject.stringValue(name: String): String? =
    (this[name] as? JsonPrimitive)?.contentOrNull
