package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.navigation.SearchFilter
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.SearchRepository
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview
import kotlinx.coroutines.delay

/** Wait after the last keystroke before asking every addon. */
private const val SearchDebounceMillis = 350L

/**
 * Search and Discover, in that order down one page.
 *
 * A top-level destination, so the field never collapses and there is no back arrow to
 * draw. With the field empty the page is somewhere to browse: recent searches, then every
 * browsable catalog as a shelf. A query replaces all of it with results — someone who has
 * typed is looking for one title, not for something to scroll past.
 *
 * The filter chips narrow both halves, which is what the old TV and Movies tabs did before
 * Discover absorbed them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
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
                searched && searchable.isEmpty() -> NoSearchableAddons(onAddAddons)
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
 * What the page is when nothing has been typed: recent searches over Discover, sharing one
 * scroll so browsing runs on past the end of the history rather than stopping at it.
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
            item(key = "discover-empty") { NoCatalogs(onAddAddons = onAddAddons) }
        } else {
            discoverShelves(shelves = shelves, onOpenDetail = onOpenDetail)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    )
}

/** Shown while the field is empty, the way a search screen normally fills that space. */
private fun LazyListScope.recentSearches(
    history: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    item(key = "history-header") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recent searches",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
    items(history, key = { "history:$it" }) { term ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPick(term) }
                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Text(text = term, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onRemove(term) }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove \"$term\" from history",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResultGrid(
    results: List<MetaPreview>,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(results, key = { it.id }) { meta ->
            ResultCard(meta) { onOpenDetail(meta.type, meta.id) }
        }
    }
}

@Composable
private fun ResultCard(meta: MetaPreview, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (meta.poster != null) {
                AsyncImage(
                    model = meta.poster,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = meta.name ?: meta.id,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (meta.releaseInfo != null) {
            Text(
                text = meta.releaseInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CentredNote(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoSearchableAddons(onAddAddons: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Extension,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Nothing to search",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Add an add-on with a searchable catalog to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Button(onClick = onAddAddons) { Text("Add an add-on") }
    }
}
