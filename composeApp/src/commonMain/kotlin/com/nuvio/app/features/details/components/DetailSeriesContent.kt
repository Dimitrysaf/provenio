package com.nuvio.app.features.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.i18n.localizedSeasonEpisodeCode
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.details.SPECIALS_SEASON_NUMBER
import com.nuvio.app.features.details.isReleasedBy
import com.nuvio.app.features.details.seasonSortKey
import com.nuvio.app.features.home.components.HeroOnArtworkColor
import com.nuvio.app.features.settings.segmentShape
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.buildPlaybackVideoId
import com.nuvio.app.features.watching.application.WatchingState
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Only the default season starts open; a season the user toggles keeps their choice.
internal class EpisodeSeasonExpansion(initialOverrides: Map<Int, Boolean>) {
    var overrides by mutableStateOf(initialOverrides)
        private set

    fun isExpanded(season: Int, defaultSeason: Int?): Boolean =
        overrides[season] ?: (season == defaultSeason)

    fun toggle(season: Int, defaultSeason: Int?) {
        overrides = overrides + (season to !isExpanded(season, defaultSeason))
    }
}

@Composable
internal fun rememberEpisodeSeasonExpansion(key: Any?): EpisodeSeasonExpansion =
    rememberSaveable(key, saver = EpisodeSeasonExpansionSaver) { EpisodeSeasonExpansion(emptyMap()) }

private val EpisodeSeasonExpansionSaver = listSaver<EpisodeSeasonExpansion, Int>(
    save = { expansion ->
        expansion.overrides.flatMap { (season, open) -> listOf(season, if (open) 1 else 0) }
    },
    restore = { saved ->
        EpisodeSeasonExpansion(saved.chunked(2).associate { (season, open) -> season to (open == 1) })
    },
)

internal class EpisodeWatchState(
    val isWatched: Boolean,
    val inProgress: WatchProgressEntry?,
)

internal fun episodeWatchState(
    meta: MetaDetails,
    episode: MetaVideo,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
): EpisodeWatchState {
    val videoId = buildPlaybackVideoId(
        parentMetaId = meta.id,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
        fallbackVideoId = episode.id,
    )
    val progressEntry = progressByVideoId[videoId]
    val isWatched = progressEntry?.isEffectivelyCompleted == true ||
        WatchingState.isEpisodeWatched(
            watchedKeys = watchedKeys,
            metaType = meta.type,
            metaId = meta.id,
            episode = episode,
        )
    return EpisodeWatchState(
        isWatched = isWatched,
        inProgress = progressEntry?.takeIf { !isWatched && it.durationMs > 0L && !it.isCompleted },
    )
}

internal data class EpisodeSeasonSummary(
    val completedSeasons: Set<Int>,
    val defaultSeason: Int?,
)

// The open season is the one being watched, else the furthest season started (or the one after it once finished).
internal fun summarizeEpisodeSeasons(
    groupedEpisodes: Map<Int, List<MetaVideo>>,
    todayIsoDate: String,
    watchState: (MetaVideo) -> EpisodeWatchState,
): EpisodeSeasonSummary {
    val seasons = groupedEpisodes.keys.sortedBy(::seasonSortKey)
    val completed = mutableSetOf<Int>()
    val started = mutableSetOf<Int>()
    var watchingSeason: Int? = null
    var watchingUpdatedAt = Long.MIN_VALUE

    seasons.forEach { season ->
        var aired = 0
        var watchedAired = 0
        groupedEpisodes.getValue(season).forEach { episode ->
            val state = watchState(episode)
            if (state.isWatched || state.inProgress != null) started += season
            state.inProgress?.let { entry ->
                if (watchingSeason == null || entry.lastUpdatedEpochMs > watchingUpdatedAt) {
                    watchingSeason = season
                    watchingUpdatedAt = entry.lastUpdatedEpochMs
                }
            }
            if (episode.isReleasedBy(todayIsoDate)) {
                aired++
                if (state.isWatched) watchedAired++
            }
        }
        if (aired > 0 && watchedAired == aired) completed += season
    }

    val regularSeasons = seasons.filter { it > SPECIALS_SEASON_NUMBER }.ifEmpty { seasons }
    val furthestStarted = regularSeasons.lastOrNull { it in started }
    val defaultSeason = watchingSeason ?: when {
        furthestStarted == null -> regularSeasons.firstOrNull()
        furthestStarted !in completed -> furthestStarted
        else -> regularSeasons.getOrNull(regularSeasons.indexOf(furthestStarted) + 1)
    }
    return EpisodeSeasonSummary(completedSeasons = completed, defaultSeason = defaultSeason)
}

// Season rows and episode rows flattened into one segmented list, like the Streams sheet.
internal sealed interface EpisodeListEntry {
    val key: String

    data class Season(
        override val key: String,
        val season: Int,
        val episodeCount: Int,
        val expanded: Boolean,
        val completed: Boolean,
    ) : EpisodeListEntry

    data class Episode(
        override val key: String,
        val episode: MetaVideo,
    ) : EpisodeListEntry
}

internal fun buildEpisodeListEntries(
    groupedEpisodes: Map<Int, List<MetaVideo>>,
    expandedSeasons: Set<Int>,
    completedSeasons: Set<Int> = emptySet(),
): List<EpisodeListEntry> = buildList {
    groupedEpisodes.keys.sortedBy(::seasonSortKey).forEach { season ->
        val episodes = groupedEpisodes.getValue(season)
        if (episodes.isEmpty()) return@forEach
        val expanded = season in expandedSeasons
        add(
            EpisodeListEntry.Season(
                key = "season-$season",
                season = season,
                episodeCount = episodes.size,
                expanded = expanded,
                completed = season in completedSeasons,
            ),
        )
        if (!expanded) return@forEach
        episodes.forEachIndexed { index, episode ->
            add(
                EpisodeListEntry.Episode(
                    key = "episode-$season-${episode.episode}-${episode.id}-$index",
                    episode = episode,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailEpisodeListRow(
    entry: EpisodeListEntry,
    index: Int,
    count: Int,
    meta: MetaDetails,
    todayIsoDate: String,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    blurUnwatchedEpisodes: Boolean,
    onSeasonClick: (Int) -> Unit,
    onSeasonLongPress: ((Int) -> Unit)?,
    onEpisodeClick: ((MetaVideo) -> Unit)?,
    onEpisodeLongPress: ((MetaVideo) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = segmentShape(index = index, count = count)
    val shapes = ListItemDefaults.shapes(
        shape = shape,
        selectedShape = shape,
        pressedShape = shape,
        focusedShape = shape,
        hoveredShape = shape,
    )

    when (entry) {
        is EpisodeListEntry.Season -> SeasonRow(
            label = if (meta.type != "series" && entry.season <= 0) {
                stringResource(Res.string.details_videos)
            } else if (entry.season <= 0) {
                stringResource(Res.string.episodes_specials)
            } else {
                stringResource(Res.string.episodes_season, entry.season)
            },
            episodeCount = entry.episodeCount,
            expanded = entry.expanded,
            completed = entry.completed,
            shapes = shapes,
            onClick = { onSeasonClick(entry.season) },
            onLongClick = onSeasonLongPress?.let { handler -> { handler(entry.season) } },
            modifier = modifier,
        )

        is EpisodeListEntry.Episode -> {
            val episode = entry.episode
            val watchState = episodeWatchState(meta, episode, progressByVideoId, watchedKeys)
            val status = when {
                watchState.isWatched -> EpisodeStatus.Watched
                !episode.isReleasedBy(todayIsoDate) -> EpisodeStatus.Unaired
                else -> null
            }
            EpisodeRow(
                episode = episode,
                imageUrl = episode.thumbnail ?: meta.background ?: meta.poster,
                status = status,
                progress = watchState.inProgress?.progressFraction,
                blurArtwork = blurUnwatchedEpisodes && !watchState.isWatched,
                shape = shape,
                shapes = shapes,
                onClick = { onEpisodeClick?.invoke(episode) },
                onLongClick = onEpisodeLongPress?.let { handler -> { handler(episode) } },
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SeasonRow(
    label: String,
    episodeCount: Int,
    expanded: Boolean,
    completed: Boolean,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "season_chevron",
    )

    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier.fillMaxWidth(),
        onLongClick = onLongClick,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = episodeCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (completed) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(Res.string.episodes_cd_watched),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodeRow(
    episode: MetaVideo,
    imageUrl: String?,
    status: EpisodeStatus?,
    progress: Float?,
    blurArtwork: Boolean,
    shape: Shape,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val episodeCode = localizedSeasonEpisodeCode(
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
    )

    Box(modifier = modifier.fillMaxWidth().clip(shape)) {
        SegmentedListItem(
            selected = progress != null,
            onClick = onClick,
            shapes = shapes,
            modifier = Modifier.fillMaxWidth(),
            onLongClick = onLongClick,
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            leadingContent = {
                EpisodeThumbnail(
                    imageUrl = imageUrl,
                    status = status,
                    blurArtwork = blurArtwork,
                )
            },
            overlineContent = episodeCode?.let { code ->
                {
                    Text(
                        text = code,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            },
        ) {
            Text(
                text = episode.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(EpisodeProgressHeight),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun EpisodeThumbnail(
    imageUrl: String?,
    status: EpisodeStatus?,
    blurArtwork: Boolean,
) {
    Box(
        modifier = Modifier
            .width(EpisodeThumbnailWidth)
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .then(if (blurArtwork) Modifier.blur(12.dp) else Modifier),
                contentScale = ContentScale.Crop,
            )
        }
        if (status != null) {
            Box(
                modifier = Modifier
                    .size(EpisodeStatusSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = status.icon,
                    contentDescription = stringResource(status.label),
                    modifier = Modifier.size(EpisodeStatusIconSize),
                    tint = HeroOnArtworkColor,
                )
            }
        }
    }
}

private enum class EpisodeStatus(val icon: ImageVector, val label: StringResource) {
    Watched(Icons.Rounded.Check, Res.string.episodes_cd_watched),
    Unaired(Icons.Rounded.Timer, Res.string.player_next_episode_unaired),
}

private val EpisodeThumbnailWidth = 128.dp
private val EpisodeStatusSize = 36.dp
private val EpisodeStatusIconSize = 20.dp
private val EpisodeProgressHeight = 4.dp
