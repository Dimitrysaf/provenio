package com.nuvio.app.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nuvio.app.shell.components.DisintegrationRequest
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.theme.NuvioTokens
import com.nuvio.app.core.cloud.CloudLibraryContentType
import com.nuvio.app.core.cloud.CloudLibraryFile
import com.nuvio.app.core.cloud.CloudLibraryItem
import com.nuvio.app.core.home.HomeCatalogSection
import com.nuvio.app.shell.screens.home.HomeScreen
import com.nuvio.app.core.home.MetaPreview
import com.nuvio.app.core.library.LibraryItem
import com.nuvio.app.shell.screens.library.LibraryScreen
import com.nuvio.app.core.library.LibrarySection
import com.nuvio.app.core.library.LibrarySortOption
import com.nuvio.app.core.profiles.NuvioProfile
import com.nuvio.app.shell.screens.profiles.ProfileBackgroundBackdrop
import com.nuvio.app.shell.screens.search.SearchScreen
import com.nuvio.app.shell.screens.settings.AppBrandWordmark
import com.nuvio.app.shell.screens.settings.SettingsScreen
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.shell.nav.AppRoute
import com.nuvio.app.shell.nav.NuvioNavigator
import kotlinx.coroutines.flow.Flow
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_brand_name
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun rememberGuardedPopBackStack(
    navController: NuvioNavigator,
    route: AppRoute,
    beforePop: () -> Unit = {},
): () -> Unit {
    var popHandled by remember(route) { mutableStateOf(false) }

    return remember(navController, route, popHandled, beforePop) {
        {
            if (!popHandled && navController.currentRoute == route) {
                popHandled = true
                beforePop()
                navController.popBackStack(expectedRoute = route)
            }
        }
    }
}

internal data class AppTabState(
    val searchListState: LazyListState,
    val homeContentGeneration: Int = 0,
    val profileId: Int? = null,
    val searchFocusRequestCount: Int = 0,
    val rootActionsEnabled: Boolean = true,
    val animateHomeCollectionGifs: Boolean = true,
    val libraryDisintegrationRequest: DisintegrationRequest<String>? = null,
    val continueWatchingDisintegrationRequest: DisintegrationRequest<String>? = null,
    val requestedSettingsPageName: String? = null,
)

internal data class AppTabRequests(
    val homeScrollToTopRequests: Flow<Unit>,
    val searchScrollToTopRequests: Flow<Unit>,
    val libraryScrollToTopRequests: Flow<Unit>,
    val settingsRootActionRequests: Flow<Unit>,
)

internal data class AppTabActions(
    val onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    val onPosterClick: ((MetaPreview) -> Unit)? = null,
    val onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    val onLibraryPosterClick: ((LibraryItem) -> Unit)? = null,
    val onLibraryPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)? = null,
    val onLibrarySectionViewAllClick: ((LibrarySection, LibrarySortOption) -> Unit)? = null,
    val onCloudFilePlay: ((CloudLibraryItem, CloudLibraryFile) -> Unit)? = null,
    val onConnectCloudClick: (() -> Unit)? = null,
    val onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
    val onContinueWatchingLongPress: ((ContinueWatchingItem) -> Unit)? = null,
    val onSwitchProfile: (() -> Unit)? = null,
    val onSettingsPageClick: ((pageName: String, title: String) -> Unit)? = null,
    val onHomescreenSettingsClick: () -> Unit = {},
    val onMetaScreenSettingsClick: () -> Unit = {},
    val onContinueWatchingSettingsClick: () -> Unit = {},
    val onDownloadsSettingsClick: () -> Unit = {},
    val onAddonsSettingsClick: () -> Unit = {},
    val onPluginsSettingsClick: () -> Unit = {},
    val onAccountSettingsClick: () -> Unit = {},
    val onSupportersContributorsSettingsClick: () -> Unit = {},
    val onLicensesAttributionsSettingsClick: () -> Unit = {},
    val onCheckForUpdatesClick: (() -> Unit)? = null,
    val onTestUpdateBannerClick: (() -> Unit)? = null,
    val onCollectionsSettingsClick: () -> Unit = {},
    val onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    val onRequestedSettingsPageConsumed: () -> Unit = {},
    val onInitialHomeContentRendered: () -> Unit = {},
)

@Composable
internal fun AppTabHost(
    selectedTab: AppScreenTab,
    requests: AppTabRequests,
    state: AppTabState,
    actions: AppTabActions,
    modifier: Modifier = Modifier,
) {
    RootTabHost(
        selectedTab = selectedTab,
        modifier = modifier,
        active = state.rootActionsEnabled,
        profileId = state.profileId,
    ) { tab ->
        when (tab) {
            AppScreenTab.Home -> {
                key(state.homeContentGeneration) {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        animateCollectionGifs = state.animateHomeCollectionGifs,
                        scrollToTopRequests = requests.homeScrollToTopRequests,
                        onCatalogClick = actions.onCatalogClick,
                        onPosterClick = actions.onPosterClick,
                        onPosterLongClick = actions.onPosterLongClick,
                        onContinueWatchingClick = actions.onContinueWatchingClick,
                        onContinueWatchingLongPress = actions.onContinueWatchingLongPress,
                        continueWatchingDisintegrationRequest = state.continueWatchingDisintegrationRequest,
                        onFolderClick = actions.onFolderClick,
                        onFirstCatalogRendered = actions.onInitialHomeContentRendered,
                    )
                }
            }

            AppScreenTab.Search -> {
                SearchScreen(
                    modifier = Modifier.fillMaxSize(),
                    listState = state.searchListState,
                    onPosterClick = actions.onPosterClick,
                    onPosterLongClick = actions.onPosterLongClick,
                    searchFocusRequestCount = state.searchFocusRequestCount,
                    scrollToTopRequests = requests.searchScrollToTopRequests,
                )
            }

            AppScreenTab.Library -> {
                LibraryScreen(
                    modifier = Modifier.fillMaxSize(),
                    scrollToTopRequests = requests.libraryScrollToTopRequests,
                    onPosterClick = actions.onLibraryPosterClick,
                    onPosterLongClick = actions.onLibraryPosterLongClick,
                    onSectionViewAllClick = actions.onLibrarySectionViewAllClick,
                    onCloudFilePlay = actions.onCloudFilePlay,
                    onConnectCloudClick = actions.onConnectCloudClick,
                    disintegrationRequest = state.libraryDisintegrationRequest,
                )
            }

            AppScreenTab.Settings -> {
                SettingsScreen(
                    modifier = Modifier.fillMaxSize(),
                    rootActionRequests = requests.settingsRootActionRequests,
                    requestedPageName = state.requestedSettingsPageName,
                    onRequestedPageConsumed = actions.onRequestedSettingsPageConsumed,
                    rootActionsEnabled = state.rootActionsEnabled,
                    onNavigatePage = actions.onSettingsPageClick,
                    onSwitchProfile = actions.onSwitchProfile,
                    onHomescreenClick = actions.onHomescreenSettingsClick,
                    onMetaScreenClick = actions.onMetaScreenSettingsClick,
                    onContinueWatchingClick = actions.onContinueWatchingSettingsClick,
                    onDownloadsClick = actions.onDownloadsSettingsClick,
                    onAddonsClick = actions.onAddonsSettingsClick,
                    onPluginsClick = actions.onPluginsSettingsClick,
                    onAccountClick = actions.onAccountSettingsClick,
                    onSupportersContributorsClick = actions.onSupportersContributorsSettingsClick,
                    onLicensesAttributionsClick = actions.onLicensesAttributionsSettingsClick,
                    onCheckForUpdatesClick = actions.onCheckForUpdatesClick,
                    onTestUpdateBannerClick = actions.onTestUpdateBannerClick,
                    onCollectionsClick = actions.onCollectionsSettingsClick,
                )
            }
        }
    }
}

internal fun ContinueWatchingItem.isCloudLibraryContinueWatchingItem(): Boolean =
    parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)

/**
 * The app while it is still coming up: the wordmark, and something moving under it.
 *
 * It paints no background of its own, so it takes whatever it is laid over — the profile's
 * backdrop in [AppLaunchOverlay], the theme's surface in the gate.
 */
@Composable
internal fun AppLoadingContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppBrandWordmark(
                contentDescription = stringResource(Res.string.app_brand_name),
                modifier = Modifier
                    .fillMaxWidth(0.48f)
                    .height(44.dp),
            )
            Spacer(modifier = Modifier.height(LoadingContentGap))
            NuvioLoadingIndicator()
        }
    }
}

/** The gap between the wordmark and the indicator under it. */
private val LoadingContentGap = 24.dp

@Composable
internal fun AppLaunchOverlay(
    profile: NuvioProfile?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.zIndex(NuvioTokens.Z.dialog),
    ) {
        ProfileBackgroundBackdrop(
            profile = profile,
            modifier = Modifier.fillMaxSize(),
        )
        AppLoadingContent(modifier = Modifier.fillMaxSize())
    }
}
