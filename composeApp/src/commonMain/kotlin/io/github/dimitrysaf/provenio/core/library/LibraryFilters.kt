package io.github.dimitrysaf.provenio.core.library

import io.github.dimitrysaf.provenio.core.search.releaseYears
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider

/** What the Library's filters narrow its titles to, besides the list and the order, as Search's do. */
data class LibraryFilterState(
    val type: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val hideWatched: Boolean = false,
) {
    val isActive: Boolean
        get() = type != null || genre != null || year != null || hideWatched
}

/** The type the Library's Type filter knows the title by: its tracker's category, else its own. */
internal fun LibraryItem.libraryFilterType(): String = (mediaCategory ?: type).normalizedLibraryType()

/**
 * Each section's titles the filters keep. The shelves filter by [LibraryFilterState.type] here and
 * drop the shelves left empty; the grid picks its type and its list itself, so for it
 * [forShelves] is false and every list stays, empty or not, to be chosen.
 */
internal fun List<LibrarySection>.filteredBy(
    filters: LibraryFilterState,
    forShelves: Boolean,
    currentYear: Int = currentLibraryYear(),
    isWatched: (LibraryItem) -> Boolean,
): List<LibrarySection> {
    if (!filters.isActive) return this
    val genre = filters.genre?.trim()
    val filtered = map { section ->
        section.copy(
            items = section.items.filter { item ->
                (!forShelves || filters.type == null || item.libraryFilterType() == filters.type) &&
                    (genre == null || item.genres.any { it.trim().equals(genre, ignoreCase = true) }) &&
                    (filters.year == null || item.toMetaPreview().releaseYears(currentYear)?.contains(filters.year) == true) &&
                    (!filters.hideWatched || !isWatched(item))
            },
        )
    }
    return if (forShelves) filtered.filter { it.items.isNotEmpty() } else filtered
}

/** The types, genres and years the Library's titles have, so no choice can empty the page. */
internal fun List<LibrarySection>.libraryFilterTypes(): List<String> =
    flatMap { it.items }.map { it.libraryFilterType() }.filter { it.isNotBlank() }.distinct().sorted()

internal fun List<LibrarySection>.libraryFilterGenres(): List<String> =
    flatMap { it.items }.flatMap { it.genres }.map { it.trim() }.filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }

internal fun List<LibrarySection>.libraryFilterYears(currentYear: Int = currentLibraryYear()): List<Int> =
    flatMap { it.items }.flatMap { item -> item.toMetaPreview().releaseYears(currentYear)?.toList().orEmpty() }
        .distinct().sortedDescending()

private fun currentLibraryYear(): Int = CurrentDateProvider.todayIsoDate().take(4).toIntOrNull() ?: 0
