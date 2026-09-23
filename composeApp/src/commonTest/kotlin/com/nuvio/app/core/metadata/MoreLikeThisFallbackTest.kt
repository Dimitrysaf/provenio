package com.nuvio.app.core.metadata

import com.nuvio.app.core.catalog.CatalogTarget
import com.nuvio.app.core.home.HomeCatalogSection
import com.nuvio.app.core.home.MetaPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoreLikeThisFallbackTest {

    @Test
    fun `prefers titles sharing a genre`() {
        val fallback = moreLikeThisFallback(
            meta = meta(genres = listOf("Horror")),
            sections = listOf(
                section(preview("comedy", genres = listOf("Comedy")), preview("horror", genres = listOf("horror"))),
            ),
        )

        assertEquals(listOf("horror"), fallback.map { it.id })
    }

    @Test
    fun `falls back to the lists as they are when no genre matches`() {
        val fallback = moreLikeThisFallback(
            meta = meta(genres = listOf("Western")),
            sections = listOf(section(preview("a", genres = listOf("Drama")), preview("b"))),
        )

        assertEquals(listOf("a", "b"), fallback.map { it.id })
    }

    @Test
    fun `leaves out the title itself, other types and duplicates`() {
        val fallback = moreLikeThisFallback(
            meta = meta(),
            sections = listOf(
                section(preview("tt1"), preview("show", type = "series"), preview("a")),
                section(preview("a"), preview("b")),
            ),
        )

        assertEquals(listOf("a", "b"), fallback.map { it.id })
    }

    @Test
    fun `caps the row and is empty without catalogs`() {
        val many = moreLikeThisFallback(meta(), listOf(section(*Array(30) { preview("item-$it") })))

        assertEquals(20, many.size)
        assertTrue(moreLikeThisFallback(meta(), emptyList()).isEmpty())
    }

    private fun meta(genres: List<String> = emptyList()): MetaDetails =
        MetaDetails(id = "tt1", type = "movie", name = "Test title", genres = genres)

    private fun preview(id: String, type: String = "movie", genres: List<String> = emptyList()): MetaPreview =
        MetaPreview(id = id, type = type, name = id, genres = genres)

    private fun section(vararg items: MetaPreview): HomeCatalogSection =
        HomeCatalogSection(
            key = "addon:movie:popular",
            title = "Popular",
            subtitle = "Addon",
            addonName = "Addon",
            target = CatalogTarget.Addon(
                manifestUrl = "https://example.com/manifest.json",
                contentType = "movie",
                catalogId = "popular",
                supportsPagination = false,
            ),
            items = items.toList(),
        )
}
