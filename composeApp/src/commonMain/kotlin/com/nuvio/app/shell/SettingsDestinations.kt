package com.nuvio.app.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nuvio.app.shell.screens.collection.CollectionEditorPage
import com.nuvio.app.shell.screens.collection.CollectionEditorScreen
import com.nuvio.app.shell.screens.collection.CollectionManagementScreen
import com.nuvio.app.core.collection.CollectionRepository
import com.nuvio.app.core.collection.FolderDetailRepository
import com.nuvio.app.shell.screens.collection.FolderDetailScreen
import com.nuvio.app.core.downloads.DownloadItem
import com.nuvio.app.shell.screens.downloads.DownloadsScreen
import com.nuvio.app.core.home.HomeCatalogSection
import com.nuvio.app.core.home.MetaPreview
import com.nuvio.app.shell.screens.settings.SettingsScreen
import com.nuvio.app.shell.nav.AppRoute
import com.nuvio.app.shell.nav.CollectionEditorPageRoute
import com.nuvio.app.shell.nav.CollectionEditorRoute
import com.nuvio.app.shell.nav.CollectionsRoute
import com.nuvio.app.shell.nav.DetailRoute
import com.nuvio.app.shell.nav.DownloadShowRoute
import com.nuvio.app.shell.nav.DownloadsSettingsRoute
import com.nuvio.app.shell.nav.FolderDetailRoute
import com.nuvio.app.shell.nav.NuvioNavigator
import com.nuvio.app.shell.nav.SettingsPageRoute

@Composable
internal fun SettingsDestination(
    route: AppRoute,
    navController: NuvioNavigator,
    content: @Composable (onBack: () -> Unit) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    content(onBack)
}

@Composable
internal fun SettingsRootDestination(
    route: SettingsPageRoute,
    navController: NuvioNavigator,
    useNativeNavigation: Boolean,
    downloadsTitle: String,
    collectionsTitle: String,
    onCheckForUpdates: (() -> Unit)?,
    onTestUpdateBanner: (() -> Unit)?,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    SettingsScreen(
        modifier = Modifier.fillMaxSize(),
        initialPageName = route.pageName,
        rootActionsEnabled = false,
        onNavigatePage = { pageName, title ->
            navController.navigate(SettingsPageRoute(pageName, title))
        },
        onExternalBack = onBack,
        showInternalHeader = !useNativeNavigation,
        onDownloadsClick = {
            navController.navigate(DownloadsSettingsRoute(downloadsTitle))
        },
        onCollectionsClick = {
            navController.navigate(CollectionsRoute(collectionsTitle))
        },
        onCheckForUpdatesClick = onCheckForUpdates,
        onTestUpdateBannerClick = onTestUpdateBanner,
    )
}

@Composable
internal fun DownloadsDestination(
    route: DownloadsSettingsRoute,
    navController: NuvioNavigator,
    useNativeNavigation: Boolean,
    onOpenDownload: (DownloadItem) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    DownloadsScreen(
        onBack = onBack,
        onOpenDownload = onOpenDownload,
        onNavigateToShow = if (useNativeNavigation) {
            { showId, title -> navController.navigate(DownloadShowRoute(showId, title)) }
        } else {
            null
        },
    )
}

@Composable
internal fun DownloadShowDestination(
    route: DownloadShowRoute,
    navController: NuvioNavigator,
    onOpenDownload: (DownloadItem) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    DownloadsScreen(
        onBack = onBack,
        onOpenDownload = onOpenDownload,
        initialShowId = route.showId,
        onBackFromShow = onBack,
    )
}

@Composable
internal fun CollectionsDestination(
    route: CollectionsRoute,
    navController: NuvioNavigator,
    newCollectionTitle: String,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    CollectionManagementScreen(
        onBack = onBack,
        onNavigateToEditor = { collectionId ->
            val editorTitle = collectionId
                ?.let { id ->
                    CollectionRepository.collections.value.firstOrNull { it.id == id }?.title
                }
                .orEmpty()
            navController.navigate(
                CollectionEditorRoute(
                    collectionId = collectionId,
                    title = editorTitle.ifBlank { newCollectionTitle },
                ),
            )
        },
    )
}

@Composable
internal fun CollectionEditorDestination(
    route: CollectionEditorRoute,
    navController: NuvioNavigator,
    useNativeNavigation: Boolean,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    CollectionEditorScreen(
        collectionId = route.collectionId,
        onBack = onBack,
        initialPage = if (useNativeNavigation) CollectionEditorPage.Root else null,
        onNavigateToPage = if (useNativeNavigation) {
            { page, title ->
                navController.navigate(
                    CollectionEditorPageRoute(
                        collectionId = route.collectionId,
                        pageName = page.name,
                        title = title,
                    ),
                )
            }
        } else {
            null
        },
    )
}

@Composable
internal fun CollectionEditorPageDestination(
    route: CollectionEditorPageRoute,
    navController: NuvioNavigator,
) {
    val page = remember(route.pageName) {
        runCatching { CollectionEditorPage.valueOf(route.pageName) }.getOrNull()
    }
    val onBack = rememberGuardedPopBackStack(navController, route)
    if (page == null || page == CollectionEditorPage.Root) {
        LaunchedEffect(route) { onBack() }
        return
    }
    CollectionEditorScreen(
        collectionId = route.collectionId,
        initialPage = page,
        initializeRepository = false,
        onBack = onBack,
        onNavigateToPage = { nextPage, title ->
            navController.navigate(
                CollectionEditorPageRoute(
                    collectionId = route.collectionId,
                    pageName = nextPage.name,
                    title = title,
                ),
            )
        },
    )
}

@Composable
internal fun FolderDestination(
    route: FolderDetailRoute,
    navController: NuvioNavigator,
    onCatalogClick: (HomeCatalogSection) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    LaunchedEffect(route.collectionId, route.folderId) {
        FolderDetailRepository.initialize(route.collectionId, route.folderId)
    }
    FolderDetailScreen(
        onBack = onBack,
        onCatalogClick = onCatalogClick,
        onPosterClick = { meta: MetaPreview ->
            navController.navigate(DetailRoute(type = meta.type, id = meta.id, title = meta.name))
        },
    )
}
