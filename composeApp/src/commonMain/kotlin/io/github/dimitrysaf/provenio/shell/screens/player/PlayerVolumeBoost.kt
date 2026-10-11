package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max

/** How loud the boost can make the player: twice its full volume. */
internal const val VolumeBoostMaxGain = 2f

/** The boost range moves at half the speed of the normal one, so a swipe climbs into it slowly. */
private const val BoostRangeSensitivity = 0.5f

private const val FullVolume = 1f
private const val FullVolumeTolerance = 0.005f

/**
 * The player's volume boost: amplification above the device's full volume, up to
 * [VolumeBoostMaxGain]. Volume is one level from 0 to 2, where 0 to 1 is the device volume and
 * 1 to 2 is the boost on top of it.
 *
 * Guardrails keep a slipped finger or a held key from making it loud:
 * - It is off whenever the player opens and only the switch in the audio sheet turns it on.
 * - A swipe or key press that starts below full volume stops at full volume. Going past it takes
 *   a new swipe, or a new key press, started at full volume.
 * - Inside the boost range a swipe moves at half speed.
 * - Lowering the device volume, by any means, drops the boost first.
 * - Turning it off, or leaving the player, puts the gain back to 1.
 */
@Stable
internal class PlayerVolumeBoost {
    var enabled by mutableStateOf(false)
        private set

    var gain by mutableFloatStateOf(FullVolume)
        private set

    fun switchOn(value: Boolean) {
        enabled = value
        if (!value) gain = FullVolume
    }

    /** Drops the boost back to 1, keeping the switch as it is. */
    fun dropGain() {
        gain = FullVolume
    }

    /**
     * The level now, from 0 to 2: [deviceFraction] is the device volume, or null where the
     * player cannot read it, which counts as full.
     */
    fun level(deviceFraction: Float?): Float = (deviceFraction ?: FullVolume) + (gain - FullVolume)

    /**
     * Moves the level by [delta] from [startLevel], where the swipe or key press began, and
     * applies it: below full volume to the device through [controller], above it to the player's
     * gain. Returns the level to show, or null when nothing could change.
     */
    fun moveBy(
        startLevel: Float,
        delta: Float,
        controller: PlayerGestureController?,
    ): PlayerAudioLevel? {
        val startedAtFull = startLevel >= FullVolume - FullVolumeTolerance
        val ceiling = if (enabled && startedAtFull) VolumeBoostMaxGain else FullVolume
        val floor = if (controller == null) FullVolume else 0f
        if (controller == null && !enabled) return null
        val raw = startLevel + delta
        val boostStart = max(startLevel, FullVolume)
        val target = if (raw > boostStart) boostStart + (raw - boostStart) * BoostRangeSensitivity else raw
        val clamped = target.coerceIn(floor, ceiling)
        if (clamped > FullVolume) {
            controller?.setVolume(FullVolume)
            gain = clamped
            return PlayerAudioLevel(fraction = clamped, isMuted = false)
        }
        gain = FullVolume
        return controller?.setVolume(clamped) ?: PlayerAudioLevel(fraction = FullVolume, isMuted = false)
    }
}
