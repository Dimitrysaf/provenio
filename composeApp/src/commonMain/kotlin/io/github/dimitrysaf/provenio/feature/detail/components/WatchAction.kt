package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.layout.Column
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
import io.github.dimitrysaf.provenio.player.PlaybackPosition
import io.github.dimitrysaf.provenio.simkl.SimklPlaybackSession
import io.github.dimitrysaf.provenio.simkl.SimklStatus
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.detail_not_started
import io.github.dimitrysaf.provenio.resources.detail_not_tracked_yet
import io.github.dimitrysaf.provenio.resources.detail_resume_episode
import io.github.dimitrysaf.provenio.resources.detail_resume_now
import io.github.dimitrysaf.provenio.resources.detail_watch_episode
import io.github.dimitrysaf.provenio.resources.detail_watch_first_episode
import io.github.dimitrysaf.provenio.resources.detail_watch_now
import io.github.dimitrysaf.provenio.resources.detail_watch_progress
import io.github.dimitrysaf.provenio.resources.detail_watched_of
import io.github.dimitrysaf.provenio.resources.not_watched
import io.github.dimitrysaf.provenio.resources.watched
import org.jetbrains.compose.resources.stringResource

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
    /** Locally saved positions, keyed by video id. These outrank Simkl's. */
    positions: Map<String, PlaybackPosition>,
    onChooseSource: (String, Float?) -> Unit,
) {
    // What this device was last part way through, film or episode. More recent and more
    // exact than anything Simkl reports, so it decides both the button and where playback
    // picks up; the player reads the same row again by video id when it starts.
    val localResume = positions.entries
        .filter { (id, _) -> id == meta.id || meta.videos.any { it.id == id } }
        .maxByOrNull { it.value.updatedAtMillis }
    val localEpisode = localResume?.let { entry -> meta.videos.firstOrNull { it.id == entry.key } }
    val localIsThisFilm = localResume != null && localResume.key == meta.id

    // Only trusted once matched back to a video this title actually lists — a season and
    // episode number alone say nothing about whether the addon agrees they exist.
    val resumeEpisode = resumeSession?.episode?.let { ep ->
        meta.videos.firstOrNull { it.season == ep.season && it.episode == ep.number }
    }
    val isMovieResume = resumeSession != null && resumeSession.episode == null
    val resumeProgress = resumeSession?.progress?.takeIf { resumeEpisode != null || isMovieResume }

    val next = localEpisode
        ?: resumeEpisode
        ?: meta.firstUnwatchedEpisode(simklWatchedEpisodes)
        ?: meta.firstRegularEpisode()
    // The season and episode numbers are padded before they are handed over, so the
    // translated string carries them as text and no locale reformats them into something
    // an episode label should not be.
    val resuming = localEpisode != null || localIsThisFilm || resumeProgress != null
    val label = when {
        resuming && next?.season != null && next.episode != null ->
            stringResource(
                Res.string.detail_resume_episode,
                pad(next.season),
                pad(next.episode),
            )
        resuming -> stringResource(Res.string.detail_resume_now)
        next?.season != null && next.episode != null ->
            stringResource(
                Res.string.detail_watch_episode,
                pad(next.season),
                pad(next.episode),
            )
        meta.videos.isNotEmpty() -> stringResource(Res.string.detail_watch_first_episode)
        else -> stringResource(Res.string.detail_watch_now)
    }

    // A series plays its first episode; a film plays itself. Either way the id decides
    // which streams the addons are asked for.
    val playId = next?.id ?: meta.id

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Button(
            onClick = { onChooseSource(playId, resumeProgress) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(label)
        }

        // Only a film gets a bar here. An episode has its own row further down the page,
        // and drawing the same progress twice on one screen reads as two things.
        val filmProgress = localResume?.value?.fraction?.takeIf { localIsThisFilm && it > 0f }
        if (filmProgress != null) {
            LinearProgressIndicator(
                progress = { filmProgress },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

/**
 * Simkl's synced count is preferred when it has one, because it reflects everything the
 * user has ever marked watched, on this device or anywhere else Simkl is connected.
 * The local, per-episode tally ([watchedIds]) is only the fallback, for a title Simkl has
 * no record of yet or while signed out.
 *
 * A film has no episodes to count towards, so it gets its own binary card below rather
 * than running the episode-count arithmetic — [SimklItem.totalEpisodes] is a TV show field
 * and is always zero for a movie, whatever its actual watched state.
 */
@Composable
fun WatchProgress(meta: Meta, simklItem: SimklItem?, watchedIds: Set<String>) {
    if (meta.type == "movie") {
        MovieWatchProgress(simklItem)
        return
    }

    val localWatched = meta.videos.count {
        (it.season ?: SpecialsSeason) != SpecialsSeason && it.id in watchedIds
    }
    val fromSimkl = simklItem?.takeIf { it.totalEpisodes > 0 }
    val total = fromSimkl?.totalEpisodes?.toInt() ?: meta.trackedEpisodeCount()
    val watched = fromSimkl?.watchedEpisodes?.toInt() ?: localWatched

    SectionCard(
        title = stringResource(Res.string.detail_watch_progress),
        icon = Icons.Outlined.Visibility,
    ) {
        // A bar pinned at zero says nothing that the text below it does not.
        if (watched > 0 && total > 0) {
            LinearProgressIndicator(
                progress = { (watched.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        Text(
            text = when {
                total == 0 -> stringResource(Res.string.detail_not_tracked_yet)
                watched == 0 -> stringResource(Res.string.detail_not_started, total)
                else -> stringResource(Res.string.detail_watched_of, watched, total)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A film's watch state: binary, from Simkl's own list status rather than an episode count
 * it does not have. [simklItem] is null when Simkl has no record of the film at all — signed
 * out, or simply never added to a list — which reads as "not watched" rather than an error.
 */
@Composable
private fun MovieWatchProgress(simklItem: SimklItem?) {
    val isWatched = simklItem?.status == SimklStatus.Completed
    SectionCard(
        title = stringResource(Res.string.detail_watch_progress),
        icon = Icons.Outlined.Visibility,
    ) {
        Text(
            text = stringResource(if (isWatched) Res.string.watched else Res.string.not_watched),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
