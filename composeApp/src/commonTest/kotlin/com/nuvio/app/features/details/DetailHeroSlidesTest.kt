package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetailHeroSlidesTest {

    @Test
    fun `main artwork leads, trailers follow, extra artwork closes`() {
        val slides = buildDetailHeroSlides(
            meta = meta(
                background = "main.jpg",
                extraArtwork = listOf("extra-one.jpg", "extra-two.jpg"),
                trailers = listOf(trailer("teaser", type = "Teaser"), trailer("official", official = true)),
            ),
            includeTrailers = true,
        )

        assertEquals(
            listOf(
                DetailHeroSlide.Artwork("main.jpg"),
                DetailHeroSlide.Trailer(trailer("official", official = true)),
                DetailHeroSlide.Trailer(trailer("teaser", type = "Teaser")),
                DetailHeroSlide.Artwork("extra-one.jpg"),
                DetailHeroSlide.Artwork("extra-two.jpg"),
            ),
            slides,
        )
    }

    @Test
    fun `extra artwork never repeats the main artwork`() {
        val slides = buildDetailHeroSlides(
            meta = meta(
                background = "main.jpg",
                extraArtwork = listOf("main.jpg", "extra.jpg", "extra.jpg"),
            ),
            includeTrailers = true,
        )

        assertEquals(
            listOf(DetailHeroSlide.Artwork("main.jpg"), DetailHeroSlide.Artwork("extra.jpg")),
            slides,
        )
    }

    @Test
    fun `falls back to the poster when there is no backdrop`() {
        val slides = buildDetailHeroSlides(meta = meta(poster = "poster.jpg"), includeTrailers = true)

        assertEquals(listOf(DetailHeroSlide.Artwork("poster.jpg")), slides)
    }

    @Test
    fun `leaves trailers out when hero trailer playback is off`() {
        val slides = buildDetailHeroSlides(
            meta = meta(background = "main.jpg", trailers = listOf(trailer("official"))),
            includeTrailers = false,
        )

        assertEquals(listOf(DetailHeroSlide.Artwork("main.jpg")), slides)
    }

    @Test
    fun `caps the pages a title can add`() {
        val slides = buildDetailHeroSlides(
            meta = meta(
                background = "main.jpg",
                extraArtwork = List(12) { "extra-$it.jpg" },
                trailers = List(9) { trailer("trailer-$it") },
            ),
            includeTrailers = true,
        )

        assertEquals(4, slides.count { it is DetailHeroSlide.Trailer })
        assertEquals(6, slides.count { it is DetailHeroSlide.Artwork })
        assertTrue(slides.first() is DetailHeroSlide.Artwork)
    }

    @Test
    fun `has no pages without artwork or trailers`() {
        assertTrue(buildDetailHeroSlides(meta = meta(), includeTrailers = true).isEmpty())
    }

    private fun meta(
        background: String? = null,
        poster: String? = null,
        extraArtwork: List<String> = emptyList(),
        trailers: List<MetaTrailer> = emptyList(),
    ): MetaDetails =
        MetaDetails(
            id = "tt1",
            type = "movie",
            name = "Test title",
            poster = poster,
            background = background,
            extraArtwork = extraArtwork,
            trailers = trailers,
        )

    private fun trailer(
        id: String,
        type: String = "Trailer",
        official: Boolean = false,
    ): MetaTrailer =
        MetaTrailer(
            id = id,
            key = id,
            name = id,
            site = "YouTube",
            type = type,
            official = official,
        )
}
