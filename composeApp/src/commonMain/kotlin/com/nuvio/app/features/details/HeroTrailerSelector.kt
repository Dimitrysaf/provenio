package com.nuvio.app.features.details

internal fun selectHeroTrailer(trailers: List<MetaTrailer>): MetaTrailer? =
    selectHeroTrailers(trailers, limit = 1).firstOrNull()

/** The hero carousel's trailers, best first. */
internal fun selectHeroTrailers(trailers: List<MetaTrailer>, limit: Int): List<MetaTrailer> =
    trailers
        .asSequence()
        .filter { it.isPlayableYouTubeTrailerCandidate() }
        .distinctBy { it.key }
        .sortedWith(heroTrailerOrder.reversed())
        .take(limit)
        .toList()

internal fun MetaTrailer.youtubePlaybackUrl(): String =
    key.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        ?: "https://www.youtube.com/watch?v=$key"

private val heroTrailerOrder = compareBy<MetaTrailer>(
    { it.heroTrailerPriority() },
    { it.publishedAt.orEmpty() },
    { it.size ?: 0 },
    { it.name },
)

private fun MetaTrailer.isPlayableYouTubeTrailerCandidate(): Boolean =
    key.isNotBlank() && site.equals("YouTube", ignoreCase = true)

private fun MetaTrailer.heroTrailerPriority(): Int {
    val isSeriesTrailer = seasonNumber != null
    val isTrailerType = type.equals("Trailer", ignoreCase = true)
    return when {
        !isSeriesTrailer && isTrailerType && official -> 70
        !isSeriesTrailer && isTrailerType -> 60
        !isSeriesTrailer && official -> 50
        !isSeriesTrailer -> 40
        isTrailerType && official -> 30
        isTrailerType -> 20
        official -> 10
        else -> 0
    }
}

/** The trailer's YouTube still, for keys that are plain video ids rather than full URLs. */
internal fun MetaTrailer.youtubeThumbnailUrl(): String? =
    key.takeIf { it.isNotBlank() && !it.startsWith("http") }
        ?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
