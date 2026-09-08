package io.github.dimitrysaf.provenio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import io.github.dimitrysaf.provenio.ui.responsive.WindowSizeClass
import io.github.dimitrysaf.provenio.ui.responsive.windowSizeClassOf

@Composable
private fun DestinationIcon(entry: Destination, selected: Boolean) {
    Icon(
        imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
        contentDescription = entry.label,
    )
}

@Composable
private fun DestinationLabel(entry: Destination, selected: Boolean) {
    Text(text = entry.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    var useDynamicColor by remember { mutableStateOf(true) }

    AppTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        var selectedTab by remember { mutableStateOf(Destination.Profile) }
        var screen by remember { mutableStateOf<Screen>(Screen.Tab(Destination.Profile)) }
        var manualBarsVisible by remember { mutableStateOf(true) }

        // Nav auto-hides on any screen outside the tab set (e.g. Settings, Search).
        val navBarVisible = manualBarsVisible && screen is Screen.Tab

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
                val isCompact = windowSizeClassOf(maxWidth) == WindowSizeClass.Compact
                val density = LocalDensity.current

                var railWidthPx by remember { mutableStateOf(0) }
                var navBarHeightPx by remember { mutableStateOf(0) }
                val railWidth = with(density) { railWidthPx.toDp() }
                val navBarHeight = with(density) { navBarHeightPx.toDp() }

                // The rail/bottom bar always stays laid out in place; content overlays it
                // (rather than being removed from composition) so hiding never leaves a
                // reflow gap behind.
                val startInset by animateDpAsState(if (!isCompact && navBarVisible) railWidth else 0.dp)
                val bottomInset by animateDpAsState(if (isCompact && navBarVisible) navBarHeight else 0.dp)

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isCompact) {
                        NavigationRail(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(96.dp)
                                .onSizeChanged { size -> railWidthPx = size.width },
                        ) {
                            Destination.entries.forEach { entry ->
                                val selected = entry == selectedTab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        selectedTab = entry
                                        screen = Screen.Tab(entry)
                                    },
                                    icon = { DestinationIcon(entry, selected) },
                                    label = { DestinationLabel(entry, selected) },
                                )
                            }
                        }
                    } else {
                        NavigationBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { size -> navBarHeightPx = size.height },
                        ) {
                            Destination.entries.forEach { entry ->
                                val selected = entry == selectedTab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        selectedTab = entry
                                        screen = Screen.Tab(entry)
                                    },
                                    icon = { DestinationIcon(entry, selected) },
                                    label = { DestinationLabel(entry, selected) },
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = startInset, bottom = bottomInset),
                        contentWindowInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0),
                    ) { innerPadding ->
                        AnimatedContent(
                            targetState = screen,
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            transitionSpec = {
                                fadeIn(tween(100)).togetherWith(fadeOut(tween(100)))
                            },
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
