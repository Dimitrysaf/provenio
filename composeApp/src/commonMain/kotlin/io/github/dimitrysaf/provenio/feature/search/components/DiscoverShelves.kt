package io.github.dimitrysaf.provenio.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.designsystem.components.PosterCard
import io.github.dimitrysaf.provenio.designsystem.components.PosterShelfWidth
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview

/** How close to the end of a shelf the user gets before the next page is asked for. */
private const val PrefetchDistance = 5

/**
 * Every browsable catalog, as a shelf each, emitted into the caller's own list.
 *
 * A [LazyListScope] extension rather than a screen of its own, because Discover shares one
 * scroll with whatever sits above it — a LazyColumn inside a LazyColumn has no height to
 * measure against and throws.
 *
 * Shelves are independent: each owns its paging and one failing does not empty the screen.
 * Two addons offering a catalog with the same name produce two shelves, labelled by addon,
 * rather than being merged. Merging would mean deciding whose copy of a title wins, which
 * is the seat cascade and is not designed yet.
 */
fun LazyListScope.discoverShelves(
    shelves: List<Pair<InstalledAddon, ManifestCatalog>>,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    items(
        items = shelves,
        key = { (addon, catalog) -> "${addon.manifest.id}:${catalog.type}:${catalog.id}" },
    ) { (addon, catalog) ->
        CatalogShelf(
            addon = addon,
            catalog = catalog,
            showAddonName = shelves.count { it.second.name == catalog.name } > 1,
            onOpenDetail = onOpenDetail,
        )
    }
}

@Composable
private fun CatalogShelf(
    addon: InstalledAddon,
    catalog: ManifestCatalog,
    showAddonName: Boolean,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    var items by remember { mutableStateOf<List<MetaPreview>>(emptyList()) }
    var skip by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var exhausted by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= items.size - PrefetchDistance
        }
    }

    LaunchedEffect(addon.manifest.id, catalog.id, nearEnd) {
        if (loading || exhausted) return@LaunchedEffect
        if (items.isNotEmpty() && !nearEnd) return@LaunchedEffect

        loading = true
        val page = AddonRepository.catalogPage(addon, catalog, skip)
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

    if (items.isEmpty() && exhausted) return

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            text = if (showAddonName) {
                "${catalog.name ?: catalog.id} · ${addon.manifest.name}"
            } else {
                catalog.name ?: catalog.id
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { meta ->
                PosterCard(
                    title = meta.name ?: meta.id,
                    posterUrl = meta.poster,
                    onClick = { onOpenDetail(meta.type, meta.id) },
                    modifier = Modifier.width(PosterShelfWidth),
                )
            }
            if (loading) {
                item {
                    Box(
                        modifier = Modifier.width(PosterShelfWidth).aspectRatio(2f / 3f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
