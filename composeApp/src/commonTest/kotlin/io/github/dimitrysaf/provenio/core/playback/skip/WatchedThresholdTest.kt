package io.github.dimitrysaf.provenio.core.playback.skip

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchedThresholdTest {
    private fun reached(
        positionMs: Long,
        durationMs: Long,
        mode: NextEpisodeThresholdMode = NextEpisodeThresholdMode.PERCENTAGE,
        skipIntervals: List<SkipInterval> = emptyList(),
    ) = PlayerNextEpisodeRules.isWatchedThresholdReached(
        positionMs = positionMs,
        durationMs = durationMs,
        skipIntervals = skipIntervals,
        thresholdMode = mode,
        thresholdPercent = 98f,
        thresholdMinutesBeforeEnd = 2f,
    )

    @Test
    fun notReachedAtStartOfPlayback() {
        assertFalse(reached(positionMs = 0L, durationMs = 0L))
        assertFalse(reached(positionMs = 0L, durationMs = 2_400_000L))
        assertFalse(reached(positionMs = 0L, durationMs = 60_000L, mode = NextEpisodeThresholdMode.MINUTES_BEFORE_END))
    }

    @Test
    fun percentageThreshold() {
        assertFalse(reached(positionMs = 2_300_000L, durationMs = 2_400_000L))
        assertTrue(reached(positionMs = 2_352_000L, durationMs = 2_400_000L))
    }

    @Test
    fun minutesBeforeEndThreshold() {
        val mode = NextEpisodeThresholdMode.MINUTES_BEFORE_END
        assertFalse(reached(positionMs = 2_200_000L, durationMs = 2_400_000L, mode = mode))
        assertTrue(reached(positionMs = 2_280_000L, durationMs = 2_400_000L, mode = mode))
    }

    @Test
    fun outroStartingAtZeroIsIgnored() {
        val outro = SkipInterval(startTime = 0.0, endTime = 2_400.0, type = "outro", provider = "test")
        assertFalse(reached(positionMs = 1_000L, durationMs = 2_400_000L, skipIntervals = listOf(outro)))
    }
}
