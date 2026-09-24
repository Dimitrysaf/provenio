package com.nuvio.app.shell.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.NewEntryRow
import com.nuvio.app.shell.components.TextPromptDialog
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.ListSubheader
import com.nuvio.app.shell.components.NuvioScreen
import com.nuvio.app.shell.components.NuvioStatusModal
import com.nuvio.app.shell.components.withDuplicateSafeLazyKeys
import com.nuvio.app.shell.screens.settings.ListItemBetweenSpace
import com.nuvio.app.shell.screens.settings.OuterCorner
import com.nuvio.app.shell.screens.settings.segmentShape
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.nuvio.app.core.collection.Collection
import com.nuvio.app.core.collection.CollectionEditorRepository
import com.nuvio.app.core.collection.CollectionRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionManagementScreen(
    onBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,
) {
    val collections by CollectionRepository.collections.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showCopyError by remember { mutableStateOf(false) }
    var showNewCollectionDialog by remember { mutableStateOf(false) }

    NuvioScreen(
        title = stringResource(Res.string.collections_header),
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                val json = CollectionRepository.exportToJson()
                showCopyError = runCatching {
                    clipboardManager.setText(AnnotatedString(json))
                }.isFailure
            }) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(Res.string.collections_copy_json),
                )
            }
            IconButton(onClick = { showImportDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = stringResource(Res.string.collections_import),
                )
            }
        },
    ) {
        item { ListSubheader(text = stringResource(Res.string.collections_your_collections)) }

        item {
            CollectionReorderableList(
                collections = collections,
                onNew = { showNewCollectionDialog = true },
                onEdit = { onNavigateToEditor(it) },
                onDelete = { showDeleteConfirm = it },
            )
        }

        if (collections.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.CollectionsBookmark,
                    title = stringResource(Res.string.collections_empty_title),
                    message = stringResource(Res.string.collections_empty_subtitle),
                )
            }
        }
    }

    if (showNewCollectionDialog) {
        // The collection is named here and built by the editor: the name is handed over for the
        // initialize that the navigation below triggers, so the editor opens already titled.
        TextPromptDialog(
            title = stringResource(Res.string.collections_new),
            label = stringResource(Res.string.collections_editor_placeholder_name),
            initialValue = "",
            onConfirm = { name ->
                showNewCollectionDialog = false
                CollectionEditorRepository.setPendingNewTitle(name)
                onNavigateToEditor(null)
            },
            onDismiss = { showNewCollectionDialog = false },
        )
    }

    if (showImportDialog) {
        ImportDialog(
            importText = importText,
            importError = importError,
            onTextChange = {
                importText = it
                importError = null
            },
            onConfirm = {
                val result = CollectionRepository.validateJson(importText)
                if (result.valid) {
                    CollectionRepository.importFromJson(importText)
                    showImportDialog = false
                    importText = ""
                    importError = null
                } else {
                    importError = result.error
                }
            },
            onDismiss = {
                showImportDialog = false
                importText = ""
                importError = null
            },
        )
    }

    NuvioStatusModal(
        title = stringResource(Res.string.collections_copy_json),
        message = stringResource(Res.string.collections_copy_json_error),
        isVisible = showCopyError,
        onConfirm = { showCopyError = false },
        onDismiss = { showCopyError = false },
    )

    val deleteId = showDeleteConfirm
    val deleteCollection = deleteId?.let { id -> collections.find { it.id == id } }
    NuvioStatusModal(
        title = stringResource(Res.string.collections_delete_title),
        message = stringResource(Res.string.collections_delete_message, deleteCollection?.title.orEmpty()),
        isVisible = deleteId != null,
        confirmText = stringResource(Res.string.action_delete),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = {
            if (deleteId != null) {
                CollectionRepository.removeCollection(deleteId)
            }
            showDeleteConfirm = null
        },
        onDismiss = { showDeleteConfirm = null },
    )
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionReorderableList(
    collections: List<Collection>,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val keyedCollections = remember(collections) {
        collections.withDuplicateSafeLazyKeys { collection -> collection.id }
    }
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        // These are lazy list indices, and index 0 is the fixed new-collection row, so the
        // collections' own indices start one later. A drag that reaches the fixed row is not a
        // move at all, so it is dropped rather than clamped onto the first collection.
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex >= 0 && toIndex >= 0) {
            CollectionRepository.moveByIndex(fromIndex, toIndex)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // A segmented list group: each row is its own container, so the first and last take the
    // group's outer corner and the ones between take the inner one, and the 2dp between them is
    // what separates the rows instead of a divider. The row that makes a collection is the first
    // of them, which is why the collections themselves are counted from one.
    val rowCount = keyedCollections.size + 1

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        item(key = NEW_COLLECTION_ROW_KEY) {
            // Outside the reorderable items on purpose: it is the list's action, not one of its
            // entries, so it neither drags nor gets dragged past.
            NewEntryRow(
                text = stringResource(Res.string.collections_new),
                shape = segmentShape(index = 0, count = rowCount),
                onClick = onNew,
            )
        }

        itemsIndexed(keyedCollections, key = { _, collection -> collection.lazyKey }) { index, keyedCollection ->
            val collection = keyedCollection.value
            ReorderableItem(reorderableLazyListState, key = keyedCollection.lazyKey) { isDragging ->
                // Dragging is the one selection these rows have, so the item's own selected
                // shape is the group's outer corner on every edge, which is the morph the spec
                // defines for that state. Unselected, the row takes its place in the group.
                val unselected = segmentShape(index = index + 1, count = rowCount)
                val selectedShape = RoundedCornerShape(OuterCorner)

                CollectionListItem(
                    collection = collection,
                    selected = isDragging,
                    shapes = ListItemDefaults.shapes(
                        shape = unselected,
                        pressedShape = unselected,
                        focusedShape = unselected,
                        hoveredShape = unselected,
                        selectedShape = selectedShape,
                    ),
                    onEdit = { onEdit(collection.id) },
                    onDelete = { onDelete(collection.id) },
                    dragHandleScope = this@ReorderableItem,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionListItem(
    collection: Collection,
    selected: Boolean,
    shapes: ListItemShapes,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val summary = buildString {
        append(stringResource(Res.string.collections_folder_count, collection.folders.size))
        if (collection.pinToTop) {
            append(" · ")
            append(stringResource(Res.string.collections_pinned))
        }
    }

    // The expressive list item itself, not a container built to look like one: it owns the
    // segmented shape, the selected container pair and the morph between them, so dragging only
    // has to say the row is selected. The handle leads, the row's own actions trail it.
    SegmentedListItem(
        selected = selected,
        onClick = onEdit,
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            IconButton(
                modifier = with(dragHandleScope) {
                    Modifier.draggableHandle(
                        onDragStarted = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    )
                },
                onClick = {},
            ) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(Res.string.action_reorder),
                )
            }
        },
        supportingContent = { Text(summary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(Res.string.action_edit),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(Res.string.action_delete),
                    )
                }
            }
        },
    ) {
        Text(collection.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Pasting a collection's JSON in. Built on the dialog itself rather than a bare surface with the
 * parts laid out by hand: the title, the supporting text and the two actions are the dialog's own
 * slots, so they take its spec's placement, tone and shape. The field keeps the defaults too, so
 * its error state is the one the text field already draws.
 */
@Composable
private fun ImportDialog(
    importText: String,
    importError: String?,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.collections_import_header)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.collections_import_paste_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(Res.string.collections_import_json_placeholder))
                    },
                    isError = importError != null,
                    supportingText = importError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    // Pasted JSON is many lines, so the field opens tall enough to show it is for
                    // more than a word, and grows to a bound rather than to a fixed height.
                    minLines = 4,
                    maxLines = 10,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = importText.isNotBlank()) {
                Text(stringResource(Res.string.action_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

/**
 * The new-collection row's lazy key. It is a constant so it can never collide with a collection's
 * own id, which is what the rows below it are keyed by.
 */
private const val NEW_COLLECTION_ROW_KEY = "new-collection-row"
