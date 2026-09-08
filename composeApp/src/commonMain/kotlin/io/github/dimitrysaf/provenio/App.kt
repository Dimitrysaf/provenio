package io.github.dimitrysaf.provenio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import io.github.dimitrysaf.provenio.navigation.Destination
import io.github.dimitrysaf.provenio.navigation.Overlay
import io.github.dimitrysaf.provenio.pages.ListsPage
import io.github.dimitrysaf.provenio.pages.MoviesPage
import io.github.dimitrysaf.provenio.pages.ProfilePage
import io.github.dimitrysaf.provenio.pages.SearchPage
import io.github.dimitrysaf.provenio.pages.SettingsPage
import io.github.dimitrysaf.provenio.pages.TvPage
import io.github.dimitrysaf.provenio.theme.AppTheme
import io.github.dimitrysaf.provenio.theme.MotionTokens
import io.github.dimitrysaf.provenio.theme.ThemeMode
import io.github.dimitrysaf.provenio.theme.isDynamicColorSupported
import io.github.dimitrysaf.provenio.ui.backhandler.SystemBackHandler
import io.github.dimitrysaf.provenio.ui.responsive.usesExpandedRail
import io.github.dimitrysaf.provenio.ui.responsive.usesRail
import io.github.dimitrysaf.provenio.ui.responsive.windowSizeClassOf
import kotlinx.coroutines.launch

/** How far the leaving page shrinks and slides at full predictive-back progress. */
private const val BackScaleAtFullProgress = 0.9f
private const val BackSlideFractionOfWidth = 0.08f
private val BackCornerRadiusAtFullProgress = 28.dp

@Composable
private fun DestinationIcon(entry: Destination, selected: Boolean) {
    Icon(
        imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
        contentDescription = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemeMode.System) }
    var useDynamicColor by remember { mutableStateOf(true) }

    AppTheme(themeMode = themeMode, useDynamicColor = useDynamicColor) {
        var selectedTab by remember { mutableStateOf(Destination.Profile) }
        var overlay by remember { mutableStateOf<Overlay?>(null) }

        val scope = rememberCoroutineScope()
        val backProgress = remember { Animatable(0f) }
        var backFromStart by remember { mutableStateOf(true) }

        fun open(target: Overlay) {
            // Clear any progress left over from the previous dismissal before entering.
            scope.launch { backProgress.snapTo(0f) }
            overlay = target
        }

        SystemBackHandler(
            enabled = overlay != null,
            onProgress = { gesture ->
                backFromStart = gesture.fromStart
                scope.launch { backProgress.snapTo(gesture.progress) }
            },
            onCancel = {
                scope.launch {
                    backProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationShort4,
                            easing = MotionTokens.EmphasizedDecelerate,
                        ),
                    )
                }
            },
            // Progress deliberately stays where the drag left it, so the exit animation
            // continues from the previewed position instead of snapping back first.
            onBack = { overlay = null },
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val sizeClass = windowSizeClassOf(maxWidth)
            val showRail = sizeClass.usesRail()
            val expandRail = sizeClass.usesExpandedRail()

            // M3 puts a collapsed rail at medium/expanded and an expanded one once there
            // is Large-breakpoint width to spare.
            val railState = rememberWideNavigationRailState()
            LaunchedEffect(expandRail) {
                if (expandRail) railState.expand() else railState.collapse()
            }

            // ── Layer 1: tabs and navigation ────────────────────────────────────────
            // Laid out with plain weights and no animated insets. Nothing above it can
            // change its size, so its children never re-measure and never bounce.
            Row(modifier = Modifier.fillMaxSize()) {
                if (showRail) {
                    WideNavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        state = railState,
                    ) {
                        Destination.entries.forEach { entry ->
                            val selected = entry == selectedTab
                            WideNavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = entry },
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
                        // M3 fade-through between top-level destinations.
                        transitionSpec = {
                            val enterSpec = tween<Float>(
                                durationMillis = MotionTokens.DurationMedium1,
                                easing = MotionTokens.EmphasizedDecelerate,
                            )
                            (
                                fadeIn(enterSpec) +
                                    scaleIn(animationSpec = enterSpec, initialScale = 0.92f)
                                ).togetherWith(
                                fadeOut(
                                    tween(
                                        durationMillis = MotionTokens.DurationShort4,
                                        easing = MotionTokens.StandardAccelerate,
                                    ),
                                ),
                            )
                        },
                        label = "tab",
                    ) { tab ->
                        when (tab) {
                            Destination.Profile -> ProfilePage(
                                modifier = Modifier.fillMaxSize(),
                                onSearchClick = { open(Overlay.Search) },
                                onSettingsClick = { open(Overlay.Settings) },
                            )
                            Destination.Tv -> TvPage(
                                modifier = Modifier.fillMaxSize(),
                                onSearchClick = { open(Overlay.Search) },
                                onFilterClick = {},
                            )
                            Destination.Movies -> MoviesPage(
                                modifier = Modifier.fillMaxSize(),
                                onSearchClick = { open(Overlay.Search) },
                                onFilterClick = {},
                            )
                            Destination.Lists -> ListsPage(
                                modifier = Modifier.fillMaxSize(),
                                onSearchClick = { open(Overlay.Search) },
                                onFilterClick = {},
                            )
                        }
                    }

                    if (!showRail) {
                        ShortNavigationBar {
                            Destination.entries.forEach { entry ->
                                val selected = entry == selectedTab
                                ShortNavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = entry },
                                    icon = { DestinationIcon(entry, selected) },
                                    label = { Text(entry.label) },
                                )
                            }
                        }
                    }
                }
            }

            // ── Layer 2: sub-pages, stacked over the navigation ─────────────────────
            // An overlay covers the rail/bar rather than asking it to move aside, and
            // carries the predictive-back transform as the user drags.
            AnimatedContent(
                targetState = overlay,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val enter = slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationMedium4,
                            easing = MotionTokens.EmphasizedDecelerate,
                        ),
                        initialOffsetX = { it },
                    ) + fadeIn(
                        tween(
                            durationMillis = MotionTokens.DurationMedium2,
                            easing = MotionTokens.Standard,
                        ),
                    )
                    val exit = slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationMedium2,
                            easing = MotionTokens.EmphasizedAccelerate,
                        ),
                        targetOffsetX = { it },
                    ) + fadeOut(
                        tween(
                            durationMillis = MotionTokens.DurationShort4,
                            easing = MotionTokens.Standard,
                        ),
                    )
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "overlay",
            ) { current ->
                if (current == null) {
                    // Nothing stacked; layer 1 shows through untouched.
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val progress = backProgress.value
                                val scale = lerpFloat(1f, BackScaleAtFullProgress, progress)
                                scaleX = scale
                                scaleY = scale
                                translationX = (if (backFromStart) 1f else -1f) *
                                    progress * size.width * BackSlideFractionOfWidth
                                alpha = lerpFloat(1f, 0.9f, progress)
                                shape = RoundedCornerShape(
                                    lerp(0.dp, BackCornerRadiusAtFullProgress, progress),
                                )
                                clip = progress > 0f
                            }
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        when (current) {
                            Overlay.Settings -> SettingsPage(
                                modifier = Modifier.fillMaxSize(),
                                onBack = { overlay = null },
                                themeMode = themeMode,
                                onThemeModeChange = { themeMode = it },
                                useDynamicColor = useDynamicColor,
                                onUseDynamicColorChange = { useDynamicColor = it },
                                dynamicColorAvailable = isDynamicColorSupported(),
                            )
                            Overlay.Search -> SearchPage(
                                modifier = Modifier.fillMaxSize(),
                                onBack = { overlay = null },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
