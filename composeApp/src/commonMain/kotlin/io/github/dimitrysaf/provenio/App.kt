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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.navigation.Destination
import io.github.dimitrysaf.provenio.pages.ListsPage
import io.github.dimitrysaf.provenio.pages.MoviesPage
import io.github.dimitrysaf.provenio.pages.ProfilePage
import io.github.dimitrysaf.provenio.pages.SettingsPage
import io.github.dimitrysaf.provenio.pages.TvPage
import io.github.dimitrysaf.provenio.theme.AppTheme
import io.github.dimitrysaf.provenio.theme.ThemeMode
import io.github.dimitrysaf.provenio.theme.isDynamicColorSupported
import io.github.dimitrysaf.provenio.ui.chrome.LocalSetBarsVisible
import io.github.dimitrysaf.provenio.ui.components.AppTopBar
import io.github.dimitrysaf.provenio.ui.responsive.WindowSizeClass
import io.github.dimitrysaf.provenio.ui.responsive.contentHorizontalPadding
import io.github.dimitrysaf.provenio.ui.responsive.maxContentWidth
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
        var destination by remember { mutableStateOf(Destination.Profile) }
        var barsVisible by remember { mutableStateOf(true) }

        CompositionLocalProvider(LocalSetBarsVisible provides { visible -> barsVisible = visible }) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompact = windowSizeClassOf(maxWidth) == WindowSizeClass.Compact

                Row(modifier = Modifier.fillMaxSize()) {
                    if (!isCompact) {
                        AnimatedVisibility(
                            visible = barsVisible,
                            enter = fadeIn() + slideInHorizontally { width -> -width },
                            exit = fadeOut() + slideOutHorizontally { width -> -width },
                        ) {
                            NavigationRail {
                                Destination.entries.forEach { entry ->
                                    val selected = entry == destination
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { destination = entry },
                                        icon = { DestinationIcon(entry, selected) },
                                        label = { Text(entry.label) },
                                    )
                                }
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        topBar = {
                            AnimatedVisibility(
                                visible = barsVisible,
                                enter = fadeIn() + slideInVertically { height -> -height },
                                exit = fadeOut() + slideOutVertically { height -> -height },
                            ) {
                                AppTopBar()
                            }
                        },
                        bottomBar = {
                            if (isCompact) {
                                AnimatedVisibility(
                                    visible = barsVisible,
                                    enter = fadeIn() + slideInVertically { height -> height },
                                    exit = fadeOut() + slideOutVertically { height -> height },
                                ) {
                                    NavigationBar {
                                        Destination.entries.forEach { entry ->
                                            val selected = entry == destination
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = { destination = entry },
                                                icon = { DestinationIcon(entry, selected) },
                                                label = { Text(entry.label) },
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ) { innerPadding ->
                        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            val horizontalPadding = contentHorizontalPadding(maxWidth)

                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                AnimatedContent(
                                    targetState = destination,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = maxContentWidth)
                                        .padding(horizontal = horizontalPadding),
                                    transitionSpec = {
                                        (fadeIn() + slideInVertically { height -> height / 8 })
                                            .togetherWith(fadeOut() + slideOutVertically { height -> -height / 8 })
                                    },
                                ) { current ->
                                    when (current) {
                                        Destination.Profile -> ProfilePage(Modifier.fillMaxSize())
                                        Destination.Tv -> TvPage(Modifier.fillMaxSize())
                                        Destination.Movies -> MoviesPage(Modifier.fillMaxSize())
                                        Destination.Lists -> ListsPage(Modifier.fillMaxSize())
                                        Destination.Settings -> SettingsPage(
                                            modifier = Modifier.fillMaxSize(),
                                            themeMode = themeMode,
                                            onThemeModeChange = { themeMode = it },
                                            useDynamicColor = useDynamicColor,
                                            onUseDynamicColorChange = { useDynamicColor = it },
                                            dynamicColorAvailable = isDynamicColorSupported(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
