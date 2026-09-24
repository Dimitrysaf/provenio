package io.github.dimitrysaf.provenio.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.core.addons.AddAddonResult
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.auth.AuthState
import io.github.dimitrysaf.provenio.core.auth.DeviceSessionRegistration
import io.github.dimitrysaf.provenio.core.collection.CollectionSyncService
import io.github.dimitrysaf.provenio.core.deeplink.AppDeepLink
import io.github.dimitrysaf.provenio.core.deeplink.AppDeepLinkRepository
import io.github.dimitrysaf.provenio.core.membership.MemberAccessRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsRepository
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.sync.AppForegroundMonitor
import io.github.dimitrysaf.provenio.core.sync.AppVisibility
import io.github.dimitrysaf.provenio.core.sync.ProfileSettingsSync
import io.github.dimitrysaf.provenio.core.sync.SyncManager
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressSourceCoordinator
import io.github.dimitrysaf.provenio.shell.components.NativeTabBridge
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.nav.*
import provenio.composeapp.generated.resources.*
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// Starts the services the app runtime observes for its whole life.
@Composable
internal fun AppRuntimeServices(enabled: Boolean) {
    if (enabled) {
        remember {
            EpisodeReleaseNotificationsRepository.ensureLoaded()
        }
        remember {
            CollectionSyncService.startObserving()
        }
        remember {
            ProfileSettingsSync.startObserving()
        }
    }
}

// Keeps the poster open motion in step with the current route.
@Composable
internal fun PosterNavigationEffects(
    posterNavigation: PosterNavigationState,
    currentRoute: AppRoute?,
    posterNavigationEnabled: Boolean,
) {
    LaunchedEffect(currentRoute, posterNavigationEnabled) {
        val request = posterNavigation.active
        if (!posterNavigationEnabled || (request != null && currentRoute != request.to)) posterNavigation.clear()
    }
    LaunchedEffect(posterNavigation.active?.to) {
        posterNavigation.active?.to?.let { route ->
            MetaDetailsRepository.load(route.type, route.id)
        }
    }
    DisposableEffect(posterNavigation) {
        onDispose { posterNavigation.clear() }
    }
}

// Publishes the tab titles to the native tab bar and to the host.
@Composable
internal fun NativeTabTitlesEffect(
    onTabTitles: (
        (
            home: String,
            search: String,
            library: String,
            profile: String,
            switchProfile: String,
            addProfile: String,
        ) -> Unit
    )?,
) {
    val nativeTabHomeTitle = stringResource(Res.string.compose_nav_home)
    val nativeTabSearchTitle = stringResource(Res.string.compose_nav_search)
    val nativeTabLibraryTitle = stringResource(Res.string.compose_nav_library)
    val nativeTabProfileTitle = stringResource(Res.string.compose_nav_profile)
    val nativeSwitchProfileTitle = stringResource(Res.string.compose_settings_root_switch_profile_title)
    val nativeAddProfileTitle = stringResource(Res.string.compose_profile_add_profile)
    LaunchedEffect(
        nativeTabHomeTitle,
        nativeTabSearchTitle,
        nativeTabLibraryTitle,
        nativeTabProfileTitle,
        nativeSwitchProfileTitle,
        nativeAddProfileTitle,
        onTabTitles,
    ) {
        NativeTabBridge.publishTabTitles(
            home = nativeTabHomeTitle,
            search = nativeTabSearchTitle,
            library = nativeTabLibraryTitle,
            profile = nativeTabProfileTitle,
        )
        onTabTitles?.invoke(
            nativeTabHomeTitle,
            nativeTabSearchTitle,
            nativeTabLibraryTitle,
            nativeTabProfileTitle,
            nativeSwitchProfileTitle,
            nativeAddProfileTitle,
        )
    }
}

// Toasts when the network drops or comes back.
@Composable
internal fun NetworkToastEffect(enabled: Boolean, condition: NetworkCondition) {
    var networkToastBaselineReady by rememberSaveable { mutableStateOf(false) }
    var lastNetworkToastCondition by rememberSaveable { mutableStateOf(NetworkCondition.Unknown.name) }
    LaunchedEffect(condition) {
        if (!enabled) return@LaunchedEffect
        if (!networkToastBaselineReady) {
            networkToastBaselineReady = true
            lastNetworkToastCondition = condition.name
            return@LaunchedEffect
        }

        val previousConditionName = lastNetworkToastCondition
        if (previousConditionName == condition.name) return@LaunchedEffect

        when (condition) {
            NetworkCondition.NoInternet -> {
                ToastController.show(getString(Res.string.network_no_internet_connection))
            }

            NetworkCondition.ServersUnreachable -> {
                ToastController.show(getString(Res.string.network_cannot_reach_servers))
            }

            NetworkCondition.Online -> {
                if (
                    previousConditionName == NetworkCondition.NoInternet.name ||
                    previousConditionName == NetworkCondition.ServersUnreachable.name
                ) {
                    MemberAccessRepository.refresh()
                    ToastController.show(getString(Res.string.network_back_online))
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }

        lastNetworkToastCondition = condition.name
    }
}

// Refreshes the watch progress source once the network is back after an outage.
@Composable
internal fun WatchSourceReconnectEffect(
    enabled: Boolean,
    condition: NetworkCondition,
    authState: AuthState,
    activeProfileIndex: Int?,
) {
    var watchSourceReconnectPending by remember { mutableStateOf(false) }
    LaunchedEffect(
        condition,
        (authState as? AuthState.Authenticated)?.userId,
        activeProfileIndex,
    ) {
        if (!enabled) return@LaunchedEffect
        when (condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> watchSourceReconnectPending = true

            NetworkCondition.Online -> {
                if (!watchSourceReconnectPending) return@LaunchedEffect

                val profileId = activeProfileIndex
                    ?: ProfileRepository.activeProfileId
                val authenticatedState = authState as? AuthState.Authenticated
                if (authenticatedState != null && !authenticatedState.isAnonymous) {
                    SyncManager.requestForegroundPull(profileId = profileId)
                    watchSourceReconnectPending = false
                } else {
                    val result = WatchProgressSourceCoordinator.refreshActiveSource(
                        profileId = profileId,
                        force = true,
                    )
                    if (result.succeeded) {
                        watchSourceReconnectPending = false
                    }
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }
}

// Pulls sync data while the app is in the foreground.
@Composable
internal fun ForegroundSyncEffect(
    enabled: Boolean,
    authState: AuthState,
    activeProfileIndex: Int?,
) {
    LaunchedEffect(authState, activeProfileIndex) {
        if (!enabled) return@LaunchedEffect
        val authenticatedState = authState as? AuthState.Authenticated
        val activeProfileId = activeProfileIndex
        val syncProfileId = activeProfileId?.takeIf {
            authenticatedState != null && !authenticatedState.isAnonymous
        }
        syncProfileId?.let(SyncManager::pullAllForProfile)
        try {
            AppForegroundMonitor.events().collect { visibility ->
                when (visibility) {
                    AppVisibility.Foreground -> {
                        NetworkStatusRepository.requestForegroundRefresh()
                        DeviceSessionRegistration.registerIfAuthenticated()
                        MemberAccessRepository.refreshIfStale()
                        if (syncProfileId != null) {
                            SyncManager.startPeriodicAccountSyncPull(syncProfileId)
                            SyncManager.requestForegroundPull(syncProfileId)
                        } else {
                            SyncManager.stopPeriodicAccountSyncPull()
                        }
                    }
                    AppVisibility.Background -> SyncManager.stopPeriodicAccountSyncPull()
                }
            }
        } finally {
            SyncManager.stopPeriodicAccountSyncPull()
        }
    }
}

// Opens pending deep links: titles, addon installs and downloads.
@Composable
internal fun AppDeepLinkEffect(
    navController: Navigator,
    enabled: Boolean,
    onActivateTab: (AppScreenTab) -> Unit,
) {
    val currentOnActivateTab by rememberUpdatedState(onActivateTab)
    val detailsFallbackTitle = stringResource(Res.string.meta_section_details_title)
    val addonsSettingsTitle = stringResource(Res.string.compose_settings_page_addons)
    val downloadsSettingsTitle = stringResource(Res.string.compose_settings_root_downloads_title)
    LaunchedEffect(navController) {
        if (!enabled) return@LaunchedEffect
        AppDeepLinkRepository.pendingDeepLink.collectLatest { deepLink ->
            when (deepLink) {
                is AppDeepLink.Meta -> {
                    currentOnActivateTab(AppScreenTab.Home)
                    val routeTitle = runCatching {
                        MetaDetailsRepository.fetch(deepLink.type, deepLink.id)?.name
                    }.getOrNull().orEmpty().ifBlank { detailsFallbackTitle }
                    navController.navigate(
                        DetailRoute(
                            type = deepLink.type,
                            id = deepLink.id,
                            title = routeTitle,
                        )
                    ) {
                        launchSingleTop = true
                    }
                    AppDeepLinkRepository.markConsumed(deepLink)
                }

                is AppDeepLink.AddonInstall -> {
                    currentOnActivateTab(AppScreenTab.Settings)
                    navController.navigate(AddonsSettingsRoute(addonsSettingsTitle)) {
                        launchSingleTop = true
                    }
                    ToastController.show(getString(Res.string.addons_modal_checking_title))
                    AddonRepository.initialize()
                    when (val result = AddonRepository.addAddon(deepLink.manifestUrl)) {
                        is AddAddonResult.Success -> {
                            ToastController.show(
                                getString(Res.string.addons_modal_success_message, result.manifest.name),
                            )
                        }

                        is AddAddonResult.Error -> {
                            ToastController.show(result.message)
                        }
                    }
                    AppDeepLinkRepository.markConsumed(deepLink)
                }

                AppDeepLink.Downloads -> {
                    currentOnActivateTab(AppScreenTab.Settings)
                    navController.navigate(DownloadsSettingsRoute(downloadsSettingsTitle)) {
                        launchSingleTop = true
                    }
                    AppDeepLinkRepository.markConsumed(deepLink)
                }

                null -> Unit
            }
        }
    }
}
