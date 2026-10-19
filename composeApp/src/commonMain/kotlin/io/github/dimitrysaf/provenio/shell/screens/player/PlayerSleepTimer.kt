package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/** When the sleep timer pauses playback: after a set time, at the end of what is playing, or never. */
internal enum class PlayerSleepTimerChoice(val minutes: Int?) {
    Off(null),
    Minutes15(15),
    Minutes30(30),
    Minutes45(45),
    Minutes60(60),
    Minutes90(90),
    EndOfVideo(null),
}

/** How long the sound fades out before the sleep timer pauses. */
internal const val SleepTimerFadeMs = 15_000L

/**
 * The player's sleep timer. A timed choice counts down on the clock, whether or not anything is
 * playing, and the player fades the sound out over its last [SleepTimerFadeMs] before pausing.
 * [PlayerSleepTimerChoice.EndOfVideo] instead lets what is playing finish and keeps the next
 * episode from starting. Either way the timer turns itself off once it has paused playback.
 */
@Stable
internal class PlayerSleepTimer {
    var choice by mutableStateOf(PlayerSleepTimerChoice.Off)
        private set

    /** When the timer runs out, for a timed choice; null otherwise. */
    var deadline by mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null)
        private set

    /** What is left of a timed choice, updated by the player while it counts down. */
    var remainingMs by mutableLongStateOf(0L)
        private set

    val stopsAtEndOfVideo: Boolean
        get() = choice == PlayerSleepTimerChoice.EndOfVideo

    fun set(value: PlayerSleepTimerChoice) {
        choice = value
        val minutes = value.minutes
        if (minutes == null) {
            deadline = null
            remainingMs = 0L
        } else {
            deadline = TimeSource.Monotonic.markNow() + minutes.minutes
            remainingMs = minutes.minutes.inWholeMilliseconds
        }
    }

    /** Updates [remainingMs] from the clock and returns it. */
    fun tick(): Long {
        val mark = deadline ?: return 0L
        remainingMs = (-mark.elapsedNow().inWholeMilliseconds).coerceAtLeast(0L)
        return remainingMs
    }

    fun clear() = set(PlayerSleepTimerChoice.Off)
}
