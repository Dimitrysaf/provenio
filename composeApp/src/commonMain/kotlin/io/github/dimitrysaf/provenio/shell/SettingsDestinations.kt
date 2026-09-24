package io.github.dimitrysaf.provenio.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.shell.screens.collection.CollectionEditorPage
import io.github.dimitrysaf.provenio.shell.screens.collection.CollectionEditorScreen
import io.github.dimitrysaf.provenio.shell.screens.collection.CollectionManagementScreen
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.collection.FolderDetailRepository
import io.github.dimitrysaf.provenio.shell.screens.collection.FolderDetailScreen
import io.github.dimitrysaf.provenio.core.downloads.DownloadItem
import io.github.dimitrysaf.provenio.shell.screens.downloads.DownloadsScreen
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsScreen
import io.github.dimitrysaf.provenio.shell.nav.AppRoute
import io.github.dimitrysaf.provenio.shell.nav.CollectionEditorPageRoute
import io.github.dimitrysaf.provenio.shell.nav.CollectionEditorRoute
import io.github.dimitrysaf.provenio.shell.nav.CollectionsRoute
import io.github.dimitrysaf.provenio.shell.nav.DetailRoute
import io.github.dimitrysaf.provenio.shell.nav.DownloadShowRoute
import io.github.dimitrysaf.provenio.shell.nav.DownloadsSettingsRoute
import io.github.dimitrysaf.provenio.shell.nav.FolderDetailRoute
import io.github.dimitrysaf.provenio.shell.nav.Navigator
import io.github.dimitrysaf.provenio.shell.nav.SettingsPageRoute

@Composable
internal fun SettingsDestination(
    route: AppRoute,
    navController: Navigator,
    content: @Composable (onBack: () -> Unit) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    content(onBack)
}

@Composable
internal fun SettingsRootDestination(
    route: SettingsPageRoute,
    navController: Navigator,
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
    navController: Navigator,
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
    navController: Navigator,
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
    navController: Navigator,
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
    navController: Navigator,
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
    navController: Navigator,
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
    navController: Navigator,
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
