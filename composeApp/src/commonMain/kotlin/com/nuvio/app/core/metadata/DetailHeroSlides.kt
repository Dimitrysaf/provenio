package com.nuvio.app.core.metadata

private const val MAX_HERO_TRAILERS = 4
private const val MAX_HERO_EXTRA_ARTWORK = 5

/** One page of the details hero carousel. */
sealed interface DetailHeroSlide {
    data class Artwork(val url: String) : DetailHeroSlide

    data class Trailer(val trailer: MetaTrailer) : DetailHeroSlide
}

/** The hero pages in swipe order: the main artwork always leads, then trailers, then more artwork. */
internal fun buildDetailHeroSlides(
    meta: MetaDetails,
    includeTrailers: Boolean,
): List<DetailHeroSlide> {
    val mainArtwork = (meta.background ?: meta.poster)?.trim()?.takeIf(String::isNotBlank)
    val trailers = if (includeTrailers) {
        selectHeroTrailers(meta.trailers, limit = MAX_HERO_TRAILERS)
    } else {
        emptyList()
    }
    val extraArtwork = meta.extraArtwork
        .asSequence()
        .map(String::trim)
        .filter { it.isNotBlank() && it != mainArtwork }
        .distinct()
        .take(MAX_HERO_EXTRA_ARTWORK)

    return buildList {
        mainArtwork?.let { add(DetailHeroSlide.Artwork(it)) }
        trailers.forEach { add(DetailHeroSlide.Trailer(it)) }
        extraArtwork.forEach { add(DetailHeroSlide.Artwork(it)) }
    }
}
