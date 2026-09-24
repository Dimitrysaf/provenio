package com.nuvio.app.shell.screens.home

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.shell.components.LocalNuvioBottomNavigationOverlayPadding
import com.nuvio.app.shell.components.ScreenActivityEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.NuvioScreen
import com.nuvio.app.shell.components.NuvioNetworkOfflineCard
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.shell.components.rememberPosterCardStyleUiState
import com.nuvio.app.shell.components.withDuplicateSafeLazyKeys
import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.addons.firstEnabledManifestError
import com.nuvio.app.core.cloud.CloudLibraryRepository
import com.nuvio.app.shell.screens.home.components.HomeCatalogRowSection
import com.nuvio.app.shell.screens.home.components.HomeEmptyStateCard
import com.nuvio.app.shell.screens.home.components.HomeHeroReservedSpace
import com.nuvio.app.shell.screens.home.components.HomeHeroSection
import com.nuvio.app.shell.screens.home.components.HomeSkeletonHero
import com.nuvio.app.shell.screens.home.components.HomeSkeletonRow
import com.nuvio.app.core.tracking.TrackingSettingsRepository
import com.nuvio.app.core.watch.watched.WatchedRepository
import com.nuvio.app.core.watch.watched.WatchedUiState
import com.nuvio.app.core.watch.watched.resolveWatchedBadgesBulk
import com.nuvio.app.core.watch.progress.CurrentDateProvider
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesRepository
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.core.watch.progress.WatchProgressRepository
import com.nuvio.app.core.watch.progress.WatchProgressSourceCoordinator
import com.nuvio.app.shell.components.DisintegrationRequest
import com.nuvio.app.core.collection.CollectionRepository
import com.nuvio.app.core.profiles.ProfileRepository
import com.nuvio.app.shell.screens.home.components.HomeCollectionRowSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.nuvio.app.shell.screens.home.components.continueWatchingHeroViewportReserveHeight
import com.nuvio.app.shell.screens.home.components.homeSectionHorizontalPaddingForWidth
import com.nuvio.app.shell.screens.home.components.rememberContinueWatchingLayout
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.home.HomeCatalogSection
import com.nuvio.app.core.home.MetaPreview
import com.nuvio.app.core.home.canOpenCatalog
import com.nuvio.app.core.home.shouldShowHomeHeroSlot
import com.nuvio.app.core.home.shouldShowInitialHomeLoading
import com.nuvio.app.core.home.HomeCatalogSettingsItem
import com.nuvio.app.core.home.HomeCatalogSettingsRepository
import com.nuvio.app.core.home.HomeRepository

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
    var observedOfflineState by remember { mutableStateOf(false) }

    ScreenActivityEffect(scrollToTopRequests) { active ->
        if (!active) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            homeListState.animateScrollToItem(0)
        }
    }

    ScreenActivityEffect(networkStatusUiState.condition) { active ->
        if (!active) return@ScreenActivityEffect
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (observedOfflineState) {
                    observedOfflineState = false
                    HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

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
            if (LocalNuvioBottomNavigationOverlayPadding.current > 0.dp) {
                nuvioSafeBottomPadding()
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
        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = 0.dp,
            topPadding = if (showHeroSlot) 0.dp else null,
            listState = homeListState,
        ) {
            if (showHeroSlot) {
                item(key = "home_hero", contentType = "hero") {
                    Crossfade(
                        targetState = showHeroSkeleton,
                        animationSpec = tween(320),
                        label = "HomeHeroLoading",
                    ) { isLoading ->
                        when {
                            isLoading -> HomeSkeletonHero(
                                modifier = Modifier,
                                viewportHeight = maxHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                            )

                            homeUiState.heroItems.isNotEmpty() -> HomeHeroSection(
                                items = homeUiState.heroItems,
                                modifier = Modifier,
                                viewportHeight = maxHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                                onItemClick = onPosterClick,
                            )

                            else -> HomeHeroReservedSpace(
                                modifier = Modifier,
                                viewportHeight = maxHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                            )
                        }
                    }
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

                    keyedEnabledHomeItems.forEach { keyedSettingsItem ->
                        val settingsItem = keyedSettingsItem.value
                        if (settingsItem.isCollection) {
                            val collection = collectionsMap[settingsItem.key]
                            if (collection != null) {
                                item(key = keyedSettingsItem.lazyKey, contentType = "collection") {
                                    HomeCollectionRowSection(
                                        collection = collection,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        sectionPadding = homeSectionPadding,
                                        animateGifs = animateCollectionGifs,
                                        onFolderClick = onFolderClick,
                                    )
                                }
                            }
                        } else {
                            val section = sectionsMap[settingsItem.key]
                            if (section != null && section.items.isNotEmpty()) {
                                item(key = keyedSettingsItem.lazyKey, contentType = "catalog") {
                                    HomeCatalogRowSection(
                                        section = section,
                                        entries = section.items.take(HOME_CATALOG_PREVIEW_LIMIT),
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        sectionPadding = homeSectionPadding,
                                        onViewAllClick = if (section.canOpenCatalog(HOME_CATALOG_PREVIEW_LIMIT)) {
                                            onCatalogClick?.let { { it(section) } }
                                        } else {
                                            null
                                        },
                                        watchedKeys = watchedUiState.watchedKeys,
                                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                        onPosterClick = onPosterClick,
                                        onPosterLongClick = onPosterLongClick,
                                    )
                                }
                            }
                        }
                    }
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
            NuvioNetworkOfflineCard(
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
        NuvioNetworkOfflineCard(
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
