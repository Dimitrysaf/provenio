package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.foundation.background
import io.github.dimitrysaf.provenio.shell.components.SmallLoadingSpinner
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalRippleConfiguration
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.PosterCard
import io.github.dimitrysaf.provenio.shell.components.LocalPosterProgress
import io.github.dimitrysaf.provenio.shell.components.PosterCardShape
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import io.github.dimitrysaf.provenio.core.watch.watching.application.PosterImdbAliases
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import provenio.composeapp.generated.resources.action_back
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.components.withDuplicateSafeLazyKeys
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.PosterShape
import io.github.dimitrysaf.provenio.core.home.canOpenCatalog
import io.github.dimitrysaf.provenio.core.home.stableKey
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeCatalogRowSection
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import io.github.dimitrysaf.provenio.shell.nav.LocalUseNativeNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.collections_folder_empty_items
import provenio.composeapp.generated.resources.collections_folder_not_found
import provenio.composeapp.generated.resources.collections_tab_all
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.collection.FolderDetailUiState
import io.github.dimitrysaf.provenio.core.collection.FolderViewMode
import io.github.dimitrysaf.provenio.core.collection.FolderDetailRepository


@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun FolderDetailScreen(
    onBack: () -> Unit,
    onCatalogClick: (HomeCatalogSection) -> Unit,
    onPosterClick: (MetaPreview) -> Unit,
) {
    val uiState by FolderDetailRepository.uiState.collectAsState()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsState()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsState()
    val folder = uiState.folder
    val useNativeNavigation = LocalUseNativeNavigation.current
    // The same collapsing app bar as a catalog's page: large over the top of the titles, a title
    // bar once they scroll.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(if (useNativeNavigation) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!useNativeNavigation) {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = folder?.title ?: uiState.collectionTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    subtitle = uiState.collectionTitle.takeIf { folder != null && it.isNotBlank() }?.let { text ->
                        { Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    },
                    navigationIcon = {
                        WithTooltip(stringResource(Res.string.action_back)) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(Res.string.action_back),
                                )
                            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            if (folder == null && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.collections_folder_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            when (uiState.viewMode) {
                FolderViewMode.TABBED_GRID -> TabbedGridContent(
                    uiState = uiState,
                    watchedKeys = watchedUiState.watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    modifier = Modifier.weight(1f),
                    onTabSelected = { FolderDetailRepository.selectTab(it) },
                    onPosterClick = onPosterClick,
                )
                FolderViewMode.ROWS -> RowsContent(
                    uiState = uiState,
                    watchedKeys = watchedUiState.watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    modifier = Modifier.weight(1f),
                    onCatalogClick = onCatalogClick,
                    onPosterClick = onPosterClick,
                )
                FolderViewMode.FOLLOW_LAYOUT -> RowsContent(
                    uiState = uiState,
                    watchedKeys = watchedUiState.watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    modifier = Modifier.weight(1f),
                    onCatalogClick = onCatalogClick,
                    onPosterClick = onPosterClick,
                )
            }
        }
    }
}

@Composable
private fun TabbedGridContent(
    uiState: FolderDetailUiState,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    modifier: Modifier = Modifier,
    onTabSelected: (Int) -> Unit,
    onPosterClick: (MetaPreview) -> Unit,
) {
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, uiState.selectedTabIndex, uiState.selectedTabCanLoadMore, uiState.selectedTabIsLoadingMore) {
        snapshotFlow { gridState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= layoutInfo.totalItemsCount - 6
            }
            .distinctUntilChanged()
            .filter { it && uiState.selectedTabCanLoadMore && !uiState.selectedTabIsLoadingMore }
            .collect {
                FolderDetailRepository.loadMoreSelectedTab()
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.tabs.size > 1) {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    divider = {},
                ) {
                    uiState.tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = index == uiState.selectedTabIndex,
                            onClick = { onTabSelected(index) },
                            text = {
                                Text(
                                    text = if (tab.isAllTab) {
                                        stringResource(Res.string.collections_tab_all)
                                    } else {
                                        tab.label
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val selectedTab = uiState.tabs.getOrNull(uiState.selectedTabIndex)
        if (selectedTab == null) return
        // TMDB-built tabs carry TMDB ids; their IMDb ones are what match the watched titles.
        LaunchedEffect(selectedTab.items) { PosterImdbAliases.resolve(selectedTab.items) }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = remember(maxWidth) { folderDetailGridColumnsForWidth(maxWidth) }

            when {
                selectedTab.isLoading && selectedTab.items.isEmpty() -> LoadingIndicator()
                selectedTab.error != null && selectedTab.items.isEmpty() -> ErrorMessage(selectedTab.error)
                selectedTab.items.isEmpty() -> EmptyMessage()
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = safeBottomPadding(18.dp),
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = selectedTab.items.withDuplicateSafeLazyKeys { item -> item.stableKey() },
                            key = { item -> item.lazyKey },
                        ) { keyedItem ->
                            val item = keyedItem.value
                            PosterCard(
                                title = item.name,
                                imageUrl = item.poster,
                                shape = PosterCardShape.Poster,
                                detailLine = item.releaseInfo,
                                isWatched = WatchingState.isPosterWatched(
                                    watchedKeys = watchedKeys,
                                    item = item,
                                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                ),
                                progress = LocalPosterProgress.current(item),
                                onClick = { onPosterClick(item) },
                            )
                        }

                        if (uiState.selectedTabIsLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PaginationLoadingFooter()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowsContent(
    uiState: FolderDetailUiState,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    modifier: Modifier = Modifier,
    onCatalogClick: (HomeCatalogSection) -> Unit,
    onPosterClick: (MetaPreview) -> Unit,
) {
    val sections = FolderDetailRepository.getCatalogSectionsForRows()
    LaunchedEffect(sections) { PosterImdbAliases.resolve(sections.flatMap { it.items.take(18) }) }

    if (uiState.isLoading && sections.isEmpty()) {
        LoadingIndicator()
        return
    }

    if (sections.isEmpty() && !uiState.isLoading) {
        EmptyMessage()
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = safeBottomPadding(18.dp),
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(
            items = sections.withDuplicateSafeLazyKeys { it.key },
            key = { it.lazyKey },
        ) { keyedSection ->
            val section = keyedSection.value
            HomeCatalogRowSection(
                section = section,
                entries = section.items.take(18),
                onViewAllClick = if (section.canOpenCatalog(18)) {
                    { onCatalogClick(section) }
                } else {
                    null
                },
                watchedKeys = watchedKeys,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                onPosterClick = { onPosterClick(it) },
            )
        }
    }
}

@Composable
private fun PaginationLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        SmallLoadingSpinner(size = 28.dp)
    }
}

private fun folderDetailGridColumnsForWidth(screenWidth: Dp): Int =
    when {
        screenWidth >= 1400.dp -> 7
        screenWidth >= 1200.dp -> 6
        screenWidth >= 1000.dp -> 5
        screenWidth >= 840.dp -> 4
        else -> 3
    }

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        SmallLoadingSpinner(size = 32.dp)
    }
}

@Composable
private fun ErrorMessage(error: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyMessage() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.collections_folder_empty_items),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun PosterShape.toPosterCardShape(): PosterCardShape =
    when (this) {
        PosterShape.Poster -> PosterCardShape.Poster
        PosterShape.Square -> PosterCardShape.Square
        PosterShape.Landscape -> PosterCardShape.Landscape
    }
