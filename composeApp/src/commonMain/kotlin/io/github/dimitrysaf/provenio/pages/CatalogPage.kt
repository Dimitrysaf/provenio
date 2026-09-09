package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview

/** How close to the end of a shelf the user gets before the next page is asked for. */
private const val PrefetchDistance = 5

/**
 * Every browsable catalog of one type, as a shelf each.
 *
 * Shelves are independent: each owns its paging and one failing does not empty the page.
 * Two addons offering a catalog with the same name produce two shelves, labelled by addon,
 * rather than being merged. Merging would mean deciding whose copy of a title wins, which
 * is the seat cascade and is not designed yet.
 */
@Composable
fun CatalogPage(
    type: String,
    modifier: Modifier = Modifier,
    onOpenDetail: (type: String, id: String) -> Unit,
    onAddAddons: () -> Unit,
) {
    val collection by AddonRepository.collection.collectAsState()
    val shelves = collection.browsableCatalogs(type)

    if (shelves.isEmpty()) {
        NoCatalogs(onAddAddons = onAddAddons, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
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
                CatalogCard(meta) { onOpenDetail(meta.type, meta.id) }
            }
            if (loading) {
                item {
                    Box(
                        modifier = Modifier.width(120.dp).aspectRatio(2f / 3f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(meta: MetaPreview, onClick: () -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (meta.poster != null) {
                AsyncImage(
                    model = meta.poster,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
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
    }
}

@Composable
private fun NoCatalogs(onAddAddons: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
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
            text = "Nothing to browse",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Add an add-on with a catalog to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Button(onClick = onAddAddons) { Text("Add an add-on") }
    }
}
