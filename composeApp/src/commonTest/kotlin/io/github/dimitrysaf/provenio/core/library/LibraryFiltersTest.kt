package io.github.dimitrysaf.provenio.core.library

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryFiltersTest {
    private val heat = item("tt1", "movie", "Heat", "1995", listOf("Crime"), rating = "8.3")
    private val ozark = item("tt2", "series", "Ozark", "2017-2022", listOf("Crime", "Drama"), rating = "8.5")
    private val up = item("tt3", "movie", "Up", "2009", listOf("Animation"), rating = null)
    private val sections = listOf(
        LibrarySection(type = "watchlist", displayTitle = "Watchlist", items = listOf(heat, ozark)),
        LibrarySection(type = "favorites", displayTitle = "Favorites", items = listOf(up)),
    )

    @Test
    fun `shelves keep what the filters match and drop the shelves left empty`() {
        val crime = sections.filteredBy(LibraryFilterState(genre = "crime"), forShelves = true, currentYear = 2026) { false }
        assertEquals(listOf(listOf("Heat", "Ozark")), crime.map { section -> section.items.map { it.name } })

        val series = sections.filteredBy(LibraryFilterState(type = "series"), forShelves = true, currentYear = 2026) { false }
        assertEquals(listOf("Ozark"), series.flatMap { it.items }.map { it.name })
    }

    @Test
    fun `a series matches every year it ran and watched titles can be hidden`() {
        val ran2020 = sections.filteredBy(LibraryFilterState(year = 2020), forShelves = true, currentYear = 2026) { false }
        assertEquals(listOf("Ozark"), ran2020.flatMap { it.items }.map { it.name })

        val unwatched = sections.filteredBy(LibraryFilterState(hideWatched = true), forShelves = true, currentYear = 2026) {
            it.id == "tt1"
        }
        assertEquals(listOf("Ozark", "Up"), unwatched.flatMap { it.items }.map { it.name })
    }

    @Test
    fun `the grid keeps every list so each can still be chosen`() {
        val grid = sections.filteredBy(LibraryFilterState(genre = "Animation"), forShelves = false, currentYear = 2026) { false }
        assertEquals(listOf(0, 1), grid.map { it.items.size })
    }

    @Test
    fun `newest, oldest and rating order as Search does`() {
        val items = listOf(heat, ozark, up)
        assertEquals(listOf("Ozark", "Up", "Heat"), sortLibraryItems(items, LibrarySortOption.RELEASE_DESC, LibrarySourceMode.LOCAL).map { it.name })
        assertEquals(listOf("Heat", "Up", "Ozark"), sortLibraryItems(items, LibrarySortOption.RELEASE_ASC, LibrarySourceMode.LOCAL).map { it.name })
        assertEquals(listOf("Ozark", "Heat", "Up"), sortLibraryItems(items, LibrarySortOption.RATING, LibrarySourceMode.LOCAL).map { it.name })
    }

    private fun item(id: String, type: String, name: String, release: String, genres: List<String>, rating: String?) =
        LibraryItem(
            id = id,
            type = type,
            name = name,
            releaseInfo = release,
            genres = genres,
            imdbRating = rating,
            savedAtEpochMs = 0L,
        )
}
