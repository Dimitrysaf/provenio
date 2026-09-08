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
import androidx.compose.runtime.mutableFloatStateOf
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
import io.github.dimitrysaf.provenio.pages.AppearancePage
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
private const val BackAlphaAtFullProgress = 0.9f
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
        var overlayStack by remember { mutableStateOf<List<Overlay>>(emptyList()) }

        val scope = rememberCoroutineScope()
        val backProgress = remember { Animatable(0f) }
        var backFromStart by remember { mutableStateOf(true) }
        // Progress frozen at the moment a gesture committed, so the page that is leaving
        // keeps the position the drag gave it instead of snapping back to full size first.
        var exitProgress by remember { mutableFloatStateOf(0f) }

        fun push(target: Overlay) {
            scope.launch { backProgress.snapTo(0f) }
            exitProgress = 0f
            overlayStack = overlayStack + target
        }

        fun pop() {
            exitProgress = backProgress.value
            scope.launch { backProgress.snapTo(0f) }
            overlayStack = overlayStack.dropLast(1)
        }

        SystemBackHandler(
            enabled = overlayStack.isNotEmpty(),
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
            onBack = { pop() },
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
                                onSearchClick = { push(Overlay.Search) },
                                onSettingsClick = { push(Overlay.Settings) },
                            )
                            Destination.Tv -> TvPage(
                                modifier = Modifier.fillMaxSize(),
                                onSearchClick = { push(Overlay.Search) },
                                onFilterClick = {},
                            )
                            Destination.Movies -> MoviesPage(
                                modifier = Modifier.fillMaxSize(),
                                onSearchClick = { push(Overlay.Search) },
                                onFilterClick = {},
                            )
                            Destination.Lists -> ListsPage(
                                modifier = Modifier.fillMaxSize(),
                                onSearchClick = { push(Overlay.Search) },
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

            // ── Layer 2: the overlay stack, over the navigation ─────────────────────
            // The page one level down is composed underneath the top one so that dragging
            // back reveals the page you are actually returning to, not the tabs behind it.
            val beneath = overlayStack.getOrNull(overlayStack.lastIndex - 1)
            if (beneath != null) {
                OverlaySurface(modifier = Modifier.fillMaxSize()) {
                    OverlayPage(
                        overlay = beneath,
                        onBack = { pop() },
                        onPush = { push(it) },
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it },
                        useDynamicColor = useDynamicColor,
                        onUseDynamicColorChange = { useDynamicColor = it },
                    )
                }
            }

            AnimatedContent(
                targetState = overlayStack,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    // Pushing slides the new page in from the end edge; popping sends the
                    // leaving page back out that way and brings the previous one in from
                    // the start, which is the direction the back gesture already implies.
                    val forward = targetState.size >= initialState.size
                    val enter = slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = MotionTokens.DurationMedium4,
                            easing = MotionTokens.EmphasizedDecelerate,
                        ),
                        initialOffsetX = { width -> if (forward) width else -width },
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
                        targetOffsetX = { width -> if (forward) -width else width },
                    ) + fadeOut(
                        tween(
                            durationMillis = MotionTokens.DurationShort4,
                            easing = MotionTokens.Standard,
                        ),
                    )
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "overlay",
            ) { stack ->
                val current = stack.lastOrNull()
                if (current == null) {
                    // Nothing stacked; layer 1 shows through untouched.
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    // The page still on top follows the live gesture; one already on its
                    // way out holds the progress it was released at.
                    val isTop = stack == overlayStack
                    OverlaySurface(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val progress =
                                    if (isTop) backProgress.value else exitProgress
                                val scale = 1f + (BackScaleAtFullProgress - 1f) * progress
                                scaleX = scale
                                scaleY = scale
                                translationX = (if (backFromStart) 1f else -1f) *
                                    progress * size.width * BackSlideFractionOfWidth
                                alpha = 1f + (BackAlphaAtFullProgress - 1f) * progress
                                shape = RoundedCornerShape(
                                    lerp(0.dp, BackCornerRadiusAtFullProgress, progress),
                                )
                                clip = progress > 0f
                            },
                    ) {
                        OverlayPage(
                            overlay = current,
                            onBack = { pop() },
                            onPush = { push(it) },
                            themeMode = themeMode,
                            onThemeModeChange = { themeMode = it },
                            useDynamicColor = useDynamicColor,
                            onUseDynamicColorChange = { useDynamicColor = it },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Opaque backing for a stacked page. Material's Surface also blocks pointer events, so an
 * overlay swallows touches meant for the navigation it covers.
 */
@Composable
private fun OverlaySurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        color = MaterialTheme.colorScheme.background,
        content = content,
    )
}

@Composable
private fun OverlayPage(
    overlay: Overlay,
    onBack: () -> Unit,
    onPush: (Overlay) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
) {
    when (overlay) {
        Overlay.Settings -> SettingsPage(
            modifier = Modifier.fillMaxSize(),
            onBack = onBack,
            onOpenAppearance = { onPush(Overlay.Appearance) },
        )
        Overlay.Appearance -> AppearancePage(
            modifier = Modifier.fillMaxSize(),
            onBack = onBack,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            useDynamicColor = useDynamicColor,
            onUseDynamicColorChange = onUseDynamicColorChange,
            dynamicColorAvailable = isDynamicColorSupported(),
        )
        Overlay.Search -> SearchPage(
            modifier = Modifier.fillMaxSize(),
            onBack = onBack,
        )
    }
}
