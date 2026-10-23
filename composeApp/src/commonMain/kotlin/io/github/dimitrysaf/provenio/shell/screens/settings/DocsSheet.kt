@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.docs.Docs
import io.github.dimitrysaf.provenio.core.docs.DocsPageResult
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.markdown.markdownItems
import io.github.dimitrysaf.provenio.shell.components.markdown.parseMarkdown
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * A documentation page over whatever is open: a bottom sheet on phones, a side sheet on wide
 * screens. Links to other pages open in the sheet, and Back returns to the page before.
 */
@Composable
internal fun DocsSheet(page: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss) } }
    var history by remember { mutableStateOf(listOf(page)) }
    val current = history.last()
    val back: () -> Unit = { if (history.size > 1) history = history.dropLast(1) else close() }
    LaunchedEffect(Unit) { Docs.refresh() }
    PlatformBackHandler(enabled = history.size > 1) { history = history.dropLast(1) }

    val docs by Docs.state.collectAsStateWithLifecycle()
    val title = docs.pages.firstOrNull { it.name.equals(current, ignoreCase = true) }?.title ?: current.replace('-', ' ')
    val uriHandler = LocalUriHandler.current
    val openInBrowser = stringResource(Res.string.docs_open_on_github)

    ModalSheet(onDismissRequest = close, sheetState = sheetState, fullHeight = true) {
        SheetHeader(
            title = title,
            navigation = if (history.size > 1) SheetNavigation.Back else SheetNavigation.Close,
            onNavigate = back,
            actions = {
                WithTooltip(openInBrowser) {
                    IconButton(onClick = { runCatching { uriHandler.openUri(Docs.pageWebUrl(current)) } }) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = openInBrowser)
                    }
                }
            },
        )
        // Each page starts at its top, so going to another page and back does not keep the old place.
        key(current) {
            var result by remember { mutableStateOf<DocsPageResult?>(null) }
            var attempt by remember { mutableStateOf(0) }
            LaunchedEffect(attempt) {
                result = null
                result = Docs.page(current)
            }
            val text = docs.texts[current] ?: (result as? DocsPageResult.Found)?.text
            val document = remember(text) { text?.let { parseMarkdown(it, Docs.pageBaseUrl(current)) } }
            val listState = rememberLazyListState()
            // A newer copy is on its way over the one shown.
            if (document != null && docs.loading) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = BottomSheetBodyMargin))
            } else if (document != null && docs.offline) {
                OfflineNotice(
                    onRetry = { Docs.refresh(force = true) },
                    modifier = Modifier.padding(horizontal = BottomSheetBodyMargin),
                )
            }
            when {
                document != null -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = BottomSheetBodyMargin, end = BottomSheetBodyMargin, top = 8.dp, bottom = 32.dp),
                ) {
                    markdownItems(document) { link ->
                        val anchor = link.removePrefix("#").takeIf { link.startsWith("#") }
                        val linkedPage = Docs.pageNameOf(link)
                        when {
                            anchor != null -> document.anchors[anchor]?.let { scope.launch { listState.animateScrollToItem(it) } }
                            linkedPage != null -> if (!linkedPage.equals(current, ignoreCase = true)) history = history + linkedPage
                            else -> runCatching { uriHandler.openUri(link) }
                        }
                    }
                }
                result == DocsPageResult.Missing -> EmptyState(
                    icon = Icons.Rounded.Description,
                    title = stringResource(Res.string.docs_page_missing),
                    message = stringResource(Res.string.docs_page_missing_message),
                )
                result == DocsPageResult.Unreachable -> EmptyState(
                    icon = Icons.Rounded.CloudOff,
                    title = stringResource(Res.string.docs_page_unavailable),
                    message = stringResource(Res.string.docs_unavailable_message),
                    actionLabel = stringResource(Res.string.action_retry),
                    onActionClick = { attempt++ },
                )
                else -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    LoadingSpinner()
                }
            }
        }
    }
}
