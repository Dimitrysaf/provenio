package io.github.dimitrysaf.provenio.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner
import io.github.dimitrysaf.provenio.shell.components.LocalScreenActive
import io.github.dimitrysaf.provenio.shell.components.M3Motion
import kotlinx.coroutines.launch

@Composable
internal fun RootTabHost(
    selectedTab: AppScreenTab,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    profileId: Int? = null,
    /** The tabs are switched from a rail, so they move up and down rather than sideways. */
    vertical: Boolean = false,
    content: @Composable (AppScreenTab) -> Unit,
) {
    val parentLifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val hostActive = active && parentLifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val focusManager = LocalFocusManager.current
    DisposableEffect(selectedTab, hostActive, profileId) {
        onDispose { focusManager.clearFocus(force = true) }
    }

    key(profileId) {
        val tabStateHolder = rememberSaveableStateHolder()
        val visitedTabs = remember { mutableSetOf<AppScreenTab>() }
        val displayedTabs = remember(selectedTab) { (visitedTabs + selectedTab).toList() }
        SideEffect { visitedTabs += selectedTab }
        // Tabs switch with the shared axis pattern, along the bar that switches them: the old one
        // slides away and fades out, and the new one slides in from the side of the tab chosen.
        val firstTab = remember { selectedTab }
        val lastTab = remember { arrayOf(selectedTab) }
        val direction = remember(selectedTab) {
            (if (selectedTab.ordinal >= lastTab[0].ordinal) 1 else -1).also { lastTab[0] = selectedTab }
        }
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val slidePx = with(LocalDensity.current) { M3Motion.SharedAxisSlide.toPx() } * if (rtl && !vertical) -1 else 1
        val visibilities = remember { HashMap<AppScreenTab, Animatable<Float, AnimationVector1D>>() }

        Layout(
            modifier = modifier.fillMaxSize(),
            content = {
                displayedTabs.forEach { tab ->
                    key(tab) {
                        val selected = tab == selectedTab
                        val visibility = remember { Animatable(if (tab == firstTab) 1f else 0f) }
                        // How far the pane is from its place: from all the way, coming in, or to it, going.
                        val travel = remember { Animatable(0f) }
                        visibilities[tab] = visibility
                        LaunchedEffect(selected) {
                            if (selected) {
                                if (visibility.value < 1f) {
                                    travel.snapTo(1f)
                                    launch { travel.animateTo(0f, M3Motion.sharedAxisSlideSpec()) }
                                }
                                visibility.animateTo(1f, M3Motion.sharedAxisFadeInSpec())
                            } else {
                                launch { travel.animateTo(1f, M3Motion.sharedAxisSlideSpec()) }
                                visibility.animateTo(0f, M3Motion.sharedAxisFadeOutSpec())
                            }
                        }
                        RootTabPane(
                            tab = tab,
                            active = hostActive && selected,
                            visibility = { visibility.value },
                            offset = { (if (selected) direction else -direction) * slidePx * travel.value },
                            vertical = vertical,
                            stateHolder = tabStateHolder,
                            content = content,
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            val shown = displayedTabs.indices.filter { index ->
                val tab = displayedTabs[index]
                tab != selectedTab && (visibilities[tab]?.value ?: 0f) > 0f
            } + displayedTabs.indexOf(selectedTab)
            val placeables = shown.map { measurables[it].measure(constraints) }
            val selectedPlaceable = placeables.last()
            layout(selectedPlaceable.width, selectedPlaceable.height) {
                placeables.forEach { it.placeRelative(0, 0) }
            }
        }
    }
}

@Composable
private fun RootTabPane(
    tab: AppScreenTab,
    active: Boolean,
    visibility: () -> Float,
    offset: () -> Float,
    vertical: Boolean,
    stateHolder: SaveableStateHolder,
    content: @Composable (AppScreenTab) -> Unit,
) {
    val lifecycleOwner = rememberLifecycleOwner(
        maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.CREATED,
    )
    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycleOwner,
        LocalScreenActive provides active,
    ) {
        stateHolder.SaveableStateProvider(tab.name) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        alpha = visibility()
                        if (vertical) translationY = offset() else translationX = offset()
                    }
                    // A pane fading out, or kept for later, is no place for focus to go: a remote
                    // or keyboard moving into it would land on a control nobody can see. As a
                    // group, the pane turns away every move into it, wherever its controls sit.
                    .focusProperties {
                        onEnter = { if (!active) cancelFocusChange() }
                    }
                    .focusGroup()
                    .pointerInput(active) {
                        if (!active) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .then(if (active) Modifier else Modifier.clearAndSetSemantics {}),
            ) {
                content(tab)
            }
        }
    }
}
