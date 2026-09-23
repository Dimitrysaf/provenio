package com.nuvio.app.core.watch.progress

import com.nuvio.app.core.time.parseEpisodeReleaseEpochMs
import nuvio.composeapp.generated.resources.*

fun parseReleaseDateToEpochMs(raw: String?): Long? {
    return parseEpisodeReleaseEpochMs(raw)
}

class ReleaseAlertState(
    val isReleaseAlert: Boolean,
    val isNewSeasonRelease: Boolean,
)

fun calculateReleaseAlertState(
    seedLastUpdatedEpochMs: Long,
    seedSeasonNumber: Int?,
    nextSeasonNumber: Int?,
    releasedIso: String?,
    releaseEpochMs: Long? = parseReleaseDateToEpochMs(releasedIso),
    nowEpochMs: Long = WatchProgressClock.nowEpochMs(),
): ReleaseAlertState {
    val releaseEpoch = releaseEpochMs
    val nowMs = nowEpochMs

    if (releaseEpoch == null) {
        return ReleaseAlertState(false, false)
    }

    val hasAired = nowMs >= releaseEpoch
    val sixtyDaysMs = 60L * 24 * 60 * 60 * 1000
    val isReleaseAlert = hasAired &&
        releaseEpoch > seedLastUpdatedEpochMs &&
        (nowMs - releaseEpoch) < sixtyDaysMs

    val isNewSeasonRelease = isReleaseAlert &&
        seedSeasonNumber != null &&
        nextSeasonNumber != null &&
        nextSeasonNumber != seedSeasonNumber

    return ReleaseAlertState(
        isReleaseAlert = isReleaseAlert,
        isNewSeasonRelease = isNewSeasonRelease
    )
}
