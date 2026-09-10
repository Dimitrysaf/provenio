package io.github.dimitrysaf.provenio.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.designsystem.components.EmptyState
import io.github.dimitrysaf.provenio.designsystem.components.PosterCard
import io.github.dimitrysaf.provenio.feature.search.components.CentredNote
import io.github.dimitrysaf.provenio.feature.search.components.DiscoverControls
import io.github.dimitrysaf.provenio.feature.search.components.ResultGrid
import io.github.dimitrysaf.provenio.feature.search.components.SearchFilterSheet
import io.github.dimitrysaf.provenio.feature.search.components.displayName
import io.github.dimitrysaf.provenio.feature.search.components.recentSearches
import io.github.dimitrysaf.provenio.navigation.SearchFilter
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.SearchRepository
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview
import kotlinx.coroutines.delay

/** Wait after the last keystroke before asking every addon. */
private const val SearchDebounceMillis = 350L

/** How close to the end of the grid the user gets before the next page is asked for. */
private const val PrefetchDistance = 8

/** A release year is four digits or it is not a year. */
private const val YearLength = 4

/** The page margin every row on this screen lines up against. */
private val PageMargin = 16.dp

/**
 * Search above, Discover below.
 *
 * A top-level destination, so the field never collapses and there is no back arrow to
 * draw. With the field empty this is somewhere to browse: recent searches, then one
 * add-on catalog at a time, chosen by type, catalog and genre. A query replaces all of it
 * with results — someone who has typed is looking for one title, not for something to
 * scroll past.
 *
 * The field runs edge to edge and is closed off by a rule, so the chrome reads as one
 * band rather than a floating box. What narrows a query lives behind the filter button
 * inside it; what drives Discover lives with Discover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onAddAddons: () -> Unit,
    onOpenDetail: (type: String, id: String) -> Unit,
    initialFilter: SearchFilter = SearchFilter.All,
) {
    val collection by AddonRepository.collection.collectAsState()
    val allSearchable = collection.searchableCatalogs()

    var filter by rememberSaveable { mutableStateOf(initialFilter.name) }
    val activeFilter = SearchFilter.entries.firstOrNull { it.name == filter } ?: SearchFilter.All
    var year by rememberSaveable { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    val filtersActive = activeFilter != SearchFilter.All || year.length == YearLength

    val searchable = allSearchable.filter { (_, catalog) ->
        activeFilter.type == null || catalog.type == activeFilter.type
    }

    // Seeded from the repository so returning from a title restores what was found.
    var query by remember { mutableStateOf(SearchRepository.lastQuery) }
    var results by remember { mutableStateOf(SearchRepository.lastResults) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(SearchRepository.lastResults.isNotEmpty()) }
    val history by SearchRepository.history.collectAsState()

    // Applied here rather than sent with the request: the protocol's `search` extra carries
    // the query and nothing else, so no addon can be asked for one year in particular.
    val shown = remember(results, year) {
        if (year.length != YearLength) results
        else results.filter { it.releaseInfo?.startsWith(year) == true }
    }

    LaunchedEffect(query, activeFilter, allSearchable.size) {
        if (query.isBlank()) {
            results = emptyList()
            searched = false
            SearchRepository.remember("", activeFilter, emptyList())
            return@LaunchedEffect
        }
        // Nothing to redo when this is the search we already have.
        if (query == SearchRepository.lastQuery &&
            activeFilter == SearchRepository.lastFilter &&
            results.isNotEmpty()
        ) {
            searched = true
            return@LaunchedEffect
        }
        // Debounced so typing does not fire a request per character at every addon.
        delay(SearchDebounceMillis)
        searching = true
        val found = AddonRepository.search(query.trim(), activeFilter.type)
        results = found
        searching = false
        searched = true
        SearchRepository.remember(query, activeFilter, found)
        if (found.isNotEmpty()) SearchRepository.record(query)
    }

    if (showFilters) {
        SearchFilterSheet(
            selectedType = activeFilter,
            onSelectType = { filter = it.name },
            year = year,
            onYearChange = { year = it },
            onDismiss = { showFilters = false },
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(vertical = 8.dp),
        ) {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                // Always expanded. This screen is the expanded state.
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search movies and shows") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    Row {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                imageVector = Icons.Filled.FilterList,
                                contentDescription = "Filters",
                                // Tinted while something is set, so an active filter is
                                // visible without opening the sheet to check.
                                tint = if (filtersActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                },
            )
        }

        HorizontalDivider()

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                searching -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                shown.isNotEmpty() -> ResultGrid(shown, onOpenDetail)
                searched && searchable.isEmpty() -> EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    icon = Icons.Outlined.Extension,
                    title = "Nothing to search",
                    description = "Add an add-on with a searchable catalog to get started.",
                    actionLabel = "Add an add-on",
                    onAction = onAddAddons,
                )
                searched -> CentredNote("No results for \"$query\"")
                else -> Discover(
                    catalogs = collection.discoverCatalogs(),
                    history = history,
                    onPick = { query = it },
                    onRemove = { SearchRepository.remove(it) },
                    onClear = { SearchRepository.clear() },
                    onOpenDetail = onOpenDetail,
                    onAddAddons = onAddAddons,
                )
            }
        }
    }
}

/**
 * One catalog at a time, as a grid, with the history above it in the same scroll.
 *
 * The grid owns the page margin for everything in it, so the controls and the history
 * rows draw no side padding of their own.
 *
 * Selection is held by name rather than by object so it survives the collection being
 * reloaded: an addon refresh produces new [ManifestCatalog] instances for the same
 * catalogs, and holding the old ones would silently reset the picker.
 */
@Composable
private fun Discover(
    catalogs: List<Pair<InstalledAddon, ManifestCatalog>>,
    history: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onOpenDetail: (type: String, id: String) -> Unit,
    onAddAddons: () -> Unit,
) {
    val types = remember(catalogs) { catalogs.map { it.second.type }.distinct() }

    var typeChoice by rememberSaveable { mutableStateOf<String?>(null) }
    val type = typeChoice?.takeIf { it in types } ?: types.firstOrNull()
    val ofType = remember(catalogs, type) { catalogs.filter { it.second.type == type } }

    var catalogChoice by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = ofType.firstOrNull { keyOf(it) == catalogChoice } ?: ofType.firstOrNull()

    val genres = remember(selected) {
        selected?.second?.normalizedExtra()
            ?.firstOrNull { it.name == "genre" }
            ?.options
            .orEmpty()
    }
    var genre by rememberSaveable(selected?.let(::keyOf)) { mutableStateOf<String?>(null) }

    var items by remember { mutableStateOf<List<MetaPreview>>(emptyList()) }
    var skip by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var exhausted by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= items.size - PrefetchDistance
        }
    }

    // A different catalog or genre is a different list, not more of this one.
    LaunchedEffect(selected?.let(::keyOf), genre) {
        items = emptyList()
        skip = 0
        exhausted = false
    }

    LaunchedEffect(selected?.let(::keyOf), genre, nearEnd) {
        val target = selected ?: return@LaunchedEffect
        if (loading || exhausted) return@LaunchedEffect
        if (items.isNotEmpty() && !nearEnd) return@LaunchedEffect

        loading = true
        val page = AddonRepository.catalogPage(target.first, target.second, skip, genre)
        loading = false

        if (page.isNullOrEmpty()) {
            exhausted = true
            return@LaunchedEffect
        }
        // Some addons answer a skip past the end by repeating the last page, so a page
        // that adds nothing new is treated as the end rather than looping forever.
        val merged = (items + page).distinctBy { it.id }
        if (merged.size == items.size) {
            exhausted = true
            return@LaunchedEffect
        }
        items = merged
        skip = merged.size
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = PageMargin,
            end = PageMargin,
            top = PageMargin,
            bottom = 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (history.isNotEmpty()) {
            recentSearches(
                history = history,
                onPick = onPick,
                onRemove = onRemove,
                onClear = onClear,
            )
        }

        item(key = "discover-controls", span = { GridItemSpan(maxLineSpan) }) {
            DiscoverControls(
                types = types,
                selectedType = type,
                onSelectType = {
                    typeChoice = it
                    catalogChoice = null
                },
                catalogs = ofType,
                selected = selected,
                onSelectCatalog = { catalogChoice = keyOf(it) },
                genres = genres,
                selectedGenre = genre,
                onSelectGenre = { genre = it },
            )
        }

        if (selected == null) {
            item(key = "discover-empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = Icons.Outlined.Extension,
                    title = "Nothing to browse",
                    description = "Add an add-on with a catalog to get started.",
                    actionLabel = "Add an add-on",
                    onAction = onAddAddons,
                )
            }
            return@LazyVerticalGrid
        }

        items(items, key = { it.id }) { meta ->
            PosterCard(
                title = meta.name ?: meta.id,
                posterUrl = meta.poster,
                onClick = { onOpenDetail(meta.type, meta.id) },
                subtitle = meta.releaseInfo,
            )
        }

        if (loading) {
            item(key = "discover-loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (items.isEmpty() && exhausted) {
            item(key = "discover-none", span = { GridItemSpan(maxLineSpan) }) {
                // Not CentredNote: that fills its height, which a grid item does not have.
                Text(
                    text = "${selected.second.displayName()} returned nothing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Stable across addon refreshes, unlike the catalog objects themselves. */
private fun keyOf(entry: Pair<InstalledAddon, ManifestCatalog>): String =
    "${entry.first.manifest.id}:${entry.second.type}:${entry.second.id}"
