package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// The window's breakpoint for the whole app, so a sheet or a menu can adapt without measuring itself.
val LocalWindowBreakpoint = compositionLocalOf { WindowBreakpoint.Compact }

// Where the last press landed in the window, so a long-press menu can open right under it.
internal object LastPressPosition {
    private var position: Offset? = null
    private var pressedAt: TimeMark? = null

    fun record(offset: Offset) {
        position = offset
        pressedAt = TimeSource.Monotonic.markNow()
    }

    // Only a press from the last moment counts, so an old one never places a menu somewhere unrelated.
    fun recent(): Offset? = position?.takeIf { pressedAt?.elapsedNow()?.inWholeMilliseconds?.let { it < RECENT_PRESS_MS } == true }

    private const val RECENT_PRESS_MS = 3_000L
}

internal object ActiveWindow {
    private val _token = MutableStateFlow<Any?>(null)
    val token: StateFlow<Any?> = _token.asStateFlow()

    fun mark(token: Any) {
        _token.value = token
    }

    fun release(token: Any) {
        _token.compareAndSet(token, null)
    }
}

internal val LocalWindowToken = staticCompositionLocalOf<Any?> { null }

// Wraps the app: publishes the breakpoint and watches presses without consuming them.
@Composable
fun AdaptiveWindowRoot(content: @Composable () -> Unit) {
    val windowToken = remember { Any() }
    DisposableEffect(windowToken) {
        ActiveWindow.mark(windowToken)
        onDispose { ActiveWindow.release(windowToken) }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .recordPressPositions(windowToken),
    ) {
        CompositionLocalProvider(
            LocalWindowBreakpoint provides WindowBreakpoint.forWidth(maxWidth),
            LocalWindowToken provides windowToken,
        ) {
            content()
        }
    }
}

internal fun Modifier.recordPressPositions(windowToken: Any?): Modifier = composed {
    val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
    onGloballyPositioned { coordinates[0] = it }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.pressed && !it.previousPressed } ?: continue
                    windowToken?.let(ActiveWindow::mark)
                    val layout = coordinates[0]?.takeIf { it.isAttached }
                    LastPressPosition.record(layout?.localToWindow(change.position) ?: change.position)
                }
            }
        }
}
