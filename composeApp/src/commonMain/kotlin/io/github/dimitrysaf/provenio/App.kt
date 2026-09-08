package io.github.dimitrysaf.provenio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.navigation.Destination
import io.github.dimitrysaf.provenio.navigation.Screen
import io.github.dimitrysaf.provenio.pages.ListsPage
import io.github.dimitrysaf.provenio.pages.MoviesPage
import io.github.dimitrysaf.provenio.pages.ProfilePage
import io.github.dimitrysaf.provenio.pages.SearchPage
import io.github.dimitrysaf.provenio.pages.SettingsPage
import io.github.dimitrysaf.provenio.pages.TvPage
import io.github.dimitrysaf.provenio.theme.AppTheme
import io.github.dimitrysaf.provenio.theme.ThemeMode
import io.github.dimitrysaf.provenio.theme.isDynamicColorSupported
import io.github.dimitrysaf.provenio.ui.backhandler.SystemBackHandler
import io.github.dimitrysaf.provenio.ui.chrome.LocalBarsVisible
import io.github.dimitrysaf.provenio.ui.chrome.LocalSetBarsVisible
import io.github.dimitrysaf.provenio.ui.responsive.usesExpandedRail
import io.github.dimitrysaf.provenio.ui.responsive.usesRail
import io.github.dimitrysaf.provenio.ui.responsive.windowSizeClassOf

@Composable
private fun DestinationIcon(entry: Destination, selected: Boolean) {
    Icon(
        imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
        contentDescription = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    var useDynamicColor by remember { mutableStateOf(true) }

    AppTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        var selectedTab by remember { mutableStateOf(Destination.Profile) }
        var screen by remember { mutableStateOf<Screen>(Screen.Tab(Destination.Profile)) }
        var manualBarsVisible by remember { mutableStateOf(true) }

        // Nav auto-hides on any screen outside the tab set (e.g. Settings, Search).
        val navVisible = manualBarsVisible && screen is Screen.Tab

        // Settings/Search are logically sub-pages; let the system back gesture/button pop
        // back to the previous tab instead of exiting the app.
        SystemBackHandler(enabled = screen !is Screen.Tab) {
            screen = Screen.Tab(selectedTab)
        }

        CompositionLocalProvider(
            LocalBarsVisible provides rememberUpdatedState(manualBarsVisible),
            LocalSetBarsVisible provides { visible -> manualBarsVisible = visible },
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sizeClass = windowSizeClassOf(maxWidth)
                val showRail = sizeClass.usesRail()
                val expandRail = sizeClass.usesExpandedRail()
                val density = LocalDensity.current
                val motionScheme = MaterialTheme.motionScheme

                // M3 puts a collapsed rail at medium/expanded and an expanded one once
                // there is Large-breakpoint width to spare.
                val railState = rememberWideNavigationRailState()
                LaunchedEffect(expandRail) {
                    if (expandRail) railState.expand() else railState.collapse()
                }

                var railWidthPx by remember { mutableStateOf(0) }
                var navBarHeightPx by remember { mutableStateOf(0) }
                val railWidth = with(density) { railWidthPx.toDp() }
                val navBarHeight = with(density) { navBarHeightPx.toDp() }

                // The rail/bottom bar always stays laid out in place; content overlays it
                // (rather than being removed from composition) so hiding never leaves a
                // reflow gap behind.
                val startInset by animateDpAsState(
                    targetValue = if (showRail && navVisible) railWidth else 0.dp,
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    label = "navRailInset",
                )
                val bottomInset by animateDpAsState(
                    targetValue = if (!showRail && navVisible) navBarHeight else 0.dp,
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    label = "navBarInset",
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showRail) {
                        WideNavigationRail(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .onSizeChanged { size -> railWidthPx = size.width },
                            state = railState,
                        ) {
                            Destination.entries.forEach { entry ->
                                val selected = entry == selectedTab
                                WideNavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        selectedTab = entry
                                        screen = Screen.Tab(entry)
                                    },
                                    icon = { DestinationIcon(entry, selected) },
                                    label = { Text(entry.label) },
                                    railExpanded = expandRail,
                                )
                            }
                        }
                    } else {
                        ShortNavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { size -> navBarHeightPx = size.height },
                        ) {
                            Destination.entries.forEach { entry ->
                                val selected = entry == selectedTab
                                ShortNavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        selectedTab = entry
                                        screen = Screen.Tab(entry)
                                    },
                                    icon = { DestinationIcon(entry, selected) },
                                    label = { Text(entry.label) },
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = startInset, bottom = bottomInset),
                        // The nav components and each page's top app bar apply their own
                        // insets, so the Scaffold must not add them a second time.
                        contentWindowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0),
                    ) { innerPadding ->
                        AnimatedContent(
                            targetState = screen,
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            // M3 fade-through: the outgoing screen fades, the incoming one
                            // fades and settles in from a slightly smaller scale.
                            transitionSpec = {
                                (
                                    fadeIn(motionScheme.defaultEffectsSpec()) +
                                        scaleIn(
                                            animationSpec = motionScheme.defaultSpatialSpec(),
                                            initialScale = 0.92f,
                                        )
                                    ).togetherWith(fadeOut(motionScheme.defaultEffectsSpec()))
                            },
                            label = "screen",
                        ) { current ->
                            when (current) {
                                is Screen.Tab -> when (current.destination) {
                                    Destination.Profile -> ProfilePage(
                                        modifier = Modifier.fillMaxSize(),
                                        onSearchClick = { screen = Screen.Search },
                                        onSettingsClick = { screen = Screen.Settings },
                                    )
                                    Destination.Tv -> TvPage(
                                        modifier = Modifier.fillMaxSize(),
                                        onSearchClick = { screen = Screen.Search },
                                        onFilterClick = {},
                                    )
                                    Destination.Movies -> MoviesPage(
                                        modifier = Modifier.fillMaxSize(),
                                        onSearchClick = { screen = Screen.Search },
                                        onFilterClick = {},
                                    )
                                    Destination.Lists -> ListsPage(
                                        modifier = Modifier.fillMaxSize(),
                                        onSearchClick = { screen = Screen.Search },
                                        onFilterClick = {},
                                    )
                                }
                                Screen.Settings -> SettingsPage(
                                    modifier = Modifier.fillMaxSize(),
                                    onBack = { screen = Screen.Tab(selectedTab) },
                                    themeMode = themeMode,
                                    onThemeModeChange = { themeMode = it },
                                    useDynamicColor = useDynamicColor,
                                    onUseDynamicColorChange = { useDynamicColor = it },
                                    dynamicColorAvailable = isDynamicColorSupported(),
                                )
                                Screen.Search -> SearchPage(
                                    modifier = Modifier.fillMaxSize(),
                                    onBack = { screen = Screen.Tab(selectedTab) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
