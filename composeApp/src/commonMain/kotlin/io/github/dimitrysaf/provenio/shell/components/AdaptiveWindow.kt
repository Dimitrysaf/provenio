package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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

// Wraps the app: publishes the breakpoint and watches presses without consuming them.
@Composable
fun AdaptiveWindowRoot(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes
                            .firstOrNull { it.pressed && !it.previousPressed }
                            ?.let { LastPressPosition.record(it.position) }
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalWindowBreakpoint provides WindowBreakpoint.forWidth(maxWidth)) {
            content()
        }
    }
}
