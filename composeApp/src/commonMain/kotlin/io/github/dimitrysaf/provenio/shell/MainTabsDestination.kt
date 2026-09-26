package io.github.dimitrysaf.provenio.shell

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.WideNavigationRail
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
                // Collapsed only. The menu is optional in the rail's anatomy, and the expanded
                // variant exists to reveal secondary destinations — with four items and nothing
                // hidden behind them there is nothing for it to reveal, and the collapsed rail
                // already sits inside the 3–7 item range it is specified for.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (initialHomeReady) 1f else 0f),
                ) {
                    WideNavigationRail {
                        // On tablets, centre the destinations so they are easier to reach.
                        Spacer(modifier = Modifier.weight(1f))
                        destinations.forEach { destination ->
                            WideNavigationRailItem(
                                selected = selectedTab == destination.tab,
                                onClick = { onTabSelected(destination.tab) },
                                icon = destination.icon,
                                label = { Text(destination.label) },
                                railExpanded = false,
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Box(modifier = Modifier.weight(1f)) { content() }
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

