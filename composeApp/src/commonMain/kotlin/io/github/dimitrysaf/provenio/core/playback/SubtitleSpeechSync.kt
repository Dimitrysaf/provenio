package io.github.dimitrysaf.provenio.core.playback

import kotlin.math.abs
import kotlin.math.sqrt

/** How loud the sound was at a point in the video: [positionMs] in the video, [levelDb] in dB. */
data class PlayerAudioLevelSample(val positionMs: Long, val levelDb: Float)

/**
 * Works out how far add-on subtitles are from the speech in the video, the way subtitle sync tools
 * do it: the video's loudness over time is set against when the subtitle lines are on screen, and
 * the shift that lines the two up best is the subtitle delay.
 *
 * Only timing is compared, never words, so it works whatever language the subtitles are in. It
 * keeps the last [MaxWindowMs] of what was heard, so a later check follows subtitles that drift.
 */
class SubtitleSpeechSync {
    private val levels = LinkedHashMap<Long, Float>()

    /** How much of the video has been heard so far, in ms. */
    val heardMs: Long
        get() = levels.size * BinMs

    fun add(sample: PlayerAudioLevelSample) {
        if (sample.positionMs < 0L) return
        val bin = sample.positionMs / BinMs
        levels.remove(bin)
        levels[bin] = sample.levelDb
        while (levels.size > MaxWindowMs / BinMs) {
            levels.remove(levels.keys.first())
        }
    }

    fun clear() = levels.clear()

    /**
     * The subtitle delay that lines [cues] up with the speech, or null when the match is not clear
     * enough to trust: too little heard, or no shift that stands out from the rest.
     */
    fun estimateDelayMs(cues: List<SubtitleSyncCue>): Int? {
        if (cues.isEmpty() || heardMs < MinHeardMs) return null
        val bins = levels.keys.sorted()
        val activity = activityOf(bins)
        val firstBin = bins.first() - MaxShiftBins
        val lastBin = bins.last() + MaxShiftBins
        val shown = shownBins(cues, firstBin, lastBin)
        val shiftCount = (MaxShiftBins * 2 + 1).toInt()
        val scores = DoubleArray(shiftCount)
        for (index in 0 until shiftCount) {
            val shift = index - MaxShiftBins
            var score = 0.0
            bins.forEachIndexed { position, bin ->
                score += activity[position] * shown[(bin - shift - firstBin).toInt()]
            }
            scores[index] = score / bins.size
        }
        val bestIndex = scores.indices.maxBy { scores[it] }
        val mean = scores.average()
        val spread = sqrt(scores.sumOf { (it - mean) * (it - mean) } / scores.size)
        if (spread <= 0.0 || (scores[bestIndex] - mean) / spread < MinPeakStrength) return null
        val runnerUp = scores.indices
            .filter { abs(it - bestIndex) > PeakWidthBins }
            .maxOfOrNull { scores[it] }
            ?: return null
        if (scores[bestIndex] - runnerUp < MinPeakLead * spread) return null
        return ((bestIndex - MaxShiftBins) * BinMs).toInt()
    }

    /**
     * How strongly each heard bin sounds active, from -1 to 1: its loudness against the middle
     * loudness of everything heard, so it works the same whatever the overall volume.
     */
    private fun activityOf(bins: List<Long>): DoubleArray {
        val sorted = bins.map { levels.getValue(it) }.sorted()
        val median = sorted[sorted.size / 2]
        return DoubleArray(bins.size) { index ->
            ((levels.getValue(bins[index]) - median) / ActivitySpanDb).toDouble().coerceIn(-1.0, 1.0)
        }
    }

    /** For every bin from [firstBin] to [lastBin], 1 while a subtitle line is on screen and -1 otherwise. */
    private fun shownBins(cues: List<SubtitleSyncCue>, firstBin: Long, lastBin: Long): DoubleArray {
        val shown = DoubleArray((lastBin - firstBin + 1).toInt()) { -1.0 }
        cues.forEach { cue ->
            val start = (cue.startTimeMs / BinMs).coerceAtLeast(firstBin)
            val end = (cue.endTimeMs / BinMs).coerceAtMost(lastBin)
            for (bin in start..end) shown[(bin - firstBin).toInt()] = 1.0
        }
        return shown
    }

    companion object {
        /** How finely loudness and subtitle lines are compared. */
        const val BinMs = 100L

        /** How much has to be heard before the first estimate. */
        const val MinHeardMs = 90_000L

        /** How much of what was heard is kept: the most recent ten minutes. */
        const val MaxWindowMs = 600_000L

        private val MaxShiftBins = SUBTITLE_DELAY_MAX_MS / BinMs
        private const val PeakWidthBins = 10
        private const val ActivitySpanDb = 6f
        private const val MinPeakStrength = 4.0
        private const val MinPeakLead = 1.0
    }
}
