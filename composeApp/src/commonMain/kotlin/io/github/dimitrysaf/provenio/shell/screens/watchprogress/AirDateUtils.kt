package io.github.dimitrysaf.provenio.shell.screens.watchprogress

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.core.format.formatReleaseDateWithoutYear
import io.github.dimitrysaf.provenio.core.time.daysUntilEpisodeRelease
import io.github.dimitrysaf.provenio.core.time.parseEpisodeReleaseEpochMs
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressClock

@Composable
fun computeAirDateBadgeText(
    releasedIso: String?,
    todayIsoDate: String,
): String? {
    if (releasedIso.isNullOrBlank() || todayIsoDate.isBlank()) {
        return null
    }

    val releaseEpoch = parseEpisodeReleaseEpochMs(releasedIso)
    if (releaseEpoch != null && WatchProgressClock.nowEpochMs() >= releaseEpoch) {
        return null
    }

    val daysUntil = daysUntilEpisodeRelease(
        todayIsoDate = todayIsoDate,
        releasedDate = releasedIso,
    ) ?: return null

    return when {
        daysUntil < 0 -> null
        daysUntil == 0 -> stringResource(Res.string.cw_airs_today)
        daysUntil == 1 -> stringResource(Res.string.cw_airs_tomorrow)
        daysUntil in 2..7 -> pluralStringResource(Res.plurals.cw_airs_in_days, daysUntil, daysUntil)
        else -> {
            val formattedDate = formatReleaseDateWithoutYear(releasedIso)
            stringResource(Res.string.cw_airs_date, formattedDate)
        }
    }
}
