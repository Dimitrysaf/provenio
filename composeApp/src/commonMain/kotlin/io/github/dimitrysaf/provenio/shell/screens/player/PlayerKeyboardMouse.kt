package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

private const val KeyboardSeekMs = 10_000L
private const val KeyboardVolumeStep = 0.05f

// Keyboard shortcuts act silently, and moving the mouse brings up the controls and hides the cursor again with them.
@Composable
internal fun PlayerScreenRuntime.playerKeyboardAndMouse(): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    return Modifier
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            event.type == KeyEventType.KeyDown && handlePlayerKey(event.key)
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val mouseMoved = event.type == PointerEventType.Move && event.changes.any { it.type == PointerType.Mouse }
                    if (mouseMoved && !playerControlsLocked) {
                        controlsVisible = true
                        controlsActivity++
                    }
                }
            }
        }
        .playerCursorHidden(!controlsVisible)
}

private fun PlayerScreenRuntime.handlePlayerKey(key: Key): Boolean {
    if (playerControlsLocked) return false
    when (key) {
        Key.Spacebar, Key.K -> togglePlaybackQuietly()
        Key.DirectionLeft -> seekByQuietly(-KeyboardSeekMs)
        Key.DirectionRight -> seekByQuietly(KeyboardSeekMs)
        Key.DirectionUp -> changeVolumeQuietly(KeyboardVolumeStep)
        Key.DirectionDown -> changeVolumeQuietly(-KeyboardVolumeStep)
        Key.F -> return togglePlayerFullscreen()
        else -> return false
    }
    return true
}

private fun PlayerScreenRuntime.changeVolumeQuietly(delta: Float) {
    val controller = gestureController ?: return
    val current = controller.currentVolume()?.fraction ?: return
    controller.setVolume((current + delta).coerceIn(0f, 1f))
}

// Switches the window between full screen and windowed where the platform has one; false where it does not.
internal expect fun togglePlayerFullscreen(): Boolean

internal expect fun Modifier.playerCursorHidden(hidden: Boolean): Modifier
