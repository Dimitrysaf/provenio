package io.github.dimitrysaf.provenio.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.designsystem.components.EmptyState
import io.github.dimitrysaf.provenio.feature.search.components.CentredNote
import io.github.dimitrysaf.provenio.feature.search.components.ResultGrid
import io.github.dimitrysaf.provenio.feature.search.components.SectionHeader
import io.github.dimitrysaf.provenio.feature.search.components.discoverShelves
import io.github.dimitrysaf.provenio.feature.search.components.recentSearches
import io.github.dimitrysaf.provenio.navigation.SearchFilter
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.SearchRepository
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog
import kotlinx.coroutines.delay

/** Wait after the last keystroke before asking every addon. */
private const val SearchDebounceMillis = 350L

/**
 * Search and Discover, in that order down one screen.
 *
 * A top-level destination, so the field never collapses and there is no back arrow to
 * draw. With the field empty the screen is somewhere to browse: recent searches, then every
 * browsable catalog as a shelf. A query replaces all of it with results — someone who has
 * typed is looking for one title, not for something to scroll past.
 *
 * The filter chips narrow both halves, which is what the old TV and Movies tabs did before
 * Discover absorbed them.
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
    val searchable = allSearchable.filter { (_, catalog) ->
        activeFilter.type == null || catalog.type == activeFilter.type
    }
    val shelves = collection.browsableCatalogs(activeFilter.type)

    // Seeded from the repository so returning from a title restores what was found.
    var query by remember { mutableStateOf(SearchRepository.lastQuery) }
    var results by remember { mutableStateOf(SearchRepository.lastResults) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(SearchRepository.lastResults.isNotEmpty()) }
    val history by SearchRepository.history.collectAsState()

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

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchFilter.entries.forEach { option ->
                FilterChip(
                    selected = option == activeFilter,
                    onClick = { filter = option.name },
                    label = { Text(option.label) },
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                searching -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                results.isNotEmpty() -> ResultGrid(results, onOpenDetail)
                searched && searchable.isEmpty() -> EmptyState(
                    modifier = Modifier.align(Alignment.Center),
                    icon = Icons.Outlined.Extension,
                    title = "Nothing to search",
                    description = "Add an add-on with a searchable catalog to get started.",
                    actionLabel = "Add an add-on",
                    onAction = onAddAddons,
                )
                searched -> CentredNote("No results for \"$query\"")
                else -> Browse(
                    history = history,
                    shelves = shelves,
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
 * What the screen is when nothing has been typed: recent searches over Discover, sharing
 * one scroll so browsing runs on past the end of the history rather than stopping at it.
 */
@Composable
private fun Browse(
    history: List<String>,
    shelves: List<Pair<InstalledAddon, ManifestCatalog>>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onOpenDetail: (type: String, id: String) -> Unit,
    onAddAddons: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (history.isNotEmpty()) {
            recentSearches(
                history = history,
                onPick = onPick,
                onRemove = onRemove,
                onClear = onClear,
            )
        }

        item(key = "discover-header") { SectionHeader("Discover") }

        if (shelves.isEmpty()) {
            item(key = "discover-empty") {
                EmptyState(
                    icon = Icons.Outlined.Extension,
                    title = "Nothing to browse",
                    description = "Add an add-on with a catalog to get started.",
                    actionLabel = "Add an add-on",
                    onAction = onAddAddons,
                )
            }
        } else {
            discoverShelves(shelves = shelves, onOpenDetail = onOpenDetail)
        }
    }
}
