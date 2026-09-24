package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.NewEntryRow
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import io.github.dimitrysaf.provenio.core.collection.AvailableCatalog
import io.github.dimitrysaf.provenio.core.collection.CollectionCatalogSource
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorRepository
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorUiState
import io.github.dimitrysaf.provenio.core.collection.CollectionFolder
import io.github.dimitrysaf.provenio.core.collection.CollectionSource
import io.github.dimitrysaf.provenio.core.collection.findAvailableCatalog

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FolderReorderableList(
    folders: List<CollectionFolder>,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        // These are lazy list indices, and index 0 is the fixed new-folder row, so the folders'
        // own indices start one later. A drag that reaches the fixed row is not a move at all, so
        // it is dropped rather than clamped onto the first folder.
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex >= 0 && toIndex >= 0) {
            CollectionEditorRepository.moveFolderByIndex(fromIndex, toIndex)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // A segmented group, like every other list in the editor. The new-folder row is the first of
    // it, which is why the folders themselves are counted from one.
    val rowCount = folders.size + 1

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        item(key = NEW_FOLDER_ROW_KEY) {
            // Outside the reorderable items on purpose: it is the list's action, not one of its
            // entries, so it neither drags nor gets dragged past.
            NewEntryRow(
                text = stringResource(Res.string.collections_editor_new_folder),
                shape = segmentShape(index = 0, count = rowCount),
                onClick = onNew,
            )
        }

        itemsIndexed(folders, key = { _, folder -> folder.id }) { index, folder ->
            ReorderableItem(reorderableLazyListState, key = folder.id) { isDragging ->
                // Dragging is the one selection these rows have, so the selected shape is the
                // group's outer corner on every edge.
                val unselected = segmentShape(index = index + 1, count = rowCount)
                val selectedShape = RoundedCornerShape(OuterCorner)

                FolderListItem(
                    folder = folder,
                    selected = isDragging,
                    shapes = ListItemDefaults.shapes(
                        shape = unselected,
                        pressedShape = unselected,
                        focusedShape = unselected,
                        hoveredShape = unselected,
                        selectedShape = selectedShape,
                    ),
                    onEdit = { onEdit(folder.id) },
                    onDelete = { onDelete(folder.id) },
                    dragHandleScope = this@ReorderableItem,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FolderListItem(
    folder: CollectionFolder,
    selected: Boolean,
    shapes: ListItemShapes,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val summary = stringResource(
        Res.string.collections_editor_source_count,
        folder.resolvedSources.size,
        posterShapeLabel(folder.posterShape),
    )

    // One row: the handle leads, the cover and text are the row's content, the actions trail it.
    SegmentedListItem(
        selected = selected,
        onClick = onEdit,
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                if (folder.coverEmoji != null) {
                    Text(
                        text = folder.coverEmoji,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
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
        Text(folder.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun FolderEditorPage(
    state: CollectionEditorUiState,
    onBack: () -> Unit,
    onNavigateToPage: ((page: CollectionEditorPage, title: String) -> Unit)?,
    onSave: () -> Unit,
) {
    val folder = state.editingFolder ?: return
    val tmdbSourcePickerTitle = stringResource(Res.string.collections_editor_tmdb_sources)
    val traktSourcePickerTitle = stringResource(Res.string.collections_editor_trakt_sources)
    val editTraktSourcePickerTitle = stringResource(Res.string.collections_editor_edit_trakt_source)

    var showDiscardConfirm by remember { mutableStateOf(false) }
    val guardedBack: () -> Unit = {
        if (CollectionEditorRepository.hasUnsavedFolderChanges()) {
            showDiscardConfirm = true
        } else {
            onBack()
        }
    }

    StatusModal(
        title = stringResource(Res.string.collections_editor_discard_title),
        message = stringResource(Res.string.collections_editor_discard_folder_message),
        isVisible = showDiscardConfirm,
        confirmText = stringResource(Res.string.action_discard),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = {
            showDiscardConfirm = false
            onBack()
        },
        onDismiss = { showDiscardConfirm = false },
    )

    PlatformBackHandler(enabled = true) {
        guardedBack()
    }

    ScreenScaffold(
        title = if (state.folders.any { it.id == folder.id }) {
                    stringResource(Res.string.collections_editor_edit_folder)
                } else {
                    stringResource(Res.string.collections_editor_new_folder)
                },
        modifier = Modifier.fillMaxSize(),
        onBack = guardedBack,
        bottomBar = {
            CollectionEditorActionBar {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = folder.title.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.collections_editor_save))
                }
            }
        },
    ) {

        item {
            // The whole of the folder's appearance lives here now: the header shows it, and its
            // two pencils open the only two dialogs that change it.
            var showTitleDialog by remember { mutableStateOf(false) }
            var showCoverDialog by remember { mutableStateOf(false) }

            EditableIdentityHeader(
                imageUrl = folder.coverImageUrl,
                title = folder.title,
                titlePlaceholder = stringResource(Res.string.collections_editor_placeholder_folder),
                onEditImage = { showCoverDialog = true },
                onEditTitle = { showTitleDialog = true },
                emoji = folder.coverEmoji,
                // The header is the tile, so it stands in the shape the tile will be drawn in.
                aspectRatio = folder.posterShape.aspectRatio(),
            )

            if (showTitleDialog) {
                FolderTitleDialog(
                    initialTitle = folder.title,
                    initialHideTitle = folder.hideTitle,
                    onConfirm = { title, hideTitle ->
                        CollectionEditorRepository.updateFolderTitle(title)
                        CollectionEditorRepository.updateFolderHideTitle(hideTitle)
                        showTitleDialog = false
                    },
                    onDismiss = { showTitleDialog = false },
                )
            }

            if (showCoverDialog) {
                FolderCoverDialog(
                    initialEmoji = folder.coverEmoji,
                    initialImageUrl = folder.coverImageUrl,
                    initialShape = folder.posterShape,
                    onConfirm = { emoji, imageUrl, tileShape ->
                        when {
                            emoji != null -> CollectionEditorRepository.updateFolderCoverEmoji(emoji)
                            imageUrl != null -> CollectionEditorRepository.updateFolderCoverImage(imageUrl)
                            else -> CollectionEditorRepository.clearFolderCover()
                        }
                        CollectionEditorRepository.updateFolderTileShape(tileShape)
                        showCoverDialog = false
                    },
                    onDismiss = { showCoverDialog = false },
                )
            }
        }

        item {
            FolderEditorSection(
                title = stringResource(Res.string.collections_editor_section_catalog_sources),
                actions = {
                    // The three kinds of source are peers, so they sit side by side. The group
                    // keeps them on one line and folds only what does not fit behind a chevron,
                    // which is what the hand-laid row could not do: it wrapped instead.
                    AddSourceButton(
                        label = stringResource(Res.string.collections_editor_add),
                        sheetTitle = stringResource(Res.string.collections_editor_add_source),
                        choices = listOf(
                            AddSourceChoice(
                                label = stringResource(Res.string.collections_editor_source_catalog),
                                onClick = { CollectionEditorRepository.showCatalogPicker() },
                            ),
                            AddSourceChoice(
                                label = stringResource(Res.string.source_tmdb),
                                onClick = {
                                    CollectionEditorRepository.showTmdbSourcePicker()
                                    onNavigateToPage?.invoke(
                                        CollectionEditorPage.TmdbSourcePicker,
                                        tmdbSourcePickerTitle,
                                    )
                                },
                            ),
                            AddSourceChoice(
                                label = stringResource(Res.string.source_trakt),
                                onClick = {
                                    CollectionEditorRepository.showTraktSourcePicker()
                                    onNavigateToPage?.invoke(
                                        CollectionEditorPage.TraktSourcePicker,
                                        traktSourcePickerTitle,
                                    )
                                },
                            ),
                        ),
                    )
                },
            ) {
                val sources = folder.resolvedSources
                if (sources.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.GridView,
                        title = stringResource(Res.string.collections_editor_catalog_sources_empty_title),
                        message = stringResource(Res.string.collections_editor_catalog_sources_empty_subtitle),
                    )
                } else {
                    // The sources are one segmented group, like every other list here.
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace)) {
                        sources.forEachIndexed { index, source ->
                            val addonSource = source.addonCatalogSource()
                            val shape = segmentShape(index = index, count = sources.size)
                            if (source.isTmdb) {
                                FolderTmdbSourceCard(
                                    source = source,
                                    shape = shape,
                                    onRemove = { CollectionEditorRepository.removeCatalogSource(index) },
                                )
                            } else if (source.isTrakt) {
                                FolderTraktSourceCard(
                                    source = source,
                                    shape = shape,
                                    onEdit = {
                                        CollectionEditorRepository.editTraktSource(index)
                                        onNavigateToPage?.invoke(
                                            CollectionEditorPage.TraktSourcePicker,
                                            editTraktSourcePickerTitle,
                                        )
                                    },
                                    onRemove = { CollectionEditorRepository.removeCatalogSource(index) },
                                )
                            } else if (addonSource != null) {
                                FolderCatalogSourceCard(
                                    source = addonSource,
                                    matchingCatalog = state.availableCatalogs.findAvailableCatalog(addonSource),
                                    shape = shape,
                                    onRemove = { CollectionEditorRepository.removeCatalogSource(index) },
                                    onOpenGenrePicker = { CollectionEditorRepository.showGenrePicker(index) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FolderEditorSection(
    title: String,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // The heading labels the section; the section's actions belong inside it, under that heading,
    // not squeezed onto the heading's own line where they fight it for width.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListSubheader(text = title)
        actions?.invoke()
        content()
    }
}

/** One TMDB source in a folder's list. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FolderTmdbSourceCard(
    source: CollectionSource,
    shape: RoundedCornerShape,
    onRemove: () -> Unit,
) {
    FolderSourceRow(
        title = source.title?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.source_tmdb),
        supporting = "${stringResource(Res.string.source_tmdb)} · ${tmdbSourceSubtitle(source)}",
        shape = shape,
        onClick = null,
        onRemove = onRemove,
    )
}

/** One Trakt source in a folder's list; tapping it reopens the picker to edit it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FolderTraktSourceCard(
    source: CollectionSource,
    shape: RoundedCornerShape,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    FolderSourceRow(
        title = source.title?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.source_trakt),
        supporting = "${stringResource(Res.string.source_trakt)} · ${traktSourceSubtitle(source)}",
        shape = shape,
        onClick = onEdit,
        onRemove = onRemove,
        leadingAction = {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(Res.string.action_edit),
                )
            }
        },
    )
}

/**
 * One addon catalog in a folder's list. When the catalog offers genres, the row carries the
 * chosen one and opens the genre picker, so the choice is part of the row rather than a second
 * control tucked inside it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FolderCatalogSourceCard(
    source: CollectionCatalogSource,
    matchingCatalog: AvailableCatalog?,
    shape: RoundedCornerShape,
    onRemove: () -> Unit,
    onOpenGenrePicker: () -> Unit,
) {
    val typeLabel = source.type.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
    val genreOptions = matchingCatalog?.genreOptions.orEmpty()
    val selectedGenreLabel = source.genre ?: if (matchingCatalog?.genreRequired == true) {
        stringResource(Res.string.collections_editor_select_genre)
    } else {
        stringResource(Res.string.collections_editor_all_genres)
    }
    val supporting = buildString {
        append(matchingCatalog?.addonName ?: source.addonId)
        append(" · ")
        append(typeLabel)
        if (genreOptions.isNotEmpty()) {
            append(" · ")
            append(selectedGenreLabel)
        }
    }

    FolderSourceRow(
        title = matchingCatalog?.catalogName ?: source.catalogId,
        supporting = supporting,
        shape = shape,
        onClick = if (genreOptions.isNotEmpty()) onOpenGenrePicker else null,
        onRemove = onRemove,
    )
}

/** The shape every source row in a folder shares. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FolderSourceRow(
    title: String,
    supporting: String,
    shape: RoundedCornerShape,
    onClick: (() -> Unit)?,
    onRemove: () -> Unit,
    leadingAction: (@Composable () -> Unit)? = null,
) {
    SegmentedListItem(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        supportingContent = { Text(supporting, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingAction?.invoke()
                IconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(Res.string.action_remove),
                    )
                }
            }
        },
    ) {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The new-folder row's lazy key. A constant, so it can never collide with a folder's own id, which
 * is what the rows below it are keyed by.
 */
internal const val NEW_FOLDER_ROW_KEY = "new-folder-row"
