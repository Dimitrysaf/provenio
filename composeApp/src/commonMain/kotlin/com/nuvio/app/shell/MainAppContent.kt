package com.nuvio.app.shell

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.nuvio.app.shell.components.LocalPosterClickAnchor
import com.nuvio.app.shell.nav.PosterNavigationState
import com.nuvio.app.shell.nav.posterNavigationEntry
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.sync.SyncManager
import com.nuvio.app.shell.components.DisintegrationRequestController
import com.nuvio.app.shell.components.NativeTabBridge
import com.nuvio.app.shell.components.MediaActionsSheet
import com.nuvio.app.shell.components.MediaSheetAction
import com.nuvio.app.shell.components.NuvioContinueWatchingActionSheet
import com.nuvio.app.shell.components.NuvioStatusModal
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.components.platformExitApp
import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.addons.isWaitingForFirstEnabledManifest
import com.nuvio.app.core.catalog.CatalogTarget
import com.nuvio.app.core.cloud.playbackVideoId
import com.nuvio.app.core.collection.CollectionRepository
import com.nuvio.app.core.metadata.MetaScreenSettingsRepository
import com.nuvio.app.core.downloads.DownloadsRepository
import com.nuvio.app.core.home.HomeCatalogSection
import com.nuvio.app.core.home.HomeCatalogSettingsRepository
import com.nuvio.app.core.home.HomeRepository
import com.nuvio.app.core.home.buildAddonCatalogRefreshSignature
import com.nuvio.app.shell.screens.home.components.shouldBlurContinueWatchingArtwork
import com.nuvio.app.core.library.LibraryRepository
import com.nuvio.app.core.library.LibrarySection
import com.nuvio.app.core.library.LibrarySortOption
import com.nuvio.app.core.library.LibrarySourceMode
import com.nuvio.app.shell.screens.library.LibraryListPickerHost
import com.nuvio.app.shell.screens.library.rememberLibraryListPickerState
import com.nuvio.app.shell.screens.library.PendingTrackingMembershipRemoval
import com.nuvio.app.shell.screens.library.TrackingMembershipRemovalConfirmationHost
import com.nuvio.app.shell.screens.library.executeTrackingMembershipOperation
import com.nuvio.app.core.library.librarySectionItemKey
import com.nuvio.app.core.library.toLibraryItem
import com.nuvio.app.core.library.toMetaPreview
import com.nuvio.app.core.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.core.p2p.P2pSettingsRepository
import com.nuvio.app.core.playback.PlayerSettingsRepository
import com.nuvio.app.shell.screens.player.LockPlayerToLandscape
import com.nuvio.app.shell.screens.player.HidePlayerSystemBars
import com.nuvio.app.core.profiles.ProfileRepository
import com.nuvio.app.shell.screens.settings.AccountSettingsScreen
import com.nuvio.app.shell.screens.settings.AddonsSettingsScreen
import com.nuvio.app.shell.screens.settings.ContinueWatchingSettingsScreen
import com.nuvio.app.shell.screens.settings.HomescreenSettingsScreen
import com.nuvio.app.shell.screens.settings.LicensesAttributionsSettingsScreen
import com.nuvio.app.shell.screens.settings.MetaScreenSettingsScreen
import com.nuvio.app.shell.screens.settings.PluginsSettingsScreen
import com.nuvio.app.shell.screens.settings.SupportersContributorsSettingsScreen
import com.nuvio.app.core.streams.StreamAutoPlayPolicy
import com.nuvio.app.core.tracking.TrackingMembershipApplyResult
import com.nuvio.app.core.tracking.TrackingProviderId
import com.nuvio.app.shell.screens.updater.AppUpdaterHost
import com.nuvio.app.core.updater.AppUpdaterPlatform
import com.nuvio.app.shell.screens.updater.rememberAppUpdaterController
import com.nuvio.app.core.watch.watched.WatchedRepository
import com.nuvio.app.core.watch.watching.application.WatchingActions
import com.nuvio.app.core.watch.watching.application.WatchingState
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesRepository
import com.nuvio.app.core.watch.progress.WatchProgressRepository
import com.nuvio.app.core.watch.progress.continueWatchingItemKey
import com.nuvio.app.core.watch.progress.nextUpDismissKey
import com.nuvio.app.core.watch.progress.toContinueWatchingItem
import com.nuvio.app.shell.nav.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.build.isIos
import com.nuvio.app.core.build.supportsPosterNavigationMotion

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MainAppContent(
    initialTab: AppScreenTab = AppScreenTab.Home,
    initialRoute: AppRoute = TabsRoute,
    useNativeNavigation: Boolean = false,
    useNativeTabBar: Boolean = false,
    useTabletFloatingTabBar: Boolean = false,
    ownsAppRuntime: Boolean = true,
    showLaunchOverlay: Boolean = true,
    onNavigate: ((AppRoute, launchSingleTop: Boolean) -> Unit)? = null,
    onGoBack: (() -> Unit)? = null,
    onReplace: ((AppRoute) -> Unit)? = null,
    onActivate: ((AppScreenTab) -> Unit)? = null,
    onTabTitles: ((home: String, search: String, library: String, profile: String, switchProfile: String, addProfile: String) -> Unit)? = null,
    appGateController: AppGateController? = null,
    onRootContentReady: ((Boolean) -> Unit)? = null,
    onSwitchProfile: () -> Unit = {},
) {
        val navBackStack = rememberNavBackStack(navigationSavedStateConfiguration, initialRoute)
        val posterNavigation = remember { PosterNavigationState() }
        val metaScreenSettings by remember {
            MetaScreenSettingsRepository.ensureLoaded()
            MetaScreenSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val posterNavigationEnabled = supportsPosterNavigationMotion &&
            metaScreenSettings.posterTransitionEnabled && onNavigate == null
        val routeDisposalDecorator = remember {
            RouteDisposalNavEntryDecorator<NavKey> { key ->
                if (key is AppRoute) disposeRoute(key)
            }
        }
        val navController = remember(navBackStack, onNavigate, onGoBack, onReplace, posterNavigationEnabled) {
            NuvioNavigator(
                backStack = navBackStack,
                onExternalNavigate = onNavigate,
                onExternalBack = onGoBack,
                onExternalReplace = onReplace,
                onLocalNavigate = if (posterNavigationEnabled) posterNavigation::navigate else null,
                onLocalPop = if (posterNavigationEnabled) posterNavigation::clear else null,
            )
        }
        val appUpdaterController = rememberAppUpdaterController()
        AppRuntimeServices(ownsAppRuntime)
        val hapticFeedback = LocalHapticFeedback.current
        val focusManager = LocalFocusManager.current
        val uriHandler = LocalUriHandler.current
        val coroutineScope = rememberCoroutineScope()
        var selectedTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
        var searchFocusRequestCount by remember { mutableStateOf(0) }
        val homeScrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val searchScrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val searchListState = rememberLazyListState()
        val libraryScrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val settingsRootActionRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val currentRoute = navBackStack.lastOrNull() as? AppRoute
        PosterNavigationEffects(posterNavigation, currentRoute, posterNavigationEnabled)
        var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
        var selectedPosterActionTarget by remember { mutableStateOf<PosterActionTarget?>(null) }
        var selectedContinueWatchingForActions by remember { mutableStateOf<ContinueWatchingItem?>(null) }
        val libraryDisintegrationRequests = remember { DisintegrationRequestController<String>() }
        val continueWatchingDisintegrationRequests = remember { DisintegrationRequestController<String>() }
        var requestedSettingsPageName by rememberSaveable { mutableStateOf<String?>(null) }
        val libraryListPicker = rememberLibraryListPickerState()
        var pendingTrackingRemoval by remember { mutableStateOf<PendingTrackingMembershipRemoval?>(null) }
        val trackingListsUpdateFailedMessage = stringResource(Res.string.tracking_lists_update_failed)
        val addonsUiState by remember {
            AddonRepository.initialize()
            AddonRepository.uiState
        }.collectAsStateWithLifecycle()
        val libraryUiState by remember {
            LibraryRepository.ensureLoaded()
            LibraryRepository.uiState
        }.collectAsStateWithLifecycle()
        val authState by AuthRepository.state.collectAsStateWithLifecycle()
        val openPosterActions: (PosterActionTarget) -> Unit = { target ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            focusManager.clearFocus(force = true)
            coroutineScope.launch {
                withFrameNanos { }
                selectedPosterActionTarget = target
            }
        }
        val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
        val launchOverlayProfile = profileState.activeProfile ?: profileState.profiles.firstOrNull()
    val playerSettingsUiState by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    var visiblePlayerEntries by remember { mutableIntStateOf(0) }
    var streamLandscapeLoadingVisible by remember(currentRoute) { mutableStateOf(false) }
    if (currentRoute is PlayerRoute || visiblePlayerEntries > 0 || streamLandscapeLoadingVisible) {
        LockPlayerToLandscape()
        HidePlayerSystemBars()
    }
    val p2pSettingsUiState by remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val downloadsUiState by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    val networkStatusUiState by remember {
        NetworkStatusRepository.uiState
    }.collectAsStateWithLifecycle()
    val homescreenSettingsTitle = stringResource(Res.string.compose_settings_page_homescreen)
    val metaScreenSettingsTitle = stringResource(Res.string.compose_settings_page_meta_screen)
    val continueWatchingSettingsTitle = stringResource(Res.string.compose_settings_page_continue_watching)
    val debridSettingsTitle = stringResource(Res.string.compose_settings_page_debrid)
    val downloadsSettingsTitle = stringResource(Res.string.compose_settings_root_downloads_title)
    val addonsSettingsTitle = stringResource(Res.string.compose_settings_page_addons)
    val pluginsSettingsTitle = stringResource(Res.string.compose_settings_page_plugins)
    val accountSettingsTitle = stringResource(Res.string.compose_settings_page_account)
    val supportersSettingsTitle = stringResource(Res.string.compose_settings_page_supporters_contributors)
    val licensesSettingsTitle = stringResource(Res.string.compose_settings_page_licenses_attributions)
    val collectionsTitle = stringResource(Res.string.collections_header)
    val newCollectionTitle = stringResource(Res.string.collections_new)
    val isRemoteLibrarySource = libraryUiState.sourceMode != LibrarySourceMode.LOCAL
    val appContentGeneration = if (ownsAppRuntime && appGateController != null) {
        val generation by appGateController.contentGeneration.collectAsStateWithLifecycle()
        generation
    } else {
        0
    }
    var initialHomeReady by rememberSaveable(ownsAppRuntime, appContentGeneration) {
        mutableStateOf(!ownsAppRuntime)
    }
    var offlineLaunchRouteHandled by rememberSaveable { mutableStateOf(false) }
    val homeCatalogRefreshKey = remember(addonsUiState.addons) {
        buildAddonCatalogRefreshSignature(addonsUiState.addons)
    }

    LaunchedEffect(appContentGeneration, homeCatalogRefreshKey) {
        if (!ownsAppRuntime) return@LaunchedEffect
        val enabledAddons = addonsUiState.addons.enabledAddons()
        if (enabledAddons.isWaitingForFirstEnabledManifest()) return@LaunchedEffect
        HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        HomeRepository.refresh(enabledAddons)
    }

    fun activateTab(tab: AppScreenTab) {
        if (useNativeNavigation && onActivate != null) {
            onActivate(tab)
        } else {
            selectedTab = tab
        }
    }

    fun handleRootTabClick(tab: AppScreenTab) {
        if (selectedTab != tab) {
            activateTab(tab)
            return
        }

        when (tab) {
            AppScreenTab.Home -> homeScrollToTopRequests.tryEmit(Unit)
            AppScreenTab.Search -> {
                searchFocusRequestCount++
                searchScrollToTopRequests.tryEmit(Unit)
            }
            AppScreenTab.Library -> libraryScrollToTopRequests.tryEmit(Unit)
            AppScreenTab.Settings -> settingsRootActionRequests.tryEmit(Unit)
        }
    }

    LaunchedEffect(
        useNativeNavigation,
        onActivate,
        initialTab,
        currentRoute,
    ) {
        NativeTabBridge.requestedTabs.collectLatest { requestedTab ->
            val requestedAppTab = requestedTab.toAppScreenTab()
            if (
                useNativeNavigation &&
                currentRoute is TabsRoute &&
                requestedAppTab == selectedTab
            ) {
                handleRootTabClick(requestedAppTab)
            }
        }
    }

    NativeTabTitlesEffect(onTabTitles)

    LaunchedEffect(initialTab) {
        snapshotFlow { selectedTab }.collectLatest { tab ->
            NativeTabBridge.publishSelectedTab(tab.toNativeNavigationTab())
            if (tab != AppScreenTab.Search) {
                searchFocusRequestCount = 0
            }
        }
    }

    var profileSwitchLoading by remember { mutableStateOf(false) }

    val rootContentReady = !ownsAppRuntime || (initialHomeReady && !profileSwitchLoading)
    val launchOverlayVisible = ownsAppRuntime && showLaunchOverlay && !rootContentReady
    val launchOverlayState = remember(ownsAppRuntime, showLaunchOverlay) {
        MutableTransitionState(
            launchOverlayVisible,
        )
    }
    launchOverlayState.targetState = launchOverlayVisible

    LaunchedEffect(
        rootContentReady,
        ownsAppRuntime,
        onRootContentReady,
    ) {
        if (ownsAppRuntime) {
            onRootContentReady?.invoke(rootContentReady)
        }
    }

    LaunchedEffect(appContentGeneration) {
        if (!ownsAppRuntime) return@LaunchedEffect
        NetworkStatusRepository.ensureStarted()
        EpisodeReleaseNotificationsRepository.refreshAsync()
        kotlinx.coroutines.delay(5_000)
        initialHomeReady = true
    }

    NetworkToastEffect(ownsAppRuntime, networkStatusUiState.condition)

    WatchSourceReconnectEffect(
        enabled = ownsAppRuntime,
        condition = networkStatusUiState.condition,
        authState = authState,
        activeProfileIndex = profileState.activeProfile?.profileIndex,
    )

    LaunchedEffect(
        initialHomeReady,
        offlineLaunchRouteHandled,
        networkStatusUiState.condition,
        downloadsUiState.completedItems,
    ) {
        if (!ownsAppRuntime) return@LaunchedEffect
        if (!initialHomeReady || offlineLaunchRouteHandled) return@LaunchedEffect

        when (networkStatusUiState.condition) {
            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> return@LaunchedEffect

            NetworkCondition.Online -> {
                offlineLaunchRouteHandled = true
            }

            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                offlineLaunchRouteHandled = true
                val hasPlayableDownload = downloadsUiState.completedItems.any {
                    DownloadsRepository.playableLocalFileUri(it) != null
                }
                if (hasPlayableDownload) {
                    activateTab(AppScreenTab.Settings)
                    navController.navigate(DownloadsSettingsRoute(downloadsSettingsTitle)) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    ForegroundSyncEffect(
        enabled = ownsAppRuntime,
        authState = authState,
        activeProfileIndex = profileState.activeProfile?.profileIndex,
    )
    val activePlaybackProfileId = profileState.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId
    val playback = rememberAppPlayback(navController, activePlaybackProfileId)
    val continueWatchingPreferencesUiState by remember {
        ContinueWatchingPreferencesRepository.ensureLoaded()
        ContinueWatchingPreferencesRepository.uiState
    }.collectAsStateWithLifecycle()

        AppDeepLinkEffect(navController, ownsAppRuntime, ::activateTab)

        val onCatalogClick: (HomeCatalogSection) -> Unit = { section ->
            val launchId = CatalogLaunchStore.put(
                CatalogLaunch(
                    title = section.title,
                    subtitle = section.subtitle,
                    target = section.target,
                ),
            )
            navController.navigate(
                CatalogRoute(
                    launchId = launchId,
                    title = section.title,
                    subtitle = section.subtitle,
                ),
            )
        }

        val librarySectionSubtitle = when (libraryUiState.sourceMode) {
            LibrarySourceMode.LOCAL -> stringResource(Res.string.compose_catalog_subtitle_library)
            LibrarySourceMode.TRAKT -> stringResource(Res.string.compose_catalog_subtitle_trakt_library)
            LibrarySourceMode.SIMKL -> stringResource(Res.string.compose_catalog_subtitle_simkl_library)
        }

        val onLibrarySectionViewAllClick: (LibrarySection, LibrarySortOption) -> Unit = { section, sortOption ->
            val launchId = CatalogLaunchStore.put(
                CatalogLaunch(
                    title = section.displayTitle,
                    subtitle = librarySectionSubtitle,
                    target = CatalogTarget.Library(
                        contentType = section.items.firstOrNull()?.type ?: "movie",
                        sectionType = section.type,
                        sortOption = sortOption,
                    ),
                ),
            )
            navController.navigate(
                CatalogRoute(
                    launchId = launchId,
                    title = section.displayTitle,
                    subtitle = librarySectionSubtitle,
                ),
            )
        }

        val onContinueWatchingRemove: (ContinueWatchingItem) -> Unit = { item ->
            continueWatchingDisintegrationRequests.arm(continueWatchingItemKey(item))
            if (item.isNextUp) {
                ContinueWatchingPreferencesRepository.addDismissedNextUpKey(
                    nextUpDismissKey(
                        item.parentMetaId,
                        item.nextUpSeedSeasonNumber,
                        item.nextUpSeedEpisodeNumber,
                    ),
                )
            } else {
                WatchProgressRepository.removeProgress(contentId = item.parentMetaId)
            }
        }

        val onContinueWatchingLongPress: (ContinueWatchingItem) -> Unit = { item ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            selectedContinueWatchingForActions = item
        }

        AppUpdaterHost(
            controller = appUpdaterController,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
            SharedTransitionLayout {
                CompositionLocalProvider(
                    LocalPosterClickAnchor provides if (posterNavigationEnabled) posterNavigation::prepare else null,
                    LocalUseNativeNavigation provides useNativeNavigation,
                    LocalNativeNavigationBarHidden provides (currentRoute?.hidesNavigationBar == true),
                ) {
                NavDisplay(
                    backStack = navBackStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { navController.popBackStack() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                        routeDisposalDecorator,
                    ),
                    // Sources is a sheet, so its destination draws over the one beneath it
                    // instead of replacing it.
                    sceneStrategies = remember { listOf(SheetOverlaySceneStrategy()) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    entryProvider = entryProvider<NavKey> {
                entry<TabsRoute> {
                    MainTabsDestination(
                        selectedTab = selectedTab,
                        initialHomeReady = initialHomeReady,
                        rootRouteActive = currentRoute is TabsRoute,
                        useTabletFloatingTabBar = useTabletFloatingTabBar,
                        useNativeNavigation = useNativeNavigation,
                        useNativeTabBar = useNativeTabBar,
                        requests = remember(
                            homeScrollToTopRequests,
                            searchScrollToTopRequests,
                            libraryScrollToTopRequests,
                            settingsRootActionRequests,
                        ) {
                            AppTabRequests(
                                homeScrollToTopRequests = homeScrollToTopRequests,
                                searchScrollToTopRequests = searchScrollToTopRequests,
                                libraryScrollToTopRequests = libraryScrollToTopRequests,
                                settingsRootActionRequests = settingsRootActionRequests,
                            )
                        },
                        state = remember(
                            searchListState,
                            appContentGeneration,
                            profileState.activeProfile?.profileIndex,
                            searchFocusRequestCount,
                            currentRoute is TabsRoute,
                            libraryDisintegrationRequests.current,
                            continueWatchingDisintegrationRequests.current,
                            requestedSettingsPageName,
                        ) {
                            AppTabState(
                                searchListState = searchListState,
                                homeContentGeneration = appContentGeneration,
                                profileId = profileState.activeProfile?.profileIndex,
                                searchFocusRequestCount = searchFocusRequestCount,
                                rootActionsEnabled = currentRoute is TabsRoute,
                                animateHomeCollectionGifs = currentRoute is TabsRoute,
                                libraryDisintegrationRequest = libraryDisintegrationRequests.current,
                                continueWatchingDisintegrationRequest = continueWatchingDisintegrationRequests.current,
                                requestedSettingsPageName = requestedSettingsPageName,
                            )
                        },
                        actions = { isTabletLayout ->
                            AppTabActions(
                                onCatalogClick = onCatalogClick,
                                onPosterClick = { meta ->
                                    navController.navigate(
                                        DetailRoute(type = meta.type, id = meta.id, title = meta.name),
                                    )
                                },
                                onPosterLongClick = { meta ->
                                    openPosterActions(PosterActionTarget(preview = meta))
                                },
                                onLibraryPosterClick = { item ->
                                    navController.navigate(
                                        DetailRoute(type = item.type, id = item.id, title = item.name),
                                    )
                                },
                                onLibraryPosterLongClick = { item, section ->
                                    openPosterActions(
                                        PosterActionTarget(
                                            preview = item.toMetaPreview(),
                                            libraryItem = item,
                                            libraryListKey = section.type,
                                        ),
                                    )
                                },
                                onLibrarySectionViewAllClick = onLibrarySectionViewAllClick,
                                onCloudFilePlay = { item, file ->
                                    coroutineScope.launch {
                                        val resumeItem = WatchProgressRepository
                                            .progressForVideo(
                                                videoId = item.playbackVideoId(file),
                                                parentMetaId = item.id,
                                            )
                                            ?.takeIf { it.isResumable }
                                            ?.toContinueWatchingItem()
                                        if (
                                            !playback.launchCloudLibraryFile(
                                                item = item,
                                                file = file,
                                                resumePositionMs = resumeItem?.resumePositionMs,
                                                resumeProgressFraction = resumeItem?.resumeProgressFraction,
                                            )
                                        ) {
                                            NuvioToastController.show(playback.strings.cloudPlayFailed)
                                        }
                                    }
                                },
                                onConnectCloudClick = {
                                    if (useNativeNavigation && !isTabletLayout) {
                                        activateTab(AppScreenTab.Settings)
                                        navController.navigate(
                                            SettingsPageRoute(
                                                pageName = "Debrid",
                                                title = debridSettingsTitle,
                                            )
                                        )
                                    } else {
                                        requestedSettingsPageName = "Debrid"
                                        activateTab(AppScreenTab.Settings)
                                    }
                                },
                                onContinueWatchingClick = { item -> playback.openContinueWatching(item) },
                                onContinueWatchingLongPress = onContinueWatchingLongPress,
                                onSwitchProfile = onSwitchProfile,
                                onSettingsPageClick = if (useNativeNavigation && !isTabletLayout) {
                                    { pageName, title ->
                                        navController.navigate(SettingsPageRoute(pageName, title))
                                    }
                                } else {
                                    null
                                },
                                onHomescreenSettingsClick = { navController.navigate(HomescreenSettingsRoute(homescreenSettingsTitle)) },
                                onMetaScreenSettingsClick = { navController.navigate(MetaScreenSettingsRoute(metaScreenSettingsTitle)) },
                                onContinueWatchingSettingsClick = { navController.navigate(ContinueWatchingSettingsRoute(continueWatchingSettingsTitle)) },
                                onDownloadsSettingsClick = { navController.navigate(DownloadsSettingsRoute(downloadsSettingsTitle)) },
                                onAddonsSettingsClick = { navController.navigate(AddonsSettingsRoute(addonsSettingsTitle)) },
                                onPluginsSettingsClick = {
                                    if (AppFeaturePolicy.pluginsEnabled) {
                                        navController.navigate(PluginsSettingsRoute(pluginsSettingsTitle))
                                    }
                                },
                                onAccountSettingsClick = { navController.navigate(AccountSettingsRoute(accountSettingsTitle)) },
                                onSupportersContributorsSettingsClick = {
                                    if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                                        navController.navigate(SupportersContributorsSettingsRoute(supportersSettingsTitle))
                                    }
                                },
                                onLicensesAttributionsSettingsClick = {
                                    navController.navigate(LicensesAttributionsSettingsRoute(licensesSettingsTitle))
                                },
                                onCheckForUpdatesClick = if (AppFeaturePolicy.inAppUpdaterEnabled) {
                                    {
                                        appUpdaterController.checkForUpdates(
                                            force = true,
                                            showNoUpdateFeedback = true,
                                        )
                                    }
                                } else {
                                    null
                                },
                                onTestUpdateBannerClick = if (
                                    AppFeaturePolicy.inAppUpdaterEnabled && AppUpdaterPlatform.isDebugBuild
                                ) {
                                    appUpdaterController::showDebugTestUpdate
                                } else {
                                    null
                                },
                                onCollectionsSettingsClick = { navController.navigate(CollectionsRoute(collectionsTitle)) },
                                onFolderClick = { collectionId, folderId ->
                                    val folderTitle = CollectionRepository.collections.value
                                        .firstOrNull { it.id == collectionId }
                                        ?.folders
                                        ?.firstOrNull { it.id == folderId }
                                        ?.title
                                        .orEmpty()
                                    navController.navigate(
                                        FolderDetailRoute(
                                            collectionId = collectionId,
                                            folderId = folderId,
                                            title = folderTitle.ifBlank { collectionsTitle },
                                        )
                                    )
                                },
                                onRequestedSettingsPageConsumed = {
                                    requestedSettingsPageName = null
                                },
                                onInitialHomeContentRendered = { initialHomeReady = true },
                            )
                        },
                        onBack = {
                            if (selectedTab != AppScreenTab.Home) {
                                activateTab(AppScreenTab.Home)
                            } else {
                                showExitConfirmation = !showExitConfirmation
                            }
                        },
                        onTabSelected = ::handleRootTabClick,
                        onProfileSelected = { profile ->
                            if (profile.profileIndex != ProfileRepository.state.value.activeProfile?.profileIndex) {
                                profileSwitchLoading = true
                                activateTab(AppScreenTab.Home)
                                ProfileRepository.selectProfile(profile.profileIndex)
                                SyncManager.pullAllForProfile(profile.profileIndex)
                            }
                        },
                        onAddProfileRequested = onSwitchProfile,
                    )
                }
                entry<DetailRoute> { route ->
                    DetailsDestination(
                        route = route,
                        navController = navController,
                        onPlay = playback.onPlay,
                        onPlayManually = playback.onPlayManually,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    )
                }
                entry<PersonDetailRoute> { route ->
                    PersonDestination(
                        route = route,
                        navController = navController,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    )
                }
                entry<EntityBrowseRoute> { route ->
                    EntityDestination(route = route, navController = navController)
                }
                entry<StreamRoute>(metadata = sheetRouteMetadata()) { route ->
                    StreamDestination(
                        route = route,
                        onLandscapeLoadingChanged = { visible ->
                            if (currentRoute == route) streamLandscapeLoadingVisible = visible
                        },
                        navController = navController,
                        p2pEnabled = p2pSettingsUiState.p2pEnabled,
                        openExternalPlayback = playback::openExternalPlayback,
                        openExternalStreamUrl = playback::openExternalStreamUrl,
                    )
                }
                entry<PlayerRoute>(
                    metadata = if (isIos) {
                        NavDisplay.transitionSpec {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(220))
                        } + NavDisplay.popTransitionSpec {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(220))
                        }
                    } else {
                        emptyMap()
                    },
                ) { route ->
                    if (!isIos) {
                        DisposableEffect(route) {
                            visiblePlayerEntries += 1
                            onDispose { visiblePlayerEntries -= 1 }
                        }
                    }
                    PlayerDestination(
                        route = route,
                        navController = navController,
                        externalPlayerId = playerSettingsUiState.externalPlayerId,
                        externalPlayerNotConfiguredText = playback.strings.externalPlayerNotConfigured,
                        externalPlayerFailedText = playback.strings.externalPlayerFailed,
                        onExternalPlayerLaunch = playback.recordExternalLaunch,
                        launchExternalPlayer = playback.launchExternalPlayer,
                        openExternalStreamUrl = playback::openExternalStreamUrl,
                    )
                }
                entry<CatalogRoute> { route ->
                    CatalogDestination(
                        route = route,
                        navController = navController,
                        onPosterLongClick = openPosterActions,
                    )
                }
                entry<HomescreenSettingsRoute> { route ->
                    SettingsDestination(route, navController) { onBack ->
                        HomescreenSettingsScreen(onBack = onBack)
                    }
                }
                entry<MetaScreenSettingsRoute> { route ->
                    SettingsDestination(route, navController) { onBack ->
                        MetaScreenSettingsScreen(onBack = onBack)
                    }
                }
                entry<ContinueWatchingSettingsRoute> { route ->
                    SettingsDestination(route, navController) { onBack ->
                        ContinueWatchingSettingsScreen(onBack = onBack)
                    }
                }
                entry<SettingsPageRoute> { route ->
                    SettingsRootDestination(
                        route = route,
                        navController = navController,
                        useNativeNavigation = useNativeNavigation,
                        downloadsTitle = downloadsSettingsTitle,
                        collectionsTitle = collectionsTitle,
                        onCheckForUpdates = if (AppFeaturePolicy.inAppUpdaterEnabled) {
                            { appUpdaterController.checkForUpdates(force = true, showNoUpdateFeedback = true) }
                        } else null,
                        onTestUpdateBanner = if (
                            AppFeaturePolicy.inAppUpdaterEnabled && AppUpdaterPlatform.isDebugBuild
                        ) appUpdaterController::showDebugTestUpdate else null,
                    )
                }
                entry<DownloadsSettingsRoute> { route ->
                    DownloadsDestination(
                        route = route,
                        navController = navController,
                        useNativeNavigation = useNativeNavigation,
                        onOpenDownload = playback::openDownloadedItem,
                    )
                }
                entry<DownloadShowRoute> { route ->
                    DownloadShowDestination(
                        route = route,
                        navController = navController,
                        onOpenDownload = playback::openDownloadedItem,
                    )
                }
                entry<AddonsSettingsRoute> { route ->
                    SettingsDestination(route, navController) { onBack ->
                        AddonsSettingsScreen(onBack = onBack)
                    }
                }
                if (AppFeaturePolicy.pluginsEnabled) {
                    entry<PluginsSettingsRoute> { route ->
                        SettingsDestination(route, navController) { onBack ->
                            PluginsSettingsScreen(onBack = onBack)
                        }
                    }
                }
                entry<AccountSettingsRoute> { route ->
                    SettingsDestination(route, navController) { onBack ->
                        AccountSettingsScreen(onBack = onBack)
                    }
                }
                entry<SupportersContributorsSettingsRoute> { route ->
                    SettingsDestination(route, navController) { onBack ->
                        if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                            SupportersContributorsSettingsScreen(onBack = onBack)
                        } else {
                            LaunchedEffect(Unit) { onBack() }
                        }
                    }
                }
                entry<LicensesAttributionsSettingsRoute> { route ->
                    SettingsDestination(route, navController) { onBack ->
                        LicensesAttributionsSettingsScreen(onBack = onBack)
                    }
                }
                entry<CollectionsRoute> { route ->
                    CollectionsDestination(
                        route = route,
                        navController = navController,
                        newCollectionTitle = newCollectionTitle,
                    )
                }
                entry<CollectionEditorRoute> { route ->
                    CollectionEditorDestination(
                        route = route,
                        navController = navController,
                        useNativeNavigation = useNativeNavigation,
                    )
                }
                entry<CollectionEditorPageRoute> { route ->
                    CollectionEditorPageDestination(
                        route = route,
                        navController = navController,
                    )
                }
                entry<FolderDetailRoute> { route ->
                    FolderDestination(
                        route = route,
                        navController = navController,
                        onCatalogClick = onCatalogClick,
                    )
                }
                    }.let { provider ->
                        { key ->
                            routeDisposalDecorator.register(
                                key = key,
                                entry = if (posterNavigationEnabled) {
                                    posterNavigationEntry(key, provider(key), posterNavigation)
                                } else {
                                    provider(key)
                                },
                            )
                        }
                    },
                )
                }
            }
            }

            selectedPosterActionTarget?.let { posterActionTarget ->
                key(posterActionTarget) {
                    val preview = posterActionTarget.preview
                    val isSaved = LibraryRepository.isSaved(preview.id, preview.type)
                    val isWatched = WatchingState.isPosterWatched(
                        watchedKeys = watchedUiState.watchedKeys,
                        item = preview,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    )
                    val removesFromLibrary = isSaved &&
                        (posterActionTarget.libraryItem != null || !isRemoteLibrarySource)
                    MediaActionsSheet(
                        imageUrl = preview.poster,
                        title = preview.name,
                        subtitle = preview.releaseInfo
                            ?.takeIf { it.isNotBlank() }
                            ?.let { formatReleaseDateForDisplay(it) }
                            ?: preview.type.replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase() else char.toString()
                            },
                        actions = listOf(
                            MediaSheetAction(
                                icon = if (isSaved) Icons.Default.DeleteOutline else Icons.Default.Add,
                                label = if (isSaved) {
                                    stringResource(Res.string.hero_remove_from_library)
                                } else {
                                    stringResource(Res.string.hero_add_to_library)
                                },
                                isDestructive = removesFromLibrary,
                                onSelected = {
                                    val libraryItem = posterActionTarget.libraryItem
                                        ?: preview.toLibraryItem(savedAtEpochMs = 0L)
                                    if (posterActionTarget.libraryItem != null) {
                                        val animationKey = posterActionTarget.libraryListKey
                                            ?.let { listKey -> librarySectionItemKey(listKey, libraryItem) }
                                        if (isRemoteLibrarySource) {
                                            coroutineScope.launch {
                                                val listKey = posterActionTarget.libraryListKey
                                                val removeMembership: suspend (Set<TrackingProviderId>) ->
                                                    TrackingMembershipApplyResult = { confirmedProviders ->
                                                    if (listKey.isNullOrBlank()) {
                                                        val currentMembership = LibraryRepository.getMembershipSnapshot(libraryItem)
                                                        LibraryRepository.applyMembershipChanges(
                                                            item = libraryItem,
                                                            desiredMembership = currentMembership.mapValues { false },
                                                            confirmedRemovalProviders = confirmedProviders,
                                                        )
                                                    } else {
                                                        LibraryRepository.removeFromList(
                                                            item = libraryItem,
                                                            listKey = listKey,
                                                            confirmedRemovalProviders = confirmedProviders,
                                                        )
                                                    }
                                                }
                                                val removeMembershipWithAnimation:
                                                    suspend (Set<TrackingProviderId>) -> TrackingMembershipApplyResult =
                                                    { confirmedProviders ->
                                                        val request = if (removesFromLibrary) {
                                                            animationKey?.let(libraryDisintegrationRequests::arm)
                                                        } else {
                                                            null
                                                        }
                                                        try {
                                                            removeMembership(confirmedProviders).also { result ->
                                                                if (result.requiresRemovalConfirmation && request != null) {
                                                                    libraryDisintegrationRequests.cancel(request)
                                                                }
                                                            }
                                                        } catch (error: Throwable) {
                                                            request?.let(libraryDisintegrationRequests::cancel)
                                                            throw error
                                                        }
                                                    }
                                                executeTrackingMembershipOperation(
                                                    operation = { removeMembershipWithAnimation(emptySet()) },
                                                    onSuccess = { result ->
                                                        if (result.requiresRemovalConfirmation) {
                                                            pendingTrackingRemoval = PendingTrackingMembershipRemoval(
                                                                itemTitle = libraryItem.name,
                                                                confirmations = result.requiredRemovalConfirmations,
                                                                retry = removeMembershipWithAnimation,
                                                                onApplied = {},
                                                                onFailure = { error ->
                                                                    NuvioToastController.show(
                                                                        error.message
                                                                            ?: trackingListsUpdateFailedMessage,
                                                                    )
                                                                },
                                                            )
                                                        }
                                                    },
                                                    onFailure = { error ->
                                                        NuvioToastController.show(
                                                            error.message ?: trackingListsUpdateFailedMessage,
                                                        )
                                                    },
                                                )
                                            }
                                        } else {
                                            if (removesFromLibrary) {
                                                animationKey?.let(libraryDisintegrationRequests::arm)
                                            }
                                            LibraryRepository.remove(libraryItem.id)
                                        }
                                    } else {
                                        if (!isRemoteLibrarySource) {
                                            LibraryRepository.toggleLocalSaved(libraryItem)
                                        } else {
                                            libraryListPicker.open(libraryItem, preview.name)
                                        }
                                    }
                                },
                            ),
                            MediaSheetAction(
                                icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                label = if (isWatched) {
                                    stringResource(Res.string.hero_mark_unwatched)
                                } else {
                                    stringResource(Res.string.hero_mark_watched)
                                },
                                onSelected = {
                                    coroutineScope.launch {
                                        WatchingActions.togglePosterWatched(preview)
                                    }
                                },
                            ),
                        ),
                        onDismiss = { selectedPosterActionTarget = null },
                    )
                }
            }

            // The item is taken from the state once, here: the sheet clears that state as it
            // closes, so a callback that read it again would find nothing to act on.
            selectedContinueWatchingForActions?.let { actionsItem ->
                NuvioContinueWatchingActionSheet(
                    item = actionsItem,
                    showManualPlayOption = StreamAutoPlayPolicy.isEffectivelyEnabled(playerSettingsUiState) &&
                        playback.canSelectContinueWatchingStreams(actionsItem),
                    showDetailsOption = !actionsItem.isCloudLibraryContinueWatchingItem(),
                    blurThumbnail = actionsItem.shouldBlurContinueWatchingArtwork(
                        blurUnwatchedEpisodes = continueWatchingPreferencesUiState.blurNextUp,
                        useEpisodeThumbnails = continueWatchingPreferencesUiState.useEpisodeThumbnails,
                        artworkUrl = actionsItem.poster ?: actionsItem.imageUrl,
                    ),
                    onDismiss = { selectedContinueWatchingForActions = null },
                    onOpenDetails = {
                        navController.navigate(
                            DetailRoute(
                                type = actionsItem.parentMetaType,
                                id = actionsItem.parentMetaId,
                                title = actionsItem.title,
                            ),
                        )
                    },
                    onStartFromBeginning = actionsItem
                        .takeIf { !it.isNextUp && playback.canPlayContinueWatching(it) }
                        ?.let { item -> { playback.openContinueWatching(item, startFromBeginning = true) } },
                    onPlayManually = { playback.openContinueWatching(actionsItem, manualSelection = true) },
                    onRemove = { onContinueWatchingRemove(actionsItem) },
                )
            }

            LibraryListPickerHost(
                state = libraryListPicker,
                onRemovalNeedsConfirmation = { pendingTrackingRemoval = it },
            )

            TrackingMembershipRemovalConfirmationHost(
                pending = pendingTrackingRemoval,
                onPendingChange = { pendingTrackingRemoval = it },
            )

            NuvioStatusModal(
                title = stringResource(Res.string.app_exit_title),
                message = stringResource(Res.string.app_exit_message),
                isVisible = showExitConfirmation,
                confirmText = stringResource(Res.string.action_yes),
                dismissText = stringResource(Res.string.action_no),
                onConfirm = {
                    showExitConfirmation = false
                    platformExitApp()
                },
                onDismiss = {
                    showExitConfirmation = false
                },
            )

            androidx.compose.animation.AnimatedVisibility(
                visibleState = launchOverlayState,
                enter = fadeIn(),
                exit = fadeOut(androidx.compose.animation.core.tween(400)),
            ) {
                AppLaunchOverlay(
                    profile = launchOverlayProfile,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (profileSwitchLoading) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1200)
                    profileSwitchLoading = false
                }
            }

            }
        }
}
