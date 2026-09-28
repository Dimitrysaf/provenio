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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
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

// Keyboard shortcuts press the same buttons the controls have, and moving the mouse brings up the controls and hides the cursor again with them.
@Composable
internal fun PlayerScreenRuntime.playerKeyboardAndMouse(): Modifier {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val spaceHold = remember { SpaceHoldState() }
    val mute = remember { MuteState() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    return Modifier
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.key == Key.Spacebar) {
                handleSpaceKey(event.type, scope, spaceHold)
            } else {
                event.type == KeyEventType.KeyDown && handlePlayerKey(event, mute)
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

private class MuteState {
    var mutedController: PlayerEngineController? = null
}

private fun PlayerScreenRuntime.handlePlayerKey(event: KeyEvent, mute: MuteState): Boolean {
    if (playerControlsLocked) return false
    if (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return false
    val digit = event.key.digitOrNull()
    when {
        digit != null -> seekToFraction(digit / 10f)
        event.key == Key.K -> togglePlayback()
        event.key == Key.J -> seekBy(-KeyboardSeekMs)
        event.key == Key.L -> seekBy(KeyboardSeekMs)
        event.key == Key.DirectionLeft -> seekBy(-KeyboardSeekMs)
        event.key == Key.DirectionRight -> seekBy(KeyboardSeekMs)
        event.key == Key.DirectionUp -> changeVolumeBy(KeyboardVolumeStep)
        event.key == Key.DirectionDown -> changeVolumeBy(-KeyboardVolumeStep)
        event.key == Key.MoveHome -> seekToFraction(0f)
        event.key == Key.M -> toggleMute(mute)
        event.key == Key.C -> {
            refreshTracks()
            showSubtitleModal = true
        }
        event.key == Key.A -> {
            refreshTracks()
            showAudioModal = true
        }
        event.key == Key.S -> showSpeedSheet = true
        event.key == Key.N -> {
            if (nextEpisodeInfo?.hasAired != true) return false
            nextEpisodeAutoPlayJob?.cancel()
            playNextEpisode()
        }
        event.key == Key.Escape -> {
            flushWatchProgress()
            args.onBack()
            return true
        }
        event.key == Key.F -> return togglePlayerFullscreen()
        else -> return false
    }
    controlsActivity++
    return true
}

private fun Key.digitOrNull(): Int? = when (this) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

private fun PlayerScreenRuntime.seekToFraction(fraction: Float) {
    val durationMs = playbackSnapshot.durationMs.takeIf { it > 0L } ?: return
    playerController?.seekTo((durationMs * fraction).toLong())
    scheduleProgressSyncAfterSeek()
    controlsVisible = true
}

private fun PlayerScreenRuntime.toggleMute(mute: MuteState) {
    val controller = playerController ?: return
    val muted = mute.mutedController !== controller
    controller.setMuted(muted)
    mute.mutedController = if (muted) controller else null
    val fraction = gestureController?.currentVolume()?.fraction ?: 1f
    showVolumeFeedback(PlayerAudioLevel(fraction = if (muted) 0f else fraction, isMuted = muted))
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
                togglePlayback()
                controlsActivity++
            }
        }
    }
    return true
}

private fun PlayerScreenRuntime.changeVolumeBy(delta: Float) {
    val controller = gestureController ?: return
    val current = controller.currentVolume()?.fraction ?: return
    controller.setVolume((current + delta).coerceIn(0f, 1f))?.let { showVolumeFeedback(it) }
}

// Switches the window between full screen and windowed where the platform has one; false where it does not.
internal expect fun togglePlayerFullscreen(): Boolean

internal expect fun Modifier.playerCursorHidden(hidden: Boolean): Modifier
