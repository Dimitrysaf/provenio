package io.github.dimitrysaf.provenio.shell

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.navigation3.ui.NavDisplay
import io.github.dimitrysaf.provenio.shell.components.LocalPosterClickAnchor
import io.github.dimitrysaf.provenio.shell.nav.PosterNavigationState
import io.github.dimitrysaf.provenio.shell.nav.posterNavigationEntry
import io.github.dimitrysaf.provenio.core.auth.AuthRepository
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.core.sync.SyncManager
import io.github.dimitrysaf.provenio.shell.components.DisintegrationRequestController
import io.github.dimitrysaf.provenio.shell.components.NativeTabBridge
import io.github.dimitrysaf.provenio.shell.components.ContinueWatchingActionSheet
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.platformExitApp
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.addons.isWaitingForFirstEnabledManifest
import io.github.dimitrysaf.provenio.core.catalog.CatalogTarget
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.downloads.DownloadsRepository
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeRepository
import io.github.dimitrysaf.provenio.core.home.buildAddonCatalogRefreshSignature
import io.github.dimitrysaf.provenio.shell.screens.home.components.shouldBlurContinueWatchingArtwork
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.core.library.LibrarySection
import io.github.dimitrysaf.provenio.core.library.LibrarySortOption
import io.github.dimitrysaf.provenio.core.library.LibrarySourceMode
import io.github.dimitrysaf.provenio.shell.screens.library.LibraryListPickerHost
import io.github.dimitrysaf.provenio.shell.screens.library.rememberLibraryListPickerState
import io.github.dimitrysaf.provenio.shell.screens.library.PendingTrackingMembershipRemoval
import io.github.dimitrysaf.provenio.shell.screens.library.TrackingMembershipRemovalConfirmationHost
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.shell.screens.player.LockPlayerToLandscape
import io.github.dimitrysaf.provenio.shell.screens.player.HidePlayerSystemBars
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.streams.StreamAutoPlayPolicy
import io.github.dimitrysaf.provenio.shell.screens.updater.AppUpdaterHost
import io.github.dimitrysaf.provenio.shell.screens.updater.rememberAppUpdaterController
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingItem
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.progress.continueWatchingItemKey
import io.github.dimitrysaf.provenio.core.watch.progress.nextUpDismissKey
import io.github.dimitrysaf.provenio.shell.nav.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.build.supportsPosterNavigationMotion

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
            Navigator(
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
    val visiblePlayerEntries = remember { mutableIntStateOf(0) }
    var streamLandscapeLoadingVisible by remember(currentRoute) { mutableStateOf(false) }
    if (currentRoute is PlayerRoute || visiblePlayerEntries.intValue > 0 || streamLandscapeLoadingVisible) {
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
    val titles = appPageTitles()
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
                    navController.navigate(DownloadsSettingsRoute(titles.downloads)) {
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
                    entryProvider = appEntryProvider(
                        navController = navController,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        titles = titles,
                        playback = playback,
                        useNativeNavigation = useNativeNavigation,
                        p2pEnabled = p2pSettingsUiState.p2pEnabled,
                        externalPlayerId = playerSettingsUiState.externalPlayerId,
                        appUpdaterController = appUpdaterController,
                        visiblePlayerEntries = visiblePlayerEntries,
                        onStreamLandscapeLoadingChanged = { route, visible ->
                            if (currentRoute == route) streamLandscapeLoadingVisible = visible
                        },
                        openPosterActions = openPosterActions,
                        onCatalogClick = onCatalogClick,
                    ) {
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
                            buildAppTabActions(
                                isTabletLayout = isTabletLayout,
                                navController = navController,
                                titles = titles,
                                playback = playback,
                                scope = coroutineScope,
                                useNativeNavigation = useNativeNavigation,
                                appUpdaterController = appUpdaterController,
                                onCatalogClick = onCatalogClick,
                                onLibrarySectionViewAllClick = onLibrarySectionViewAllClick,
                                openPosterActions = openPosterActions,
                                onContinueWatchingLongPress = onContinueWatchingLongPress,
                                onSwitchProfile = onSwitchProfile,
                                activateTab = ::activateTab,
                                onRequestSettingsPage = { pageName -> requestedSettingsPageName = pageName },
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
                    PosterActionsSheet(
                        target = posterActionTarget,
                        watchedKeys = watchedUiState.watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        isRemoteLibrarySource = isRemoteLibrarySource,
                        scope = coroutineScope,
                        libraryDisintegrationRequests = libraryDisintegrationRequests,
                        libraryListPicker = libraryListPicker,
                        onRemovalNeedsConfirmation = { pendingTrackingRemoval = it },
                        onDismiss = { selectedPosterActionTarget = null },
                    )
                }
            }

            // The item is taken from the state once, here: the sheet clears that state as it
            // closes, so a callback that read it again would find nothing to act on.
            selectedContinueWatchingForActions?.let { actionsItem ->
                ContinueWatchingActionSheet(
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

            StatusModal(
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
