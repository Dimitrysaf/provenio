package io.github.dimitrysaf.provenio.shell.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import io.github.dimitrysaf.provenio.core.collection.Collection
import io.github.dimitrysaf.provenio.shell.components.DuplicateSafeLazyEntry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.auth.AuthRepository
import io.github.dimitrysaf.provenio.core.auth.AuthState
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.shell.components.LocalBottomNavigationOverlayPadding
import io.github.dimitrysaf.provenio.shell.components.ScreenActivityEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.NetworkOfflineCard
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.shell.components.withDuplicateSafeLazyKeys
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.addons.firstEnabledManifestError
import io.github.dimitrysaf.provenio.core.cloud.CloudLibraryRepository
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeCatalogRowSection
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeEmptyStateCard
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeHeroReservedSpace
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeHeroSection
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeSkeletonHero
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeSkeletonRow
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedUiState
import io.github.dimitrysaf.provenio.core.watch.watched.resolveWatchedBadgesBulk
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingItem
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressSourceCoordinator
import io.github.dimitrysaf.provenio.shell.components.DisintegrationRequest
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeCollectionRowSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import io.github.dimitrysaf.provenio.shell.screens.home.components.continueWatchingHeroViewportReserveHeight
import io.github.dimitrysaf.provenio.shell.screens.home.components.homeSectionHorizontalPaddingForWidth
import io.github.dimitrysaf.provenio.shell.screens.home.components.rememberContinueWatchingLayout
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.canOpenCatalog
import io.github.dimitrysaf.provenio.core.home.shouldShowHomeHeroSlot
import io.github.dimitrysaf.provenio.core.home.shouldShowInitialHomeLoading
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsItem
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeRepository

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    animateCollectionGifs: Boolean = true,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
    onContinueWatchingLongPress: ((ContinueWatchingItem) -> Unit)? = null,
    continueWatchingDisintegrationRequest: DisintegrationRequest<String>? = null,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    onFirstCatalogRendered: (() -> Unit)? = null,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
        CollectionRepository.initialize()
        ContinueWatchingPreferencesRepository.ensureLoaded()
        WatchedRepository.ensureLoaded()
        WatchProgressRepository.ensureLoaded()
        val authState = AuthRepository.state.value
        if (authState !is AuthState.Authenticated || authState.isAnonymous) {
            WatchProgressSourceCoordinator.ensureStarted()
        }
    }

    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val homeUiState by HomeRepository.uiState.collectAsStateWithLifecycle()
    val homeSettingsUiState by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val homeListState = rememberLazyListState()
    val continueWatchingListState = rememberLazyListState()
    val upcomingListState = rememberLazyListState()
    ScreenActivityEffect(homeListState, continueWatchingListState, upcomingListState) { active ->
        if (!active) {
            homeListState.stopScroll(MutatePriority.PreventUserInput)
            continueWatchingListState.stopScroll(MutatePriority.PreventUserInput)
            upcomingListState.stopScroll(MutatePriority.PreventUserInput)
        }
    }
    val collections by CollectionRepository.collections.collectAsStateWithLifecycle()
    val continueWatchingPreferences by ContinueWatchingPreferencesRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by WatchedRepository.uiState.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val watchProgressUiState by WatchProgressRepository.uiState.collectAsStateWithLifecycle()
    val effectiveWatchProgressSource = watchProgressUiState.source
    val cloudLibraryUiState by CloudLibraryRepository.uiState.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val trackingSettingsUiState by remember {
        TrackingSettingsRepository.ensureLoaded()
        TrackingSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    ScreenActivityEffect(scrollToTopRequests) { active ->
        if (!active) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            homeListState.animateScrollToItem(0)
        }
    }

    HomeReconnectEffect(networkStatusUiState.condition, addonsUiState.addons)

    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val activeProfileId = profileState.activeProfile?.profileIndex ?: 1
    val enabledAddons = remember(addonsUiState.addons) {
        addonsUiState.addons.enabledAddons()
    }
    val continueWatchingRows = rememberHomeContinueWatchingRows(
        activeProfileId = activeProfileId,
        watchProgressUiState = watchProgressUiState,
        watchedUiState = watchedUiState,
        continueWatchingPreferences = continueWatchingPreferences,
        continueWatchingDaysCap = trackingSettingsUiState.continueWatchingDaysCap,
        cloudLibraryUiState = cloudLibraryUiState,
        enabledAddons = enabledAddons,
        networkCondition = networkStatusUiState.condition,
    )
    val continueWatchingItems = continueWatchingRows.continueWatching
    val upcomingItems = continueWatchingRows.upcoming
    val hasContinueWatchingRows = continueWatchingItems.isNotEmpty() || upcomingItems.isNotEmpty()
    KeepRowAtStartUntilScrolled(continueWatchingListState, activeProfileId, continueWatchingItems.isNotEmpty())
    KeepRowAtStartUntilScrolled(upcomingListState, activeProfileId, upcomingItems.isNotEmpty())


    ScreenActivityEffect(activeProfileId, collections) { active ->
        if (!active) return@ScreenActivityEffect
        HomeCatalogSettingsRepository.syncCollections(collections)
    }

    val hasActiveAddons = enabledAddons.any { it.manifest != null }
    val addonManifestsLoading = enabledAddons.any { it.isRefreshing }
    val addonManifestErrorMessage = enabledAddons.firstEnabledManifestError()
    val isResolvingHeroSources = addonManifestsLoading || homeUiState.isLoading
    var firstCatalogReported by remember { mutableStateOf(false) }

    LaunchedEffect(homeUiState.sections.firstOrNull()?.key, onFirstCatalogRendered) {
        if (firstCatalogReported || homeUiState.sections.isEmpty()) return@LaunchedEffect
        firstCatalogReported = true
        onFirstCatalogRendered?.invoke()
    }

    val visibleCollections = remember(collections) {
        collections.filter { it.folders.isNotEmpty() }
    }
    val collectionsMap = remember(visibleCollections) {
        visibleCollections.associateBy { "collection_${it.id}" }
    }
    val sectionsMap = remember(homeUiState.sections) {
        homeUiState.sections.associateBy(HomeCatalogSection::key)
    }
    val enabledHomeItems = remember(homeSettingsUiState.items) {
        homeSettingsUiState.items.filter { it.enabled }
    }
    val keyedEnabledHomeItems = remember(enabledHomeItems) {
        enabledHomeItems.withDuplicateSafeLazyKeys(HomeCatalogSettingsItem::key)
    }
    val resolvedBadgeInputs = remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf<Triple<WatchedUiState, List<WatchProgressEntry>, String>?>(null)
    }
    ScreenActivityEffect(
        activeProfileId,
        effectiveWatchProgressSource,
        watchedUiState,
        watchProgressUiState.entries,
    ) { active ->
        if (!active) return@ScreenActivityEffect
        val inputs = Triple(watchedUiState, watchProgressUiState.entries, CurrentDateProvider.todayIsoDate())
        if (resolvedBadgeInputs.value == inputs) return@ScreenActivityEffect
        if (
            resolveWatchedBadgesBulk(
                watchedItems = watchedUiState.items,
                progressEntries = watchProgressUiState.entries,
                todayIsoDate = inputs.third,
            )
        ) {
            resolvedBadgeInputs.value = inputs
        }
    }
    val hasRenderableCollectionRows = remember(enabledHomeItems, collectionsMap) {
        enabledHomeItems.any { item ->
            item.isCollection && collectionsMap[item.key] != null
        }
    }
    val hasRenderableHomeRows = homeUiState.sections.isNotEmpty() || hasRenderableCollectionRows
    val showHeroSlot = shouldShowHomeHeroSlot(
        heroEnabled = homeSettingsUiState.heroEnabled,
        hasHeroItems = homeUiState.heroItems.isNotEmpty(),
        isResolvingHeroSources = isResolvingHeroSources,
        hasRenderableHomeRows = hasRenderableHomeRows,
    )
    MaintainHomeScrollPosition(
        listState = homeListState,
        profileId = activeProfileId,
        showHeroSlot = showHeroSlot,
    )
    val showHeroSkeleton = showHeroSlot &&
        homeUiState.heroItems.isEmpty() &&
        isResolvingHeroSources
    val isInitialHomeContentLoading = shouldShowInitialHomeLoading(
        hasRenderableHomeRows = hasRenderableHomeRows,
        addonManifestsLoading = addonManifestsLoading,
        homeCatalogLoading = homeUiState.isLoading,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val homeSectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value)
        val posterCardStyle = rememberPosterCardStyleUiState()
        val continueWatchingLayout = rememberContinueWatchingLayout(maxWidth.value, posterCardStyle)
        val nativeBottomNavigationOverlayHeight =
            if (LocalBottomNavigationOverlayPadding.current > 0.dp) {
                safeBottomPadding()
            } else {
                0.dp
            }
        val mobileHeroBelowSectionHeightHint = remember(
            maxWidth.value,
            continueWatchingPreferences.isVisible,
            continueWatchingPreferences.style,
            hasContinueWatchingRows,
            continueWatchingLayout,
            posterCardStyle.widthDp,
            nativeBottomNavigationOverlayHeight,
        ) {
            if (
                maxWidth.value < 600f &&
                continueWatchingPreferences.isVisible &&
                hasContinueWatchingRows
            ) {
                continueWatchingHeroViewportReserveHeight(
                    style = continueWatchingPreferences.style,
                    layout = continueWatchingLayout,
                    basePosterWidthDp = posterCardStyle.widthDp,
                ) + nativeBottomNavigationOverlayHeight
            } else {
                null
            }
        }

        val continueWatchingSections: LazyListScope.() -> Unit = {
            homeContinueWatchingSections(
                preferences = continueWatchingPreferences,
                continueWatchingItems = continueWatchingItems,
                upcomingItems = upcomingItems,
                dataSourceKey = effectiveWatchProgressSource,
                sectionPadding = homeSectionPadding,
                layout = continueWatchingLayout,
                continueWatchingListState = continueWatchingListState,
                upcomingListState = upcomingListState,
                onItemClick = onContinueWatchingClick,
                onItemLongPress = onContinueWatchingLongPress,
                disintegrationRequest = continueWatchingDisintegrationRequest,
            )
        }

        // No title: Home's hero is its heading, so it gets no app bar.
        ScreenScaffold(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = 0.dp,
            topPadding = if (showHeroSlot) 0.dp else null,
            listState = homeListState,
        ) {
            if (showHeroSlot) {
                item(key = "home_hero", contentType = "hero") {
                    HomeHeroSlot(
                        showSkeleton = showHeroSkeleton,
                        heroItems = homeUiState.heroItems,
                        viewportHeight = maxHeight,
                        mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                        onItemClick = onPosterClick,
                    )
                }
            }

            when {
                isInitialHomeContentLoading -> {
                    continueWatchingSections()
                    items(
                        count = 3,
                        key = { "home_skeleton_$it" },
                        contentType = { "skeleton" },
                    ) {
                        HomeSkeletonRow(
                            horizontalPadding = homeSectionPadding,
                        )
                    }
                }

                !hasActiveAddons && !hasRenderableCollectionRows -> {
                    continueWatchingSections()
                    item(key = "home_empty", contentType = "empty") {
                        HomeNoAddonsState(
                            condition = networkStatusUiState.condition,
                            isOfflineLike = networkStatusUiState.isOfflineLike,
                            manifestErrorMessage = addonManifestErrorMessage,
                        )
                    }
                }

                homeUiState.sections.isEmpty() && homeUiState.heroItems.isEmpty() &&
                    (!continueWatchingPreferences.isVisible || !hasContinueWatchingRows) &&
                    !hasRenderableCollectionRows -> {
                    item(key = "home_empty", contentType = "empty") {
                        HomeNoRowsState(
                            condition = networkStatusUiState.condition,
                            isOfflineLike = networkStatusUiState.isOfflineLike,
                            errorMessage = homeUiState.errorMessage,
                            onRetry = {
                                NetworkStatusRepository.requestRefresh(force = true)
                                HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                            },
                        )
                    }
                }

                else -> {
                    continueWatchingSections()

                    homeRows(
                        keyedItems = keyedEnabledHomeItems,
                        collectionsMap = collectionsMap,
                        sectionsMap = sectionsMap,
                        sectionPadding = homeSectionPadding,
                        animateCollectionGifs = animateCollectionGifs,
                        watchedKeys = watchedUiState.watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        onCatalogClick = onCatalogClick,
                        onFolderClick = onFolderClick,
                        onPosterClick = onPosterClick,
                        onPosterLongClick = onPosterLongClick,
                    )
                }
            }
        }
    }
}

// Shown when no addon is active: why, and a retry when the manifests failed to load.
@Composable
private fun HomeNoAddonsState(
    condition: NetworkCondition,
    isOfflineLike: Boolean,
    manifestErrorMessage: String?,
) {
    val retry: () -> Unit = {
        NetworkStatusRepository.requestRefresh(force = true)
        AddonRepository.refreshAll()
    }
    when {
        isOfflineLike && manifestErrorMessage != null -> {
            NetworkOfflineCard(
                condition = condition,
                modifier = Modifier.padding(horizontal = 16.dp),
                onRetry = retry,
            )
        }

        manifestErrorMessage != null -> {
            HomeEmptyStateCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(Res.string.home_load_failed_title),
                message = manifestErrorMessage,
                actionLabel = stringResource(Res.string.action_retry),
                onActionClick = retry,
            )
        }

        else -> {
            EmptyState(
                icon = Icons.Rounded.Extension,
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(Res.string.compose_search_empty_no_active_addons_title),
                message = stringResource(Res.string.home_empty_no_active_addons_message),
            )
        }
    }
}

// Shown when addons are active but produced no rows, or the catalogs failed to load.
@Composable
private fun HomeNoRowsState(
    condition: NetworkCondition,
    isOfflineLike: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    val loadFailed = !errorMessage.isNullOrBlank()
    if (isOfflineLike && loadFailed) {
        NetworkOfflineCard(
            condition = condition,
            modifier = Modifier.padding(horizontal = 16.dp),
            onRetry = onRetry,
        )
    } else {
        HomeEmptyStateCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = stringResource(
                if (loadFailed) Res.string.home_load_failed_title else Res.string.home_empty_no_rows_title,
            ),
            message = errorMessage ?: stringResource(Res.string.home_empty_no_rows_message),
            actionLabel = if (loadFailed) stringResource(Res.string.action_retry) else null,
            onActionClick = if (loadFailed) onRetry else null,
        )
    }
}

private const val HOME_CATALOG_PREVIEW_LIMIT = 18

internal const val HOME_CONTINUE_WATCHING_SECTION_KEY = "home_continue_watching"

internal const val HOME_UPCOMING_SECTION_KEY = "home_upcoming"

// Refreshes the catalogs once the connection comes back after being lost.
@Composable
private fun HomeReconnectEffect(condition: NetworkCondition, addons: List<ManagedAddon>) {
    var observedOfflineState by remember { mutableStateOf(false) }
    ScreenActivityEffect(condition) { active ->
        if (!active) return@ScreenActivityEffect
        when (condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (observedOfflineState) {
                    observedOfflineState = false
                    HomeRepository.refresh(addons.enabledAddons(), force = true)
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }
}

// The hero at the top: a skeleton while its sources load, then the hero or the space it keeps.
@Composable
private fun HomeHeroSlot(
    showSkeleton: Boolean,
    heroItems: List<MetaPreview>,
    viewportHeight: Dp,
    mobileBelowSectionHeightHint: Dp?,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    Crossfade(
        targetState = showSkeleton,
        animationSpec = tween(320),
        label = "HomeHeroLoading",
    ) { isLoading ->
        when {
            isLoading -> HomeSkeletonHero(
                modifier = Modifier,
                viewportHeight = viewportHeight,
                mobileBelowSectionHeightHint = mobileBelowSectionHeightHint,
            )

            heroItems.isNotEmpty() -> HomeHeroSection(
                items = heroItems,
                modifier = Modifier,
                viewportHeight = viewportHeight,
                mobileBelowSectionHeightHint = mobileBelowSectionHeightHint,
                onItemClick = onItemClick,
            )

            else -> HomeHeroReservedSpace(
                modifier = Modifier,
                viewportHeight = viewportHeight,
                mobileBelowSectionHeightHint = mobileBelowSectionHeightHint,
            )
        }
    }
}

// The collection and catalog rows, in the order the home settings put them.
private fun LazyListScope.homeRows(
    keyedItems: List<DuplicateSafeLazyEntry<HomeCatalogSettingsItem>>,
    collectionsMap: Map<String, Collection>,
    sectionsMap: Map<String, HomeCatalogSection>,
    sectionPadding: Dp,
    animateCollectionGifs: Boolean,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    onCatalogClick: ((HomeCatalogSection) -> Unit)?,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)?,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onPosterLongClick: ((MetaPreview) -> Unit)?,
) {
    keyedItems.forEach { keyedSettingsItem ->
        val settingsItem = keyedSettingsItem.value
        if (settingsItem.isCollection) {
            val collection = collectionsMap[settingsItem.key] ?: return@forEach
            item(key = keyedSettingsItem.lazyKey, contentType = "collection") {
                HomeCollectionRowSection(
                    collection = collection,
                    modifier = Modifier.padding(bottom = 12.dp),
                    sectionPadding = sectionPadding,
                    animateGifs = animateCollectionGifs,
                    onFolderClick = onFolderClick,
                )
            }
        } else {
            val section = sectionsMap[settingsItem.key]
            if (section == null || section.items.isEmpty()) return@forEach
            item(key = keyedSettingsItem.lazyKey, contentType = "catalog") {
                HomeCatalogRowSection(
                    section = section,
                    entries = section.items.take(HOME_CATALOG_PREVIEW_LIMIT),
                    modifier = Modifier.padding(bottom = 12.dp),
                    sectionPadding = sectionPadding,
                    onViewAllClick = if (section.canOpenCatalog(HOME_CATALOG_PREVIEW_LIMIT)) {
                        onCatalogClick?.let { { it(section) } }
                    } else {
                        null
                    },
                    watchedKeys = watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onPosterClick = onPosterClick,
                    onPosterLongClick = onPosterLongClick,
                )
            }
        }
    }
}
