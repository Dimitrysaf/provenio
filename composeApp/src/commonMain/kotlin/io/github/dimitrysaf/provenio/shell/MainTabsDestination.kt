package io.github.dimitrysaf.provenio.shell

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import io.github.dimitrysaf.provenio.shell.components.ProvidePosterAreaWidth
import io.github.dimitrysaf.provenio.shell.components.SnackbarAnchor
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import io.github.dimitrysaf.provenio.shell.components.openedWithKeys
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.WindowBreakpoint
import io.github.dimitrysaf.provenio.core.profiles.Profile
import io.github.dimitrysaf.provenio.shell.screens.profiles.ProfileSwitcherTab
import io.github.dimitrysaf.provenio.shell.screens.settings.AppBrandSymbol
import io.github.dimitrysaf.provenio.shell.screens.settings.AppBrandWordmark
import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import androidx.compose.animation.Crossfade
import androidx.compose.material3.MaterialTheme
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_nav_home
import provenio.composeapp.generated.resources.compose_nav_library
import provenio.composeapp.generated.resources.compose_nav_profile
import provenio.composeapp.generated.resources.compose_nav_search
import org.jetbrains.compose.resources.stringResource

/** One top-level destination, so the bar and the rail render from a single list. */
private class NavDestination(
    val tab: AppScreenTab,
    val label: String,
    val icon: @Composable () -> Unit,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MainTabsDestination(
    selectedTab: AppScreenTab,
    initialHomeReady: Boolean,
    rootRouteActive: Boolean,
    useTabletFloatingTabBar: Boolean,
    useNativeNavigation: Boolean,
    useNativeTabBar: Boolean,
    requests: AppTabRequests,
    state: AppTabState,
    actions: (isTabletLayout: Boolean) -> AppTabActions,
    onBack: () -> Unit,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (Profile) -> Unit,
    onAddProfileRequested: () -> Unit,
) {
    PlatformBackHandler(enabled = rootRouteActive, onBack = onBack)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val breakpoint = WindowBreakpoint.forWidth(maxWidth)

        // "Compact windows should always use a navigation bar"; rails are for medium and up, and
        // the two are never shown at the same time.
        val useRail = useTabletFloatingTabBar || breakpoint >= WindowBreakpoint.Medium
        val isTabletLayout = useRail
        val tabActions = remember(actions, isTabletLayout) { actions(isTabletLayout) }
        val useNativeBottomTabs = useNativeNavigation && useNativeTabBar

        val content: @Composable () -> Unit = {
            AppTabHost(
                selectedTab = selectedTab,
                requests = requests,
                state = state,
                actions = tabActions,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val destinations = listOf(
            NavDestination(
                tab = AppScreenTab.Home,
                label = stringResource(Res.string.compose_nav_home),
                icon = {
                    NavIcon(
                        selected = selectedTab == AppScreenTab.Home,
                        selectedIcon = Icons.Filled.Home,
                        icon = Icons.Outlined.Home,
                    )
                },
            ),
            NavDestination(
                tab = AppScreenTab.Search,
                label = stringResource(Res.string.compose_nav_search),
                icon = {
                    NavIcon(
                        selected = selectedTab == AppScreenTab.Search,
                        selectedIcon = Icons.Filled.Search,
                        icon = Icons.Outlined.Search,
                    )
                },
            ),
            NavDestination(
                tab = AppScreenTab.Library,
                label = stringResource(Res.string.compose_nav_library),
                icon = {
                    NavIcon(
                        selected = selectedTab == AppScreenTab.Library,
                        selectedIcon = Icons.Filled.VideoLibrary,
                        icon = Icons.Outlined.VideoLibrary,
                    )
                },
            ),
            NavDestination(
                tab = AppScreenTab.Settings,
                label = stringResource(Res.string.compose_nav_profile),
                icon = {
                    ProfileSwitcherTab(
                        selected = selectedTab == AppScreenTab.Settings,
                        onClick = { onTabSelected(AppScreenTab.Settings) },
                        onProfileSelected = onProfileSelected,
                        onAddProfileRequested = onAddProfileRequested,
                    )
                },
            ),
        )

        val localDensity = LocalDensity.current
        var navigationBarHeight by remember { mutableStateOf(0.dp) }
        val showsBottomBar = !useNativeBottomTabs && !useRail
        LaunchedEffect(showsBottomBar, rootRouteActive, navigationBarHeight) {
            SnackbarAnchor.bottomInset.value = if (showsBottomBar && rootRouteActive) navigationBarHeight else 0.dp
        }
        DisposableEffect(Unit) {
            onDispose { SnackbarAnchor.bottomInset.value = 0.dp }
        }

        when {
            useNativeBottomTabs -> content()

            useRail -> {
                // Collapsed, until a remote or a keyboard moves focus into it: then it expands to
                // name its destinations, as there is no hovering over an icon to learn what it
                // is. A mouse or a touch never expands it, as the mouse sets the input mode to
                // touch and neither ever focuses a destination. It expands over the page rather
                // than pushing it aside, so the page does not reflow each time focus passes
                // through.
                val railState = rememberWideNavigationRailState()
                var railHasFocus by remember { mutableStateOf(false) }
                val railExpanded = railHasFocus && openedWithKeys()
                LaunchedEffect(railExpanded) {
                    if (railExpanded) railState.expand() else railState.collapse()
                }
                // Switching tabs clears focus, so a destination picked with keys takes it back
                // once the new tab is in, and the remote stays where it was.
                val destinationFocus = remember { AppScreenTab.entries.associateWith { FocusRequester() } }
                var refocusSelectedDestination by remember { mutableStateOf(false) }
                LaunchedEffect(selectedTab, refocusSelectedDestination) {
                    if (!refocusSelectedDestination) return@LaunchedEffect
                    withFrameNanos {}
                    runCatching { destinationFocus.getValue(selectedTab).requestFocus(FocusDirection.Enter) }
                    refocusSelectedDestination = false
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (initialHomeReady) 1f else 0f),
                ) {
                    ProvidePosterAreaWidth(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start))
                            .padding(start = RailCollapsedWidth),
                    ) { content() }

                    WideNavigationRail(
                        state = railState,
                        modifier = Modifier
                            .shadow(if (railExpanded) ExpandedRailElevation else 0.dp)
                            .onFocusChanged { railHasFocus = it.hasFocus },
                        header = {
                            // The rail sets its header at the start, not the middle, and lets it be
                            // as wide as an expanded rail, so each logo is given its own size: the
                            // symbol centred in a box as wide as the collapsed rail, and the full
                            // name when expanded, starting where the symbol and the destinations'
                            // icons start.
                            Crossfade(targetState = railExpanded, label = "rail_logo") { expanded ->
                                if (expanded) {
                                    Box(modifier = Modifier.padding(start = RailExpandedContentStart, top = 8.dp, bottom = 8.dp)) {
                                        AppBrandWordmark(
                                            modifier = Modifier
                                                .height(RailLogoHeight)
                                                .aspectRatio(RailWordmarkAspectRatio),
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .width(RailCollapsedWidth)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AppBrandSymbol(
                                            modifier = Modifier
                                                .height(RailLogoHeight)
                                                .aspectRatio(RailLogoAspectRatio),
                                        )
                                    }
                                }
                            }
                        },
                    ) {
                        // The rail gives each of its children a destination's height, so these
                        // leave a destination's space before and after them.
                        Spacer(modifier = Modifier)
                        destinations.forEach { destination ->
                            WideNavigationRailItem(
                                selected = selectedTab == destination.tab,
                                onClick = {
                                    refocusSelectedDestination = railHasFocus
                                    onTabSelected(destination.tab)
                                },
                                modifier = Modifier.focusRequester(destinationFocus.getValue(destination.tab)),
                                icon = destination.icon,
                                label = { Text(destination.label) },
                                railExpanded = railExpanded,
                            )
                        }
                        Spacer(modifier = Modifier)
                    }

                    // The version sits at the foot of the rail, over it rather than in it: the
                    // rail lays its children out from the top, one destination's height each.
                    Crossfade(
                        targetState = railExpanded,
                        label = "rail_version",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .windowInsetsPadding(
                                WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start + WindowInsetsSides.Bottom),
                            )
                            .padding(bottom = 16.dp),
                    ) { expanded ->
                        Box(
                            modifier = if (expanded) {
                                Modifier.padding(start = RailExpandedContentStart)
                            } else {
                                Modifier.width(RailCollapsedWidth)
                            },
                            contentAlignment = if (expanded) Alignment.CenterStart else Alignment.Center,
                        ) {
                            Text(
                                text = AppVersionConfig.VERSION_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (initialHomeReady) 1f else 0f),
                ) {
                    Box(modifier = Modifier.weight(1f)) { content() }

                    // The flexible navigation bar; the baseline bar is no longer recommended.
                    ShortNavigationBar(
                        modifier = Modifier.onSizeChanged { size ->
                            navigationBarHeight = with(localDensity) { size.height.toDp() }
                        },
                    ) {
                        destinations.forEach { destination ->
                            ShortNavigationBarItem(
                                selected = selectedTab == destination.tab,
                                onClick = { onTabSelected(destination.tab) },
                                icon = destination.icon,
                                label = { Text(destination.label) },
                                iconPosition = NavigationItemIconPosition.Top,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A destination's icon fills when its page is the active one. */
@Composable
private fun NavIcon(
    selected: Boolean,
    selectedIcon: ImageVector,
    icon: ImageVector,
) {
    Icon(
        imageVector = if (selected) selectedIcon else icon,
        contentDescription = null,
    )
}

/** The app's symbol at the top of the rail, the height of a destination's icon and label. */
private val RailLogoHeight = 32.dp

/** The width of a collapsed rail, which Material keeps to itself. */
private val RailCollapsedWidth = 96.dp

/** An expanded rail lies over the page, so it takes the second elevation level to stand apart. */
private val ExpandedRailElevation = 3.dp

/** The symbol's own width over its height. */
private const val RailLogoAspectRatio = 160f / 222f

/** The full name's own width over its height. */
private const val RailWordmarkAspectRatio = 1387f / 344f

/**
 * Where an expanded rail's destinations start their icons: the rail's 20dp item padding and the
 * 16dp before the icon inside its indicator. The collapsed symbol's left edge falls there too.
 */
private val RailExpandedContentStart = 36.dp
