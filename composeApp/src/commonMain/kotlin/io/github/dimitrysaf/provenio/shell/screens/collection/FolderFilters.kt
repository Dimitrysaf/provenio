package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.search.SearchSortOrder
import io.github.dimitrysaf.provenio.core.search.releaseYears
import io.github.dimitrysaf.provenio.core.search.releasedIn
import io.github.dimitrysaf.provenio.core.search.sortedFor
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import io.github.dimitrysaf.provenio.shell.screens.search.displayTypeLabel
import io.github.dimitrysaf.provenio.shell.screens.search.label
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** What a folder's search narrows its titles to: the Search page's own filters, kept for this page. */
internal data class FolderFilterState(
    val query: String = "",
    val type: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val sort: SearchSortOrder = SearchSortOrder.Default,
) {
    val hasFilters: Boolean
        get() = type != null || genre != null || year != null || sort != SearchSortOrder.Default
}

/** The titles the search and filters keep, in the order the sort asks for. */
internal fun List<MetaPreview>.filteredBy(filters: FolderFilterState): List<MetaPreview> {
    val query = filters.query.trim()
    if (query.isEmpty() && !filters.hasFilters) return this
    return filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
        .filter { filters.type == null || it.type == filters.type }
        .filter { item -> filters.genre == null || item.genres.any { it.trim().equals(filters.genre, ignoreCase = true) } }
        .releasedIn(filters.year)
        .sortedFor(filters.sort)
}

private enum class FolderFilter { TYPE, GENRE, YEAR, SORT }

/** The filter button in the folder's search field, as Search's: type, genre, year and sort. */
@Composable
internal fun FolderFiltersButton(
    items: List<MetaPreview>,
    filters: FolderFilterState,
    onChange: (FolderFilterState) -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    var openFilter by remember { mutableStateOf<FolderFilter?>(null) }
    val allTypesLabel = stringResource(Res.string.search_filter_all_types)
    val allGenresLabel = stringResource(Res.string.discover_all_genres)
    val anyYearLabel = stringResource(Res.string.search_filter_any_year)

    // The choices come from the titles themselves, so none of them can empty the page.
    val types = remember(items) { items.map { it.type }.distinct() }
    val genres = remember(items) {
        items.flatMap { it.genres }.map { it.trim() }.filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
    }
    val years = remember(items) {
        val currentYear = CurrentDateProvider.todayIsoDate().take(4).toIntOrNull() ?: 0
        items.flatMap { item -> item.releaseYears(currentYear)?.toList().orEmpty() }.distinct().sortedDescending()
    }

    WithTooltip(stringResource(Res.string.discover_filters)) {
        IconButton(onClick = { showFilters = true }, enabled = items.isNotEmpty() || filters.hasFilters) {
            BadgedBox(badge = { if (filters.hasFilters) Badge() }) {
                Icon(imageVector = Icons.Rounded.Tune, contentDescription = stringResource(Res.string.discover_filters))
            }
        }
    }

    if (showFilters) {
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_filters),
            options = listOf(
                SingleChoiceOption(
                    value = FolderFilter.TYPE,
                    label = stringResource(Res.string.discover_type),
                    supportingText = filters.type?.displayTypeLabel() ?: allTypesLabel,
                    enabled = types.size > 1 || filters.type != null,
                ),
                SingleChoiceOption(
                    value = FolderFilter.GENRE,
                    label = stringResource(Res.string.discover_genre),
                    supportingText = filters.genre ?: allGenresLabel,
                    enabled = genres.isNotEmpty() || filters.genre != null,
                ),
                SingleChoiceOption(
                    value = FolderFilter.YEAR,
                    label = stringResource(Res.string.search_filter_year),
                    supportingText = filters.year?.toString() ?: anyYearLabel,
                    enabled = years.isNotEmpty() || filters.year != null,
                ),
                SingleChoiceOption(
                    value = FolderFilter.SORT,
                    label = stringResource(Res.string.search_sort),
                    supportingText = filters.sort.label(),
                ),
            ),
            isSelected = { false },
            onSelected = { openFilter = it },
            onDismiss = { showFilters = false },
        )
    }

    when (openFilter) {
        FolderFilter.TYPE -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_select_type),
            options = buildList {
                add(SingleChoiceOption(value = "", label = allTypesLabel))
                types.forEach { add(SingleChoiceOption(value = it, label = it.displayTypeLabel())) }
            },
            isSelected = { it == (filters.type ?: "") },
            onSelected = { onChange(filters.copy(type = it.ifBlank { null })) },
            onDismiss = { openFilter = null },
        )

        FolderFilter.GENRE -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_select_genre),
            options = buildList {
                add(SingleChoiceOption(value = "", label = allGenresLabel))
                genres.forEach { add(SingleChoiceOption(value = it, label = it)) }
            },
            isSelected = { it == (filters.genre ?: "") },
            onSelected = { onChange(filters.copy(genre = it.ifBlank { null })) },
            onDismiss = { openFilter = null },
        )

        FolderFilter.YEAR -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.search_filter_year),
            options = buildList<SingleChoiceOption<Int?>> {
                add(SingleChoiceOption(value = null, label = anyYearLabel))
                years.forEach { add(SingleChoiceOption(value = it, label = it.toString())) }
            },
            isSelected = { it == filters.year },
            onSelected = { onChange(filters.copy(year = it)) },
            onDismiss = { openFilter = null },
        )

        FolderFilter.SORT -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.search_sort),
            options = SearchSortOrder.entries.map { SingleChoiceOption(value = it, label = it.label()) },
            isSelected = { it == filters.sort },
            onSelected = { onChange(filters.copy(sort = it)) },
            onDismiss = { openFilter = null },
        )

        null -> Unit
    }
}
