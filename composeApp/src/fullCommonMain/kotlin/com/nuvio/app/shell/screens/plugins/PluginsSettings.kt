package com.nuvio.app.shell.screens.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.ListSubheader
import com.nuvio.app.shell.components.NewEntryRow
import com.nuvio.app.shell.components.NuvioStatusModal
import com.nuvio.app.shell.components.TextPromptDialog
import com.nuvio.app.core.plugins.runtime.PluginRuntime
import com.nuvio.app.shell.screens.settings.ListItemBetweenSpace
import com.nuvio.app.shell.screens.settings.SettingsList
import com.nuvio.app.shell.screens.settings.segmentShape
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.action_done
import nuvio.composeapp.generated.resources.plugins_add_repo_row
import nuvio.composeapp.generated.resources.plugins_badge_providers
import nuvio.composeapp.generated.resources.plugins_badge_refreshing
import nuvio.composeapp.generated.resources.plugins_button_install_repo
import nuvio.composeapp.generated.resources.plugins_button_installing
import nuvio.composeapp.generated.resources.plugins_button_test_provider
import nuvio.composeapp.generated.resources.plugins_button_testing
import nuvio.composeapp.generated.resources.plugins_cd_delete_repo
import nuvio.composeapp.generated.resources.plugins_cd_refresh_repo
import nuvio.composeapp.generated.resources.plugins_empty_providers
import nuvio.composeapp.generated.resources.plugins_empty_providers_subtitle
import nuvio.composeapp.generated.resources.plugins_empty_repos_subtitle
import nuvio.composeapp.generated.resources.plugins_empty_repos_title
import nuvio.composeapp.generated.resources.plugins_enable_globally_desc
import nuvio.composeapp.generated.resources.plugins_enable_globally_title
import nuvio.composeapp.generated.resources.plugins_group_by_repo_desc
import nuvio.composeapp.generated.resources.plugins_group_by_repo_title
import nuvio.composeapp.generated.resources.plugins_input_manifest_placeholder
import nuvio.composeapp.generated.resources.plugins_message_installed
import nuvio.composeapp.generated.resources.plugins_modal_failure_title
import nuvio.composeapp.generated.resources.plugins_modal_installing_message
import nuvio.composeapp.generated.resources.plugins_modal_installing_title
import nuvio.composeapp.generated.resources.plugins_modal_success_title
import nuvio.composeapp.generated.resources.plugins_provider_disabled_by_repo
import nuvio.composeapp.generated.resources.plugins_provider_no_description
import nuvio.composeapp.generated.resources.plugins_provider_settings
import nuvio.composeapp.generated.resources.plugins_provider_version
import nuvio.composeapp.generated.resources.plugins_repo_fallback_label
import nuvio.composeapp.generated.resources.plugins_repo_version
import nuvio.composeapp.generated.resources.plugins_section_installed_repos
import nuvio.composeapp.generated.resources.plugins_section_overview
import nuvio.composeapp.generated.resources.plugins_section_providers
import nuvio.composeapp.generated.resources.plugins_test_error_title
import nuvio.composeapp.generated.resources.plugins_test_failed
import nuvio.composeapp.generated.resources.plugins_test_results_count
import nuvio.composeapp.generated.resources.plugins_test_results_title
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.plugins.AddPluginRepositoryResult
import com.nuvio.app.core.plugins.PluginRepository
import com.nuvio.app.core.plugins.PluginRepositoryItem
import com.nuvio.app.core.plugins.PluginRuntimeResult
import com.nuvio.app.core.plugins.PluginScraper

private const val AddRepositoryRowKey = "plugins:add-repo"

/** How tall either list may grow before it scrolls inside the page. */
private val PluginListMaxHeight = 640.dp

@Composable
fun PluginsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        PluginRepository.initialize()
    }

    val uiState by PluginRepository.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showAddRepoDialog by rememberSaveable { mutableStateOf(false) }
    var installState by remember { mutableStateOf<RepositoryInstallState?>(null) }

    var testingScraperId by remember { mutableStateOf<String?>(null) }
    val testResults = remember { mutableStateMapOf<String, List<PluginRuntimeResult>>() }
    var resultsForScraper by remember { mutableStateOf<PluginScraper?>(null) }

    var configuringScraper by remember { mutableStateOf<PluginScraper?>(null) }
    var configuringLayout by remember { mutableStateOf<String?>(null) }

    val sortedRepos = remember(uiState.repositories) {
        uiState.repositories.sortedBy { it.name.lowercase() }
    }
    val repositoryNameByUrl = remember(sortedRepos) {
        sortedRepos.associate { it.manifestUrl to it.name }
    }
    val sortedScrapers = remember(uiState.scrapers, repositoryNameByUrl) {
        uiState.scrapers.sortedWith(
            compareBy<PluginScraper>(
                { repositoryNameByUrl[it.repositoryUrl]?.lowercase() ?: it.repositoryUrl.lowercase() },
                { it.name.lowercase() },
            ),
        )
    }

    val repoFallbackLabel = stringResource(Res.string.plugins_repo_fallback_label)
    val testFailedDefault = stringResource(Res.string.plugins_test_failed)
    val testErrorTitle = stringResource(Res.string.plugins_test_error_title)
    val installedTemplate = stringResource(Res.string.plugins_message_installed)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ListSubheader(stringResource(Res.string.plugins_section_overview))
        SettingsList {
            switchRow(
                title = stringResource(Res.string.plugins_enable_globally_title),
                description = stringResource(Res.string.plugins_enable_globally_desc),
                checked = { uiState.pluginsEnabled },
                onCheckedChange = { PluginRepository.setPluginsEnabled(it) },
            )
            switchRow(
                title = stringResource(Res.string.plugins_group_by_repo_title),
                description = stringResource(Res.string.plugins_group_by_repo_desc),
                checked = { uiState.groupStreamsByRepository },
                onCheckedChange = { PluginRepository.setGroupStreamsByRepository(it) },
            )
        }

        // A repository has no enabled state of its own, so its heading states how many there are
        // rather than a ratio, and states nothing at all when there are none.
        val reposHeading = stringResource(Res.string.plugins_section_installed_repos)
        ListSubheader(
            text = if (sortedRepos.isEmpty()) reposHeading else "$reposHeading (${sortedRepos.size})",
        )

        // The add row holds the first slot, so the repositories start one further in.
        val repoRowCount = sortedRepos.size + 1
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = PluginListMaxHeight),
            verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
        ) {
            item(key = AddRepositoryRowKey) {
                NewEntryRow(
                    text = stringResource(Res.string.plugins_add_repo_row),
                    shape = segmentShape(index = 0, count = repoRowCount),
                    onClick = { showAddRepoDialog = true },
                )
            }

            itemsIndexed(sortedRepos, key = { _, repo -> repo.manifestUrl }) { index, repo ->
                PluginRepositoryRow(
                    repo = repo,
                    shape = segmentShape(index = index + 1, count = repoRowCount),
                    onRefresh = {
                        PluginRepository.refreshRepository(repo.manifestUrl, pushAfterRefresh = true)
                    },
                    onDelete = { PluginRepository.removeRepository(repo.manifestUrl) },
                )
            }
        }

        if (sortedRepos.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Hub,
                title = stringResource(Res.string.plugins_empty_repos_title),
                message = stringResource(Res.string.plugins_empty_repos_subtitle),
            )
        }

        // A provider does have an enabled state, so its heading is the ratio.
        val providersHeading = stringResource(Res.string.plugins_section_providers)
        val enabledScrapers = sortedScrapers.count { it.enabled }
        ListSubheader(
            text = if (sortedScrapers.isEmpty()) {
                providersHeading
            } else {
                "$providersHeading ($enabledScrapers/${sortedScrapers.size})"
            },
        )

        if (sortedScrapers.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Extension,
                title = stringResource(Res.string.plugins_empty_providers),
                message = stringResource(Res.string.plugins_empty_providers_subtitle),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = PluginListMaxHeight),
                verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
            ) {
                itemsIndexed(sortedScrapers, key = { _, scraper -> scraper.id }) { index, scraper ->
                    val repositoryName = repositoryNameByUrl[scraper.repositoryUrl]
                        ?: scraper.repositoryUrl.fallbackRepositoryLabel(repoFallbackLabel)

                    PluginProviderRow(
                        scraper = scraper,
                        repositoryName = repositoryName,
                        shape = segmentShape(index = index, count = sortedScrapers.size),
                        isTesting = testingScraperId == scraper.id,
                        resultCount = testResults[scraper.id].orEmpty().size,
                        onEnabledChange = { PluginRepository.toggleScraper(scraper.id, it) },
                        onOpenSettings = {
                            coroutineScope.launch {
                                val layout = PluginRuntime.getPluginSettingsLayout(scraper.code, scraper.id)
                                if (layout != null) {
                                    configuringScraper = scraper
                                    configuringLayout = layout
                                }
                            }
                        },
                        onTest = {
                            testingScraperId = scraper.id
                            coroutineScope.launch {
                                PluginRepository.testScraper(scraper.id)
                                    .onSuccess { results -> testResults[scraper.id] = results }
                                    .onFailure { error ->
                                        testResults[scraper.id] = listOf(
                                            PluginRuntimeResult(
                                                title = testErrorTitle,
                                                name = error.message ?: testFailedDefault,
                                                url = "about:error",
                                            ),
                                        )
                                    }
                                testingScraperId = null
                                resultsForScraper = scraper
                            }
                        },
                        onShowResults = { resultsForScraper = scraper },
                    )
                }
            }
        }
    }

    if (showAddRepoDialog) {
        TextPromptDialog(
            title = stringResource(Res.string.plugins_add_repo_row),
            label = stringResource(Res.string.plugins_input_manifest_placeholder),
            initialValue = "",
            confirmText = stringResource(Res.string.plugins_button_install_repo),
            keyboardType = KeyboardType.Uri,
            onConfirm = { requested ->
                showAddRepoDialog = false
                installState = RepositoryInstallState.Installing
                coroutineScope.launch {
                    installState = when (val result = PluginRepository.addRepository(requested)) {
                        is AddPluginRepositoryResult.Success -> RepositoryInstallState.Success(
                            installedTemplate.replace("%1\$s", result.repository.name),
                        )
                        is AddPluginRepositoryResult.Error -> RepositoryInstallState.Error(result.message)
                    }
                }
            },
            onDismiss = { showAddRepoDialog = false },
        )
    }

    val currentInstallState = installState
    if (currentInstallState != null) {
        NuvioStatusModal(
            title = when (currentInstallState) {
                RepositoryInstallState.Installing -> stringResource(Res.string.plugins_modal_installing_title)
                is RepositoryInstallState.Success -> stringResource(Res.string.plugins_modal_success_title)
                is RepositoryInstallState.Error -> stringResource(Res.string.plugins_modal_failure_title)
            },
            message = when (currentInstallState) {
                RepositoryInstallState.Installing -> stringResource(Res.string.plugins_modal_installing_message)
                is RepositoryInstallState.Success -> currentInstallState.summary
                is RepositoryInstallState.Error -> currentInstallState.reason
            },
            isVisible = true,
            isBusy = currentInstallState.isBusy,
            confirmText = when (currentInstallState) {
                RepositoryInstallState.Installing -> stringResource(Res.string.plugins_button_installing)
                is RepositoryInstallState.Success -> stringResource(Res.string.action_done)
                is RepositoryInstallState.Error -> stringResource(Res.string.action_close)
            },
            onConfirm = {
                if (!currentInstallState.isBusy) {
                    installState = null
                }
            },
        )
    }

    val shownResultsScraper = resultsForScraper
    if (shownResultsScraper != null) {
        PluginTestResultsDialog(
            scraperName = shownResultsScraper.name,
            results = testResults[shownResultsScraper.id].orEmpty(),
            onDismiss = { resultsForScraper = null },
        )
    }

    if (configuringScraper != null && configuringLayout != null) {
        PluginSettingsDialog(
            scraperId = configuringScraper!!.id,
            scraperName = configuringScraper!!.name,
            layoutJson = configuringLayout!!,
            onDismiss = {
                configuringScraper = null
                configuringLayout = null
            }
        )
    }
}

private sealed interface RepositoryInstallState {
    val isBusy: Boolean

    data object Installing : RepositoryInstallState {
        override val isBusy: Boolean = true
    }

    data class Success(val summary: String) : RepositoryInstallState {
        override val isBusy: Boolean = false
    }

    data class Error(val reason: String) : RepositoryInstallState {
        override val isBusy: Boolean = false
    }
}

/** One installed repository, with what can be done to it behind the trailing button. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PluginRepositoryRow(
    repo: PluginRepositoryItem,
    shape: RoundedCornerShape,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val supporting = listOfNotNull(
        repo.version?.let { stringResource(Res.string.plugins_repo_version, it) },
        stringResource(Res.string.plugins_badge_providers, repo.scraperCount),
        stringResource(Res.string.plugins_badge_refreshing).takeIf { repo.isRefreshing },
        repo.manifestUrl,
    ).joinToString(" · ")

    SegmentedListItem(
        onClick = { menuOpen = true },
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
        leadingContent = {
            Icon(imageVector = Icons.Rounded.Hub, contentDescription = null)
        },
        supportingContent = {
            Column {
                Text(supporting)
                repo.errorMessage?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(Res.string.plugins_cd_refresh_repo),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.plugins_cd_refresh_repo)) },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                        enabled = !repo.isRefreshing,
                        onClick = {
                            menuOpen = false
                            onRefresh()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.plugins_cd_delete_repo)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        },
    ) {
        Text(repo.name)
    }
}

/** One provider, with its switch and, behind the trailing button, what can be done to it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PluginProviderRow(
    scraper: PluginScraper,
    repositoryName: String,
    shape: RoundedCornerShape,
    isTesting: Boolean,
    resultCount: Int,
    onEnabledChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onTest: () -> Unit,
    onShowResults: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val supporting = listOfNotNull(
        repositoryName,
        stringResource(Res.string.plugins_provider_version, scraper.version),
        scraper.supportedTypes.joinToString(" | ").takeIf { it.isNotBlank() },
        stringResource(Res.string.plugins_provider_disabled_by_repo).takeIf { !scraper.manifestEnabled },
        scraper.description.ifBlank { stringResource(Res.string.plugins_provider_no_description) },
    ).joinToString(" · ")

    SegmentedListItem(
        onClick = { menuOpen = true },
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
        leadingContent = {
            Icon(
                imageVector = Icons.Rounded.Extension,
                contentDescription = null,
                tint = if (scraper.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        supportingContent = { Text(supporting) },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = scraper.enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = scraper.manifestEnabled,
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(Res.string.plugins_provider_settings),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (scraper.hasSettings) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.plugins_provider_settings)) },
                                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onOpenSettings()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isTesting) {
                                        stringResource(Res.string.plugins_button_testing)
                                    } else {
                                        stringResource(Res.string.plugins_button_test_provider)
                                    },
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null) },
                            enabled = !isTesting,
                            onClick = {
                                menuOpen = false
                                onTest()
                            },
                        )
                        if (resultCount > 0) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.plugins_test_results_count, resultCount)) },
                                leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onShowResults()
                                },
                            )
                        }
                    }
                }
            }
        },
    ) {
        Text(scraper.name)
    }
}

/**
 * What a provider returned when it was tested.
 *
 * The results used to unfold under the row that asked for them, which made one row taller than
 * the rest of the list; they are a reply to an action, so they come back as a dialog.
 */
@Composable
private fun PluginTestResultsDialog(
    scraperName: String,
    results: List<PluginRuntimeResult>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.plugins_test_results_title, scraperName)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.plugins_test_results_count, results.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                results.take(8).forEach { result ->
                    Column {
                        Text(text = result.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = result.url,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

private fun String.fallbackRepositoryLabel(fallback: String): String {
    val withoutQuery = substringBefore("?")
    val withoutManifest = withoutQuery.removeSuffix("/manifest.json")
    val host = withoutManifest.substringAfter("://", withoutManifest).substringBefore('/')
    return host.ifBlank {
        withoutManifest.substringAfterLast('/').ifBlank { fallback }
    }
}
