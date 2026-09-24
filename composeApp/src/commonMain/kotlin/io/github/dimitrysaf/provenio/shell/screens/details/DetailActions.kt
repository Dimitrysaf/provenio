package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.metadata.SeriesPrimaryAction
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressUiState
import io.github.dimitrysaf.provenio.core.watch.progress.buildPlaybackVideoId
import io.github.dimitrysaf.provenio.core.watch.watched.previousReleasedEpisodesBefore
import io.github.dimitrysaf.provenio.core.watch.watched.releasedEpisodesForSeason
import io.github.dimitrysaf.provenio.core.watch.watched.releasedPlayableEpisodes
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingActions
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import io.github.dimitrysaf.provenio.shell.screens.details.components.EpisodeWatchedActionSheet
import io.github.dimitrysaf.provenio.shell.screens.details.components.SeasonWatchedActionSheet
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.episodes_season
import provenio.composeapp.generated.resources.episodes_specials
import org.jetbrains.compose.resources.stringResource

internal typealias DetailPlayHandler = (
    type: String,
    videoId: String,
    parentMetaId: String,
    parentMetaType: String,
    title: String,
    logo: String?,
    poster: String?,
    background: String?,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    episodeThumbnail: String?,
    pauseDescription: String?,
    resumePositionMs: Long?,
) -> Unit

// Plays the primary action: the series episode it points at, or the movie from where it was left.
internal fun MetaDetails.playPrimary(
    handler: DetailPlayHandler,
    seriesAction: SeriesPrimaryAction?,
    seriesStreamVideoId: String?,
    seriesPauseDescription: String?,
    movieResumePositionMs: Long?,
    hasEpisodes: Boolean,
) {
    if ((type == "series" || hasEpisodes) && seriesAction != null) {
        handler(
            type,
            seriesStreamVideoId ?: seriesAction.videoId,
            id,
            type,
            name,
            logo,
            poster,
            background,
            seriesAction.seasonNumber,
            seriesAction.episodeNumber,
            seriesAction.episodeTitle,
            seriesAction.episodeThumbnail,
            seriesPauseDescription,
            seriesAction.resumePositionMs,
        )
    } else {
        handler(
            type,
            id,
            id,
            type,
            name,
            logo,
            poster,
            background,
            null,
            null,
            null,
            null,
            description,
            movieResumePositionMs,
        )
    }
}

// Plays one episode, resuming it when it was left part way through.
internal fun MetaDetails.playEpisode(
    handler: DetailPlayHandler,
    video: MetaVideo,
    watchProgress: WatchProgressUiState,
) {
    val season = video.season
    val episode = video.episode
    val playbackVideoId = buildPlaybackVideoId(
        parentMetaId = id,
        seasonNumber = season,
        episodeNumber = episode,
        fallbackVideoId = video.id,
    )
    val streamVideoId = video.id.takeIf { it.isNotBlank() } ?: playbackVideoId
    val savedProgress = watchProgress.progressForVideo(
        videoId = streamVideoId,
        parentMetaId = id,
        seasonNumber = season,
        episodeNumber = episode,
    )
        ?.takeUnless { it.isCompleted }
    handler(
        type,
        streamVideoId,
        id,
        type,
        name,
        logo,
        poster,
        background,
        season,
        episode,
        video.title,
        video.thumbnail,
        video.overview,
        savedProgress?.lastPositionMs,
    )
}

// The watched actions for one episode, opened by a long press.
@Composable
internal fun EpisodeActionsSheet(
    meta: MetaDetails,
    episode: MetaVideo,
    watchedKeys: Set<String>,
    progressByVideoId: Map<String, WatchProgressEntry>,
    todayIsoDate: String,
    blurUnwatchedEpisodes: Boolean,
    showPlayManually: Boolean,
    onDismiss: () -> Unit,
    onPlayManually: () -> Unit,
) {
    val isEpisodeWatched = remember(meta, episode, watchedKeys, progressByVideoId) {
        isEpisodeWatchedForActions(meta, episode, watchedKeys, progressByVideoId)
    }
    val previousEpisodes = remember(meta, episode, todayIsoDate) {
        meta.previousReleasedEpisodesBefore(target = episode, todayIsoDate = todayIsoDate)
    }
    val seasonEpisodes = remember(meta, episode, todayIsoDate) {
        meta.releasedEpisodesForSeason(seasonNumber = episode.season, todayIsoDate = todayIsoDate)
    }
    val arePreviousEpisodesWatched = remember(previousEpisodes, watchedKeys, progressByVideoId) {
        areEpisodesWatchedForActions(meta, previousEpisodes, watchedKeys, progressByVideoId)
    }
    val isSeasonWatched = remember(seasonEpisodes, watchedKeys, progressByVideoId) {
        areEpisodesWatchedForActions(meta, seasonEpisodes, watchedKeys, progressByVideoId)
    }
    EpisodeWatchedActionSheet(
        episode = episode,
        seasonLabel = episode.season?.let {
            stringResource(Res.string.episodes_season, it)
        } ?: stringResource(Res.string.episodes_specials),
        isEpisodeWatched = isEpisodeWatched,
        canMarkPreviousEpisodes = previousEpisodes.isNotEmpty(),
        arePreviousEpisodesWatched = arePreviousEpisodesWatched,
        isSeasonWatched = isSeasonWatched,
        thumbnailUrl = episode.thumbnail ?: meta.background ?: meta.poster,
        blurThumbnail = blurUnwatchedEpisodes && !isEpisodeWatched,
        onDismiss = onDismiss,
        onToggleWatched = {
            WatchingActions.toggleEpisodeWatched(
                meta = meta,
                episode = episode,
                isCurrentlyWatched = isEpisodeWatched,
            )
        },
        onTogglePreviousWatched = {
            WatchingActions.togglePreviousEpisodesWatched(
                meta = meta,
                episodes = previousEpisodes,
                areCurrentlyWatched = arePreviousEpisodesWatched,
            )
        },
        onToggleSeasonWatched = {
            WatchingActions.toggleSeasonWatched(
                meta = meta,
                episodes = seasonEpisodes,
                areCurrentlyWatched = isSeasonWatched,
            )
        },
        showPlayManually = showPlayManually,
        onPlayManually = onPlayManually,
    )
}

// The watched actions for a whole season, opened by a long press on its header.
@Composable
internal fun SeasonActionsSheet(
    meta: MetaDetails,
    season: Int,
    watchedKeys: Set<String>,
    progressByVideoId: Map<String, WatchProgressEntry>,
    todayIsoDate: String,
    onDismiss: () -> Unit,
) {
    val seasonEpisodes = remember(meta, season, todayIsoDate) {
        meta.releasedEpisodesForSeason(seasonNumber = season, todayIsoDate = todayIsoDate)
    }
    val previousSeasonEpisodes = remember(meta, season, todayIsoDate) {
        val normalizedSeason = season.coerceAtLeast(0)
        meta.releasedPlayableEpisodes(todayIsoDate)
            .filter { episode ->
                val episodeSeason = episode.season?.coerceAtLeast(0) ?: 0
                episodeSeason > 0 && episodeSeason < normalizedSeason
            }
    }
    val isSeasonWatched = remember(seasonEpisodes, watchedKeys, progressByVideoId) {
        areEpisodesWatchedForActions(meta, seasonEpisodes, watchedKeys, progressByVideoId)
    }
    val canMarkPreviousSeasons = remember(previousSeasonEpisodes, watchedKeys, progressByVideoId) {
        previousSeasonEpisodes.any { episode ->
            !isEpisodeWatchedForActions(meta, episode, watchedKeys, progressByVideoId)
        }
    }
    SeasonWatchedActionSheet(
        seasonLabel = seasonLabel(season),
        posterUrl = meta.poster ?: meta.background,
        showTitle = meta.name,
        isSeasonWatched = isSeasonWatched,
        canMarkPreviousSeasons = canMarkPreviousSeasons,
        onDismiss = onDismiss,
        onToggleSeasonWatched = {
            WatchingActions.toggleSeasonWatched(
                meta = meta,
                episodes = seasonEpisodes,
                areCurrentlyWatched = isSeasonWatched,
            )
        },
        onMarkPreviousSeasonsWatched = {
            WatchingActions.togglePreviousEpisodesWatched(
                meta = meta,
                episodes = previousSeasonEpisodes,
                areCurrentlyWatched = false,
            )
        },
    )
}

@Composable
private fun seasonLabel(season: Int): String =
    if (season == 0) {
        stringResource(Res.string.episodes_specials)
    } else {
        stringResource(Res.string.episodes_season, season)
    }

private fun isEpisodeWatchedForActions(
    meta: MetaDetails,
    episode: MetaVideo,
    watchedKeys: Set<String>,
    progressByVideoId: Map<String, WatchProgressEntry>,
): Boolean {
    val episodeVideoId = buildPlaybackVideoId(
        parentMetaId = meta.id,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
        fallbackVideoId = episode.id,
    )
    return progressByVideoId[episodeVideoId]?.isEffectivelyCompleted == true ||
        WatchingState.isEpisodeWatched(
            watchedKeys = watchedKeys,
            metaType = meta.type,
            metaId = meta.id,
            episode = episode,
        )
}

private fun areEpisodesWatchedForActions(
    meta: MetaDetails,
    episodes: Collection<MetaVideo>,
    watchedKeys: Set<String>,
    progressByVideoId: Map<String, WatchProgressEntry>,
): Boolean = episodes.isNotEmpty() && episodes.all { episode ->
    isEpisodeWatchedForActions(meta, episode, watchedKeys, progressByVideoId)
}
