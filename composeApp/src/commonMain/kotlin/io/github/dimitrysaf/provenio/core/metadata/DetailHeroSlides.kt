package io.github.dimitrysaf.provenio.core.metadata

private const val MAX_HERO_EXTRA_ARTWORK = 5

/** One page of the details hero carousel. */
sealed interface DetailHeroSlide {
    data class Artwork(val url: String) : DetailHeroSlide
}

/** The hero pages in swipe order: the main artwork always leads, then more artwork. Trailers have their own section. */
internal fun buildDetailHeroSlides(meta: MetaDetails): List<DetailHeroSlide> {
    val mainArtwork = (sequenceOf(meta.background) + meta.extraArtwork.asSequence() + sequenceOf(meta.poster))
        .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
        .firstOrNull()
    val extraArtwork = meta.extraArtwork
        .asSequence()
        .map(String::trim)
        .filter { it.isNotBlank() && it != mainArtwork }
        .distinct()
        .take(MAX_HERO_EXTRA_ARTWORK)

    return buildList {
        mainArtwork?.let { add(DetailHeroSlide.Artwork(it)) }
        extraArtwork.forEach { add(DetailHeroSlide.Artwork(it)) }
    }
}
