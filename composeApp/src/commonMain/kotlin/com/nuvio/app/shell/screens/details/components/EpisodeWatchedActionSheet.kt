package com.nuvio.app.shell.screens.details.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAddCheckCircle
import androidx.compose.runtime.Composable
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.shell.components.MediaActionsSheet
import com.nuvio.app.shell.components.MediaSheetAction
import com.nuvio.app.core.metadata.MetaVideo
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * What can be done with an episode, from a long press on it.
 *
 * The app's one action sheet, given this episode's actions, so a long press looks the same
 * wherever it happens.
 */
@Composable
fun EpisodeWatchedActionSheet(
    episode: MetaVideo,
    seasonLabel: String,
    isEpisodeWatched: Boolean,
    canMarkPreviousEpisodes: Boolean,
    arePreviousEpisodesWatched: Boolean,
    isSeasonWatched: Boolean,
    onDismiss: () -> Unit,
    onToggleWatched: () -> Unit,
    onTogglePreviousWatched: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    thumbnailUrl: String? = null,
    blurThumbnail: Boolean = false,
    showPlayManually: Boolean = false,
    onPlayManually: (() -> Unit)? = null,
) {
    val actions = buildList {
        add(
            MediaSheetAction(
                icon = Icons.Rounded.CheckCircle,
                label = if (isEpisodeWatched) {
                    stringResource(Res.string.episode_mark_unwatched)
                } else {
                    stringResource(Res.string.episode_mark_watched)
                },
                onSelected = onToggleWatched,
            ),
        )
        if (canMarkPreviousEpisodes) {
            add(
                MediaSheetAction(
                    icon = Icons.Rounded.DoneAll,
                    label = if (arePreviousEpisodesWatched) {
                        stringResource(Res.string.episode_mark_previous_unwatched)
                    } else {
                        stringResource(Res.string.episode_mark_previous_watched)
                    },
                    onSelected = onTogglePreviousWatched,
                ),
            )
        }
        add(
            MediaSheetAction(
                icon = Icons.Rounded.PlaylistAddCheckCircle,
                label = if (isSeasonWatched) {
                    stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
                } else {
                    stringResource(Res.string.episode_mark_season_watched, seasonLabel)
                },
                onSelected = onToggleSeasonWatched,
            ),
        )
        if (showPlayManually && onPlayManually != null) {
            add(
                MediaSheetAction(
                    icon = Icons.Rounded.PlayArrow,
                    label = stringResource(Res.string.play_manually),
                    onSelected = onPlayManually,
                ),
            )
        }
    }

    MediaActionsSheet(
        imageUrl = thumbnailUrl,
        title = episode.title,
        subtitle = buildString {
            localizedSeasonEpisodeCode(
                seasonNumber = episode.season,
                episodeNumber = episode.episode,
            )?.let {
                append(it)
                append(" • ")
            }
            append(seasonLabel)
        },
        actions = actions,
        onDismiss = onDismiss,
        landscapeThumbnail = true,
        blurThumbnail = blurThumbnail,
    )
}

/** The same sheet for a whole season, whose artwork is the show's own. */
@Composable
fun SeasonWatchedActionSheet(
    seasonLabel: String,
    isSeasonWatched: Boolean,
    canMarkPreviousSeasons: Boolean,
    onDismiss: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    onMarkPreviousSeasonsWatched: () -> Unit,
    posterUrl: String? = null,
    showTitle: String? = null,
) {
    val actions = buildList {
        add(
            MediaSheetAction(
                icon = Icons.Rounded.PlaylistAddCheckCircle,
                label = if (isSeasonWatched) {
                    stringResource(Res.string.episode_mark_season_unwatched, seasonLabel)
                } else {
                    stringResource(Res.string.episode_mark_season_watched, seasonLabel)
                },
                onSelected = onToggleSeasonWatched,
            ),
        )
        if (canMarkPreviousSeasons) {
            add(
                MediaSheetAction(
                    icon = Icons.Rounded.DoneAll,
                    label = stringResource(Res.string.episode_mark_previous_seasons_watched),
                    onSelected = onMarkPreviousSeasonsWatched,
                ),
            )
        }
    }

    MediaActionsSheet(
        imageUrl = posterUrl,
        title = seasonLabel,
        subtitle = showTitle,
        actions = actions,
        onDismiss = onDismiss,
    )
}
