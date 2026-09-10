package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.feature.detail.SpecialsSeason
import io.github.dimitrysaf.provenio.feature.detail.firstRegularEpisode
import io.github.dimitrysaf.provenio.feature.detail.firstUnwatchedEpisode
import io.github.dimitrysaf.provenio.feature.detail.trackedEpisodeCount
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.stremio.model.Meta

/**
 * The primary action. Prefers resuming Simkl's saved playback position, when it has one
 * for an episode this title actually lists; otherwise the first regular episode Simkl has
 * not recorded as watched, when it has watched-episode data for this title; otherwise the
 * first episode of the first regular season, never a special.
 */
@Composable
fun WatchAction(
    meta: Meta,
    simklWatchedEpisodes: Set<Pair<Int, Int>>,
    resumeSession: SimklPlaybackSession?,
    onChooseSource: (String, Float?) -> Unit,
) {
    // Only trusted once matched back to a video this title actually lists — a season and
    // episode number alone say nothing about whether the addon agrees they exist.
    val resumeEpisode = resumeSession?.episode?.let { ep ->
        meta.videos.firstOrNull { it.season == ep.season && it.episode == ep.number }
    }
    val isMovieResume = resumeSession != null && resumeSession.episode == null
    val resumeProgress = resumeSession?.progress?.takeIf { resumeEpisode != null || isMovieResume }

    val next = resumeEpisode
        ?: meta.firstUnwatchedEpisode(simklWatchedEpisodes)
        ?: meta.firstRegularEpisode()
    val label = when {
        resumeProgress != null && next?.season != null && next.episode != null ->
            "Resume S${pad(next.season)}E${pad(next.episode)} now"
        resumeProgress != null -> "Resume now"
        next?.season != null && next.episode != null ->
            "Watch S${pad(next.season)}E${pad(next.episode)} now"
        meta.videos.isNotEmpty() -> "Watch first episode now"
        else -> "Watch now"
    }

    // A series plays its first episode; a film plays itself. Either way the id decides
    // which streams the addons are asked for.
    val playId = next?.id ?: meta.id

    Button(
        onClick = { onChooseSource(playId, resumeProgress) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/**
 * Simkl's synced count is preferred when it has one, because it reflects everything the
 * user has ever marked watched, on this device or anywhere else Simkl is connected.
 * The local, per-episode tally ([watchedIds]) is only the fallback, for a title Simkl has
 * no record of yet or while signed out.
 */
@Composable
fun WatchProgress(meta: Meta, simklItem: SimklItem?, watchedIds: Set<String>) {
    val localWatched = meta.videos.count {
        (it.season ?: SpecialsSeason) != SpecialsSeason && it.id in watchedIds
    }
    val fromSimkl = simklItem?.takeIf { it.totalEpisodes > 0 }
    val total = fromSimkl?.totalEpisodes?.toInt() ?: meta.trackedEpisodeCount()
    val watched = fromSimkl?.watchedEpisodes?.toInt() ?: localWatched

    SectionCard(title = "Watch progress", icon = Icons.Outlined.Visibility) {
        // A bar pinned at zero says nothing that the text below it does not.
        if (watched > 0 && total > 0) {
            LinearProgressIndicator(
                progress = { (watched.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        Text(
            text = when {
                total == 0 -> "Not tracked yet"
                watched == 0 -> "Not started, $total episodes"
                else -> "$watched of $total episodes watched"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
