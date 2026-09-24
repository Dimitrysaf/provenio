package io.github.dimitrysaf.provenio.shell

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.build.isIos
import io.github.dimitrysaf.provenio.core.cloud.playbackVideoId
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.library.LibrarySection
import io.github.dimitrysaf.provenio.core.library.LibrarySortOption
import io.github.dimitrysaf.provenio.core.library.toMetaPreview
import io.github.dimitrysaf.provenio.core.updater.AppUpdaterPlatform
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingItem
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.progress.toContinueWatchingItem
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.nav.*
import io.github.dimitrysaf.provenio.shell.screens.settings.AccountSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.settings.AddonsSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.settings.ContinueWatchingSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.settings.HomescreenSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.settings.LicensesAttributionsSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.settings.MetaScreenSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.settings.PluginsSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.settings.SupportersContributorsSettingsScreen
import io.github.dimitrysaf.provenio.shell.screens.updater.AppUpdaterController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// The page titles settings routes carry, resolved once per composition.
internal data class AppPageTitles(
    val homescreen: String,
    val metaScreen: String,
    val continueWatching: String,
    val debrid: String,
    val downloads: String,
    val addons: String,
    val plugins: String,
    val account: String,
    val supporters: String,
    val licenses: String,
    val collections: String,
    val newCollection: String,
)

@Composable
internal fun appPageTitles(): AppPageTitles = AppPageTitles(
    homescreen = stringResource(Res.string.compose_settings_page_homescreen),
    metaScreen = stringResource(Res.string.compose_settings_page_meta_screen),
    continueWatching = stringResource(Res.string.compose_settings_page_continue_watching),
    debrid = stringResource(Res.string.compose_settings_page_debrid),
    downloads = stringResource(Res.string.compose_settings_root_downloads_title),
    addons = stringResource(Res.string.compose_settings_page_addons),
    plugins = stringResource(Res.string.compose_settings_page_plugins),
    account = stringResource(Res.string.compose_settings_page_account),
    supporters = stringResource(Res.string.compose_settings_page_supporters_contributors),
    licenses = stringResource(Res.string.compose_settings_page_licenses_attributions),
    collections = stringResource(Res.string.collections_header),
    newCollection = stringResource(Res.string.collections_new),
)

// What the tab screens do when something in them is tapped.
internal fun buildAppTabActions(
    isTabletLayout: Boolean,
    navController: Navigator,
    titles: AppPageTitles,
    playback: AppPlayback,
    scope: CoroutineScope,
    useNativeNavigation: Boolean,
    appUpdaterController: AppUpdaterController,
    onCatalogClick: (HomeCatalogSection) -> Unit,
    onLibrarySectionViewAllClick: (LibrarySection, LibrarySortOption) -> Unit,
    openPosterActions: (PosterActionTarget) -> Unit,
    onContinueWatchingLongPress: (ContinueWatchingItem) -> Unit,
    onSwitchProfile: () -> Unit,
    activateTab: (AppScreenTab) -> Unit,
    onRequestSettingsPage: (String?) -> Unit,
    onInitialHomeContentRendered: () -> Unit,
): AppTabActions {
    return AppTabActions(
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
            scope.launch {
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
                    ToastController.show(playback.strings.cloudPlayFailed)
                }
            }
        },
        onConnectCloudClick = {
            if (useNativeNavigation && !isTabletLayout) {
                activateTab(AppScreenTab.Settings)
                navController.navigate(
                    SettingsPageRoute(
                        pageName = "Debrid",
                        title = titles.debrid,
                    )
                )
            } else {
                onRequestSettingsPage("Debrid")
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
        onHomescreenSettingsClick = { navController.navigate(HomescreenSettingsRoute(titles.homescreen)) },
        onMetaScreenSettingsClick = { navController.navigate(MetaScreenSettingsRoute(titles.metaScreen)) },
        onContinueWatchingSettingsClick = { navController.navigate(ContinueWatchingSettingsRoute(titles.continueWatching)) },
        onDownloadsSettingsClick = { navController.navigate(DownloadsSettingsRoute(titles.downloads)) },
        onAddonsSettingsClick = { navController.navigate(AddonsSettingsRoute(titles.addons)) },
        onPluginsSettingsClick = {
            if (AppFeaturePolicy.pluginsEnabled) {
                navController.navigate(PluginsSettingsRoute(titles.plugins))
            }
        },
        onAccountSettingsClick = { navController.navigate(AccountSettingsRoute(titles.account)) },
        onSupportersContributorsSettingsClick = {
            if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                navController.navigate(SupportersContributorsSettingsRoute(titles.supporters))
            }
        },
        onLicensesAttributionsSettingsClick = {
            navController.navigate(LicensesAttributionsSettingsRoute(titles.licenses))
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
        onCollectionsSettingsClick = { navController.navigate(CollectionsRoute(titles.collections)) },
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
                    title = folderTitle.ifBlank { titles.collections },
                )
            )
        },
        onRequestedSettingsPageConsumed = {
            onRequestSettingsPage(null)
        },
        onInitialHomeContentRendered = onInitialHomeContentRendered,
    )
}

// Every destination of the app. The tabs are passed in, since they share state with the shell.
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun appEntryProvider(
    navController: Navigator,
    sharedTransitionScope: SharedTransitionScope,
    titles: AppPageTitles,
    playback: AppPlayback,
    useNativeNavigation: Boolean,
    p2pEnabled: Boolean,
    externalPlayerId: String?,
    appUpdaterController: AppUpdaterController,
    visiblePlayerEntries: MutableIntState,
    onStreamLandscapeLoadingChanged: (StreamRoute, Boolean) -> Unit,
    openPosterActions: (PosterActionTarget) -> Unit,
    onCatalogClick: (HomeCatalogSection) -> Unit,
    tabs: @Composable () -> Unit,
): (NavKey) -> NavEntry<NavKey> = entryProvider<NavKey> {
    entry<TabsRoute> { tabs() }
    entry<DetailRoute> { route ->
        DetailsDestination(
            route = route,
            navController = navController,
            onPlay = playback.onPlay,
            onPlayManually = playback.onPlayManually,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
        )
    }
    entry<PersonDetailRoute> { route ->
        PersonDestination(
            route = route,
            navController = navController,
            sharedTransitionScope = sharedTransitionScope,
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
                onStreamLandscapeLoadingChanged(route, visible)
            },
            navController = navController,
            p2pEnabled = p2pEnabled,
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
                visiblePlayerEntries.intValue += 1
                onDispose { visiblePlayerEntries.intValue -= 1 }
            }
        }
        PlayerDestination(
            route = route,
            navController = navController,
            externalPlayerId = externalPlayerId,
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
            downloadsTitle = titles.downloads,
            collectionsTitle = titles.collections,
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
            newCollectionTitle = titles.newCollection,
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
}
