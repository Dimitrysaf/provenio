package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * One press of an arrow or OK is one move. Holding the key does not repeat it, as a remote's
 * keys are easy to hold a moment too long; a quick tap and then a hold does, for going a long
 * way on purpose.
 */
internal object KeyPresses {
    private class Press {
        var down = false
        var lastEventAt = TimeSource.Monotonic.markNow()
        var lastUpAt: TimeSource.Monotonic.ValueTimeMark? = null
        var repeats = false
    }

    private val presses = mutableMapOf<Key, Press>()

    // A window and a sheet in it each see the same event; it is judged once. Events can be
    // reused for later ones, so only one seen just now is the same.
    private var lastEvent: NativeKeyEvent? = null
    private var lastEventAt = TimeSource.Monotonic.markNow()
    private var lastDropped = false

    /** Whether [event] is a held key's repeat, which is not passed on. */
    fun dropsRepeat(event: KeyEvent): Boolean {
        if (event.key !in RepeatlessKeys) return false
        if (event.nativeKeyEvent === lastEvent && lastEventAt.elapsedNow() < SameEventGap) return lastDropped
        lastEvent = event.nativeKeyEvent
        lastEventAt = TimeSource.Monotonic.markNow()
        lastDropped = judge(event)
        return lastDropped
    }

    private fun judge(event: KeyEvent): Boolean {
        val press = presses.getOrPut(event.key) { Press() }
        when (event.type) {
            KeyEventType.KeyDown -> {
                val sinceUp = press.lastUpAt?.elapsedNow()
                // Some systems repeat a held key as a release and a press at once.
                val isRepeat = (press.down && press.lastEventAt.elapsedNow() < HeldGap) ||
                    (sinceUp != null && sinceUp < ReleasePressGap)
                press.down = true
                press.lastEventAt = TimeSource.Monotonic.markNow()
                if (isRepeat) return !press.repeats
                press.repeats = sinceUp != null && sinceUp < TapThenHoldWindow
                return false
            }
            KeyEventType.KeyUp -> {
                press.down = false
                press.lastUpAt = TimeSource.Monotonic.markNow()
                press.lastEventAt = TimeSource.Monotonic.markNow()
            }
        }
        return false
    }
}

/** Drops a held key's repeats for everything inside, see [KeyPresses]. */
internal fun Modifier.singleKeyPresses(): Modifier = onPreviewKeyEvent(KeyPresses::dropsRepeat)

private val RepeatlessKeys = setOf(
    Key.DirectionUp,
    Key.DirectionDown,
    Key.DirectionLeft,
    Key.DirectionRight,
    Key.DirectionCenter,
    Key.Enter,
    Key.NumPadEnter,
    Key.Tab,
)

/** Longer than a held key's repeats are apart, so a release that was missed is not held forever. */
private val HeldGap = 600.milliseconds

private val SameEventGap = 5.milliseconds

/** Shorter than any two presses a hand makes. */
private val ReleasePressGap = 20.milliseconds

/** How soon after a tap a press has to come to repeat when held. */
private val TapThenHoldWindow = 400.milliseconds
