package io.github.dimitrysaf.provenio.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner
import io.github.dimitrysaf.provenio.shell.components.LocalScreenActive
import io.github.dimitrysaf.provenio.shell.components.M3Motion

@Composable
internal fun RootTabHost(
    selectedTab: AppScreenTab,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    profileId: Int? = null,
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
        // Tabs switch with the fade through pattern: the old one fades out, then the new one fades and scales in.
        val firstTab = remember { selectedTab }
        val visibilities = remember { HashMap<AppScreenTab, Animatable<Float, AnimationVector1D>>() }

        Layout(
            modifier = modifier.fillMaxSize(),
            content = {
                displayedTabs.forEach { tab ->
                    key(tab) {
                        val selected = tab == selectedTab
                        val visibility = remember { Animatable(if (tab == firstTab) 1f else 0f) }
                        visibilities[tab] = visibility
                        LaunchedEffect(selected) {
                            if (selected) {
                                visibility.animateTo(1f, M3Motion.fadeThroughInSpec())
                            } else {
                                visibility.animateTo(0f, M3Motion.fadeThroughOutSpec())
                            }
                        }
                        RootTabPane(
                            tab = tab,
                            active = hostActive && selected,
                            selected = selected,
                            visibility = { visibility.value },
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
    selected: Boolean,
    visibility: () -> Float,
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
                        val progress = visibility()
                        alpha = progress
                        if (selected) {
                            val scale = 0.92f + 0.08f * progress
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                    .focusProperties { canFocus = active }
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
