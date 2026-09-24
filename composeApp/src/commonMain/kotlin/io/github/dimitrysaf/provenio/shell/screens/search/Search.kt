package io.github.dimitrysaf.provenio.shell.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.NetworkOfflineCard
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.SearchField
import io.github.dimitrysaf.provenio.shell.components.ScreenActivityEffect
import io.github.dimitrysaf.provenio.shell.components.withDuplicateSafeLazyKeys
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.firstEnabledManifestError
import io.github.dimitrysaf.provenio.core.addons.hasPendingEnabledManifests
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.buildAddonCatalogRefreshSignature
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeCatalogRowSection
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeSkeletonRow
import io.github.dimitrysaf.provenio.shell.screens.home.components.homeSectionHorizontalPaddingForWidth
import io.github.dimitrysaf.provenio.shell.screens.home.components.rememberPosterGridColumnCount
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_retry
import provenio.composeapp.generated.resources.compose_search_empty_failed_message
import provenio.composeapp.generated.resources.compose_search_empty_failed_title
import provenio.composeapp.generated.resources.compose_search_empty_no_active_addons_message
import provenio.composeapp.generated.resources.compose_search_empty_no_active_addons_title
import provenio.composeapp.generated.resources.compose_search_empty_no_results_message
import provenio.composeapp.generated.resources.compose_search_empty_no_results_title
import provenio.composeapp.generated.resources.compose_search_empty_no_search_catalogs_message
import provenio.composeapp.generated.resources.compose_search_empty_no_search_catalogs_title
import provenio.composeapp.generated.resources.compose_search_placeholder
import provenio.composeapp.generated.resources.compose_search_recent_searches
import provenio.composeapp.generated.resources.compose_search_remove_recent_search
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.search.SearchEmptyStateReason
import io.github.dimitrysaf.provenio.core.search.SearchHistoryRepository
import io.github.dimitrysaf.provenio.core.search.SearchRepository

/** The side margin the discover grid sits in, which its column count has to allow for. */
private val DiscoverGridHorizontalPadding = 16.dp

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    searchFocusRequestCount: Int = 0,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
) {
    val focusRequester = remember { FocusRequester() }

    ScreenActivityEffect(listState) { screenActive ->
        if (!screenActive) {
            listState.stopScroll()
        }
    }

    ScreenActivityEffect(searchFocusRequestCount) { screenActive ->
        if (screenActive && searchFocusRequestCount > 0) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        AddonRepository.initialize()
        WatchedRepository.ensureLoaded()
        SearchHistoryRepository.ensureLoaded()
    }

    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val uiState by SearchRepository.uiState.collectAsStateWithLifecycle()
    val discoverUiState by SearchRepository.discoverUiState.collectAsStateWithLifecycle()
    val homeCatalogSettingsUiState by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val recentSearches by SearchHistoryRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by WatchedRepository.uiState.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var lastRequestedQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var observedOfflineState by remember { mutableStateOf(false) }
    ScreenActivityEffect(scrollToTopRequests) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            listState.animateScrollToItem(0)
        }
    }

    val addonRefreshKey = remember(addonsUiState.addons) {
        buildAddonCatalogRefreshSignature(addonsUiState.addons)
    }
    val addonManifestsLoading = addonsUiState.addons.hasPendingEnabledManifests()

    ScreenActivityEffect(addonRefreshKey, homeCatalogSettingsUiState.hideUnreleasedContent) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        SearchRepository.refreshDiscover(addonsUiState.addons)
    }

    ScreenActivityEffect(query, addonRefreshKey, homeCatalogSettingsUiState.hideUnreleasedContent) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            lastRequestedQuery = null
            SearchRepository.clear()
        } else {
            delay(350)
            lastRequestedQuery = normalizedQuery
            SearchRepository.search(
                query = normalizedQuery,
                addons = addonsUiState.addons,
            )
        }
    }

    ScreenActivityEffect(listState, query, discoverUiState.canLoadMore, discoverUiState.isLoading) { screenActive ->
        if (!screenActive || query.isNotBlank()) return@ScreenActivityEffect

        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= layoutInfo.totalItemsCount - 4
            }
            .distinctUntilChanged()
            .filter { it && discoverUiState.canLoadMore && !discoverUiState.isLoading }
            .collect {
                SearchRepository.loadMoreDiscover()
            }
    }

    ScreenActivityEffect(query, lastRequestedQuery, uiState.isLoading, uiState.sections) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return@ScreenActivityEffect
        if (lastRequestedQuery != normalizedQuery) return@ScreenActivityEffect
        if (uiState.isLoading || uiState.sections.isEmpty()) return@ScreenActivityEffect
        SearchHistoryRepository.recordSearch(normalizedQuery)
    }

    ScreenActivityEffect(networkStatusUiState.condition, query, addonRefreshKey) { screenActive ->
        if (!screenActive) return@ScreenActivityEffect
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@ScreenActivityEffect
                observedOfflineState = false

                val normalizedQuery = query.trim()
                if (normalizedQuery.isBlank()) {
                    SearchRepository.refreshDiscover(
                        addons = addonsUiState.addons,
                        forceRefresh = true,
                    )
                } else {
                    SearchRepository.search(
                        query = normalizedQuery,
                        addons = addonsUiState.addons,
                        forceRefresh = true,
                    )
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val discoverColumns = rememberPosterGridColumnCount(maxWidth - DiscoverGridHorizontalPadding * 2)
        val homeSectionPadding = remember(maxWidth) {
            homeSectionHorizontalPaddingForWidth(maxWidth.value)
        }
        // No app bar: the field is the heading. A screen whose whole purpose is one text input
        // does not also need its name written above it.
        ScreenScaffold(
            title = null,
            horizontalPadding = 0.dp,
            listState = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "search_field") {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(Res.string.compose_search_placeholder),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    focusRequester = focusRequester,
                )
            }

            // Full bleed: the divider separates the field from the page, so it runs the whole
            // width rather than being inset with the content.
            item(key = "search_field_divider") {
                HorizontalDivider()
            }

        if (query.isBlank()) {
            if (recentSearches.isNotEmpty()) {
                item(key = "recent_searches") {
                    SearchRecentSection(
                        recentSearches = recentSearches,
                        onSearchPress = { recentQuery -> query = recentQuery },
                        onRemoveSearch = SearchHistoryRepository::removeSearch,
                    )
                }
            }
                discoverContent(
                    state = discoverUiState,
                    isSourceLoading = addonManifestsLoading,
                    columns = discoverColumns,
                    networkCondition = networkStatusUiState.condition,
                    onTypeSelected = SearchRepository::selectDiscoverType,
                    onCatalogSelected = SearchRepository::selectDiscoverCatalog,
                    onGenreSelected = SearchRepository::selectDiscoverGenre,
                    onRetry = {
                        NetworkStatusRepository.requestRefresh(force = true)
                        if (addonsUiState.addons.firstEnabledManifestError() != null) {
                            AddonRepository.refreshAll()
                        } else {
                            SearchRepository.refreshDiscover(
                                addons = addonsUiState.addons,
                                forceRefresh = true,
                            )
                        }
                    },
                    watchedKeys = watchedUiState.watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onPosterClick = onPosterClick,
                    onPosterLongClick = onPosterLongClick,
                )
            } else {
                val normalizedQuery = query.trim()
                val isWaitingForSearch = normalizedQuery.isNotBlank() && lastRequestedQuery != normalizedQuery
                when {
                    isWaitingForSearch -> {
                        items(2) {
                            HomeSkeletonRow(
                                horizontalPadding = homeSectionPadding,
                            )
                        }
                    }

                    (uiState.isLoading || addonManifestsLoading) && uiState.sections.isEmpty() -> {
                        items(2) {
                            HomeSkeletonRow(
                                horizontalPadding = homeSectionPadding,
                            )
                        }
                    }

                    uiState.sections.isEmpty() -> {
                        item {
                            SearchEmptyStateCard(
                                reason = uiState.emptyStateReason,
                                errorMessage = uiState.errorMessage,
                                networkCondition = networkStatusUiState.condition,
                                onRetry = {
                                    if (normalizedQuery.isNotBlank()) {
                                        NetworkStatusRepository.requestRefresh(force = true)
                                        if (addonsUiState.addons.firstEnabledManifestError() != null) {
                                            AddonRepository.refreshAll()
                                        } else {
                                            SearchRepository.search(
                                                query = normalizedQuery,
                                                addons = addonsUiState.addons,
                                                forceRefresh = true,
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = homeSectionPadding),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = uiState.sections.withDuplicateSafeLazyKeys { section -> section.key },
                            key = { section -> section.lazyKey },
                        ) { keyedSection ->
                            val section = keyedSection.value
                            HomeCatalogRowSection(
                                section = section,
                                modifier = Modifier.padding(bottom = 12.dp),
                                watchedKeys = watchedUiState.watchedKeys,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                onPosterClick = onPosterClick,
                                onPosterLongClick = onPosterLongClick,
                            )
                        }
                        if (uiState.isLoading) {
                            item(key = "search_loading_more") {
                                HomeSkeletonRow(
                                    horizontalPadding = homeSectionPadding,
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
private fun SearchEmptyStateCard(
    reason: SearchEmptyStateReason?,
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (
        reason == SearchEmptyStateReason.RequestFailed &&
        (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable)
    ) {
        NetworkOfflineCard(
            condition = networkCondition,
            modifier = modifier,
            onRetry = onRetry,
        )
        return
    }

    val title: String
    val message: String
    val icon: ImageVector

    when (reason) {
        SearchEmptyStateReason.NoActiveAddons -> {
            title = stringResource(Res.string.compose_search_empty_no_active_addons_title)
            message = stringResource(Res.string.compose_search_empty_no_active_addons_message)
            icon = Icons.Rounded.Extension
        }

        SearchEmptyStateReason.NoSearchCatalogs -> {
            title = stringResource(Res.string.compose_search_empty_no_search_catalogs_title)
            message = stringResource(Res.string.compose_search_empty_no_search_catalogs_message)
            icon = Icons.Rounded.GridView
        }

        SearchEmptyStateReason.RequestFailed -> {
            title = stringResource(Res.string.compose_search_empty_failed_title)
            message = errorMessage ?: stringResource(Res.string.compose_search_empty_failed_message)
            icon = Icons.Rounded.ErrorOutline
        }

        SearchEmptyStateReason.NoResults, null -> {
            title = stringResource(Res.string.compose_search_empty_no_results_title)
            message = stringResource(Res.string.compose_search_empty_no_results_message)
            icon = Icons.Rounded.SearchOff
        }
    }

    EmptyState(
        icon = icon,
        modifier = modifier,
        title = title,
        message = message,
        actionLabel = if (reason == SearchEmptyStateReason.RequestFailed) {
            stringResource(Res.string.action_retry)
        } else {
            null
        },
        onActionClick = if (reason == SearchEmptyStateReason.RequestFailed) onRetry else null,
    )
}

/** What was searched before, ready to search again. */
@Composable
private fun SearchRecentSection(
    recentSearches: List<String>,
    onSearchPress: (String) -> Unit,
    onRemoveSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.compose_search_recent_searches),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        recentSearches.forEach { recentQuery ->
            SearchRecentRow(
                query = recentQuery,
                onSearchPress = { onSearchPress(recentQuery) },
                onRemovePress = { onRemoveSearch(recentQuery) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * One past search.
 *
 * A list item, so the row carries Material's own heights, leading and trailing slots and state
 * layer rather than a hand-built row with a background of its own.
 */
@Composable
private fun SearchRecentRow(
    query: String,
    onSearchPress: () -> Unit,
    onRemovePress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(text = query, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier.clickable(onClick = onSearchPress),
        leadingContent = {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
            )
        },
        trailingContent = {
            IconButton(onClick = onRemovePress) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.compose_search_remove_recent_search),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

