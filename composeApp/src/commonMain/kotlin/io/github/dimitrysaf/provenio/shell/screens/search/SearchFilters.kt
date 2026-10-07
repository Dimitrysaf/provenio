package io.github.dimitrysaf.provenio.shell.screens.search

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
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.search.DiscoverUiState
import io.github.dimitrysaf.provenio.core.search.SearchFilterState
import io.github.dimitrysaf.provenio.core.search.SearchRepository
import io.github.dimitrysaf.provenio.core.search.SearchSortOrder
import io.github.dimitrysaf.provenio.core.search.searchCatalogOptions
import io.github.dimitrysaf.provenio.core.search.searchGenreOptions
import io.github.dimitrysaf.provenio.core.search.searchTypeOptions
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * The one filter button for both Search and Discover, sitting in the search field. It narrows the
 * search results while there is a query and the Discover feed while there is none.
 */
@Composable
internal fun SearchFiltersButton(
    searching: Boolean,
    searchSections: List<HomeCatalogSection>,
    discoverState: DiscoverUiState,
    filters: SearchFilterState,
) {
    var showFilters by remember { mutableStateOf(false) }
    var openFilter by remember { mutableStateOf<SearchFilter?>(null) }

    val allTypesLabel = stringResource(Res.string.search_filter_all_types)
    val allCatalogsLabel = stringResource(Res.string.search_filter_all_catalogs)
    val allGenresLabel = stringResource(Res.string.discover_all_genres)
    val sortLabel = filters.sort.label()

    val searchTypes = remember(searchSections) { searchSections.searchTypeOptions() }
    val searchCatalogs = remember(searchSections, filters.type) { searchSections.searchCatalogOptions(filters.type) }
    val searchGenres = remember(searchSections, filters.type, filters.catalogKey) {
        searchSections.searchGenreOptions(filters.type, filters.catalogKey)
    }

    val active = if (searching) {
        filters.hasSearchFilters || filters.sort != SearchSortOrder.Default
    } else {
        filters.sort != SearchSortOrder.Default || discoverState.selectedGenre != null
    }
    val enabled = if (searching) {
        searchSections.isNotEmpty() || filters.hasSearchFilters
    } else {
        discoverState.typeOptions.isNotEmpty() || discoverState.catalogOptions.isNotEmpty()
    }

    WithTooltip(stringResource(Res.string.discover_filters)) {
        IconButton(onClick = { showFilters = true }, enabled = enabled) {
            BadgedBox(badge = { if (active) Badge() }) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(Res.string.discover_filters),
                )
            }
        }
    }

    if (showFilters) {
        val options = if (searching) {
            listOf(
                SingleChoiceOption(
                    value = SearchFilter.TYPE,
                    label = stringResource(Res.string.discover_type),
                    supportingText = filters.type?.let { it.displayTypeLabel() } ?: allTypesLabel,
                    enabled = searchTypes.isNotEmpty(),
                ),
                SingleChoiceOption(
                    value = SearchFilter.CATALOG,
                    label = stringResource(Res.string.discover_catalog),
                    supportingText = searchCatalogs.firstOrNull { it.key == filters.catalogKey }?.label ?: allCatalogsLabel,
                    enabled = searchCatalogs.isNotEmpty(),
                ),
                SingleChoiceOption(
                    value = SearchFilter.GENRE,
                    label = stringResource(Res.string.discover_genre),
                    supportingText = filters.genre ?: allGenresLabel,
                    enabled = searchGenres.isNotEmpty() || filters.genre != null,
                ),
                SingleChoiceOption(
                    value = SearchFilter.SORT,
                    label = stringResource(Res.string.search_sort),
                    supportingText = sortLabel,
                ),
            )
        } else {
            val selectedCatalog = discoverState.selectedCatalog
            listOf(
                SingleChoiceOption(
                    value = SearchFilter.TYPE,
                    label = stringResource(Res.string.discover_type),
                    supportingText = discoverState.selectedType?.let { it.displayTypeLabel() }
                        ?: stringResource(Res.string.discover_type),
                    enabled = discoverState.typeOptions.isNotEmpty(),
                ),
                SingleChoiceOption(
                    value = SearchFilter.CATALOG,
                    label = stringResource(Res.string.discover_catalog),
                    supportingText = selectedCatalog?.catalogName ?: stringResource(Res.string.discover_catalog),
                    enabled = discoverState.catalogOptions.isNotEmpty(),
                ),
                SingleChoiceOption(
                    value = SearchFilter.GENRE,
                    label = stringResource(Res.string.discover_genre),
                    supportingText = discoverState.selectedGenre ?: allGenresLabel,
                    enabled = discoverState.genreOptions.isNotEmpty() || selectedCatalog?.genreRequired == true,
                ),
                SingleChoiceOption(
                    value = SearchFilter.SORT,
                    label = stringResource(Res.string.search_sort),
                    supportingText = sortLabel,
                ),
            )
        }
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_filters),
            options = options,
            isSelected = { false },
            onSelected = { filter -> openFilter = filter },
            onDismiss = { showFilters = false },
        )
    }

    when (openFilter) {
        SearchFilter.TYPE -> if (searching) {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.discover_select_type),
                options = buildList {
                    add(SingleChoiceOption(value = "", label = allTypesLabel))
                    searchTypes.forEach { type -> add(SingleChoiceOption(value = type, label = type.displayTypeLabel())) }
                },
                isSelected = { it == (filters.type ?: "") },
                onSelected = { type -> SearchRepository.setSearchType(type.ifBlank { null }) },
                onDismiss = { openFilter = null },
            )
        } else {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.discover_select_type),
                options = discoverState.typeOptions.map { type ->
                    SingleChoiceOption(value = type, label = type.displayTypeLabel())
                },
                isSelected = { it == discoverState.selectedType },
                onSelected = SearchRepository::selectDiscoverType,
                onDismiss = { openFilter = null },
            )
        }

        SearchFilter.CATALOG -> if (searching) {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.discover_select_catalog),
                options = buildList {
                    add(SingleChoiceOption(value = "", label = allCatalogsLabel))
                    searchCatalogs.forEach { option -> add(SingleChoiceOption(value = option.key, label = option.label)) }
                },
                isSelected = { it == (filters.catalogKey ?: "") },
                onSelected = { key -> SearchRepository.setSearchCatalog(key.ifBlank { null }) },
                onDismiss = { openFilter = null },
            )
        } else {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.discover_select_catalog),
                options = discoverState.catalogOptions.map { option ->
                    SingleChoiceOption(value = option.key, label = option.catalogName)
                },
                isSelected = { it == discoverState.selectedCatalogKey },
                onSelected = SearchRepository::selectDiscoverCatalog,
                onDismiss = { openFilter = null },
            )
        }

        SearchFilter.GENRE -> if (searching) {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.discover_select_genre),
                options = buildList {
                    add(SingleChoiceOption(value = "", label = allGenresLabel))
                    searchGenres.forEach { genre -> add(SingleChoiceOption(value = genre, label = genre)) }
                },
                isSelected = { it == (filters.genre ?: "") },
                onSelected = { genre -> SearchRepository.setSearchGenre(genre.ifBlank { null }) },
                onDismiss = { openFilter = null },
            )
        } else {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.discover_select_genre),
                options = buildList {
                    if (discoverState.selectedCatalog?.genreRequired != true) {
                        add(SingleChoiceOption(value = "", label = allGenresLabel))
                    }
                    discoverState.genreOptions.forEach { genre -> add(SingleChoiceOption(value = genre, label = genre)) }
                },
                isSelected = { it == (discoverState.selectedGenre ?: "") },
                onSelected = { genre -> SearchRepository.selectDiscoverGenre(genre.ifBlank { null }) },
                onDismiss = { openFilter = null },
            )
        }

        SearchFilter.SORT -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.search_sort),
            options = SearchSortOrder.entries.map { order -> SingleChoiceOption(value = order, label = order.label()) },
            isSelected = { it == filters.sort },
            onSelected = SearchRepository::setSortOrder,
            onDismiss = { openFilter = null },
        )

        null -> Unit
    }
}

/** Which filter's choices are open. */
private enum class SearchFilter {
    TYPE,
    CATALOG,
    GENRE,
    SORT,
}

@Composable
private fun SearchSortOrder.label(): String =
    stringResource(
        when (this) {
            SearchSortOrder.Default -> Res.string.search_sort_default
            SearchSortOrder.NameAscending -> Res.string.search_sort_name_ascending
            SearchSortOrder.NameDescending -> Res.string.search_sort_name_descending
            SearchSortOrder.Newest -> Res.string.search_sort_newest
            SearchSortOrder.Oldest -> Res.string.search_sort_oldest
            SearchSortOrder.Rating -> Res.string.search_sort_rating
        },
    )
