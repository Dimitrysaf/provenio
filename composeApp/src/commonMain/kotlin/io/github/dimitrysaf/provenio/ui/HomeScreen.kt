package io.github.dimitrysaf.provenio.ui

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.navigation.Destination
import io.github.dimitrysaf.provenio.navigation.SearchFilter
import io.github.dimitrysaf.provenio.pages.ListsPage
import io.github.dimitrysaf.provenio.pages.MoviesPage
import io.github.dimitrysaf.provenio.pages.ProfilePage
import io.github.dimitrysaf.provenio.pages.TvPage
import io.github.dimitrysaf.provenio.theme.MotionTokens
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

/**
 * The app's only top-level destination: navigation plus the tab set.
 *
 * Laid out with plain weights and no animated insets. A sub-page covers this screen
 * rather than asking the navigation to move aside, so nothing here ever re-measures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenSearch: (SearchFilter) -> Unit,
    onOpenSettings: () -> Unit,
    onAddAddons: () -> Unit,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    // Saved rather than remembered: navigating to a sub-page disposes this screen's
    // composition, and the selected tab has to survive coming back.
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = Destination.entries[selectedIndex]

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
                            onClick = { selectedIndex = index },
                            icon = { DestinationIcon(entry, selected) },
                            label = { Text(entry.label) },
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
                        Destination.Profile -> ProfilePage(
                            modifier = Modifier.fillMaxSize(),
                            onSearchClick = { onOpenSearch(SearchFilter.All) },
                            onSettingsClick = onOpenSettings,
                            onAddAddons = onAddAddons,
                        )
                        Destination.Tv -> TvPage(
                            modifier = Modifier.fillMaxSize(),
                            onSearchClick = { onOpenSearch(SearchFilter.Tv) },
                            onFilterClick = {},
                            onOpenDetail = onOpenDetail,
                            onAddAddons = onAddAddons,
                        )
                        Destination.Movies -> MoviesPage(
                            modifier = Modifier.fillMaxSize(),
                            onSearchClick = { onOpenSearch(SearchFilter.Movies) },
                            onFilterClick = {},
                            onOpenDetail = onOpenDetail,
                            onAddAddons = onAddAddons,
                        )
                        Destination.Lists -> ListsPage(
                            modifier = Modifier.fillMaxSize(),
                            onSearchClick = { onOpenSearch(SearchFilter.All) },
                            onFilterClick = {},
                        )
                    }
                }

                if (!showRail) {
                    ShortNavigationBar {
                        Destination.entries.forEachIndexed { index, entry ->
                            val selected = index == selectedIndex
                            ShortNavigationBarItem(
                                selected = selected,
                                onClick = { selectedIndex = index },
                                icon = { DestinationIcon(entry, selected) },
                                label = { Text(entry.label) },
                            )
                        }
                    }
                }
            }
        }
    }
}
