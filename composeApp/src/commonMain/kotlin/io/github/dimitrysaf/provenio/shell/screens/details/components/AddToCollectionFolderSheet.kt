package io.github.dimitrysaf.provenio.shell.screens.details.components

import io.github.dimitrysaf.provenio.shell.components.stillListItemShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.collection.Collection
import io.github.dimitrysaf.provenio.core.collection.CollectionFolder
import io.github.dimitrysaf.provenio.core.collection.CollectionManualItem
import io.github.dimitrysaf.provenio.core.collection.CollectionManualItems
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.collection.hasManualItem
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ExpandableListHeader
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SelectableListRow
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.SheetNavigationButton
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.collections_editor_selected_count
import provenio.composeapp.generated.resources.collections_empty_subtitle
import provenio.composeapp.generated.resources.collections_empty_title
import provenio.composeapp.generated.resources.collections_folder_count
import provenio.composeapp.generated.resources.details_add_to_collection_folder

private sealed interface FolderPickerRow {
    val key: String

    data class Header(val collection: Collection) : FolderPickerRow {
        override val key: String get() = "collection-${collection.id}"
    }

    data class Folder(val collection: Collection, val folder: CollectionFolder) : FolderPickerRow {
        override val key: String get() = "folder-${collection.id}-${folder.id}"
    }
}

/**
 * Puts the title in a collection's folder by hand, as the catalog picker picks catalogs: each
 * collection a group that opens onto its folders, and each folder a checkbox that puts the title
 * in or takes it out. Every tick is saved at once, so the sheet is closed when the picking is done.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AddToCollectionFolderSheet(item: CollectionManualItem, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val collections by remember {
        CollectionRepository.initialize()
        CollectionRepository.collections
    }.collectAsState()
    val withFolders = collections.filter { it.folders.isNotEmpty() }
    // The collections already holding the title start open, so where it is shows at once.
    var expanded by remember {
        mutableStateOf(
            withFolders.filter { collection -> collection.folders.any { it.hasManualItem(item.id) } }
                .mapTo(mutableSetOf()) { it.id }
                .toSet(),
        )
    }
    val rows = buildList<FolderPickerRow> {
        withFolders.forEach { collection ->
            add(FolderPickerRow.Header(collection))
            if (collection.id in expanded) collection.folders.forEach { add(FolderPickerRow.Folder(collection, it)) }
        }
    }

    ModalSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.details_add_to_collection_folder),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        SheetNavigationButton(navigation = SheetNavigation.Close, onClick = onDismiss)
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (rows.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Folder,
                        title = stringResource(Res.string.collections_empty_title),
                        message = stringResource(Res.string.collections_empty_subtitle),
                    )
                }
            } else {
                itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                    // One group across every row, so each corner knows what it faces.
                    val shape = segmentShape(index = index, count = rows.size)
                    when (row) {
                        is FolderPickerRow.Header -> {
                            val open = row.collection.id in expanded
                            val holding = row.collection.folders.count { it.hasManualItem(item.id) }
                            ExpandableListHeader(
                                title = row.collection.title,
                                expanded = open,
                                shapes = stillListItemShapes(
                                    shape = shape,
                                    selectedShape = shape,
                                    pressedShape = shape,
                                    focusedShape = shape,
                                    hoveredShape = shape,
                                ),
                                onClick = {
                                    expanded = if (open) expanded - row.collection.id else expanded + row.collection.id
                                },
                                supporting = if (holding > 0) {
                                    stringResource(Res.string.collections_editor_selected_count, holding)
                                } else {
                                    stringResource(Res.string.collections_folder_count, row.collection.folders.size)
                                },
                                titleMaxLines = Int.MAX_VALUE,
                            )
                        }

                        is FolderPickerRow.Folder -> {
                            val selected = row.folder.hasManualItem(item.id)
                            SelectableListRow(
                                selected = selected,
                                onClick = { CollectionManualItems.toggle(row.collection.id, row.folder.id, item) },
                                headline = row.folder.title,
                                unselectedShape = shape,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                // Null handler: the row owns the click, so the box is an indicator.
                                trailingContent = { Checkbox(checked = selected, onCheckedChange = null) },
                            )
                        }
                    }
                }
            }

            item(key = "bottom") { Spacer(modifier = Modifier.height(safeBottomPadding(12.dp))) }
        }
    }
}
