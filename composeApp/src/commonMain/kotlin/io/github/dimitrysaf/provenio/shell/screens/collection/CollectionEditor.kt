package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.shell.components.TextPromptDialog
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorRepository
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorUiState
import io.github.dimitrysaf.provenio.core.collection.FolderViewMode
import io.github.dimitrysaf.provenio.core.collection.findAvailableCatalog

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

private fun CollectionEditorUiState.activeEditorPage(): CollectionEditorPage = when {
    // No pickers here: they are sheets over the folder editor, so the editor stays the page.
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
    /** Saves the whole collection when its folder is saved, for an editor opened on one folder. */
    saveCollectionWithFolder: Boolean = false,
    onNavigateToPage: ((page: CollectionEditorPage, title: String) -> Unit)? = null,
) {
    val state by CollectionEditorRepository.uiState.collectAsState()

    LaunchedEffect(collectionId, initializeRepository) {
        if (initializeRepository) {
            CollectionEditorRepository.initialize(collectionId)
        }
    }

    val page = initialPage ?: state.activeEditorPage()

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
                if (saveCollectionWithFolder) CollectionEditorRepository.save()
                if (initialPage == CollectionEditorPage.FolderEditor) {
                    onBack()
                }
            },
        )

        if (state.showTmdbSourcePicker) {
            TmdbSourceSheet(state = state, onDismiss = { CollectionEditorRepository.hideTmdbSourcePicker() })
        }
        if (state.showTraktSourcePicker) {
            TraktSourceSheet(state = state, onDismiss = { CollectionEditorRepository.hideTraktSourcePicker() })
        }
        if (state.showSimklSourcePicker) {
            SimklSourceSheet(state = state, onDismiss = { CollectionEditorRepository.hideSimklSourcePicker() })
        }

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

    if (
        page == CollectionEditorPage.CatalogPicker ||
        page == CollectionEditorPage.TmdbSourcePicker ||
        page == CollectionEditorPage.TraktSourcePicker
    ) {
        // The pickers are sheets on the folder editor now. The enum entries stay so a back stack
        // saved before the change still deserializes; a route that reaches one closes itself.
        LaunchedEffect(Unit) {
            disposeCollectionEditorPage(page)
            onBack()
        }
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

    StatusModal(
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

    ScreenScaffold(
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
            // What the collection is called leads the page; a collection has no picture, its
            // folders do.
            var showRenameDialog by remember { mutableStateOf(false) }

            EditableIdentityHeader(
                title = state.title,
                titlePlaceholder = stringResource(Res.string.collections_editor_placeholder_name),
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

        }

        item {
            // The options below the header are one segmented group.
            var showViewModeSheet by remember { mutableStateOf(false) }
            val modes = FolderViewMode.entries.filter { it != FolderViewMode.FOLLOW_LAYOUT }

            CollectionEditorGroup {
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

/** The label for a folder's view mode, used by both the row and its sheet. */
@Composable
private fun folderViewModeLabel(mode: FolderViewMode): String = when (mode) {
    FolderViewMode.TABBED_GRID -> stringResource(Res.string.collections_editor_view_mode_tabs)
    else -> stringResource(Res.string.collections_editor_view_mode_rows)
}
