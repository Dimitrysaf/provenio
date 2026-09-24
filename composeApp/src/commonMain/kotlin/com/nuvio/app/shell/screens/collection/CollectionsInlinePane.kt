package com.nuvio.app.shell.screens.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * The collections flow, as a pane rather than a page.
 *
 * On a wide window the settings sidebar stays put and its pages open beside it, but collections
 * were a navigation destination of their own, so opening them pushed a full-window screen over
 * that sidebar. Hosting the same screens here keeps them in the content pane where every other
 * settings page lives.
 *
 * The step between the list and the editor is held here instead of on the back stack, because a
 * pane has no back stack of its own. The editor's own pages (a folder, a source picker) are not:
 * passing `initialPage = null` leaves it to drive those from its repository state, exactly as it
 * does when the window is too narrow for this pane to exist.
 */
@Composable
internal fun CollectionsInlinePane(onExit: () -> Unit) {
    var editingCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }

    if (editorOpen) {
        CollectionEditorScreen(
            collectionId = editingCollectionId,
            onBack = { editorOpen = false },
            initialPage = null,
        )
    } else {
        CollectionManagementScreen(
            onBack = onExit,
            onNavigateToEditor = { collectionId ->
                editingCollectionId = collectionId
                editorOpen = true
            },
        )
    }
}
