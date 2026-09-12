package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.core.platform.MatchHostSystemBars
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.feature.detail.SpecialsSeason
import io.github.dimitrysaf.provenio.feature.detail.simklWatchedIds
import io.github.dimitrysaf.provenio.player.PlaybackPosition
import io.github.dimitrysaf.provenio.player.PlaybackPositionRepository
import io.github.dimitrysaf.provenio.simkl.SimklSync
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.Video
import io.github.dimitrysaf.provenio.watch.EpisodeWatchedRepository
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.detail_episode
import io.github.dimitrysaf.provenio.resources.detail_episodes
import io.github.dimitrysaf.provenio.resources.detail_no_episodes
import io.github.dimitrysaf.provenio.resources.detail_season
import io.github.dimitrysaf.provenio.resources.detail_specials
import io.github.dimitrysaf.provenio.resources.not_watched
import io.github.dimitrysaf.provenio.resources.watched
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.seasonSection(
    seasons: List<Pair<Int, List<Video>>>,
    expanded: MutableState<Int>,
    /** Called when the viewer opens a season themselves, so nothing reopens one later. */
    onSeasonToggled: () -> Unit,
    watchedIds: Set<String>,
    /** How far into each episode playback got, keyed by video id. Bars come from this. */
    positions: Map<String, PlaybackPosition>,
    onChooseSource: (String) -> Unit,
    onToggleWatched: (Video) -> Unit,
) {
    if (seasons.isEmpty()) {
        item { EmptyNote(stringResource(Res.string.detail_no_episodes)) }
        return
    }

    seasons.forEach { (season, episodes) ->
        item {
            SeasonHeader(
                season = season,
                episodeCount = episodes.size,
                watched = episodes.count { it.id in watchedIds },
                expanded = expanded.value == season,
                onToggle = {
                    onSeasonToggled()
                    expanded.value = if (expanded.value == season) -1 else season
                },
            )
        }
        item {
            AnimatedVisibility(visible = expanded.value == season) {
                Column {
                    episodes.forEach { video ->
                        EpisodeRow(
                            video = video,
                            progress = positions[video.id]?.fraction,
                            watched = video.id in watchedIds,
                            onClick = { onChooseSource(video.id) },
                            onToggleWatched = { onToggleWatched(video) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The episode list, on its own, in a sheet — for the player's "episodes" button, which has
 * no details page around it to put a [seasonSection] inside. Fetches its own meta, since
 * the player itself only ever knows the one episode it is playing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesSheet(
    type: String,
    imdbId: String,
    currentVideoId: String?,
    onDismiss: () -> Unit,
    onSelectEpisode: (Video) -> Unit,
) {
    var meta by remember(imdbId) { mutableStateOf<Meta?>(null) }
    LaunchedEffect(type, imdbId) { meta = AddonRepository.meta(type, imdbId) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        MatchHostSystemBars()
        val currentMeta = meta
        if (currentMeta == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@ModalBottomSheet
        }

        val seasons = currentMeta.videos
            .groupBy { it.season ?: SpecialsSeason }
            .toList()
            .sortedWith(compareBy({ (season, _) -> season == SpecialsSeason }, { it.first }))
        val expanded = rememberSaveable(currentMeta.id) {
            mutableStateOf(
                seasons.firstOrNull { (_, episodes) -> episodes.any { it.id == currentVideoId } }
                    ?.first ?: seasons.firstOrNull()?.first ?: 1,
            )
        }

        var simklItem by remember(currentMeta.id) { mutableStateOf<SimklItem?>(null) }
        LaunchedEffect(currentMeta.id) { simklItem = SimklSync.progressFor(currentMeta.id) }
        val simklWatchedEpisodes = remember(simklItem?.simklId) {
            simklItem?.let { SimklSync.watchedEpisodesFor(it.simklId) }.orEmpty()
        }
        val overrides by EpisodeWatchedRepository.overrides.collectAsState()
        val watchedIds = remember(currentMeta, simklWatchedEpisodes, overrides) {
            val inferred = simklWatchedIds(currentMeta, simklWatchedEpisodes).toMutableSet()
            overrides.forEach { (videoId, isWatched) ->
                if (isWatched) inferred.add(videoId) else inferred.remove(videoId)
            }
            inferred
        }
        val positions by PlaybackPositionRepository.positions.collectAsState()

        Text(
            text = stringResource(Res.string.detail_episodes),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
            seasonSection(
                seasons = seasons,
                expanded = expanded,
                onSeasonToggled = {},
                watchedIds = watchedIds,
                positions = positions,
                onChooseSource = { videoId ->
                    currentMeta.videos.firstOrNull { it.id == videoId }?.let(onSelectEpisode)
                },
                onToggleWatched = { video ->
                    EpisodeWatchedRepository.setWatched(currentMeta.id, video, video.id !in watchedIds)
                },
            )
        }
    }
}

@Composable
private fun SeasonHeader(
    season: Int,
    episodeCount: Int,
    watched: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (season == SpecialsSeason) {
                    stringResource(Res.string.detail_specials)
                } else {
                    stringResource(Res.string.detail_season, season)
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$watched/$episodeCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // A finished season says so with a tick rather than a full-width bar. A bar
            // pinned at 100% on every season a viewer has ever completed is a row of
            // identical lines carrying no information.
            if (episodeCount > 0 && watched == episodeCount) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(Res.string.watched),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
            )
        }
        // Only where the bar means something: part way through. Untouched seasons and
        // finished ones both draw nothing here.
        if (watched in 1 until episodeCount) {
            LinearProgressIndicator(
                progress = { watched.toFloat() / episodeCount },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    video: Video,
    watched: Boolean,
    /** 0..1 where playback stopped part way, null when there is nothing to resume. */
    progress: Float?,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (video.thumbnail != null) {
                    AsyncImage(
                        model = video.thumbnail,
                        contentDescription = null,
                        // Some addons ship a spoiler-blurred still for an episode nobody
                        // has watched yet. This app has no say over that image itself, so
                        // it applies its own blur before watched and lifts it after —
                        // driven by this device's own watched state either way.
                        modifier = Modifier.fillMaxSize()
                            .then(if (watched) Modifier else Modifier.blur(EpisodeSpoilerBlur)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val season = video.season
                val episode = video.episode
                if (season != null && episode != null) {
                    Text(
                        text = "S${pad(season)} | E${pad(episode)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Text(
                    text = video.title ?: stringResource(Res.string.detail_episode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            // A dedicated touch target, nested inside the row's own click target. Compose
            // resolves nested clickables front-to-back, so pressing the tick box toggles
            // watched state instead of also opening the sources sheet underneath it.
            IconButton(onClick = onToggleWatched) {
                Icon(
                    imageVector = if (watched) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.CheckCircle
                    },
                    contentDescription = stringResource(
                        if (watched) Res.string.watched else Res.string.not_watched,
                    ),
                    tint = if (watched) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        // Only for an episode stopped part way. A finished one has its tick and an untouched
        // one has nothing to say, so a bar on either would be decoration.
        if (progress != null && progress > 0f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

/** Heavy enough to hide a frame, not so heavy it reads as a broken image. */
private val EpisodeSpoilerBlur = 18.dp
