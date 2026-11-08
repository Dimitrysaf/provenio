package io.github.dimitrysaf.provenio.core.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetailHeroSlidesTest {

    @Test
    fun `main artwork leads, extra artwork follows, and trailers stay out`() {
        val slides = buildDetailHeroSlides(
            meta = meta(
                background = "main.jpg",
                extraArtwork = listOf("extra-one.jpg", "extra-two.jpg"),
                trailers = listOf(trailer("teaser", type = "Teaser"), trailer("official", official = true)),
            ),
        )

        assertEquals(
            listOf(
                DetailHeroSlide.Artwork("main.jpg"),
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
        )

        assertEquals(
            listOf(DetailHeroSlide.Artwork("main.jpg"), DetailHeroSlide.Artwork("extra.jpg")),
            slides,
        )
    }

    @Test
    fun `falls back to the poster when there is no backdrop`() {
        val slides = buildDetailHeroSlides(meta = meta(poster = "poster.jpg"))

        assertEquals(listOf(DetailHeroSlide.Artwork("poster.jpg")), slides)
    }

    @Test
    fun `uses a backdrop rather than the poster when there is no background`() {
        val slides = buildDetailHeroSlides(
            meta = meta(poster = "poster.jpg", extraArtwork = listOf("backdrop.jpg", "other.jpg")),
        )

        assertEquals(
            listOf(DetailHeroSlide.Artwork("backdrop.jpg"), DetailHeroSlide.Artwork("other.jpg")),
            slides,
        )
    }

    @Test
    fun `caps the pages a title can add`() {
        val slides = buildDetailHeroSlides(
            meta = meta(
                background = "main.jpg",
                extraArtwork = List(12) { "extra-$it.jpg" },
            ),
        )

        assertEquals(6, slides.size)
        assertTrue(slides.first() is DetailHeroSlide.Artwork)
    }

    @Test
    fun `has no pages without artwork`() {
        assertTrue(buildDetailHeroSlides(meta = meta()).isEmpty())
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
