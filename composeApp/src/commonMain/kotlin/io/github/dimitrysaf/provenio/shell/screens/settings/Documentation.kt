@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.docs.Docs
import io.github.dimitrysaf.provenio.core.docs.DocsState
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.SearchField
import io.github.dimitrysaf.provenio.shell.components.markdown.MarkdownContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * The pages read on the Documentation page, from the first opened from the list to the one shown;
 * empty while the list shows. It lives outside the page so its app bar can follow it too.
 */
internal object DocsBrowser {
    private val _trail = MutableStateFlow<List<String>>(emptyList())
    val trail: StateFlow<List<String>> = _trail.asStateFlow()

    fun reset() {
        _trail.value = emptyList()
    }

    fun open(name: String) {
        if (!name.equals(_trail.value.lastOrNull(), ignoreCase = true)) _trail.value = _trail.value + name
    }

    /** Goes back to the [depth]th crumb, the list itself being the first. */
    fun goTo(depth: Int) {
        _trail.value = _trail.value.take(depth)
    }

    /** Steps back a page, or returns false when the list already shows. */
    fun back(): Boolean {
        if (_trail.value.isEmpty()) return false
        _trail.value = _trail.value.dropLast(1)
        return true
    }
}

/** The Documentation page's own app bar button: the page being read on GitHub, or the whole wiki. */
@Composable
internal fun DocumentationAppBarActions() {
    val trail by DocsBrowser.trail.collectAsStateWithLifecycle()
    OpenDocsOnGitHubButton(trail.lastOrNull())
}

/**
 * About → Documentation: every page of the wiki by its sidebar's sections, and a search through
 * them. A page opens right here, under a breadcrumb bar of the pages that led to it.
 */
internal fun LazyListScope.documentationContent(isTablet: Boolean) {
    item { DocumentationBody(isTablet = isTablet) }
}

@Composable
private fun DocumentationBody(isTablet: Boolean) {
    LaunchedEffect(Unit) { Docs.refresh() }
    val trail by DocsBrowser.trail.collectAsStateWithLifecycle()
    PlatformBackHandler(enabled = trail.isNotEmpty()) { DocsBrowser.back() }
    val current = trail.lastOrNull()
    if (current == null) {
        DocumentationIndex(isTablet = isTablet)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DocsBreadcrumbs(trail)
            // Each page starts at its own top, so a page before it does not leave its place behind.
            key(current) {
                val loaded = rememberDocsPage(current)
                val document = loaded.document
                if (document == null) {
                    DocsPageMessage(loaded)
                } else {
                    val uriHandler = LocalUriHandler.current
                    DocsOfflineLine()
                    MarkdownContent(
                        document = document,
                        onLinkClick = { link ->
                            val linkedPage = Docs.pageNameOf(link)
                            if (linkedPage != null) DocsBrowser.open(linkedPage) else openDocsLink(link, uriHandler)
                        },
                    )
                }
            }
        }
    }
}

/** Where the reader is: Documentation › the page opened › the pages its links led to, each a way back. */
@Composable
private fun DocsBreadcrumbs(trail: List<String>) {
    val docs by Docs.state.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crumb(text = stringResource(Res.string.settings_documentation), current = false) { DocsBrowser.goTo(0) }
        trail.forEachIndexed { index, name ->
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Crumb(text = docs.titleOf(name), current = index == trail.lastIndex) { DocsBrowser.goTo(index + 1) }
        }
    }
}

@Composable
private fun Crumb(text: String, current: Boolean, onClick: () -> Unit) {
    if (current) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    } else {
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun DocsState.titleOf(name: String): String =
    pages.firstOrNull { it.name.equals(name, ignoreCase = true) }?.title ?: name.replace('-', ' ')

@Composable
private fun DocumentationIndex(isTablet: Boolean) {
    val state by Docs.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val hasCopy = state.texts.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(Res.string.docs_search),
            modifier = Modifier.fillMaxWidth(),
        )
        // Saved pages shown while the wiki cannot be reached; the pull indicator shows a refresh.
        if (hasCopy) DocsOfflineLine()
        when {
            query.isNotBlank() -> {
                val results = remember(query, state) { Docs.search(query) }
                if (results.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(Res.string.docs_no_results),
                        message = stringResource(Res.string.docs_no_results_message, query.trim()),
                    )
                } else {
                    SettingsList {
                        results.forEach { result ->
                            navigationRow(
                                title = result.page.title,
                                description = result.snippet,
                                icon = Icons.Rounded.Description,
                                opensPage = true,
                                onClick = { DocsBrowser.open(result.page.name) },
                            )
                        }
                    }
                }
            }
            hasCopy -> {
                // The sidebar's sections in its order; pages above its first heading stand untitled.
                val sections = remember(state.pages) { state.pages.groupBy { it.section } }
                sections.forEach { (section, pages) ->
                    SettingsSection(title = section.orEmpty(), isTablet = isTablet, showTitle = section != null) {
                        SettingsList {
                            pages.forEach { page ->
                                navigationRow(
                                    title = page.title,
                                    icon = Icons.Rounded.Description,
                                    opensPage = true,
                                    onClick = { DocsBrowser.open(page.name) },
                                )
                            }
                        }
                    }
                }
            }
            state.offline && !state.loading -> EmptyState(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(Res.string.docs_unavailable),
                message = stringResource(Res.string.docs_unavailable_message),
                actionLabel = stringResource(Res.string.action_retry),
                onActionClick = { Docs.refresh(force = true) },
            )
            state.empty && !state.loading -> EmptyState(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                title = stringResource(Res.string.docs_empty),
                message = stringResource(Res.string.docs_empty_message),
            )
            // Nothing saved yet and the first fetch still on its way.
            else -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingSpinner()
            }
        }
    }
}

/** Says the pages shown are the ones saved before, as the wiki cannot be reached now. */
@Composable
internal fun OfflineNotice(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp).size(20.dp),
        )
        Text(
            text = stringResource(Res.string.docs_offline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}
