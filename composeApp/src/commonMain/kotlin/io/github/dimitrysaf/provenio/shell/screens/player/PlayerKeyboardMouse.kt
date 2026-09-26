package io.github.dimitrysaf.provenio.shell.screens.player

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope
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
private const val SpaceHoldDelayMs = 400L

// Space tapped toggles playback; held, it speeds playback up like holding the screen does.
private class SpaceHoldState {
    var pressed = false
    var boosted = false
    var job: Job? = null
}

// Keyboard shortcuts act silently, and moving the mouse brings up the controls and hides the cursor again with them.
@Composable
internal fun PlayerScreenRuntime.playerKeyboardAndMouse(): Modifier {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val spaceHold = remember { SpaceHoldState() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    return Modifier
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.key == Key.Spacebar) {
                handleSpaceKey(event.type, scope, spaceHold)
            } else {
                event.type == KeyEventType.KeyDown && handlePlayerKey(event.key)
            }
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
        Key.K -> togglePlaybackQuietly()
        Key.DirectionLeft -> seekByQuietly(-KeyboardSeekMs)
        Key.DirectionRight -> seekByQuietly(KeyboardSeekMs)
        Key.DirectionUp -> changeVolumeQuietly(KeyboardVolumeStep)
        Key.DirectionDown -> changeVolumeQuietly(-KeyboardVolumeStep)
        Key.F -> return togglePlayerFullscreen()
        else -> return false
    }
    return true
}

private fun PlayerScreenRuntime.handleSpaceKey(type: KeyEventType, scope: CoroutineScope, hold: SpaceHoldState): Boolean {
    if (playerControlsLocked) return false
    when (type) {
        KeyEventType.KeyDown -> {
            // Key repeats while held arrive as more presses; only the first one counts.
            if (hold.pressed) return true
            hold.pressed = true
            hold.job = scope.launch {
                delay(SpaceHoldDelayMs)
                if (playbackSnapshot.isPlaying) {
                    activateHoldToSpeed()
                    hold.boosted = true
                }
            }
        }
        KeyEventType.KeyUp -> {
            hold.pressed = false
            hold.job?.cancel()
            hold.job = null
            if (hold.boosted) {
                hold.boosted = false
                deactivateHoldToSpeed()
            } else {
                togglePlaybackQuietly()
            }
        }
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
