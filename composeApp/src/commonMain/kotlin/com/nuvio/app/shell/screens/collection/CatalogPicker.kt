package com.nuvio.app.shell.screens.collection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.NuvioModalBottomSheet
import com.nuvio.app.shell.components.SelectableListRow
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.shell.screens.settings.ListItemBetweenSpace
import com.nuvio.app.shell.screens.settings.segmentShape
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.collection.AvailableCatalog
import com.nuvio.app.core.collection.CollectionCatalogSource

/**
 * One row of the catalog picker's list. Headers and the catalogs they reveal are the same kind of
 * thing here: rows of one group, in the order they are drawn. Naming them lets the group be
 * counted and shaped as a whole, which is the only way a corner can know whether it faces another
 * row or the outside.
 */
private sealed interface CatalogPickerRow {
    val key: String

    data class Addon(val name: String, val catalogs: List<AvailableCatalog>) : CatalogPickerRow {
        override val key: String get() = "addon-$name"
    }

    data class Catalog(val catalog: AvailableCatalog) : CatalogPickerRow {
        override val key: String
            get() = "catalog-${catalog.addonId}-${catalog.type}-${catalog.catalogId}"
    }
}

/**
 * Picking the catalogs a folder aggregates.
 *
 * A sheet rather than a page: the choice belongs to the folder being edited, and a page pushed
 * the editor off screen to make it. One addon can publish dozens of catalogs, so each addon is a
 * collapsible group, collapsed to start, rather than one flat list hundreds of rows long.
 *
 * Several catalogs can be picked at once, so the rows are checkboxes and the sheet stays open;
 * it is dismissed when the picking is done.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CatalogPickerSheet(
    availableCatalogs: List<AvailableCatalog>,
    selectedSources: List<CollectionCatalogSource>,
    onToggle: (AvailableCatalog) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val grouped = remember(availableCatalogs) { availableCatalogs.groupBy { it.addonName } }
    var expandedAddons by remember { mutableStateOf(emptySet<String>()) }

    // Flattened in draw order: each addon's header, then its catalogs when it is open. One list,
    // so one group, so the corners can be worked out from a row's place in it.
    val rows: List<CatalogPickerRow> = remember(grouped, expandedAddons) {
        // Explicit element type: inference can otherwise pin the builder to the first subtype
        // added and reject the second.
        buildList<CatalogPickerRow> {
            grouped.forEach { (addonName, catalogs) ->
                add(CatalogPickerRow.Addon(addonName, catalogs))
                if (addonName in expandedAddons) {
                    catalogs.forEach { add(CatalogPickerRow.Catalog(it)) }
                }
            }
        }
    }

    fun isSelected(catalog: AvailableCatalog): Boolean = selectedSources.any {
        it.addonId == catalog.addonId &&
            it.type == catalog.type &&
            it.catalogId == catalog.catalogId
    }

    NuvioModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.collections_editor_select_catalogs),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.collections_editor_select_catalogs_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            Res.string.collections_editor_selected_count,
                            selectedSources.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.GridView,
                        title = stringResource(Res.string.collections_editor_catalog_picker_empty_title),
                        message = stringResource(Res.string.collections_editor_catalog_picker_empty_subtitle),
                    )
                }
            } else {
                itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                    // Indexed against the whole list, not against one addon's catalogs. Counting
                    // each addon separately is what gave every header four outer corners and left
                    // it square against the rows it had just revealed.
                    val shape = segmentShape(index = index, count = rows.size)

                    when (row) {
                        is CatalogPickerRow.Addon -> {
                            val selectedCount = row.catalogs.count(::isSelected)
                            CatalogPickerAddonHeader(
                                addonName = row.name,
                                subtitle = if (selectedCount > 0) {
                                    stringResource(
                                        Res.string.collections_editor_catalog_selected_count,
                                        selectedCount,
                                    )
                                } else {
                                    stringResource(
                                        Res.string.collections_editor_catalog_count,
                                        row.catalogs.size,
                                    )
                                },
                                expanded = row.name in expandedAddons,
                                shape = shape,
                                onToggleExpanded = {
                                    expandedAddons = if (row.name in expandedAddons) {
                                        expandedAddons - row.name
                                    } else {
                                        expandedAddons + row.name
                                    }
                                },
                            )
                        }

                        is CatalogPickerRow.Catalog -> {
                            val catalog = row.catalog
                            val selected = isSelected(catalog)
                            // The shared selectable row, so a picked catalog gets the same
                            // primary container and 4dp-to-16dp corner morph every other chosen
                            // row in the app gets. Unselected, it keeps its place in the group.
                            SelectableListRow(
                                selected = selected,
                                onClick = { onToggle(catalog) },
                                headline = catalog.catalogName,
                                supporting = catalog.type.replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                },
                                unselectedShape = shape,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                trailingContent = {
                                    // Null handler: the row owns the click, so the box is an
                                    // indicator rather than a second target inside it.
                                    Checkbox(checked = selected, onCheckedChange = null)
                                },
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(nuvioSafeBottomPadding(12.dp)))
            }
        }
    }
}

/**
 * An addon's group header in the catalog picker. Material has no accordion, so this is the
 * disclosure pattern built from a list item: a row with a chevron that turns to point at what it
 * reveals. It is a row of the same group as the catalogs it reveals, so its corners come from its
 * place in that group; only its container tone sets it apart as a header.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CatalogPickerAddonHeader(
    addonName: String,
    subtitle: String,
    expanded: Boolean,
    shape: RoundedCornerShape,
    onToggleExpanded: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "catalogPickerChevron",
    )
    SegmentedListItem(
        onClick = onToggleExpanded,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(chevronRotation),
            )
        },
    ) {
        Text(addonName)
    }
}
