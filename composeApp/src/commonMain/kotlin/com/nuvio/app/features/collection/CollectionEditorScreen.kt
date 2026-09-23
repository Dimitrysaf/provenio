package com.nuvio.app.features.collection

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.NewEntryRow
import com.nuvio.app.shell.components.TextPromptDialog
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.ListSubheader
import com.nuvio.app.shell.components.NuvioModalBottomSheet
import com.nuvio.app.shell.components.NuvioScreen
import com.nuvio.app.shell.components.NuvioSearchField
import com.nuvio.app.shell.components.NuvioStatusModal
import com.nuvio.app.shell.components.PlatformBackHandler
import com.nuvio.app.shell.components.SelectableListRow
import com.nuvio.app.shell.components.SingleChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.core.home.PosterShape
import com.nuvio.app.features.settings.ListItemBetweenSpace
import com.nuvio.app.features.settings.OuterCorner
import com.nuvio.app.features.settings.segmentShape
import com.nuvio.app.features.trakt.TraktPublicListSearchResult
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

enum class CollectionEditorPage {
    Root,
    FolderEditor,
    CatalogPicker,
    TmdbSourcePicker,
    TraktSourcePicker,
}

fun disposeCollectionEditorPage(page: CollectionEditorPage) {
    when (page) {
        CollectionEditorPage.Root -> Unit
        CollectionEditorPage.FolderEditor -> CollectionEditorRepository.cancelFolderEdit()
        CollectionEditorPage.CatalogPicker -> CollectionEditorRepository.hideCatalogPicker()
        CollectionEditorPage.TmdbSourcePicker -> CollectionEditorRepository.hideTmdbSourcePicker()
        CollectionEditorPage.TraktSourcePicker -> CollectionEditorRepository.hideTraktSourcePicker()
    }
}

private val autoDismissedPickerPages = setOf(
    CollectionEditorPage.TmdbSourcePicker,
    CollectionEditorPage.TraktSourcePicker,
)

private fun CollectionEditorUiState.activeEditorPage(): CollectionEditorPage = when {
    // No CatalogPicker here: it is a sheet over the folder editor, so the editor stays the page.
    showTmdbSourcePicker -> CollectionEditorPage.TmdbSourcePicker
    showTraktSourcePicker -> CollectionEditorPage.TraktSourcePicker
    showFolderEditor && editingFolder != null -> CollectionEditorPage.FolderEditor
    else -> CollectionEditorPage.Root
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionEditorScreen(
    collectionId: String?,
    onBack: () -> Unit,
    initialPage: CollectionEditorPage? = null,
    initializeRepository: Boolean = true,
    onNavigateToPage: ((page: CollectionEditorPage, title: String) -> Unit)? = null,
) {
    val state by CollectionEditorRepository.uiState.collectAsState()

    LaunchedEffect(collectionId, initializeRepository) {
        if (initializeRepository) {
            CollectionEditorRepository.initialize(collectionId)
        }
    }

    val page = initialPage ?: state.activeEditorPage()
    val initialPickerCompletionGeneration = remember(initialPage) {
        state.sourcePickerCompletionGeneration
    }

    LaunchedEffect(initialPage, state.sourcePickerCompletionGeneration) {
        if (
            initialPage in autoDismissedPickerPages &&
            state.sourcePickerCompletionGeneration != initialPickerCompletionGeneration
        ) {
            onBack()
        }
    }

    if (
        initialPage != null &&
        page != CollectionEditorPage.Root &&
        state.editingFolder == null
    ) {
        LaunchedEffect(initialPage) { onBack() }
        return
    }

    fun closePage(pageToClose: CollectionEditorPage, close: () -> Unit) {
        close()
        if (initialPage == pageToClose) {
            onBack()
        }
    }

    val editingFolder = state.editingFolder
    if (page == CollectionEditorPage.FolderEditor && editingFolder != null) {
        val genrePickerIndex = state.genrePickerSourceIndex
        val genrePickerSource = genrePickerIndex?.let { editingFolder.resolvedSources.getOrNull(it) }
        val genrePickerCatalogSource = genrePickerSource?.addonCatalogSource()
        val genrePickerCatalog = genrePickerCatalogSource?.let { source ->
            state.availableCatalogs.findAvailableCatalog(source)
        }

        FolderEditorPage(
            state = state,
            onBack = {
                closePage(CollectionEditorPage.FolderEditor) {
                    CollectionEditorRepository.cancelFolderEdit()
                }
            },
            onNavigateToPage = onNavigateToPage,
            onSave = {
                CollectionEditorRepository.saveFolderEdit()
                if (initialPage == CollectionEditorPage.FolderEditor) {
                    onBack()
                }
            },
        )

        if (state.showCatalogPicker) {
            CatalogPickerSheet(
                availableCatalogs = state.availableCatalogs,
                selectedSources = editingFolder.resolvedCatalogSources,
                onToggle = { CollectionEditorRepository.toggleCatalogSource(it) },
                onDismiss = { CollectionEditorRepository.hideCatalogPicker() },
            )
        }

        if (
            genrePickerIndex != null &&
            genrePickerCatalogSource != null &&
            genrePickerCatalog != null &&
            genrePickerCatalog.genreOptions.isNotEmpty()
        ) {
            GenrePickerSheet(
                title = genrePickerCatalog.catalogName,
                selectedGenre = genrePickerCatalogSource.genre,
                genreOptions = genrePickerCatalog.genreOptions,
                allowAll = !genrePickerCatalog.genreRequired,
                onSelect = {
                    CollectionEditorRepository.updateCatalogSourceGenre(genrePickerIndex, it)
                    CollectionEditorRepository.hideGenrePicker()
                },
                onDismiss = { CollectionEditorRepository.hideGenrePicker() },
            )
        }
        return
    }

    if (page == CollectionEditorPage.CatalogPicker) {
        // The picker is a sheet on the folder editor now. The enum entry stays so a back stack
        // saved before the change still deserializes; a route that reaches it closes itself.
        LaunchedEffect(Unit) {
            CollectionEditorRepository.hideCatalogPicker()
            onBack()
        }
        return
    }

    if (page == CollectionEditorPage.TmdbSourcePicker) {
        TmdbSourcePickerScreen(
            state = state,
            onBack = {
                closePage(CollectionEditorPage.TmdbSourcePicker) {
                    CollectionEditorRepository.hideTmdbSourcePicker()
                }
            },
        )
        return
    }

    if (page == CollectionEditorPage.TraktSourcePicker) {
        TraktSourcePickerScreen(
            state = state,
            onBack = {
                closePage(CollectionEditorPage.TraktSourcePicker) {
                    CollectionEditorRepository.hideTraktSourcePicker()
                }
            },
        )
        return
    }

    // Leaving with edits behind you loses them silently, so the back press asks first. Only when
    // there is something to lose: an untouched editor closes on the first tap.
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val guardedBack: () -> Unit = {
        if (CollectionEditorRepository.hasUnsavedCollectionChanges()) {
            showDiscardConfirm = true
        } else {
            onBack()
        }
    }

    NuvioStatusModal(
        title = stringResource(Res.string.collections_editor_discard_title),
        message = stringResource(Res.string.collections_editor_discard_collection_message),
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

    NuvioScreen(
        title = if (state.isNew) {
                    stringResource(Res.string.collections_new)
                } else {
                    stringResource(Res.string.collections_editor_edit_collection)
                },
        modifier = Modifier.fillMaxSize(),
        onBack = guardedBack,
        bottomBar = {
            CollectionEditorActionBar {
                Button(
                    onClick = {
                        if (CollectionEditorRepository.save()) {
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.title.isNotBlank(),
                ) {
                    Text(
                        text = if (state.isNew) {
                            stringResource(Res.string.collections_editor_create_collection)
                        } else {
                            stringResource(Res.string.collections_editor_save_changes)
                        },
                    )
                }
            }
        },
    ) {

        item {
            // What the collection is called and what it looks like lead the page instead of being
            // two more fields in the form: they are what identifies it, and each is edited in its
            // own dialog rather than in place.
            var showRenameDialog by remember { mutableStateOf(false) }
            var showBackdropDialog by remember { mutableStateOf(false) }

            EditableIdentityHeader(
                imageUrl = state.backdropImageUrl,
                title = state.title,
                titlePlaceholder = stringResource(Res.string.collections_editor_placeholder_name),
                onEditImage = { showBackdropDialog = true },
                onEditTitle = { showRenameDialog = true },
            )

            if (showRenameDialog) {
                TextPromptDialog(
                    title = stringResource(Res.string.collections_rename_collection),
                    label = stringResource(Res.string.collections_editor_placeholder_name),
                    initialValue = state.title,
                    onConfirm = {
                        CollectionEditorRepository.setTitle(it)
                        showRenameDialog = false
                    },
                    onDismiss = { showRenameDialog = false },
                )
            }

            if (showBackdropDialog) {
                // Blank is allowed: clearing the field is how the backdrop is removed, and the
                // header falls back to its placeholder.
                TextPromptDialog(
                    title = stringResource(Res.string.collections_editor_backdrop_image),
                    label = stringResource(Res.string.collections_editor_placeholder_backdrop),
                    initialValue = state.backdropImageUrl,
                    onConfirm = {
                        CollectionEditorRepository.setBackdropImageUrl(it)
                        showBackdropDialog = false
                    },
                    onDismiss = { showBackdropDialog = false },
                    keyboardType = KeyboardType.Uri,
                    allowBlank = true,
                )
            }
        }

        item {
            // The options below the header are one segmented group.
            var showViewModeSheet by remember { mutableStateOf(false) }
            val modes = FolderViewMode.entries.filter { it != FolderViewMode.FOLLOW_LAYOUT }

            CollectionEditorGroup {
                row { shape ->
                    CollectionEditorSwitchRow(
                        title = stringResource(Res.string.collections_editor_pin_above),
                        description = stringResource(Res.string.collections_editor_pin_above_desc),
                        checked = state.pinToTop,
                        onCheckedChange = { CollectionEditorRepository.setPinToTop(it) },
                        shape = shape,
                    )
                }
                row { shape ->
                    CollectionEditorChoiceRow(
                        title = stringResource(Res.string.collections_editor_view_mode),
                        value = folderViewModeLabel(state.viewMode),
                        onClick = { showViewModeSheet = true },
                        shape = shape,
                    )
                }
                row { shape ->
                    CollectionEditorSwitchRow(
                        title = stringResource(Res.string.collections_editor_show_all_tab),
                        description = stringResource(Res.string.collections_editor_show_all_tab_desc),
                        checked = state.showAllTab,
                        onCheckedChange = { CollectionEditorRepository.setShowAllTab(it) },
                        shape = shape,
                    )
                }
            }

            if (showViewModeSheet) {
                SingleChoiceBottomSheet(
                    title = stringResource(Res.string.collections_editor_view_mode),
                    options = modes.map { mode ->
                        SingleChoiceOption(value = mode, label = folderViewModeLabel(mode))
                    },
                    isSelected = { it == state.viewMode },
                    onSelected = { CollectionEditorRepository.setViewMode(it) },
                    onDismiss = { showViewModeSheet = false },
                )
            }
        }

        // Folders Section Header
        item { ListSubheader(text = stringResource(Res.string.collections_editor_folders)) }

        // Folder Items. The row that makes a folder is the first of the same group, so the action
        // belongs to the list rather than floating beside its heading.
        item {
            val newFolderTitle = stringResource(Res.string.collections_editor_new_folder)
            val editFolderTitle = stringResource(Res.string.collections_editor_edit_folder)
            var showNewFolderDialog by remember { mutableStateOf(false) }

            FolderReorderableList(
                folders = state.folders,
                onNew = { showNewFolderDialog = true },
                onEdit = {
                    CollectionEditorRepository.editFolder(it)
                    onNavigateToPage?.invoke(CollectionEditorPage.FolderEditor, editFolderTitle)
                },
                onDelete = { CollectionEditorRepository.removeFolder(it) },
            )

            if (showNewFolderDialog) {
                // The folder is built with the name the dialog took, then its editor opens on it,
                // so the name is never a placeholder the user has to go and correct.
                TextPromptDialog(
                    title = stringResource(Res.string.collections_editor_new_folder),
                    label = stringResource(Res.string.collections_editor_placeholder_folder),
                    initialValue = "",
                    onConfirm = { name ->
                        showNewFolderDialog = false
                        CollectionEditorRepository.addFolder(name)
                        onNavigateToPage?.invoke(
                            CollectionEditorPage.FolderEditor,
                            newFolderTitle,
                        )
                    },
                    onDismiss = { showNewFolderDialog = false },
                )
            }
        }

        if (state.folders.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Folder,
                    title = stringResource(Res.string.collections_editor_folder_empty_title),
                    message = stringResource(Res.string.collections_editor_folder_empty_subtitle),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderReorderableList(
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
private fun FolderListItem(
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
private fun FolderEditorPage(
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

    NuvioStatusModal(
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

    NuvioScreen(
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

@Composable
private fun TmdbSourcePickerScreen(
    state: CollectionEditorUiState,
    onBack: () -> Unit,
) {
    val sourceType = when (state.tmdbBuilderMode) {
        TmdbBuilderMode.PRESETS -> TmdbCollectionSourceType.DISCOVER
        TmdbBuilderMode.LIST -> TmdbCollectionSourceType.LIST
        TmdbBuilderMode.COLLECTION -> TmdbCollectionSourceType.COLLECTION
        TmdbBuilderMode.PRODUCTION -> TmdbCollectionSourceType.COMPANY
        TmdbBuilderMode.NETWORK -> TmdbCollectionSourceType.NETWORK
        TmdbBuilderMode.PERSON -> TmdbCollectionSourceType.PERSON
        TmdbBuilderMode.DIRECTOR -> TmdbCollectionSourceType.DIRECTOR
        TmdbBuilderMode.DISCOVER -> TmdbCollectionSourceType.DISCOVER
    }
    val requiresId = sourceType != TmdbCollectionSourceType.DISCOVER
    // These two are found by typing a name and searching, not by pasting an id.
    val searchesByName = sourceType == TmdbCollectionSourceType.COMPANY ||
        sourceType == TmdbCollectionSourceType.COLLECTION
    val showMediaControls = state.tmdbBuilderMode == TmdbBuilderMode.PRODUCTION ||
        state.tmdbBuilderMode == TmdbBuilderMode.PERSON ||
        state.tmdbBuilderMode == TmdbBuilderMode.DIRECTOR ||
        state.tmdbBuilderMode == TmdbBuilderMode.DISCOVER
    val showSortControls = state.tmdbBuilderMode == TmdbBuilderMode.PRODUCTION ||
        state.tmdbBuilderMode == TmdbBuilderMode.NETWORK ||
        state.tmdbBuilderMode == TmdbBuilderMode.PERSON ||
        state.tmdbBuilderMode == TmdbBuilderMode.DIRECTOR ||
        state.tmdbBuilderMode == TmdbBuilderMode.DISCOVER
    val showFilterControls = state.tmdbBuilderMode == TmdbBuilderMode.DISCOVER
    // Presets are added by tapping one, so that mode has no action to dock and gets no bar; the
    // scaffold then falls back to the window's own bottom inset for the content's padding.
    val tmdbActionBar: (@Composable () -> Unit)? = if (state.tmdbBuilderMode == TmdbBuilderMode.PRESETS) {
        null
    } else {
        {
            CollectionEditorActionBar {
                Button(
                    onClick = { CollectionEditorRepository.addTmdbSourceFromInput() },
                    modifier = Modifier.weight(1f),
                    enabled = !requiresId || state.tmdbInput.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.collections_editor_add_source))
                }
            }
        }
    }

    PlatformBackHandler(enabled = true) {
        onBack()
    }

    NuvioScreen(
        title = stringResource(Res.string.collections_editor_tmdb_sources),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        bottomBar = tmdbActionBar,
    ) {

        item {
            PickerTabRow(
                tabs = TmdbBuilderMode.entries,
                selected = state.tmdbBuilderMode,
                label = { tmdbBuilderModeLabel(it) },
                onSelect = { CollectionEditorRepository.setTmdbBuilderMode(it) },
            )
        }

        item {
            // Only where the mode still needs explaining: the tab already names it, so a line
            // that restates the tab is noise. Read here rather than in the builder lambda around
            // this item, which is not a composable scope.
            val modeHelp = tmdbModeHelpText(state.tmdbBuilderMode)
            if (modeHelp.isNotBlank()) {
                Text(
                    text = modeHelp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Production and Collection are searched by name, so their input is a search bar rather
        // than a plain field, in the spec's baseline style: the divider under it separates the bar
        // from the results it returns, which stay in this page's list below.
        if (searchesByName) {
            item {
                NuvioSearchField(
                    query = state.tmdbInput,
                    onQueryChange = { CollectionEditorRepository.setTmdbInput(it) },
                    placeholder = tmdbInputPlaceholder(state.tmdbBuilderMode),
                    divided = true,
                    onSearch = {
                        if (sourceType == TmdbCollectionSourceType.COMPANY) {
                            CollectionEditorRepository.searchTmdbCompanies()
                        } else {
                            CollectionEditorRepository.searchTmdbCollections()
                        }
                    },
                )
            }
        }

        if (state.tmdbBuilderMode != TmdbBuilderMode.PRESETS) {
            item {
                // The form is one segmented group, fields included, so it reads as one thing
                // rather than fields floating inside a card.
                CollectionEditorGroup {
                    if (requiresId && !searchesByName) {
                        row { shape ->
                            PresetFieldRow(
                                label = tmdbInputLabel(state.tmdbBuilderMode),
                                value = state.tmdbInput,
                                onValueChange = { CollectionEditorRepository.setTmdbInput(it) },
                                shape = shape,
                                supportingText = tmdbInputHelper(state.tmdbBuilderMode),
                                placeholder = tmdbInputPlaceholder(state.tmdbBuilderMode),
                            )
                        }
                    }
                    row { shape ->
                        PresetFieldRow(
                            label = stringResource(Res.string.collections_editor_tmdb_display_title),
                            value = state.tmdbTitleInput,
                            onValueChange = { CollectionEditorRepository.setTmdbTitleInput(it) },
                            shape = shape,
                            supportingText = stringResource(Res.string.collections_editor_tmdb_title_helper),
                            placeholder = tmdbTitlePlaceholder(state.tmdbBuilderMode),
                        )
                    }
                }
            }

            if (state.tmdbSearchError != null) {
                item {
                    Text(
                        text = state.tmdbSearchError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Media type and sort are one choice each out of three or four, which is a row that opens
        // the app's single-choice sheet, not a row of chips.
        if (showMediaControls || showSortControls) {
            item {
                var showMediaSheet by remember { mutableStateOf(false) }
                var showSortSheet by remember { mutableStateOf(false) }
                val mediaOptions = tmdbMediaChoices()
                val sortOptions = tmdbSortChoices(state)
                val currentMedia = tmdbCurrentMediaChoice(state)

                CollectionEditorGroup {
                    if (showMediaControls) {
                        row { shape ->
                            CollectionEditorChoiceRow(
                                title = stringResource(Res.string.collections_editor_tmdb_type),
                                value = mediaOptions.first { it.value == currentMedia }.label,
                                onClick = { showMediaSheet = true },
                                shape = shape,
                            )
                        }
                    }
                    if (showSortControls) {
                        row { shape ->
                            CollectionEditorChoiceRow(
                                title = stringResource(Res.string.collections_editor_tmdb_sort),
                                value = sortOptions.firstOrNull { it.value == state.tmdbSortBy }?.label
                                    ?: sortOptions.first().label,
                                onClick = { showSortSheet = true },
                                shape = shape,
                            )
                        }
                    }
                }

                if (showMediaSheet) {
                    SingleChoiceBottomSheet(
                        title = stringResource(Res.string.collections_editor_tmdb_type),
                        options = mediaOptions,
                        isSelected = { it == currentMedia },
                        onSelected = { choice ->
                            CollectionEditorRepository.setTmdbMediaBoth(choice == TmdbMediaChoice.BOTH)
                            when (choice) {
                                TmdbMediaChoice.MOVIE ->
                                    CollectionEditorRepository.setTmdbMediaType(TmdbCollectionMediaType.MOVIE)
                                TmdbMediaChoice.TV ->
                                    CollectionEditorRepository.setTmdbMediaType(TmdbCollectionMediaType.TV)
                                TmdbMediaChoice.BOTH -> Unit
                            }
                        },
                        onDismiss = { showMediaSheet = false },
                    )
                }

                if (showSortSheet) {
                    SingleChoiceBottomSheet(
                        title = stringResource(Res.string.collections_editor_tmdb_sort),
                        options = sortOptions,
                        isSelected = { it == state.tmdbSortBy },
                        onSelected = { CollectionEditorRepository.setTmdbSortBy(it) },
                        onDismiss = { showSortSheet = false },
                    )
                }
            }
        }

        if (showFilterControls) {
            item {
                PickerSectionLabel(stringResource(Res.string.collections_editor_tmdb_filters))
            }
            item {
                Text(
                    text = stringResource(Res.string.collections_editor_tmdb_filters_helper),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                TmdbDiscoverFilters(state = state)
            }
        }

        if (state.tmdbBuilderMode == TmdbBuilderMode.PRODUCTION && state.tmdbCompanyResults.isNotEmpty()) {
            item {
                PickerSectionLabel(stringResource(Res.string.collections_editor_tmdb_search_results))
            }
            item {
                val movieSuffix = stringResource(Res.string.collections_editor_tmdb_movies)
                val seriesSuffix = stringResource(Res.string.collections_editor_tmdb_series)
                CollectionEditorGroup {
                    state.tmdbCompanyResults.forEach { result ->
                        row { shape ->
                        val title = result.name ?: stringResource(Res.string.collections_editor_tmdb_company_fallback, result.id)
                        PickerOptionRow(
                            title = title,
                            subtitle = listOfNotNull(
                                stringResource(Res.string.collections_editor_tmdb_subtitle_production),
                                result.originCountry,
                            ).joinToString(" • "),
                            shape = shape,
                            onClick = {
                        val sources = tmdbSelectedMediaTypes(state).map { mediaType ->
                            CollectionSource(
                                provider = "tmdb",
                                tmdbSourceType = TmdbCollectionSourceType.COMPANY.name,
                                title = tmdbTitleForMedia(title, mediaType, state.tmdbMediaBoth, movieSuffix, seriesSuffix),
                                tmdbId = result.id,
                                mediaType = mediaType.name,
                                sortBy = state.tmdbSortBy,
                                filters = state.tmdbFilters,
                            )
                        }
                                CollectionEditorRepository.addTmdbSourcesFromPicker(sources)
                            },
                        )
                        }
                    }
                }
            }
        }

        if (state.tmdbBuilderMode == TmdbBuilderMode.COLLECTION && state.tmdbCollectionResults.isNotEmpty()) {
            item {
                PickerSectionLabel(stringResource(Res.string.collections_editor_tmdb_search_results))
            }
            item {
                CollectionEditorGroup {
                    state.tmdbCollectionResults.forEach { result ->
                        row { shape ->
                            val title = result.name
                                ?: stringResource(Res.string.collections_editor_tmdb_collection_fallback, result.id)
                            PickerOptionRow(
                                title = title,
                                subtitle = stringResource(Res.string.collections_editor_tmdb_collection),
                                shape = shape,
                                onClick = {
                                    CollectionEditorRepository.addTmdbSource(
                                        CollectionSource(
                                            provider = "tmdb",
                                            tmdbSourceType = TmdbCollectionSourceType.COLLECTION.name,
                                            title = title,
                                            tmdbId = result.id,
                                            mediaType = TmdbCollectionMediaType.MOVIE.name,
                                            sortBy = state.tmdbSortBy,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (state.tmdbBuilderMode == TmdbBuilderMode.PRESETS) {
            item {
                // One group in one item. Emitted as separate lazy items they took the screen's
                // list gap between them and stopped reading as a list at all.
                CollectionEditorGroup {
                    TmdbCollectionSourceResolver.presets().forEach { preset ->
                        row { shape ->
                            PickerOptionRow(
                                title = preset.label,
                                subtitle = tmdbSourceSubtitle(preset.source),
                                shape = shape,
                                onClick = { CollectionEditorRepository.addTmdbPreset(preset.source) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Which of the three media choices a mode is on. The state holds it as a flag plus a type. */
private enum class TmdbMediaChoice { MOVIE, TV, BOTH }

@Composable
private fun tmdbMediaChoices(): List<SingleChoiceOption<TmdbMediaChoice>> = listOf(
    SingleChoiceOption(
        value = TmdbMediaChoice.MOVIE,
        label = stringResource(Res.string.collections_editor_tmdb_movies),
    ),
    SingleChoiceOption(
        value = TmdbMediaChoice.TV,
        label = stringResource(Res.string.collections_editor_tmdb_series),
    ),
    SingleChoiceOption(
        value = TmdbMediaChoice.BOTH,
        label = stringResource(Res.string.collections_editor_tmdb_both),
    ),
)

private fun tmdbCurrentMediaChoice(state: CollectionEditorUiState): TmdbMediaChoice = when {
    state.tmdbMediaBoth -> TmdbMediaChoice.BOTH
    state.tmdbMediaType == TmdbCollectionMediaType.TV -> TmdbMediaChoice.TV
    else -> TmdbMediaChoice.MOVIE
}

@Composable
private fun tmdbSortChoices(state: CollectionEditorUiState): List<SingleChoiceOption<String>> {
    // The date sort means a different field for series than for films, so only the one that
    // applies is offered rather than both.
    val dateSort = if (state.tmdbMediaType == TmdbCollectionMediaType.TV && !state.tmdbMediaBoth) {
        TmdbCollectionSort.FIRST_AIR_DATE_DESC
    } else {
        TmdbCollectionSort.RELEASE_DATE_DESC
    }
    return listOf(
        TmdbCollectionSort.POPULAR_DESC,
        TmdbCollectionSort.VOTE_AVERAGE_DESC,
        TmdbCollectionSort.VOTE_COUNT_DESC,
        dateSort,
    ).map { SingleChoiceOption(value = it.value, label = tmdbSortLabel(it)) }
}

/**
 * Choosing a catalog's genre. One value out of a list, which is what the app's single-choice
 * sheet is for, so it is that sheet rather than a second one built by hand: the selected row's
 * container and its 4dp-to-16dp corner morph come with it.
 */
@Composable
private fun GenrePickerSheet(
    title: String,
    selectedGenre: String?,
    genreOptions: List<String>,
    allowAll: Boolean,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    // "All genres" is the absence of a genre. The sheet deals only in option values, so it is
    // carried as the empty string and turned back into null on the way out.
    val allLabel = stringResource(Res.string.collections_editor_all_genres)
    val options = buildList {
        if (allowAll) add(SingleChoiceOption(value = "", label = allLabel))
        genreOptions.forEach { add(SingleChoiceOption(value = it, label = it)) }
    }

    SingleChoiceBottomSheet(
        title = stringResource(Res.string.collections_editor_genre_filter),
        options = options,
        isSelected = { it == selectedGenre.orEmpty() },
        onSelected = { onSelect(it.ifBlank { null }) },
        onDismiss = onDismiss,
        description = title,
    )
}

@Composable
private fun TraktSourcePickerScreen(
    state: CollectionEditorUiState,
    onBack: () -> Unit,
) {
    val searchResultsTitle = stringResource(Res.string.collections_editor_trakt_search_results)
    val trendingTitle = stringResource(Res.string.collections_editor_trakt_trending)
    val popularTitle = stringResource(Res.string.collections_editor_trakt_popular)

    PlatformBackHandler(enabled = true) {
        onBack()
    }

    NuvioScreen(
        title = if (state.editingTraktSourceIndex != null) {
                    stringResource(Res.string.collections_editor_edit_trakt_source)
                } else {
                    stringResource(Res.string.collections_editor_trakt_sources)
                },
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        bottomBar = {
            CollectionEditorActionBar {
                Button(
                    onClick = { CollectionEditorRepository.addTraktSourceFromInput() },
                    modifier = Modifier.weight(1f),
                    enabled = state.traktInput.isNotBlank(),
                ) {
                    Text(
                        text = if (state.editingTraktSourceIndex != null) {
                            stringResource(Res.string.collections_editor_save)
                        } else {
                            stringResource(Res.string.collections_editor_add_source)
                        },
                    )
                }
            }
        },
    ) {

        item {
            NuvioSearchField(
                query = state.traktInput,
                onQueryChange = { CollectionEditorRepository.setTraktInput(it) },
                placeholder = stringResource(Res.string.collections_editor_trakt_input_placeholder),
                divided = true,
                onSearch = { CollectionEditorRepository.searchTraktLists() },
            )
        }

        item {
            // Same shape as the TMDB form: one segmented group, fields included.
            CollectionEditorGroup {
                row { shape ->
                    PresetFieldRow(
                        label = stringResource(Res.string.collections_editor_tmdb_display_title),
                        value = state.traktTitleInput,
                        onValueChange = { CollectionEditorRepository.setTraktTitleInput(it) },
                        shape = shape,
                        supportingText = stringResource(Res.string.collections_editor_tmdb_title_helper),
                        placeholder = stringResource(Res.string.collections_editor_trakt_title_placeholder),
                    )
                }
            }
        }

        if (state.traktSearchError != null) {
            item {
                Text(
                    text = state.traktSearchError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            var showMediaSheet by remember { mutableStateOf(false) }
            var showSortSheet by remember { mutableStateOf(false) }
            var showDirectionSheet by remember { mutableStateOf(false) }
            val mediaOptions = tmdbMediaChoices()
            val currentMedia = when {
                state.traktMediaBoth -> TmdbMediaChoice.BOTH
                state.traktMediaType == TmdbCollectionMediaType.TV -> TmdbMediaChoice.TV
                else -> TmdbMediaChoice.MOVIE
            }
            val sortOptions = traktSortOptions().map { (value, label) ->
                SingleChoiceOption(value = value, label = label)
            }
            val ascending = stringResource(Res.string.collections_editor_trakt_ascending)
            val descending = stringResource(Res.string.collections_editor_trakt_descending)
            val directionOptions = listOf(
                SingleChoiceOption(value = TraktSortHow.ASC.value, label = ascending),
                SingleChoiceOption(value = TraktSortHow.DESC.value, label = descending),
            )

            CollectionEditorGroup {
                row { shape ->
                    CollectionEditorChoiceRow(
                        title = stringResource(Res.string.collections_editor_tmdb_type),
                        value = mediaOptions.first { it.value == currentMedia }.label,
                        onClick = { showMediaSheet = true },
                        shape = shape,
                    )
                }
                row { shape ->
                    CollectionEditorChoiceRow(
                        title = stringResource(Res.string.collections_editor_tmdb_sort),
                        value = sortOptions.firstOrNull { it.value == state.traktSortBy }?.label
                            ?: sortOptions.first().label,
                        onClick = { showSortSheet = true },
                        shape = shape,
                    )
                }
                row { shape ->
                    CollectionEditorChoiceRow(
                        title = stringResource(Res.string.collections_editor_trakt_direction),
                        value = if (state.traktSortHow == TraktSortHow.DESC.value) descending else ascending,
                        onClick = { showDirectionSheet = true },
                        shape = shape,
                    )
                }
            }

            if (showMediaSheet) {
                SingleChoiceBottomSheet(
                    title = stringResource(Res.string.collections_editor_tmdb_type),
                    options = mediaOptions,
                    isSelected = { it == currentMedia },
                    onSelected = { choice ->
                        CollectionEditorRepository.setTraktMediaBoth(choice == TmdbMediaChoice.BOTH)
                        when (choice) {
                            TmdbMediaChoice.MOVIE ->
                                CollectionEditorRepository.setTraktMediaType(TmdbCollectionMediaType.MOVIE)
                            TmdbMediaChoice.TV ->
                                CollectionEditorRepository.setTraktMediaType(TmdbCollectionMediaType.TV)
                            TmdbMediaChoice.BOTH -> Unit
                        }
                    },
                    onDismiss = { showMediaSheet = false },
                )
            }

            if (showSortSheet) {
                SingleChoiceBottomSheet(
                    title = stringResource(Res.string.collections_editor_tmdb_sort),
                    options = sortOptions,
                    isSelected = { it == state.traktSortBy },
                    onSelected = { CollectionEditorRepository.setTraktSortBy(it) },
                    onDismiss = { showSortSheet = false },
                )
            }

            if (showDirectionSheet) {
                SingleChoiceBottomSheet(
                    title = stringResource(Res.string.collections_editor_trakt_direction),
                    options = directionOptions,
                    isSelected = { it == state.traktSortHow },
                    onSelected = { CollectionEditorRepository.setTraktSortHow(it) },
                    onDismiss = { showDirectionSheet = false },
                )
            }
        }

        TraktResultSection(
            title = searchResultsTitle,
            results = state.traktSearchResults,
        )
        TraktResultSection(
            title = trendingTitle,
            results = state.traktTrendingResults,
        )
        TraktResultSection(
            title = popularTitle,
            results = state.traktPopularResults,
        )
    }
}

private fun LazyListScope.TraktResultSection(
    title: String,
    results: List<TraktPublicListSearchResult>,
) {
    if (results.isEmpty()) return
    item {
        PickerSectionLabel(title)
    }
    item {
        CollectionEditorGroup {
            results.forEach { result ->
                row { shape ->
                    PickerOptionRow(
                        title = result.title,
                        subtitle = result.subtitle,
                        shape = shape,
                        onClick = { CollectionEditorRepository.addTraktSourceFromResult(result) },
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PickerOptionRow(
    title: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    // A real list item rather than a tinted box: these rows are a list of things to pick, so they
    // take the group's container, its corners and its own press handling.
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        supportingContent = subtitle?.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
    ) {
        Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PickerSectionLabel(text: String) {
    // No uppercasing here: how a heading is capitalised is its translation's business.
    ListSubheader(
        text = text,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

/**
 * A segmented group in the editor, built the way the settings lists are.
 *
 * Rows are declared rather than composed directly, so the group knows how many there are before
 * it draws any of them, which is what lets a row that only appears sometimes still take the right
 * corner. Everything the group holds is a row: fields and empty states included, since they sit
 * inside the group's container rather than beside it.
 *
 * `@NonRestartableComposable` for the reason [com.nuvio.app.features.settings.SettingsList] gives:
 * the collect and the render have to happen in the same pass.
 */
@Composable
@NonRestartableComposable
private fun CollectionEditorGroup(
    modifier: Modifier = Modifier,
    content: @Composable CollectionEditorGroupScope.() -> Unit,
) {
    val scope = remember { CollectionEditorGroupScope() }
    scope.rows.clear()
    scope.content()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        scope.rows.forEachIndexed { index, row ->
            row(segmentShape(index = index, count = scope.rows.size))
        }
    }
}

private class CollectionEditorGroupScope {
    val rows = mutableListOf<@Composable (RoundedCornerShape) -> Unit>()

    /** Declares one row; it is handed the corner its position in the group gives it. */
    fun row(content: @Composable (RoundedCornerShape) -> Unit) {
        rows += content
    }
}

/**
 * A setting the editor picks one value for. The row states the current value and opens the app's
 * single-choice sheet, which is how every other picker in the app works.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionEditorChoiceRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(OuterCorner),
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        supportingContent = { Text(value) },
    ) {
        Text(title)
    }
}

/** The label for a folder's view mode, used by both the row and its sheet. */
@Composable
private fun folderViewModeLabel(mode: FolderViewMode): String = when (mode) {
    FolderViewMode.TABBED_GRID -> stringResource(Res.string.collections_editor_view_mode_tabs)
    else -> stringResource(Res.string.collections_editor_view_mode_rows)
}

/**
 * A setting the editor toggles. The expressive list item itself, so it carries the segmented
 * container, and tapping anywhere on the row flips the switch, which keeps the row to the spec's
 * one interaction per item. Each is its own group here, since the settings it toggles are
 * separated by the choice between them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollectionEditorSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(OuterCorner),
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    ) {
        Text(title)
    }
}


/**
 * The editor's docked actions. These pages are forms that run past the fold, so the action that
 * commits them sits in the screen's bottom bar rather than in the scrolling content, where it
 * would be reachable only from the end of the list.
 *
 * It is the screen's bottom bar rather than a surface overlaid on top of it: the scaffold then
 * measures the content's bottom padding from the bar, instead of the fixed spacer these pages used
 * to reserve, which was right only while the bar happened to be that tall. The bar is opaque for
 * the same reason the profile editor's is, so content scrolls out of sight beneath it, and it owns
 * the navigation bar inset as the bottom-most element.
 */
@Composable
private fun CollectionEditorActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = nuvioSafeBottomPadding(12.dp),
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}




@Composable
private fun tmdbGenreQuickChips(mediaType: TmdbCollectionMediaType): List<Pair<String, String>> =
    when (mediaType) {
        TmdbCollectionMediaType.MOVIE -> listOf(
            stringResource(Res.string.collections_editor_tmdb_genre_action) to "28",
            stringResource(Res.string.collections_editor_tmdb_genre_adventure) to "12",
            stringResource(Res.string.collections_editor_tmdb_genre_animation) to "16",
            stringResource(Res.string.collections_editor_tmdb_genre_comedy) to "35",
            stringResource(Res.string.collections_editor_tmdb_genre_horror) to "27",
            stringResource(Res.string.collections_editor_tmdb_genre_scifi) to "878",
        )
        TmdbCollectionMediaType.TV -> listOf(
            stringResource(Res.string.collections_editor_tmdb_genre_drama) to "18",
            stringResource(Res.string.collections_editor_tmdb_genre_comedy) to "35",
            stringResource(Res.string.collections_editor_tmdb_genre_animation) to "16",
            stringResource(Res.string.collections_editor_tmdb_genre_crime) to "80",
            stringResource(Res.string.collections_editor_tmdb_genre_scifi) to "10765",
            stringResource(Res.string.collections_editor_tmdb_genre_reality) to "10764",
        )
    }

private fun tmdbSelectedMediaTypes(state: CollectionEditorUiState): List<TmdbCollectionMediaType> =
    if (state.tmdbMediaBoth) {
        listOf(TmdbCollectionMediaType.MOVIE, TmdbCollectionMediaType.TV)
    } else {
        listOf(state.tmdbMediaType)
    }

private fun tmdbTitleForMedia(
    title: String,
    mediaType: TmdbCollectionMediaType,
    addSuffix: Boolean,
    movieSuffix: String,
    seriesSuffix: String,
): String {
    if (!addSuffix) return title
    val suffix = when (mediaType) {
        TmdbCollectionMediaType.MOVIE -> movieSuffix
        TmdbCollectionMediaType.TV -> seriesSuffix
    }
    return "$title $suffix"
}


@Composable
private fun FolderEditorSection(
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
private fun FolderTmdbSourceCard(
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
private fun FolderTraktSourceCard(
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
private fun FolderCatalogSourceCard(
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
private fun FolderSourceRow(
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

@Composable
private fun tmdbBuilderModeLabel(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.PRESETS -> stringResource(Res.string.collections_editor_tmdb_presets)
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_public_list_mode)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_production_mode)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_mode)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_mode)
        TmdbBuilderMode.PERSON -> stringResource(Res.string.collections_editor_tmdb_person_mode)
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_director_mode)
        TmdbBuilderMode.DISCOVER -> stringResource(Res.string.collections_editor_tmdb_custom_mode)
    }

@Composable
private fun tmdbModeHelpText(mode: TmdbBuilderMode): String =
    when (mode) {
        // The tab is named Presets and the list below it is the presets; nothing to add.
        TmdbBuilderMode.PRESETS -> ""
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_help_list)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_help_production)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_help_network)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_help_collection)
        TmdbBuilderMode.PERSON -> stringResource(Res.string.collections_editor_tmdb_help_person)
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_help_director)
        TmdbBuilderMode.DISCOVER -> stringResource(Res.string.collections_editor_tmdb_help_discover)
    }

@Composable
private fun tmdbInputLabel(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_public_list)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_id)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_id)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_company_search)
        TmdbBuilderMode.PERSON,
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_person_id)
        else -> stringResource(Res.string.collections_editor_tmdb_id_or_url)
    }

@Composable
private fun tmdbInputPlaceholder(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_list_placeholder)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_placeholder)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_placeholder)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_company_placeholder)
        TmdbBuilderMode.PERSON,
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_person_placeholder)
        else -> stringResource(Res.string.collections_editor_tmdb_id_or_url)
    }

@Composable
private fun tmdbInputHelper(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_search_helper)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_helper)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_helper)
        // The field's own label and placeholder already say what goes in it.
        TmdbBuilderMode.LIST -> ""
        TmdbBuilderMode.PERSON,
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_person_helper)
        else -> ""
    }

@Composable
private fun tmdbTitlePlaceholder(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.DISCOVER -> stringResource(Res.string.collections_editor_tmdb_discover_title_placeholder)
        TmdbBuilderMode.PERSON -> stringResource(Res.string.collections_editor_tmdb_person_title_placeholder)
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_director_title_placeholder)
        else -> stringResource(Res.string.collections_editor_tmdb_title_placeholder)
    }

@Composable
private fun tmdbSortLabel(sort: TmdbCollectionSort): String =
    when (sort) {
        TmdbCollectionSort.ORIGINAL -> stringResource(Res.string.collections_editor_tmdb_sort_original)
        TmdbCollectionSort.POPULAR_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_popular)
        TmdbCollectionSort.VOTE_AVERAGE_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_top_rated)
        TmdbCollectionSort.VOTE_COUNT_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_vote_count)
        TmdbCollectionSort.RELEASE_DATE_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_recent)
        TmdbCollectionSort.FIRST_AIR_DATE_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_recent)
    }

@Composable
private fun traktSortOptions(): List<Pair<String, String>> =
    listOf(
        TraktListSort.RANK.value to stringResource(Res.string.collections_editor_trakt_sort_list_order),
        TraktListSort.ADDED.value to stringResource(Res.string.collections_editor_trakt_sort_recently_added),
        TraktListSort.TITLE.value to stringResource(Res.string.collections_editor_trakt_sort_title),
        TraktListSort.RELEASED.value to stringResource(Res.string.collections_editor_trakt_sort_released),
        TraktListSort.RUNTIME.value to stringResource(Res.string.collections_editor_trakt_sort_runtime),
        TraktListSort.POPULARITY.value to stringResource(Res.string.collections_editor_trakt_sort_popular),
        TraktListSort.PERCENTAGE.value to stringResource(Res.string.collections_editor_trakt_sort_percentage),
        TraktListSort.VOTES.value to stringResource(Res.string.collections_editor_trakt_sort_votes),
    )

@Composable
private fun traktSortLabel(value: String?): String =
    when (TraktListSort.normalize(value)) {
        TraktListSort.ADDED.value -> stringResource(Res.string.collections_editor_trakt_sort_recently_added)
        TraktListSort.TITLE.value -> stringResource(Res.string.collections_editor_trakt_sort_title)
        TraktListSort.RELEASED.value -> stringResource(Res.string.collections_editor_trakt_sort_released)
        TraktListSort.RUNTIME.value -> stringResource(Res.string.collections_editor_trakt_sort_runtime)
        TraktListSort.POPULARITY.value -> stringResource(Res.string.collections_editor_trakt_sort_popular)
        TraktListSort.PERCENTAGE.value -> stringResource(Res.string.collections_editor_trakt_sort_percentage)
        TraktListSort.VOTES.value -> stringResource(Res.string.collections_editor_trakt_sort_votes)
        else -> stringResource(Res.string.collections_editor_trakt_sort_list_order)
    }

@Composable
private fun traktDirectionLabel(value: String?): String =
    when (TraktSortHow.normalize(value)) {
        TraktSortHow.DESC.value -> stringResource(Res.string.collections_editor_trakt_descending)
        else -> stringResource(Res.string.collections_editor_trakt_ascending)
    }

@Composable
private fun traktSourceSubtitle(source: CollectionSource): String {
    val media = when (TmdbCollectionMediaType.fromString(source.mediaType)) {
        TmdbCollectionMediaType.MOVIE -> stringResource(Res.string.collections_editor_tmdb_movies)
        TmdbCollectionMediaType.TV -> stringResource(Res.string.collections_editor_tmdb_series)
    }
    return listOf(
        media,
        traktSortLabel(source.sortBy),
        traktDirectionLabel(source.sortHow),
        stringResource(Res.string.collections_editor_trakt_list_id_format, source.traktListId ?: ""),
    ).joinToString(" • ")
}

@Composable
private fun tmdbSourceSubtitle(source: CollectionSource): String {
    val media = when (TmdbCollectionMediaType.fromString(source.mediaType)) {
        TmdbCollectionMediaType.MOVIE -> stringResource(Res.string.collections_editor_tmdb_movies)
        TmdbCollectionMediaType.TV -> stringResource(Res.string.collections_editor_tmdb_series)
    }
    val sort = source.sortBy?.let { value ->
        TmdbCollectionSort.entries.firstOrNull { it.value == value }?.let { sort ->
            tmdbSortLabel(sort)
        }
    } ?: stringResource(Res.string.collections_editor_tmdb_sort_popular)
    val sourceType = runCatching {
        TmdbCollectionSourceType.valueOf(source.tmdbSourceType.orEmpty())
    }.getOrDefault(TmdbCollectionSourceType.DISCOVER)
    return when (sourceType) {
        TmdbCollectionSourceType.LIST -> stringResource(Res.string.collections_editor_tmdb_subtitle_list)
        TmdbCollectionSourceType.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_subtitle_movie_collection)
        TmdbCollectionSourceType.COMPANY -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_production),
            media,
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.NETWORK -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_network),
            stringResource(Res.string.collections_editor_tmdb_series),
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.PERSON -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_person),
            media,
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.DIRECTOR -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_director),
            media,
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.DISCOVER -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_discover),
            media,
            sort,
        ).joinToString(" • ")
    }
}

@Composable
internal fun posterShapeLabel(shape: PosterShape): String =
    when (shape) {
        PosterShape.Poster -> stringResource(Res.string.collections_editor_shape_poster)
        PosterShape.Square -> stringResource(Res.string.collections_editor_shape_square)
        PosterShape.Landscape -> stringResource(Res.string.collections_editor_shape_wide)
    }


/**
 * The new-folder row's lazy key. A constant, so it can never collide with a folder's own id, which
 * is what the rows below it are keyed by.
 */
private const val NEW_FOLDER_ROW_KEY = "new-folder-row"

/**
 * Discover's filters, one row each.
 *
 * Every filter that had a shortlist used to render that shortlist as a row of chips and then a
 * text field underneath for the same value, so the panel said everything twice and ran to eight
 * screens. Here a filter is one row: the field holds the value, and where a shortlist exists it
 * sits behind the field's own trailing button.
 */
@Composable
private fun TmdbDiscoverFilters(state: CollectionEditorUiState) {
    val filters = state.tmdbFilters

    CollectionEditorGroup {
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_genres),
                value = filters.withGenres.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withGenres = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_genres_helper),
                placeholder = if (state.tmdbMediaType == TmdbCollectionMediaType.MOVIE) {
                    stringResource(Res.string.collections_editor_tmdb_genres_movie_placeholder)
                } else {
                    stringResource(Res.string.collections_editor_tmdb_genres_series_placeholder)
                },
                presets = tmdbGenreQuickChips(state.tmdbMediaType),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_genres),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_genres),
                value = filters.withoutGenres.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutGenres = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_genres_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_genres_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_date_from),
                value = filters.releaseDateGte.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(releaseDateGte = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_date_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_date_from_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_date_to),
                value = filters.releaseDateLte.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(releaseDateLte = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_date_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_date_to_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_rating_min),
                value = filters.voteAverageGte?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(voteAverageGte = value.toDoubleOrNull())
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_rating_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_rating_min_placeholder),
                keyboardType = KeyboardType.Decimal,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_rating_max),
                value = filters.voteAverageLte?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(voteAverageLte = value.toDoubleOrNull())
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_rating_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_rating_max_placeholder),
                keyboardType = KeyboardType.Decimal,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_votes_min),
                value = filters.voteCountGte?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(voteCountGte = value.toIntOrNull())
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_votes_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_votes_min_placeholder),
                keyboardType = KeyboardType.Number,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_year),
                value = filters.year?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters { it.copy(year = value.toIntOrNull()) }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_year_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_year_placeholder),
                keyboardType = KeyboardType.Number,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_language),
                value = filters.withOriginalLanguage.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withOriginalLanguage = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_language_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_language_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_language_english) to "en",
                    stringResource(Res.string.collections_editor_tmdb_language_korean) to "ko",
                    stringResource(Res.string.collections_editor_tmdb_language_japanese) to "ja",
                    stringResource(Res.string.collections_editor_tmdb_language_hindi) to "hi",
                    stringResource(Res.string.collections_editor_tmdb_language_spanish) to "es",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_languages),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_country),
                value = filters.withOriginCountry.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withOriginCountry = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_country_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_country_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_country_us) to "US",
                    stringResource(Res.string.collections_editor_tmdb_country_korea) to "KR",
                    stringResource(Res.string.collections_editor_tmdb_country_japan) to "JP",
                    stringResource(Res.string.collections_editor_tmdb_country_india) to "IN",
                    stringResource(Res.string.collections_editor_tmdb_country_uk) to "GB",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_countries),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_keywords),
                value = filters.withKeywords.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withKeywords = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_keywords_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_keywords_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_keyword_superhero) to "9715",
                    stringResource(Res.string.collections_editor_tmdb_keyword_based_on_novel) to "818",
                    stringResource(Res.string.collections_editor_tmdb_keyword_time_travel) to "4379",
                    stringResource(Res.string.collections_editor_tmdb_keyword_space) to "9882",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_keywords),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_keywords),
                value = filters.withoutKeywords.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutKeywords = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_keywords_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_keywords_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_companies),
                value = filters.withCompanies.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withCompanies = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_companies_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_companies_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_studio_marvel) to "420",
                    stringResource(Res.string.collections_editor_tmdb_studio_disney) to "2",
                    stringResource(Res.string.collections_editor_tmdb_studio_pixar) to "3",
                    stringResource(Res.string.collections_editor_tmdb_studio_lucasfilm) to "1",
                    stringResource(Res.string.collections_editor_tmdb_studio_warner) to "174",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_studios),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_companies),
                value = filters.withoutCompanies.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutCompanies = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_companies_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_companies_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_networks),
                value = filters.withNetworks.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withNetworks = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_networks_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_networks_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_network_netflix) to "213",
                    stringResource(Res.string.collections_editor_tmdb_network_hbo) to "49",
                    stringResource(Res.string.collections_editor_tmdb_network_disney_plus) to "2739",
                    stringResource(Res.string.collections_editor_tmdb_network_prime_video) to "1024",
                    stringResource(Res.string.collections_editor_tmdb_network_hulu) to "453",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_networks),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_watch_providers),
                value = filters.withWatchProviders.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withWatchProviders = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_watch_providers_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_watch_providers_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_netflix) to "8",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_prime) to "119",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_disney) to "337",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_apple) to "350",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_hulu) to "15",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_watch_providers),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_watch_providers),
                value = filters.withoutWatchProviders.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutWatchProviders = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_watch_providers_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_watch_providers_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_watch_region),
                value = filters.watchRegion.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(watchRegion = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_watch_region_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_country_us),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_country_us) to "US",
                    stringResource(Res.string.collections_editor_tmdb_country_uk) to "GB",
                    stringResource(Res.string.collections_editor_tmdb_country_ca) to "CA",
                    stringResource(Res.string.collections_editor_tmdb_country_au) to "AU",
                    stringResource(Res.string.collections_editor_tmdb_country_de) to "DE",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_watch_regions),
            )
        }
    }
}
