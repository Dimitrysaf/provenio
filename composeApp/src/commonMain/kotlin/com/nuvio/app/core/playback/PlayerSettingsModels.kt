package com.nuvio.app.core.playback

import kotlin.math.abs

val STREAM_AUTO_PLAY_TIMEOUT_VALUES: List<Int> = listOf(
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, Int.MAX_VALUE
)

/**
 * Snaps [value] to the nearest allowed timeout value in [STREAM_AUTO_PLAY_TIMEOUT_VALUES].
 * Ties break to the lower value. Negative values snap to 0.
 */
fun snapToAllowedTimeout(value: Int): Int {
    if (value <= 0) return 0
    var bestValue = STREAM_AUTO_PLAY_TIMEOUT_VALUES[0]
    var bestDistance = Long.MAX_VALUE
    for (allowed in STREAM_AUTO_PLAY_TIMEOUT_VALUES) {
        val distance = abs(value.toLong() - allowed.toLong())
        if (distance < bestDistance || (distance == bestDistance && allowed < bestValue)) {
            bestDistance = distance
            bestValue = allowed
        }
    }
    return bestValue
}
