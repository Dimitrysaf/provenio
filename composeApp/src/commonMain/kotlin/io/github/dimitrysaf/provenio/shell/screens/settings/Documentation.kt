@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.docs.Docs
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.SearchField
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** About → Documentation: every page of the wiki by its sidebar's sections, and a search through them. */
internal fun LazyListScope.documentationContent(isTablet: Boolean) {
    item { DocumentationBody(isTablet = isTablet) }
}

@Composable
private fun DocumentationBody(isTablet: Boolean) {
    LaunchedEffect(Unit) { Docs.refresh() }
    val state by Docs.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var openPage by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val openOnGitHub = stringResource(Res.string.docs_open_on_github)
    val hasCopy = state.texts.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(Res.string.docs_search),
            modifier = Modifier.fillMaxWidth(),
        )
        // A refresh over pages already shown, or saved pages shown while the wiki cannot be reached.
        when {
            state.loading && hasCopy -> LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.offline && hasCopy -> OfflineNotice(onRetry = { Docs.refresh(force = true) })
        }
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
                                onClick = { openPage = result.page.name },
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
                                    onClick = { openPage = page.name },
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
        SettingsList {
            navigationRow(
                title = openOnGitHub,
                icon = Icons.Rounded.Public,
                trailingContent = { Icon(imageVector = Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                onClick = { runCatching { uriHandler.openUri(Docs.WikiUrl) } },
            )
        }
    }

    openPage?.let { page -> DocsSheet(page = page, onDismiss = { openPage = null }) }
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
