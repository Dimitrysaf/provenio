package io.github.dimitrysaf.provenio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
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

        CompositionLocalProvider(
            LocalBarsVisible provides rememberUpdatedState(manualBarsVisible),
            LocalSetBarsVisible provides { visible -> manualBarsVisible = visible },
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = windowSizeClassOf(maxWidth) == WindowSizeClass.Compact

                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isCompact) {
                        AnimatedVisibility(
                            visible = navBarVisible,
                            enter = fadeIn() + slideInHorizontally { width -> -width },
                            exit = fadeOut() + slideOutHorizontally { width -> -width },
                        ) {
                            NavigationRail {
                                Destination.entries.forEach { entry ->
                                    val selected = entry == selectedTab
                                    NavigationRailItem(
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
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        bottomBar = {
                            if (isCompact) {
                                AnimatedVisibility(
                                    visible = navBarVisible,
                                    enter = fadeIn() + slideInVertically { height -> height },
                                    exit = fadeOut() + slideOutVertically { height -> height },
                                ) {
                                    NavigationBar {
                                        Destination.entries.forEach { entry ->
                                            val selected = entry == selectedTab
                                            NavigationBarItem(
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
                            }
                        },
                    ) { innerPadding ->
                        AnimatedContent(
                            targetState = screen,
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            transitionSpec = {
                                (fadeIn() + slideInVertically { height -> height / 8 })
                                    .togetherWith(fadeOut() + slideOutVertically { height -> -height / 8 })
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
