package io.github.dimitrysaf.provenio.core.streams

import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import provenio.composeapp.generated.resources.*

internal data class StreamResumeState(
    val positionMs: Long? = null,
    val progressFraction: Float? = null,
)

internal fun resolveStreamResumeState(
    progress: WatchProgressEntry?,
    initialPositionMs: Long?,
    initialProgressFraction: Float?,
    startFromBeginning: Boolean,
): StreamResumeState {
    if (startFromBeginning || progress?.isResumable == false) return StreamResumeState()
    val fraction = (if (progress != null) progress.progressPercent?.div(100f) else initialProgressFraction)
        ?.takeIf { it > 0f }?.coerceIn(0f, 1f)
    val position = if (fraction != null) null
        else (progress?.lastPositionMs ?: initialPositionMs)?.takeIf { it > 0L }
    return StreamResumeState(positionMs = position, progressFraction = fraction)
}
