package io.github.dimitrysaf.provenio.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.navigation.Destination
import io.github.dimitrysaf.provenio.feature.home.HomeScreen
import io.github.dimitrysaf.provenio.feature.library.LibraryScreen
import io.github.dimitrysaf.provenio.feature.search.SearchScreen
import io.github.dimitrysaf.provenio.feature.settings.SettingsCategory
import io.github.dimitrysaf.provenio.feature.settings.SettingsScreen
import io.github.dimitrysaf.provenio.designsystem.theme.MotionTokens
import io.github.dimitrysaf.provenio.designsystem.theme.ThemeMode
import io.github.dimitrysaf.provenio.designsystem.layout.usesExpandedRail
import io.github.dimitrysaf.provenio.designsystem.layout.usesRail
import io.github.dimitrysaf.provenio.designsystem.layout.windowSizeClassOf
import org.jetbrains.compose.resources.stringResource

@Composable
private fun DestinationIcon(entry: Destination, selected: Boolean) {
    Icon(
        imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
        contentDescription = null,
    )
}

/**
 * The app's only top-level destination: navigation plus the tab set.
 *
 * Laid out with plain weights and no animated insets. A sub-page covers this screen
 * rather than asking the navigation to move aside, so nothing here ever re-measures.
 *
 * Search and Settings are tabs rather than pushed pages, so neither draws a back arrow and
 * neither covers the navigation. Anything that wants a particular settings category asks
 * for it through [openSettings], which switches tab and names the category in one go.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    modifier: Modifier = Modifier,
    onOpenDetail: (type: String, id: String) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
) {
    // Saved rather than remembered: navigating to a sub-page disposes this screen's
    // composition, and the selected tab has to survive coming back.
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = Destination.entries[selectedIndex]

    // Set only when a button names the category it means. Cleared as soon as a tab is
    // picked by hand, so opening Settings that way reopens wherever it was left.
    var pendingSettingsCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    val selectTab: (Int) -> Unit = { index ->
        selectedIndex = index
        pendingSettingsCategory = null
    }
    val openSettings: (SettingsCategory) -> Unit = { category ->
        pendingSettingsCategory = category
        selectedIndex = Destination.entries.indexOf(Destination.Settings)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizeClass = windowSizeClassOf(maxWidth)
        val showRail = sizeClass.usesRail()
        val expandRail = sizeClass.usesExpandedRail()

        // M3 puts a collapsed rail at medium/expanded and an expanded one once there is
        // Large-breakpoint width to spare.
        val railState = rememberWideNavigationRailState()
        LaunchedEffect(expandRail) {
            if (expandRail) railState.expand() else railState.collapse()
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (showRail) {
                WideNavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    state = railState,
                ) {
                    Destination.entries.forEachIndexed { index, entry ->
                        val selected = index == selectedIndex
                        WideNavigationRailItem(
                            selected = selected,
                            onClick = { selectTab(index) },
                            icon = { DestinationIcon(entry, selected) },
                            label = { Text(stringResource(entry.label)) },
                            railExpanded = expandRail,
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f),
                    // Same rule as page transitions: the arriving tab fades in over the
                    // outgoing one, which stays opaque underneath. Fading both out at once
                    // would show the window background through the gap.
                    transitionSpec = {
                        fadeIn(
                            tween(
                                durationMillis = MotionTokens.DurationShort2,
                                easing = MotionTokens.Standard,
                            ),
                        ) togetherWith ExitTransition.None
                    },
                    label = "tab",
                ) { tab ->
                    when (tab) {
                        Destination.Home -> HomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            onSettingsClick = { openSettings(SettingsCategory.Simkl) },
                            onAddAddons = { openSettings(SettingsCategory.Addons) },
                            onOpenDetail = onOpenDetail,
                        )
                        Destination.Search -> SearchScreen(
                            modifier = Modifier.fillMaxSize(),
                            onAddAddons = { openSettings(SettingsCategory.Addons) },
                            onOpenDetail = onOpenDetail,
                        )
                        Destination.Library -> LibraryScreen(
                            modifier = Modifier.fillMaxSize(),
                            onOpenDetail = onOpenDetail,
                            onSettingsClick = { openSettings(SettingsCategory.Simkl) },
                        )
                        Destination.Settings -> SettingsScreen(
                            modifier = Modifier.fillMaxSize(),
                            initialCategory = pendingSettingsCategory,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            useDynamicColor = useDynamicColor,
                            onUseDynamicColorChange = onUseDynamicColorChange,
                            dynamicColorAvailable = dynamicColorAvailable,
                        )
                    }
                }

                if (!showRail) {
                    ShortNavigationBar {
                        Destination.entries.forEachIndexed { index, entry ->
                            val selected = index == selectedIndex
                            ShortNavigationBarItem(
                                selected = selected,
                                onClick = { selectTab(index) },
                                icon = { DestinationIcon(entry, selected) },
                                label = { Text(stringResource(entry.label)) },
                            )
                        }
                    }
                }
            }
        }
    }
}
