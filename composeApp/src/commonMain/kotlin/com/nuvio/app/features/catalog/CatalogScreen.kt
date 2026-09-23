package com.nuvio.app.features.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.components.NuvioNetworkOfflineCard
import com.nuvio.app.shell.components.NuvioPosterWatchedOverlay
import com.nuvio.app.shell.components.SkeletonPoster
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.shell.components.posterCardClickable
import com.nuvio.app.shell.components.rememberPosterCardStyleUiState
import com.nuvio.app.shell.components.withDuplicateSafeLazyKeys
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.home.components.HomeEmptyStateCard
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import com.nuvio.app.navigation.LocalUseNativeNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CatalogScreen(
    title: String,
    subtitle: String,
    target: CatalogTarget,
    onBack: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by CatalogRepository.uiState.collectAsStateWithLifecycle()
    val homeCatalogSettingsUiState by HomeCatalogSettingsRepository.uiState.collectAsStateWithLifecycle()
    val posterCardStyle = rememberPosterCardStyleUiState()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val initialScrollPosition = remember(
        target,
        homeCatalogSettingsUiState.hideUnreleasedContent,
    ) {
        CatalogRepository.scrollPosition(
            target = target,
        )
    }
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = initialScrollPosition.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialScrollPosition.firstVisibleItemScrollOffset,
    )
    var observedOfflineState by remember { mutableStateOf(false) }

    LaunchedEffect(target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        CatalogRepository.load(
            target = target,
        )
    }

    LaunchedEffect(gridState, target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                CatalogRepository.saveScrollPosition(
                    target = target,
                    firstVisibleItemIndex = index,
                    firstVisibleItemScrollOffset = offset,
                )
            }
    }

    LaunchedEffect(gridState, uiState.canLoadMore, uiState.isLoading) {
        snapshotFlow { gridState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= layoutInfo.totalItemsCount - 6
            }
            .distinctUntilChanged()
            .filter { it && uiState.canLoadMore && !uiState.isLoading }
            .collect {
                CatalogRepository.loadMore()
            }
    }

    LaunchedEffect(networkStatusUiState.condition, target) {
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@LaunchedEffect
                observedOfflineState = false
                CatalogRepository.load(
                    target = target,
                    force = true,
                )
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    // The heading is an app bar rather than a block pinned over the grid, so it collapses into a
    // title as the grid scrolls instead of holding a third of the screen for good.
    val nativeNavigation = LocalUseNativeNavigation.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .then(if (nativeNavigation) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!nativeNavigation) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    subtitle = subtitle.takeIf { it.isNotBlank() }?.let { text ->
                        {
                            Text(
                                text = text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(Res.string.action_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = remember(maxWidth) { catalogGridColumnsForWidth(maxWidth) }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() +
                        (if (nativeNavigation) NativeNavigationBarHeight else 0.dp) + 12.dp,
                    end = 16.dp,
                    bottom = nuvioSafeBottomPadding(28.dp),
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (uiState.items.isEmpty() && uiState.isLoading) {
                    items(columns * 3) {
                        SkeletonPoster(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = posterCardStyle.cornerRadiusDp.dp,
                            showLabels = !posterCardStyle.hideLabelsEnabled,
                        )
                    }
                } else if (uiState.items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogEmptyState(
                            errorMessage = uiState.errorMessage,
                            networkCondition = networkStatusUiState.condition,
                            onRetry = {
                                NetworkStatusRepository.requestRefresh(force = true)
                                CatalogRepository.load(
                                    target = target,
                                    force = true,
                                )
                            },
                        )
                    }
                } else {
                    items(
                        items = uiState.items.withDuplicateSafeLazyKeys { item -> item.stableKey() },
                        key = { item -> item.lazyKey },
                    ) { keyedItem ->
                        val item = keyedItem.value
                        CatalogPosterTile(
                            item = item,
                            cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                            hideLabels = posterCardStyle.hideLabelsEnabled,
                            isWatched = WatchingState.isPosterWatched(
                                watchedKeys = watchedUiState.watchedKeys,
                                item = item,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            ),
                            onClick = onPosterClick?.let { { it(item) } },
                            onLongClick = onPosterLongClick?.let { { it(item) } },
                        )
                    }
                    if (uiState.isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CatalogLoadingFooter()
                        }
                    }
                }
            }
        }
    }
}

/** What the platform's own navigation bar covers on iOS, where this screen draws no app bar. */
private val NativeNavigationBarHeight = 44.dp

@Composable
private fun CatalogPosterTile(
    item: MetaPreview,
    cornerRadiusDp: Int,
    hideLabels: Boolean,
    isWatched: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(item.posterShape.catalogAspectRatio())
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .background(MaterialTheme.colorScheme.surface)
                .posterCardClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    zoomImageUrl = item.poster,
                    zoomCornerRadius = cornerRadiusDp.dp,
                ),
        ) {
            if (item.poster != null) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            NuvioPosterWatchedOverlay(isWatched = isWatched)
        }
        if (!hideLabels) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CatalogEmptyState(
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
) {
    if (
        !errorMessage.isNullOrBlank() &&
        (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable)
    ) {
        NuvioNetworkOfflineCard(
            condition = networkCondition,
            onRetry = onRetry,
        )
        return
    }

    val loadFailed = !errorMessage.isNullOrBlank()
    HomeEmptyStateCard(
        title = stringResource(
            if (loadFailed) Res.string.catalog_load_failed_title else Res.string.catalog_empty_title,
        ),
        message = errorMessage ?: stringResource(Res.string.catalog_empty_message),
        actionLabel = if (loadFailed) stringResource(Res.string.action_retry) else null,
        onActionClick = if (loadFailed) onRetry else null,
    )
}

@Composable
private fun CatalogLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        NuvioLoadingIndicator(
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun PosterShape.catalogAspectRatio(): Float =
    when (this) {
        PosterShape.Poster -> 0.68f
        PosterShape.Square -> 1f
        PosterShape.Landscape -> 1.78f
    }

private fun catalogGridColumnsForWidth(screenWidth: Dp): Int =
    when {
        screenWidth >= 1400.dp -> 7
        screenWidth >= 1200.dp -> 6
        screenWidth >= 1000.dp -> 5
        screenWidth >= 840.dp -> 4
        else -> 3
    }
