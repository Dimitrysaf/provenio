@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import io.github.dimitrysaf.provenio.shell.components.M3Motion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.docs.Docs
import io.github.dimitrysaf.provenio.core.deeplink.handleAppUrl
import io.github.dimitrysaf.provenio.core.docs.DocsPageResult
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import io.github.dimitrysaf.provenio.shell.components.markdown.MarkdownDocument
import io.github.dimitrysaf.provenio.shell.components.markdown.MarkdownPictures
import io.github.dimitrysaf.provenio.shell.components.markdown.markdownItems
import io.github.dimitrysaf.provenio.shell.components.markdown.parseMarkdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** The wiki pages the app links to from its own screens. */
internal object DocsPages {
    const val Addons = "Addons"
    const val Torrents = "Torrents"
}

/** Which documentation page is open over the app, if any, as the "?" buttons open them. */
internal object DocsViewer {
    private val _page = MutableStateFlow<String?>(null)
    val page: StateFlow<String?> = _page.asStateFlow()
    private var onClose: (() -> Unit)? = null

    /** Opens [name]; [onClose] runs once it is closed, for whatever stepped aside for it to come back. */
    fun open(name: String, onClose: (() -> Unit)? = null) {
        this.onClose = onClose
        _page.value = name
    }

    fun close() {
        _page.value = null
        val closed = onClose
        onClose = null
        closed?.invoke()
    }
}

/** A "?" that opens [page] of the documentation over the app, as an app bar or a sheet header shows it. */
@Composable
internal fun DocsHelpButton(page: String, beforeOpen: (() -> Unit)? = null, onClose: (() -> Unit)? = null) {
    val label = stringResource(Res.string.docs_help)
    WithTooltip(label) {
        IconButton(
            onClick = {
                beforeOpen?.invoke()
                DocsViewer.open(page, onClose)
            },
        ) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = label)
        }
    }
}

/**
 * Opens a link from a documentation page that leads out of it. An addon's `stremio://` install link
 * installs the addon here, as it would coming from a browser, so a page can offer one-tap installs;
 * anything else opens outside the app.
 */
internal fun openDocsLink(link: String, uriHandler: UriHandler) {
    if (link.startsWith("stremio://", ignoreCase = true)) {
        DocsViewer.close()
        handleAppUrl(link)
    } else {
        runCatching { uriHandler.openUri(link) }
    }
}

/**
 * Moving between documentation pages, as a trail of them: a page further in slides in from the
 * end and one back from the start, the way the app's own pages move. The height is not animated,
 * since pages of any length follow one another.
 */
@Composable
internal fun rememberDocsSlide(): AnimatedContentTransitionScope<List<String>>.() -> ContentTransform {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val slidePx = with(density) { M3Motion.SharedAxisSlide.roundToPx() } * if (rtl) -1 else 1
    return remember(slidePx) {
        { M3Motion.sharedAxisX(forward = targetState.size >= initialState.size, slidePx = slidePx) using null }
    }
}

/** The open documentation page, drawn over everything else in the app. */
@Composable
internal fun DocsPageHost() {
    val page by DocsViewer.page.collectAsStateWithLifecycle()
    DocsPageOverlay(page = page, onDismiss = DocsViewer::close)
}

/** A page being read: its document once there is one, and otherwise why there is none yet. */
@Stable
internal class LoadedDocsPage(
    val document: MarkdownDocument?,
    val result: DocsPageResult?,
    val retry: () -> Unit,
)

/** Loads [page], showing the copy at hand at once and the newer one when it arrives. */
@Composable
internal fun rememberDocsPage(page: String): LoadedDocsPage {
    LaunchedEffect(Unit) { Docs.refresh() }
    val docs by Docs.state.collectAsStateWithLifecycle()
    var result by remember(page) { mutableStateOf<DocsPageResult?>(null) }
    var attempt by remember(page) { mutableStateOf(0) }
    LaunchedEffect(page, attempt) {
        result = null
        result = Docs.page(page)
    }
    val text = docs.texts[page] ?: (result as? DocsPageResult.Found)?.text
    val document = remember(page, text) { text?.let { parseMarkdown(it, Docs.pageBaseUrl(page)) } }
    return LoadedDocsPage(document = document, result = result, retry = { attempt++ })
}

/** The offline note while a newer copy of what is shown cannot be fetched; the pull indicator shows the fetching. */
@Composable
internal fun DocsOfflineLine(modifier: Modifier = Modifier) {
    val docs by Docs.state.collectAsStateWithLifecycle()
    if (docs.offline && !docs.loading) OfflineNotice(onRetry = ::refreshDocs, modifier = modifier)
}

/** What stands in for a page that has no document yet: loading, missing, or out of reach. */
@Composable
internal fun DocsPageMessage(page: LoadedDocsPage) {
    when (page.result) {
        DocsPageResult.Missing -> EmptyState(
            icon = Icons.Rounded.Description,
            title = stringResource(Res.string.docs_page_missing),
            message = stringResource(Res.string.docs_page_missing_message),
        )
        DocsPageResult.Unreachable -> EmptyState(
            icon = Icons.Rounded.CloudOff,
            title = stringResource(Res.string.docs_page_unavailable),
            message = stringResource(Res.string.docs_unavailable_message),
            actionLabel = stringResource(Res.string.action_retry),
            onActionClick = page.retry,
        )
        else -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
            LoadingSpinner()
        }
    }
}

/** Fetches every page and picture again, as pulling the documentation down asks. */
internal fun refreshDocs() {
    MarkdownPictures.renew()
    Docs.refresh(force = true)
}

/** The app bar's button that opens [page] on GitHub, or the whole wiki without one. */
@Composable
internal fun OpenDocsOnGitHubButton(page: String?) {
    val uriHandler = LocalUriHandler.current
    val label = stringResource(Res.string.docs_open_on_github)
    WithTooltip(label) {
        IconButton(onClick = { runCatching { uriHandler.openUri(page?.let(Docs::pageWebUrl) ?: Docs.WikiUrl) } }) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = label)
        }
    }
}

/**
 * A documentation page over whatever is open, as the player's More settings is: a page of its own
 * with a title bar, and what is underneath, a playing video included, carries on. Links to other
 * pages open in it, and Back returns to the page before, then to where it was opened from.
 */
@Composable
private fun DocsPageOverlay(page: String?, onDismiss: () -> Unit) {
    var history by remember { mutableStateOf(emptyList<String>()) }
    // A new page starts a new trail; closing keeps the last one, so it stays drawn while it slides away.
    LaunchedEffect(page) { if (page != null) history = listOf(page) }
    val visible = page != null
    val back: () -> Unit = { if (history.size > 1) history = history.dropLast(1) else onDismiss() }
    // Added only while a page is open, so it is the newest back handler and comes before those of
    // the screen underneath; one kept from app start would be the oldest, and the last asked.
    if (visible) PlatformBackHandler(enabled = true, onBack = back)
    AnimatedVisibility(
        visible = visible && history.isNotEmpty(),
        enter = slideInHorizontally { it / 4 } + fadeIn(),
        exit = slideOutHorizontally { it / 4 } + fadeOut(),
    ) {
        AnimatedContent(
            targetState = history,
            transitionSpec = rememberDocsSlide(),
            contentKey = { it.lastOrNull() },
            label = "docs_overlay_page",
        ) { shown ->
            val current = shown.lastOrNull() ?: return@AnimatedContent
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                OverlayPage(
                    page = current,
                    onBack = back,
                    onOpenPage = { name -> if (!name.equals(current, ignoreCase = true)) history = history + name },
                )
            }
        }
    }
}

@Composable
private fun OverlayPage(page: String, onBack: () -> Unit, onOpenPage: (String) -> Unit) {
    val docs by Docs.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val title = docs.pages.firstOrNull { it.name.equals(page, ignoreCase = true) }?.title ?: page.replace('-', ' ')

    // Each page starts at its top, so going to another page and back does not keep the old place.
    key(page) {
        val loaded = rememberDocsPage(page)
        val document = loaded.document
        val listState = rememberLazyListState()
        ScreenScaffold(
            title = title,
            onBack = onBack,
            listState = listState,
            contentMaxWidth = SettingsContentMaxWidth,
            actions = { OpenDocsOnGitHubButton(page) },
            refreshing = docs.loading,
            onRefresh = ::refreshDocs,
        ) {
            if (document == null) {
                item { DocsPageMessage(loaded) }
            } else {
                item { DocsOfflineLine(modifier = Modifier.padding(bottom = 16.dp)) }
                markdownItems(document) { link ->
                    val anchor = link.removePrefix("#").takeIf { link.startsWith("#") }
                    val linkedPage = Docs.pageNameOf(link)
                    when {
                        // The line above the page comes first, so a block's item is one further on.
                        anchor != null -> document.anchors[anchor]?.let { index ->
                            scope.launch { listState.animateScrollToItem(index + 1) }
                        }
                        linkedPage != null -> onOpenPage(linkedPage)
                        else -> openDocsLink(link, uriHandler)
                    }
                }
            }
        }
    }
}
