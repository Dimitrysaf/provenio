package io.github.dimitrysaf.provenio.feature.detail

import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.Video

/** Season 0 is the specials bucket by convention across every metadata source. */
internal const val SpecialsSeason = 0

/**
 * Episodes that count towards progress.
 *
 * Specials are excluded. They are supplementary, they are not part of the run a viewer is
 * working through, and counting them makes a finished series read as incomplete.
 */
internal fun Meta.trackedEpisodeCount(): Int =
    videos.count { (it.season ?: SpecialsSeason) != SpecialsSeason }

/**
 * The episode "Watch now" opens: the lowest numbered episode of the lowest numbered
 * regular season. Never a special, regardless of how the addon orders [videos] — Season 0
 * routinely sorts first in raw addon data, and starting there is never what "watch now" on
 * a series means. Falls back to the very first video for the rare title that lists nothing
 * but specials, since playing something beats the button doing nothing.
 */
internal fun Meta.firstRegularEpisode(): Video? =
    videos
        .filter { (it.season ?: SpecialsSeason) != SpecialsSeason }
        .minWithOrNull(compareBy({ it.season }, { it.episode }))
        ?: videos.firstOrNull()

/**
 * The first regular episode, in season/episode order, that [watchedEpisodes] does not
 * cover. [watchedEpisodes] is Simkl's own watched-episode list (see
 * SimklSync.watchedEpisodesFor) — an empty set here means Simkl has no watched-episode
 * data for this title (nothing watched yet, or not signed in), which this returns null
 * for rather than treating as "watch from the start" itself; the caller decides that
 * fallback.
 */
internal fun Meta.firstUnwatchedEpisode(watchedEpisodes: Set<Pair<Int, Int>>): Video? {
    if (watchedEpisodes.isEmpty()) return null
    return videos
        .filter { (it.season ?: SpecialsSeason) != SpecialsSeason }
        .sortedWith(compareBy({ it.season }, { it.episode }))
        .firstOrNull { video ->
            val season = video.season ?: return@firstOrNull false
            val episode = video.episode ?: return@firstOrNull false
            (season to episode) !in watchedEpisodes
        }
}

/**
 * The season a viewer is part way through, if there is one.
 *
 * Part way means at least one episode watched and at least one not — a season not started
 * and a season finished are both places there is nothing left to carry on with. Specials
 * are skipped for the same reason they are skipped everywhere else: they are not part of
 * the run being worked through.
 */
internal fun inProgressSeason(
    seasons: List<Pair<Int, List<Video>>>,
    watchedIds: Set<String>,
): Int? = seasons.firstOrNull { (season, episodes) ->
    season != SpecialsSeason &&
        episodes.isNotEmpty() &&
        episodes.count { it.id in watchedIds } in 1 until episodes.size
}?.first

/** Regular episode ids covered by Simkl's own watched-episode list for this title. */
internal fun simklWatchedIds(meta: Meta, watchedEpisodes: Set<Pair<Int, Int>>): Set<String> {
    if (watchedEpisodes.isEmpty()) return emptySet()
    return meta.videos
        .filter { (it.season ?: SpecialsSeason) != SpecialsSeason }
        .filter { video ->
            val season = video.season ?: return@filter false
            val episode = video.episode ?: return@filter false
            (season to episode) in watchedEpisodes
        }
        .map { it.id }
        .toSet()
}
