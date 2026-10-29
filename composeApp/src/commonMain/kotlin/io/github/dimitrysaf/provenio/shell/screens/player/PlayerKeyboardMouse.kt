package io.github.dimitrysaf.provenio.shell.screens.player

import io.github.dimitrysaf.provenio.shell.components.OpenSheets
import io.github.dimitrysaf.provenio.shell.components.openedWithKeys
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.core.build.isDesktop
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.platform.LocalWindowInfo

private const val KeyboardVolumeStep = 0.05f
private val KeyRepeatGap = 150.milliseconds

/** How soon a second press of left or right has to follow the first to seek. */
private val ArrowSeekPressGap = 300.milliseconds

private const val SpaceHoldDelayMs = 400L
private const val MouseMoveThresholdPx = 1f

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
    val arrowSeek = remember { ArrowSeekState() }
    val focusState = remember { PlayerFocusState() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val currentWindowFocused by rememberUpdatedState(windowFocused)
    LaunchedEffect(windowFocused) { focusRequester.claimFocus(focusState) }
    // With a remote, Back puts the controls away before it leaves the player, as on a TV.
    PlatformBackHandler(enabled = !isDesktop && controlsVisible && !playerControlsLocked && openedWithKeys()) {
        controlsVisible = false
    }
    // A focused control is gone once the controls hide, which leaves focus nowhere and the keys
    // with nothing to reach; the player takes it back then.
    LaunchedEffect(focusRequester) {
        snapshotFlow { focusState.hasFocusWithin to OpenSheets.count }.collect { (hasFocus, sheets) ->
            if (!hasFocus && sheets == 0 && currentWindowFocused) focusRequester.claimFocus(focusState)
        }
    }
    return Modifier
        .onFocusChanged {
            focusState.hasFocusWithin = it.hasFocus
            focusState.controlFocused = it.hasFocus && !it.isFocused
        }
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.key == Key.DirectionUp && event.type == KeyEventType.KeyUp) {
                mute.volumeUpHeld = false
                mute.volumeUpReleasedAt = TimeSource.Monotonic.markNow()
            }
            // A sheet over the player has the keys, even one in the player's own window, and so
            // does the watch party's chat while it is open.
            if (OpenSheets.count > 0 || showPartyChat) {
                false
            } else if (event.key == Key.Spacebar) {
                handleSpaceKey(event.type, scope, spaceHold)
            } else {
                event.type == KeyEventType.KeyDown && handlePlayerKey(event, mute, arrowSeek, focusState.controlFocused)
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                var lastMousePosition: Offset? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Enter) focusRequester.claimFocus(focusState)
                    if (event.type != PointerEventType.Move) continue
                    val change = event.changes.firstOrNull { it.type == PointerType.Mouse } ?: continue
                    focusRequester.claimFocus(focusState)
                    val previous = lastMousePosition
                    lastMousePosition = change.position
                    val mouseMoved = previous == null || (change.position - previous).getDistance() >= MouseMoveThresholdPx
                    if (!mouseMoved) continue
                    if (playerControlsLocked) {
                        revealLockedOverlay()
                    } else {
                        controlsVisible = true
                    }
                    controlsActivity++
                }
            }
        }
        .playerCursorHidden(!controlsVisible && !(playerControlsLocked && lockedOverlayVisible))
}

private class PlayerFocusState {
    var hasFocusWithin by mutableStateOf(false)

    /** A control has focus, rather than the player itself: a remote's arrows move between them. */
    var controlFocused = false
}

private fun FocusRequester.claimFocus(state: PlayerFocusState) {
    if (!state.hasFocusWithin && OpenSheets.count == 0) runCatching { requestFocus() }
}

private class MuteState {
    var mutedController: PlayerEngineController? = null
    // Held keys repeat; a repeat may not raise the volume past full, only a fresh press can. Some
    // systems repeat a held key as quick release and press pairs, so a press right after a
    // release counts as a repeat too.
    var volumeUpHeld = false
    var volumeUpReleasedAt: TimeMark? = null
}

/** A remote's left or right last pressed, which a second press of soon after seeks with. */
private class ArrowSeekState {
    var key: Key? = null
    var pressedAt: TimeMark? = null
}

private fun PlayerScreenRuntime.handlePlayerKey(
    event: KeyEvent,
    mute: MuteState,
    arrowSeek: ArrowSeekState,
    controlFocused: Boolean,
): Boolean {
    if (playerControlsLocked) return false
    if (event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return false
    handleMediaKey(event.key)?.let { handled ->
        controlsActivity++
        return handled
    }
    if (!isDesktop) handleRemoteKey(event.key, arrowSeek, controlFocused)?.let { return it }
    val digit = event.key.digitOrNull()
    when {
        digit != null -> seekToFraction(digit / 10f)
        event.key == Key.K -> togglePlayback()
        event.key == Key.J -> seekBy(-seekStepMs(PlayerSeekDirection.Backward))
        event.key == Key.L -> seekBy(seekStepMs(PlayerSeekDirection.Forward))
        event.key == Key.DirectionLeft -> seekBy(-seekStepMs(PlayerSeekDirection.Backward))
        event.key == Key.DirectionRight -> seekBy(seekStepMs(PlayerSeekDirection.Forward))
        event.key == Key.DirectionUp -> {
            val fresh = !mute.volumeUpHeld &&
                (mute.volumeUpReleasedAt?.elapsedNow()?.let { it >= KeyRepeatGap } ?: true)
            mute.volumeUpHeld = true
            changeVolumeBy(KeyboardVolumeStep, mayRaiseBoost = fresh)
        }
        event.key == Key.DirectionDown -> changeVolumeBy(-KeyboardVolumeStep, mayRaiseBoost = false)
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

/** The media keys a remote or a keyboard has; null for any other key. */
private fun PlayerScreenRuntime.handleMediaKey(key: Key): Boolean? {
    when (key) {
        Key.MediaPlayPause -> togglePlayback()
        Key.MediaPlay -> if (!playbackSnapshot.isPlaying) togglePlayback()
        Key.MediaPause -> if (playbackSnapshot.isPlaying) togglePlayback()
        // A remote's own seek keys seek at one press, unlike its arrows.
        Key.MediaFastForward, Key.MediaSkipForward, Key.MediaStepForward ->
            seekBy(seekStepMs(PlayerSeekDirection.Forward))
        Key.MediaRewind, Key.MediaSkipBackward, Key.MediaStepBackward ->
            seekBy(-seekStepMs(PlayerSeekDirection.Backward))
        Key.Captions -> toggleCaptions()
        Key.MediaNext -> {
            if (nextEpisodeInfo?.hasAired != true) return false
            nextEpisodeAutoPlayJob?.cancel()
            playNextEpisode()
        }
        else -> return null
    }
    return true
}

/**
 * A remote's D-pad, where there is no keyboard to fall back on. While a control has focus the
 * arrows move between the controls and OK presses the one focused. With the controls hidden, or
 * none focused, left or right pressed twice seeks, and once only shows where playback is; up and
 * down bring the controls up with focus on play, and OK plays or pauses and shows them. A
 * keyboard on desktop keeps its arrows for seeking and volume.
 */
private fun PlayerScreenRuntime.handleRemoteKey(key: Key, arrowSeek: ArrowSeekState, controlFocused: Boolean): Boolean? {
    val navigatesControls = controlFocused && controlsVisible
    when (key) {
        Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown -> {
            controlsActivity++
            if (navigatesControls) return false
            when (key) {
                Key.DirectionLeft, Key.DirectionRight -> {
                    // A press soon after one of the same arrow seeks, and so does each that
                    // follows it as soon; a press on its own is easily made by mistake.
                    val seeks = arrowSeek.key == key &&
                        arrowSeek.pressedAt?.let { it.elapsedNow() < ArrowSeekPressGap } == true
                    arrowSeek.key = key
                    arrowSeek.pressedAt = TimeSource.Monotonic.markNow()
                    when {
                        !seeks -> controlsVisible = true
                        key == Key.DirectionLeft -> seekBy(-seekStepMs(PlayerSeekDirection.Backward))
                        else -> seekBy(seekStepMs(PlayerSeekDirection.Forward))
                    }
                }
                else -> showControlsForRemote()
            }
            return true
        }
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
            controlsActivity++
            if (navigatesControls) return false
            togglePlayback()
            showControlsForRemote()
            return true
        }
        else -> return null
    }
}

private fun PlayerScreenRuntime.showControlsForRemote() {
    controlsVisible = true
    primaryControlFocusPending = true
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
    val fraction = volumeBoost.level(gestureController?.currentVolume()?.fraction)
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

private fun PlayerScreenRuntime.changeVolumeBy(delta: Float, mayRaiseBoost: Boolean) {
    val controller = gestureController
    val device = controller?.currentVolume()?.fraction
    if (controller != null && device == null) return
    val level = volumeBoost.level(device)
    if (delta > 0f && !mayRaiseBoost && level >= 1f) {
        if (volumeBoost.enabled || controller != null) showVolumeFeedback(PlayerAudioLevel(fraction = level, isMuted = false))
        return
    }
    volumeBoost.moveBy(level, delta, controller)?.let { showVolumeFeedback(it) }
}

// Switches the window between full screen and windowed where the platform has one; false where it does not.
internal expect fun togglePlayerFullscreen(): Boolean

internal expect fun Modifier.playerCursorHidden(hidden: Boolean): Modifier
