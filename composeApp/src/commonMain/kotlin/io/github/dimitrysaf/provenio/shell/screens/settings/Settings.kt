package io.github.dimitrysaf.provenio.shell.screens.settings

import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import provenio.composeapp.generated.resources.action_back
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.components.LocalScreenActive
import io.github.dimitrysaf.provenio.shell.components.ScreenActivityEffect
import io.github.dimitrysaf.provenio.shell.components.LocalBottomNavigationOverlayPadding
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.PredictiveBackPageHost
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsUiState
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleRepository
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleUiState
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.shell.screens.collection.CollectionsInlinePane
import io.github.dimitrysaf.provenio.shell.screens.downloads.DownloadsScreen
import io.github.dimitrysaf.provenio.core.downloads.DownloadItem
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.addons.firstEnabledManifestError
import io.github.dimitrysaf.provenio.core.addons.hasPendingEnabledManifests
import io.github.dimitrysaf.provenio.core.addons.isWaitingForFirstEnabledManifest
import io.github.dimitrysaf.provenio.core.debrid.DebridSettings
import io.github.dimitrysaf.provenio.core.debrid.DebridSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.buildAddonCatalogRefreshSignature
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettingsRepository
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsRepository
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsUiState
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsUiState
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthUiState
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthUiState
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsSettings
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsUiState
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettings
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesUiState
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_settings_page_root
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.AppIconRepository
import io.github.dimitrysaf.provenio.core.settings.AppIconSettingsState
import io.github.dimitrysaf.provenio.core.settings.AppLanguage
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository

// Downloads is not a SettingsPage, so a request for it goes by this name.
internal const val SettingsDownloadsPageName = "Downloads"

private val SettingsSearchRevealThreshold = 28.dp
private const val SettingsSearchRevealAnimationMillis = 240L
private const val SettingsSearchRevealHapticDelayMillis = 90L

private fun SettingsPage.depth(): Int = generateSequence(this) { it.previousPage() }.count()

private fun SettingsPage.isEnabledByPolicy(): Boolean =
    when (this) {
        SettingsPage.SupportersContributors -> AppFeaturePolicy.supportersContributorsPageEnabled
        else -> true
    }

@Composable
private fun settingsPageTitles(): Map<SettingsPage, String> {
    val titles = mutableMapOf<SettingsPage, String>()
    for (page in SettingsPage.entries) {
        titles[page] = stringResource(page.titleRes)
    }
    return titles
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    rootActionRequests: Flow<Unit> = emptyFlow(),
    initialPageName: String = SettingsPage.Root.name,
    requestedPageName: String? = null,
    onRequestedPageConsumed: () -> Unit = {},
    rootActionsEnabled: Boolean = true,
    onNavigatePage: ((pageName: String, title: String) -> Unit)? = null,
    onExternalBack: (() -> Unit)? = null,
    showInternalHeader: Boolean = true,
    onSwitchProfile: (() -> Unit)? = null,
    onHomescreenClick: () -> Unit = {},
    onMetaScreenClick: () -> Unit = {},
    onContinueWatchingClick: () -> Unit = {},
    onAddonsClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onOpenDownload: (DownloadItem) -> Unit = {},
    onSupportersContributorsClick: () -> Unit = {},
    onLicensesAttributionsClick: () -> Unit = {},
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onCollectionsClick: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val screenActive = LocalScreenActive.current
        val pageStateHolder = rememberSaveableStateHolder()
        val data = rememberSettingsData()

        val initialPage = remember(initialPageName) {
            runCatching { SettingsPage.valueOf(initialPageName) }
                .getOrDefault(SettingsPage.Root)
                .takeIf { it.isEnabledByPolicy() }
                ?: SettingsPage.Root
        }
        var currentPage by rememberSaveable(initialPageName) { mutableStateOf(initialPage.name) }
        var downloadsOpen by rememberSaveable { mutableStateOf(false) }
        val twoPane = maxWidth >= 768.dp
        val scrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val pageTitles = if (onNavigatePage != null) settingsPageTitles() else emptyMap()
        val page = remember(currentPage) {
            runCatching { SettingsPage.valueOf(currentPage) }
                .getOrDefault(SettingsPage.Root)
                .takeIf { it.isEnabledByPolicy() }
                ?: SettingsPage.Root
        }
        val previousPage = page.previousPage()

        fun openPage(targetPage: SettingsPage) {
            if (!targetPage.isEnabledByPolicy()) return
            val externalNavigator = onNavigatePage
            if (externalNavigator == null) {
                currentPage = targetPage.name
                return
            }
            if (targetPage == SettingsPage.Root && onExternalBack != null) {
                onExternalBack()
                return
            }
            externalNavigator(
                targetPage.name,
                pageTitles.getValue(targetPage),
            )
        }

        fun navigateBack() {
            val parentPage = previousPage ?: return
            if (onNavigatePage != null && onExternalBack != null) {
                onExternalBack()
            } else {
                currentPage = parentPage.name
            }
        }

        val openHomescreen = if (onNavigatePage != null) {
            { openPage(SettingsPage.Homescreen) }
        } else {
            onHomescreenClick
        }
        val openMetaScreen = if (onNavigatePage != null) {
            { openPage(SettingsPage.MetaScreen) }
        } else {
            onMetaScreenClick
        }
        val openContinueWatching = if (onNavigatePage != null) {
            { openPage(SettingsPage.ContinueWatching) }
        } else {
            onContinueWatchingClick
        }
        val openAddons = if (onNavigatePage != null) {
            { openPage(SettingsPage.Addons) }
        } else {
            onAddonsClick
        }
        val openPlugins = if (onNavigatePage != null) {
            { openPage(SettingsPage.Plugins) }
        } else {
            onPluginsClick
        }
        val openSupportersContributors = if (onNavigatePage != null) {
            { openPage(SettingsPage.SupportersContributors) }
        } else {
            onSupportersContributorsClick
        }
        val openLicensesAttributions = if (onNavigatePage != null) {
            { openPage(SettingsPage.LicensesAttributions) }
        } else {
            onLicensesAttributionsClick
        }

        LaunchedEffect(page, currentPage) {
            if (page.name != currentPage) {
                currentPage = page.name
            }
        }

        ScreenActivityEffect(rootActionRequests, rootActionsEnabled, page) { active ->
            if (!active || !rootActionsEnabled) return@ScreenActivityEffect
            rootActionRequests.collect {
                val pageToOpen = page.previousPage()
                if (pageToOpen != null) {
                    navigateBack()
                } else {
                    scrollToTopRequests.tryEmit(Unit)
                }
            }
        }

        ScreenActivityEffect(requestedPageName, rootActionsEnabled) { active ->
            if (!active || !rootActionsEnabled) return@ScreenActivityEffect
            val requestedPage = requestedPageName ?: return@ScreenActivityEffect
            if (requestedPage == SettingsDownloadsPageName) {
                if (twoPane) downloadsOpen = true else onDownloadsClick()
                onRequestedPageConsumed()
                return@ScreenActivityEffect
            }
            val targetPage = runCatching { SettingsPage.valueOf(requestedPage) }.getOrNull()
            if (targetPage == null || !targetPage.isEnabledByPolicy()) {
                onRequestedPageConsumed()
                return@ScreenActivityEffect
            }
            openPage(targetPage)
            onRequestedPageConsumed()
        }

        val backEnabled = screenActive && previousPage != null && (rootActionsEnabled || onExternalBack != null)

        if (screenActive || page == SettingsPage.Root) {
            pageStateHolder.SaveableStateProvider("content") {
                if (twoPane) {
                    TabletSettingsScreen(
                        page = page,
                        backEnabled = backEnabled,
                        scrollToTopRequests = scrollToTopRequests,
                        onPageChange = ::openPage,
                        onNavigateBack = ::navigateBack,
                        showInternalHeader = showInternalHeader,
                        data = data,
                        onSwitchProfile = onSwitchProfile,
                        downloadsOpen = downloadsOpen,
                        onDownloadsOpenChange = { downloadsOpen = it },
                        onOpenDownload = onOpenDownload,
                        onSupportersContributorsClick = openSupportersContributors,
                        onLicensesAttributionsClick = openLicensesAttributions,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onTestUpdateBannerClick = onTestUpdateBannerClick,
                    )
                } else {
                    MobileSettingsScreen(
                        page = page,
                        backEnabled = backEnabled,
                        scrollToTopRequests = scrollToTopRequests,
                        onPageChange = ::openPage,
                        onNavigateBack = ::navigateBack,
                        showInternalHeader = showInternalHeader,
                        data = data,
                        onSwitchProfile = onSwitchProfile,
                        onHomescreenClick = openHomescreen,
                        onMetaScreenClick = openMetaScreen,
                        onContinueWatchingClick = openContinueWatching,
                        onAddonsClick = openAddons,
                        onPluginsClick = openPlugins,
                        onDownloadsClick = onDownloadsClick,
                        onSupportersContributorsClick = openSupportersContributors,
                        onLicensesAttributionsClick = openLicensesAttributions,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onTestUpdateBannerClick = onTestUpdateBannerClick,
                        onCollectionsClick = onCollectionsClick,
                    )
                }
            }
        }
    }
}

/**
 * Wide enough to hold the search bar, whose spec minimum container width is 360dp. This also
 * matches the layout spec's default fixed-pane width at the expanded breakpoint.
 */
private val SettingsSidebarWidth = 384.dp

@Composable
private fun MobileSettingsScreen(
    page: SettingsPage,
    backEnabled: Boolean,
    scrollToTopRequests: Flow<Unit>,
    onPageChange: (SettingsPage) -> Unit,
    onNavigateBack: () -> Unit,
    showInternalHeader: Boolean,
    data: SettingsData,
    onSwitchProfile: (() -> Unit)? = null,
    onHomescreenClick: () -> Unit = {},
    onMetaScreenClick: () -> Unit = {},
    onContinueWatchingClick: () -> Unit = {},
    onAddonsClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onSupportersContributorsClick: () -> Unit = {},
    onLicensesAttributionsClick: () -> Unit = {},
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onCollectionsClick: () -> Unit = {},

) {
    val saveableStateHolder = rememberSaveableStateHolder()
    PredictiveBackPageHost(
        page = page,
        backPage = page.previousPage(),
        backEnabled = backEnabled,
        isForward = { from, to -> to.depth() > from.depth() },
        onBack = onNavigateBack,
    ) { page ->
    saveableStateHolder.SaveableStateProvider(page.name) {
        var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
        var rootSearchVisible by rememberSaveable { mutableStateOf(false) }
        var rootSearchRevealAnimating by rememberSaveable { mutableStateOf(false) }
        val listState = rememberLazyListState()
        ScreenActivityEffect(listState) { screenActive ->
            if (!screenActive) listState.stopScroll()
        }
        val hapticFeedback = LocalHapticFeedback.current
        val hapticScope = rememberCoroutineScope()
        val rootSearchRevealConnection = rememberSettingsRootSearchRevealConnection(
            page = page,
            listState = listState,
            query = settingsSearchQuery,
            searchVisible = rootSearchVisible,
        ) {
            rootSearchVisible = true
            rootSearchRevealAnimating = true
            hapticScope.launch {
                delay(SettingsSearchRevealHapticDelayMillis)
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        fun openSearchTarget(target: SettingsSearchTarget) {
            when (target) {
                is SettingsSearchTarget.Page -> when (target.page) {
                    SettingsPage.SupportersContributors -> {
                        if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                            onSupportersContributorsClick()
                        }
                    }
                    SettingsPage.LicensesAttributions -> onLicensesAttributionsClick()
                    SettingsPage.ContinueWatching -> onContinueWatchingClick()
                    SettingsPage.Addons -> onAddonsClick()
                    SettingsPage.Plugins -> {
                        if (AppFeaturePolicy.pluginsEnabled) {
                            onPluginsClick()
                        }
                    }
                    SettingsPage.Homescreen -> onHomescreenClick()
                    SettingsPage.MetaScreen -> onMetaScreenClick()
                    else -> onPageChange(target.page)
                }
                SettingsSearchTarget.Downloads -> onDownloadsClick()
                SettingsSearchTarget.Collections -> onCollectionsClick()
                SettingsSearchTarget.SwitchProfile -> onSwitchProfile?.invoke()
                SettingsSearchTarget.CheckForUpdates -> onCheckForUpdatesClick?.invoke()
            }
        }

        LaunchedEffect(rootSearchRevealAnimating) {
            if (rootSearchRevealAnimating) {
                delay(SettingsSearchRevealAnimationMillis)
                rootSearchRevealAnimating = false
            }
        }

        LaunchedEffect(scrollToTopRequests) {
            scrollToTopRequests.collect {
                listState.animateScrollToItem(0)
            }
        }

        val links = SettingsPageLinks(
            openPage = onPageChange,
            openSubPage = onPageChange,
            onHomescreenClick = onHomescreenClick,
            onMetaScreenClick = onMetaScreenClick,
            onContinueWatchingClick = onContinueWatchingClick,
            onAddonsClick = onAddonsClick,
            onPluginsClick = onPluginsClick,
            onCollectionsClick = onCollectionsClick,
        )
        val previousPage = page.previousPage()

        ScreenScaffold(
            modifier = Modifier.nestedScroll(rootSearchRevealConnection),
            // Native navigation supplies its own header, so the app bar only appears when this
            // screen is responsible for one.
            title = if (showInternalHeader) stringResource(page.titleRes) else null,
            listState = listState,
            onBack = previousPage?.let { { onNavigateBack() } },
        ) {
            if (!showInternalHeader) {
                item { Spacer(modifier = Modifier.height(44.dp)) }
            }

            when (page) {
                SettingsPage.Root -> {
                    settingsSearchRootContent(
                        query = settingsSearchQuery,
                        entries = {
                            settingsSearchEntries(
                                isTablet = false,
                                pluginsEnabled = AppFeaturePolicy.pluginsEnabled,
                                supportersContributorsPageEnabled = AppFeaturePolicy.supportersContributorsPageEnabled,
                                personalMediaAddonCopyEnabled = AppFeaturePolicy.personalMediaAddonCopyEnabled,
                                switchProfileAvailable = onSwitchProfile != null,
                                checkForUpdatesAvailable = onCheckForUpdatesClick != null,
                            )
                        },
                        isTablet = false,
                        showSearchField = rootSearchVisible,
                        animateSearchField = rootSearchRevealAnimating,
                        onQueryChange = { settingsSearchQuery = it },
                        onTargetClick = { openSearchTarget(it) },
                    )
                    if (settingsSearchQuery.isBlank()) {
                        settingsRootContent(
                            isTablet = false,
                            onPlaybackClick = { onPageChange(SettingsPage.Playback) },
                            onAppearanceClick = { onPageChange(SettingsPage.Appearance) },
                            onAdvancedClick = { onPageChange(SettingsPage.Advanced) },
                            onNotificationsClick = { onPageChange(SettingsPage.Notifications) },
                            onContentDiscoveryClick = { onPageChange(SettingsPage.ContentDiscovery) },
                            onIntegrationsClick = { onPageChange(SettingsPage.Integrations) },
                            onTrackingClick = { onPageChange(SettingsPage.TraktAuthentication) },
                            onSupportersContributorsClick = onSupportersContributorsClick,
                            onLicensesAttributionsClick = onLicensesAttributionsClick,
                            onCheckForUpdatesClick = onCheckForUpdatesClick,
                            onTestUpdateBannerClick = onTestUpdateBannerClick,
                            onDownloadsClick = onDownloadsClick,
                            onSwitchProfileClick = onSwitchProfile,
                            showSupportersContributorsPage = AppFeaturePolicy.supportersContributorsPageEnabled,
                        )
                    }
                }
                else -> settingsPageContent(page, isTablet = false, data = data, links = links)
            }
        }
    }
    }
}

@Composable
private fun rememberSettingsRootSearchRevealConnection(
    page: SettingsPage,
    listState: LazyListState,
    query: String,
    searchVisible: Boolean,
    onReveal: () -> Unit,
): NestedScrollConnection {
    val revealThresholdPx = with(LocalDensity.current) { SettingsSearchRevealThreshold.toPx() }
    val currentOnReveal by rememberUpdatedState(onReveal)
    var pullDistancePx by remember(page) { mutableStateOf(0f) }
    var revealTriggered by remember(page) { mutableStateOf(false) }

    return remember(page, listState, query, searchVisible, revealThresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val isRootAtTop = page == SettingsPage.Root &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                val canRevealSearch = isRootAtTop && !searchVisible && !revealTriggered && query.isBlank()

                if (canRevealSearch && available.y > 0f) {
                    pullDistancePx += available.y
                    if (pullDistancePx >= revealThresholdPx) {
                        pullDistancePx = 0f
                        revealTriggered = true
                        currentOnReveal()
                    }
                } else if (!isRootAtTop || available.y < 0f) {
                    pullDistancePx = 0f
                }

                return Offset.Zero
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabletSettingsScreen(
    page: SettingsPage,
    backEnabled: Boolean,
    scrollToTopRequests: Flow<Unit>,
    onPageChange: (SettingsPage) -> Unit,
    onNavigateBack: () -> Unit,
    showInternalHeader: Boolean,
    data: SettingsData,
    onSwitchProfile: (() -> Unit)? = null,
    downloadsOpen: Boolean,
    onDownloadsOpenChange: (Boolean) -> Unit,
    onOpenDownload: (DownloadItem) -> Unit,
    onSupportersContributorsClick: () -> Unit = {},
    onLicensesAttributionsClick: () -> Unit = {},
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,

) {
    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.General.name) }
    val activeCategory = SettingsCategory.valueOf(selectedCategory)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topOffset = max(statusBarPadding + 24.dp, 48.dp) + 64.dp
    // Collections open in the content pane here rather than as a destination of their own, which
    // is what used to cover the sidebar. Only this layout needs the flag: the narrow one has no
    // pane to fill and still navigates.
    var collectionsOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(page) {
        if (page.opensInlineOnTablet) {
            selectedCategory = page.category.name
        }
    }

    fun openInlinePage(page: SettingsPage) {
        selectedCategory = page.category.name
        onPageChange(page)
    }

    // Downloads opens in the content pane too, so the sidebar stays on screen beside it.
    fun openDownloads() {
        collectionsOpen = false
        selectedCategory = SettingsCategory.General.name
        if (page != SettingsPage.Root) onPageChange(SettingsPage.Root)
        onDownloadsOpenChange(true)
    }

    val saveableStateHolder = rememberSaveableStateHolder()
    // Hoisted above the Row: the search bar lives in the sidebar while its results render in the
    // content pane, so both sides read the same query.
    var settingsSearchQuery by rememberSaveable { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .width(SettingsSidebarWidth)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topOffset),
            ) {
                Text(
                    text = stringResource(Res.string.compose_settings_page_root),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                SettingsSearchField(
                    query = settingsSearchQuery,
                    onQueryChange = { settingsSearchQuery = it },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
                ) {
                    SettingsCategory.entries.forEachIndexed { index, category ->
                        SettingsSidebarItem(
                            label = stringResource(category.labelRes),
                            icon = category.icon,
                            selected = category == activeCategory,
                            index = index,
                            count = SettingsCategory.entries.size,
                            onClick = {
                                selectedCategory = category.name
                                collectionsOpen = false
                                onDownloadsOpenChange(false)
                                if (page != SettingsPage.Root) {
                                    onPageChange(SettingsPage.Root)
                                }
                            },
                        )
                    }
                }
            }
        }

        if (collectionsOpen) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                CollectionsInlinePane(onExit = { collectionsOpen = false })
            }
            return@Row
        }

        if (downloadsOpen) {
            PlatformBackHandler(enabled = true) { onDownloadsOpenChange(false) }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                DownloadsScreen(
                    onBack = { onDownloadsOpenChange(false) },
                    onOpenDownload = onOpenDownload,
                )
            }
            return@Row
        }

        PredictiveBackPageHost(
            page = page,
            backPage = page.previousPage(),
            backEnabled = backEnabled,
            isForward = { from, to -> to.depth() > from.depth() },
            onBack = onNavigateBack,
            modifier = Modifier.weight(1f).fillMaxSize(),
        ) { page ->
        saveableStateHolder.SaveableStateProvider(page.name) {
            var rootSearchVisible by rememberSaveable { mutableStateOf(false) }
            var rootSearchRevealAnimating by rememberSaveable { mutableStateOf(false) }
            val hapticFeedback = LocalHapticFeedback.current
            val hapticScope = rememberCoroutineScope()
            fun openSearchTarget(target: SettingsSearchTarget) {
                when (target) {
                    is SettingsSearchTarget.Page -> {
                        if (target.page.isEnabledByPolicy()) {
                            openInlinePage(target.page)
                        }
                    }
                    SettingsSearchTarget.Downloads -> openDownloads()
                    SettingsSearchTarget.Collections -> { collectionsOpen = true }
                    SettingsSearchTarget.SwitchProfile -> onSwitchProfile?.invoke()
                    SettingsSearchTarget.CheckForUpdates -> onCheckForUpdatesClick?.invoke()
                }
            }

            val listState = rememberLazyListState()
            ScreenActivityEffect(listState) { screenActive ->
                if (!screenActive) listState.stopScroll()
            }
            val bottomOverlayPadding = LocalBottomNavigationOverlayPadding.current
            val rootSearchRevealConnection = rememberSettingsRootSearchRevealConnection(
                page = page,
                listState = listState,
                query = settingsSearchQuery,
                searchVisible = rootSearchVisible,
            ) {
                rootSearchVisible = true
                rootSearchRevealAnimating = true
                hapticScope.launch {
                    delay(SettingsSearchRevealHapticDelayMillis)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            LaunchedEffect(rootSearchRevealAnimating) {
                if (rootSearchRevealAnimating) {
                    delay(SettingsSearchRevealAnimationMillis)
                    rootSearchRevealAnimating = false
                }
            }
            LaunchedEffect(scrollToTopRequests) {
                scrollToTopRequests.collect {
                    listState.animateScrollToItem(0)
                }
            }
            val links = SettingsPageLinks(
                openPage = ::openInlinePage,
                openSubPage = onPageChange,
                onHomescreenClick = { openInlinePage(SettingsPage.Homescreen) },
                onMetaScreenClick = { openInlinePage(SettingsPage.MetaScreen) },
                onContinueWatchingClick = { openInlinePage(SettingsPage.ContinueWatching) },
                onAddonsClick = { openInlinePage(SettingsPage.Addons) },
                onPluginsClick = { openInlinePage(SettingsPage.Plugins) },
                onCollectionsClick = { collectionsOpen = true },
            )
            val previousPage = page.previousPage()
            val pageTitle = if (page == SettingsPage.Root) {
                if (settingsSearchQuery.isBlank()) {
                    stringResource(activeCategory.labelRes)
                } else {
                    stringResource(Res.string.compose_settings_page_root)
                }
            } else {
                stringResource(page.titleRes)
            }
            val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .nestedScroll(rootSearchRevealConnection),
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    if (showInternalHeader) {
                        LargeFlexibleTopAppBar(
                            title = { Text(pageTitle) },
                            navigationIcon = {
                                if (previousPage != null) {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                            contentDescription = stringResource(Res.string.action_back),
                                        )
                                    }
                                }
                            },
                            scrollBehavior = topAppBarScrollBehavior,
                        )
                    }
                },
            ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 40.dp,
                    top = if (showInternalHeader) innerPadding.calculateTopPadding() else topOffset,
                    end = 40.dp,
                    bottom = 40.dp + bottomOverlayPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (page) {
                    SettingsPage.Root -> {
                        settingsSearchRootContent(
                            query = settingsSearchQuery,
                            entries = {
                                settingsSearchEntries(
                                    isTablet = true,
                                    pluginsEnabled = AppFeaturePolicy.pluginsEnabled,
                                    supportersContributorsPageEnabled = AppFeaturePolicy.supportersContributorsPageEnabled,
                                        personalMediaAddonCopyEnabled = AppFeaturePolicy.personalMediaAddonCopyEnabled,
                                    switchProfileAvailable = onSwitchProfile != null,
                                    checkForUpdatesAvailable = onCheckForUpdatesClick != null,
                                )
                            },
                            isTablet = true,
                            showSearchField = rootSearchVisible,
                            animateSearchField = rootSearchRevealAnimating,
                            onQueryChange = { settingsSearchQuery = it },
                            onTargetClick = { openSearchTarget(it) },
                        )
                        if (settingsSearchQuery.isBlank()) {
                            settingsRootContent(
                                isTablet = true,
                                onPlaybackClick = { openInlinePage(SettingsPage.Playback) },
                                onAppearanceClick = { openInlinePage(SettingsPage.Appearance) },
                                onAdvancedClick = { openInlinePage(SettingsPage.Advanced) },
                                onNotificationsClick = { openInlinePage(SettingsPage.Notifications) },
                                onContentDiscoveryClick = { openInlinePage(SettingsPage.ContentDiscovery) },
                                onIntegrationsClick = { openInlinePage(SettingsPage.Integrations) },
                                onTrackingClick = { openInlinePage(SettingsPage.TraktAuthentication) },
                                onSupportersContributorsClick = { openInlinePage(SettingsPage.SupportersContributors) },
                                onLicensesAttributionsClick = { openInlinePage(SettingsPage.LicensesAttributions) },
                                onCheckForUpdatesClick = onCheckForUpdatesClick,
                                onTestUpdateBannerClick = onTestUpdateBannerClick,
                                onDownloadsClick = ::openDownloads,
                                onSwitchProfileClick = onSwitchProfile,
                                showAccountSection = activeCategory == SettingsCategory.Profile,
                                showGeneralSection = activeCategory == SettingsCategory.General,
                                showAboutSection = activeCategory == SettingsCategory.About,
                                showAdvancedSection = activeCategory == SettingsCategory.Advanced,
                                showSupportersContributorsPage = AppFeaturePolicy.supportersContributorsPageEnabled,
                            )
                        }
                    }
                    else -> settingsPageContent(page, isTablet = true, data = data, links = links)
                }
            }
            }
        }
        }
    }
}

// Everything the settings pages show, collected once for whichever layout is on screen.
internal data class SettingsData(
    val playerSettings: PlayerSettingsUiState,
    val rememberLastProfileEnabled: Boolean,
    val amoledEnabled: Boolean,
    val appIconState: AppIconSettingsState,
    val onAppIconSelected: (AppIconOption) -> Unit,
    val selectedAppLanguage: AppLanguage,
    val episodeReleaseNotifications: EpisodeReleaseNotificationsUiState,
    val tmdbSettings: TmdbSettings,
    val mdbListSettings: MdbListSettings,
    val debridSettings: DebridSettings,
    val traktAuth: TraktAuthUiState,
    val simklAuth: SimklAuthUiState,
    val traktCommentsEnabled: Boolean,
    val trackingSettings: TrackingSettingsUiState,
    val homescreen: HomeCatalogSettingsUiState,
    val homescreenCatalogLoading: Boolean,
    val homescreenCatalogErrorMessage: String?,
    val metaScreen: MetaScreenSettingsUiState,
    val continueWatching: ContinueWatchingPreferencesUiState,
    val posterCardStyle: PosterCardStyleUiState,
)

@Composable
internal fun rememberSettingsData(): SettingsData {
    val playerSettingsUiState by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val amoledEnabled by remember {
        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.amoledEnabled
    }.collectAsStateWithLifecycle()
    val selectedAppLanguage by remember { ThemeSettingsRepository.selectedAppLanguage }.collectAsStateWithLifecycle()
    val appIconState by remember {
        AppIconRepository.ensureLoaded()
        AppIconRepository.state
    }.collectAsStateWithLifecycle()
    val appIconScope = rememberCoroutineScope()
    val tmdbSettings by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val mdbListSettings by remember {
        MdbListSettingsRepository.ensureLoaded()
        MdbListSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val debridSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val traktAuthUiState by remember {
        TraktAuthRepository.ensureLoaded()
        TraktAuthRepository.uiState
    }.collectAsStateWithLifecycle()
    val simklAuthUiState by remember {
        SimklAuthRepository.ensureLoaded()
        SimklAuthRepository.uiState
    }.collectAsStateWithLifecycle()
    val traktCommentsEnabled by remember {
        TraktCommentsSettings.ensureLoaded()
        TraktCommentsSettings.enabled
    }.collectAsStateWithLifecycle()
    val trackingSettingsUiState by remember {
        TrackingSettingsRepository.ensureLoaded()
        TrackingSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val addonsUiState by remember {
        AddonRepository.initialize()
        AddonRepository.uiState
    }.collectAsStateWithLifecycle()
    val homescreenCatalogRefreshKey = remember(addonsUiState.addons) {
        buildAddonCatalogRefreshSignature(addonsUiState.addons)
    }
    val homescreenSettingsUiState by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val collections by CollectionRepository.collections.collectAsStateWithLifecycle()
    val metaScreenSettingsUiState by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val continueWatchingPreferencesUiState by remember {
        ContinueWatchingPreferencesRepository.ensureLoaded()
        ContinueWatchingPreferencesRepository.uiState
    }.collectAsStateWithLifecycle()
    val posterCardStyleUiState by remember {
        PosterCardStyleRepository.ensureLoaded()
        PosterCardStyleRepository.uiState
    }.collectAsStateWithLifecycle()
    val episodeReleaseNotificationsUiState by remember {
        EpisodeReleaseNotificationsRepository.ensureLoaded()
        EpisodeReleaseNotificationsRepository.uiState
    }.collectAsStateWithLifecycle()
    val profileSettingsState by remember {
        ProfileRepository.state
    }.collectAsStateWithLifecycle()

    LaunchedEffect(homescreenCatalogRefreshKey) {
        val enabledAddons = addonsUiState.addons.enabledAddons()
        if (!enabledAddons.isWaitingForFirstEnabledManifest()) {
            HomeCatalogSettingsRepository.syncCatalogs(enabledAddons)
        }
    }

    LaunchedEffect(Unit) {
        CollectionRepository.initialize()
    }

    LaunchedEffect(collections) {
        HomeCatalogSettingsRepository.syncCollections(collections)
    }

    return SettingsData(
        playerSettings = playerSettingsUiState,
        rememberLastProfileEnabled = profileSettingsState.rememberLastProfileEnabled,
        amoledEnabled = amoledEnabled,
        appIconState = appIconState,
        onAppIconSelected = { icon -> appIconScope.launch { AppIconRepository.select(icon) } },
        selectedAppLanguage = selectedAppLanguage,
        episodeReleaseNotifications = episodeReleaseNotificationsUiState,
        tmdbSettings = tmdbSettings,
        mdbListSettings = mdbListSettings,
        debridSettings = debridSettings,
        traktAuth = traktAuthUiState,
        simklAuth = simklAuthUiState,
        traktCommentsEnabled = traktCommentsEnabled,
        trackingSettings = trackingSettingsUiState,
        homescreen = homescreenSettingsUiState,
        homescreenCatalogLoading = addonsUiState.addons.hasPendingEnabledManifests(),
        homescreenCatalogErrorMessage = addonsUiState.addons.firstEnabledManifestError(),
        metaScreen = metaScreenSettingsUiState,
        continueWatching = continueWatchingPreferencesUiState,
        posterCardStyle = posterCardStyleUiState,
    )
}

// Where the links inside settings pages go; the phone and tablet layouts open pages differently.
internal class SettingsPageLinks(
    val openPage: (SettingsPage) -> Unit,
    val openSubPage: (SettingsPage) -> Unit,
    val onHomescreenClick: () -> Unit,
    val onMetaScreenClick: () -> Unit,
    val onContinueWatchingClick: () -> Unit,
    val onAddonsClick: () -> Unit,
    val onPluginsClick: () -> Unit,
    val onCollectionsClick: () -> Unit,
)

// Every settings page except the root, which each layout lays out itself.
internal fun LazyListScope.settingsPageContent(
    page: SettingsPage,
    isTablet: Boolean,
    data: SettingsData,
    links: SettingsPageLinks,
) {
    val player = data.playerSettings
    when (page) {
        SettingsPage.Root -> Unit
        SettingsPage.SupportersContributors -> {
            if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                supportersContributorsContent(isTablet = isTablet)
            }
        }
        SettingsPage.LicensesAttributions -> licensesAttributionsContent(isTablet = isTablet)
        SettingsPage.Playback -> playbackSettingsContent(
            isTablet = isTablet,
            showLoadingOverlay = player.showLoadingOverlay,
            holdToSpeedEnabled = player.holdToSpeedEnabled,
            holdToSpeedValue = player.holdToSpeedValue,
            touchGesturesEnabled = player.touchGesturesEnabled,
            preferredAudioLanguage = player.preferredAudioLanguage,
            secondaryPreferredAudioLanguage = player.secondaryPreferredAudioLanguage,
            preferredSubtitleLanguage = player.preferredSubtitleLanguage,
            secondaryPreferredSubtitleLanguage = player.secondaryPreferredSubtitleLanguage,
            streamReuseLastLinkEnabled = player.streamReuseLastLinkEnabled,
            streamReuseLastLinkCacheHours = player.streamReuseLastLinkCacheHours,
            androidPlaybackEngine = player.androidPlaybackEngine,
            androidLibmpvVideoOutput = player.androidLibmpvVideoOutput,
            androidLibmpvHardwareDecodingEnabled = player.androidLibmpvHardwareDecodingEnabled,
            androidLibmpvYuv420pEnabled = player.androidLibmpvYuv420pEnabled,
            decoderPriority = player.decoderPriority,
            mapDV7ToHevc = player.mapDV7ToHevc,
            tunnelingEnabled = player.tunnelingEnabled,
            useLibass = player.useLibass,
            libassRenderType = player.libassRenderType,
        )
        SettingsPage.Streams -> streamsSettingsContent(isTablet = isTablet)
        SettingsPage.Appearance -> appearanceSettingsContent(
            isTablet = isTablet,
            amoledEnabled = data.amoledEnabled,
            onAmoledToggle = ThemeSettingsRepository::setAmoled,
            appIconState = data.appIconState,
            onAppIconSelected = data.onAppIconSelected,
            onAppIconFailureDismissed = AppIconRepository::clearFailure,
            selectedAppLanguage = data.selectedAppLanguage,
            onAppLanguageSelected = ThemeSettingsRepository::setAppLanguage,
            onHomescreenClick = links.onHomescreenClick,
            onMetaScreenClick = links.onMetaScreenClick,
            onStreamsClick = { links.openPage(SettingsPage.Streams) },
            onCollectionsClick = links.onCollectionsClick,
            onContinueWatchingClick = links.onContinueWatchingClick,
            onPosterCustomizationClick = { links.openPage(SettingsPage.PosterCustomization) },
        )
        SettingsPage.Advanced -> advancedSettingsContent(
            isTablet = isTablet,
            rememberLastProfileEnabled = data.rememberLastProfileEnabled,
        )
        SettingsPage.Notifications -> notificationsSettingsContent(
            isTablet = isTablet,
            uiState = data.episodeReleaseNotifications,
        )
        SettingsPage.ContinueWatching -> continueWatchingSettingsContent(
            isTablet = isTablet,
            isVisible = data.continueWatching.isVisible,
            style = data.continueWatching.style,
            upNextFromFurthestEpisode = data.continueWatching.upNextFromFurthestEpisode,
            useEpisodeThumbnails = data.continueWatching.useEpisodeThumbnails,
            showUnairedNextUp = data.continueWatching.showUnairedNextUp,
            blurNextUp = data.continueWatching.blurNextUp,
            sortMode = data.continueWatching.sortMode,
        )
        SettingsPage.PosterCustomization -> posterCustomizationSettingsContent(uiState = data.posterCardStyle)
        SettingsPage.ContentDiscovery -> contentDiscoveryContent(
            isTablet = isTablet,
            showPluginsEntry = AppFeaturePolicy.pluginsEnabled,
            onAddonsClick = links.onAddonsClick,
            onPluginsClick = links.onPluginsClick,
        )
        SettingsPage.Addons -> addonsSettingsContent()
        SettingsPage.Plugins -> if (AppFeaturePolicy.pluginsEnabled) pluginsSettingsContent() else addonsSettingsContent()
        SettingsPage.Homescreen -> homescreenSettingsContent(
            isTablet = isTablet,
            heroEnabled = data.homescreen.heroEnabled,
            showCatalogType = data.homescreen.showCatalogType,
            hideUnreleasedContent = data.homescreen.hideUnreleasedContent,
            items = data.homescreen.items,
            isCatalogLoading = data.homescreenCatalogLoading,
            catalogErrorMessage = data.homescreenCatalogErrorMessage,
        )
        SettingsPage.MetaScreen -> metaScreenSettingsContent(isTablet = isTablet, uiState = data.metaScreen)
        SettingsPage.Integrations -> integrationsContent(
            isTablet = isTablet,
            onTmdbClick = { links.openSubPage(SettingsPage.TmdbEnrichment) },
            onMdbListClick = { links.openSubPage(SettingsPage.MdbListRatings) },
            onDebridClick = { links.openSubPage(SettingsPage.Debrid) },
        )
        SettingsPage.TmdbEnrichment -> tmdbSettingsContent(isTablet = isTablet, settings = data.tmdbSettings)
        SettingsPage.MdbListRatings -> mdbListSettingsContent(isTablet = isTablet, settings = data.mdbListSettings)
        SettingsPage.Debrid -> debridSettingsContent(isTablet = isTablet, settings = data.debridSettings)
        SettingsPage.TraktAuthentication -> trackingSettingsContent(
            isTablet = isTablet,
            traktUiState = data.traktAuth,
            simklUiState = data.simklAuth,
            settingsUiState = data.trackingSettings,
            commentsEnabled = data.traktCommentsEnabled,
            onCommentsEnabledChange = TraktCommentsSettings::setEnabled,
        )
    }
}
