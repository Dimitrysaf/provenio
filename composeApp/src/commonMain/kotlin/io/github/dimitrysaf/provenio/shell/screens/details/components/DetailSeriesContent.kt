package io.github.dimitrysaf.provenio.shell.screens.details.components

import io.github.dimitrysaf.provenio.shell.components.ShapedArtworkImage
import io.github.dimitrysaf.provenio.shell.components.StatusOverlay
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.screens.watchprogress.computeAirDateText
import io.github.dimitrysaf.provenio.shell.components.onSecondaryClick
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.i18n.localizedSeasonEpisodeCode
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.metadata.SPECIALS_SEASON_NUMBER
import io.github.dimitrysaf.provenio.core.metadata.isReleasedBy
import io.github.dimitrysaf.provenio.core.metadata.seasonSortKey
import io.github.dimitrysaf.provenio.shell.screens.settings.InnerCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.buildPlaybackVideoId
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import provenio.composeapp.generated.resources.*
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
        val posterUrl: String? = null,
        val name: String? = null,
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
                posterUrl = episodes.firstNotNullOfOrNull { it.seasonPoster?.takeIf(String::isNotBlank) },
                name = episodes.firstNotNullOfOrNull { it.seasonName?.takeIf(String::isNotBlank) },
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

/** The season buttons layout: a row item per season, the selected one's episodes as a plain list. */
internal class SeasonSelection(
    val seasons: List<EpisodeListEntry.Season>,
    val selectedSeason: Int,
    val episodes: List<EpisodeListEntry.Episode>,
)

internal fun buildSeasonSelection(
    groupedEpisodes: Map<Int, List<MetaVideo>>,
    selectedSeason: Int?,
    completedSeasons: Set<Int>,
): SeasonSelection? {
    val seasons = buildEpisodeListEntries(groupedEpisodes, expandedSeasons = emptySet(), completedSeasons = completedSeasons)
        .filterIsInstance<EpisodeListEntry.Season>()
    if (seasons.isEmpty()) return null
    val selected = selectedSeason?.takeIf { season -> seasons.any { it.season == season } } ?: seasons.first().season
    val episodes = buildEpisodeListEntries(
        groupedEpisodes = mapOf(selected to groupedEpisodes.getValue(selected)),
        expandedSeasons = setOf(selected),
    ).filterIsInstance<EpisodeListEntry.Episode>()
    return SeasonSelection(seasons = seasons, selectedSeason = selected, episodes = episodes)
}

/**
 * The seasons as a scrolling, horizontally segmented row of the same list items the List layout
 * uses: each with its poster, the selected one taking the list item's selected shape and colours.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SeasonItemRow(
    selection: SeasonSelection,
    meta: MetaDetails,
    onSeasonSelected: (Int) -> Unit,
    onSeasonLongPress: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val seasons = selection.seasons
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = seasons.indexOfFirst { it.season == selection.selectedSeason }.coerceAtLeast(0),
    )
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        itemsIndexed(seasons, key = { _, season -> season.key }) { index, season ->
            val label = seasonLabel(meta, season.season)
            val name = season.name?.takeIf { season.season > 0 && !isGenericSeasonName(it, season.season) }
            val posterUrl = season.posterUrl ?: meta.seasonPosters[season.season]
            val shape = horizontalSegmentShape(index = index, count = seasons.size)
            val longPress = onSeasonLongPress?.let { handler -> { handler(season.season) } }
            SegmentedListItem(
                selected = season.season == selection.selectedSeason,
                onClick = { onSeasonSelected(season.season) },
                onLongClick = longPress,
                shapes = ListItemDefaults.shapes(
                    shape = shape,
                    selectedShape = RoundedCornerShape(OuterCorner),
                    pressedShape = shape,
                    focusedShape = shape,
                    hoveredShape = shape,
                ),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.width(SeasonItemWidth).onSecondaryClick(longPress),
                // The poster tops the item, full width, with the episodes' watched mark over it;
                // the season's title sits below it.
                overlineContent = posterUrl?.let { url ->
                    {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            ShapedArtworkImage(
                                candidates = listOf(url),
                                contentDescription = null,
                                letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.matchParentSize(),
                            )
                            StatusOverlay(
                                visible = season.completed,
                                icon = EpisodeStatus.Watched.icon,
                                contentDescription = stringResource(EpisodeStatus.Watched.label),
                            )
                        }
                    }
                },
                supportingContent = name?.let {
                    { FindableText(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                },
                trailingContent = if (season.completed && posterUrl == null) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(Res.string.episodes_cd_watched),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    null
                },
            ) {
                FindableText(
                    text = name ?: label,
                    maxLines = if (name != null) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// The row's segments join side by side: outer corners at its two ends, inner ones between.
private fun horizontalSegmentShape(index: Int, count: Int): RoundedCornerShape = when {
    count == 1 -> RoundedCornerShape(OuterCorner)
    index == 0 -> RoundedCornerShape(
        topStart = OuterCorner,
        bottomStart = OuterCorner,
        topEnd = InnerCorner,
        bottomEnd = InnerCorner,
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = InnerCorner,
        bottomStart = InnerCorner,
        topEnd = OuterCorner,
        bottomEnd = OuterCorner,
    )
    else -> RoundedCornerShape(InnerCorner)
}

@Composable
private fun seasonLabel(meta: MetaDetails, season: Int): String = when {
    meta.type != "series" && season <= 0 -> stringResource(Res.string.details_videos)
    season <= 0 -> stringResource(Res.string.episodes_specials)
    else -> stringResource(Res.string.episodes_season, season)
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
    currentEpisodeId: String? = null,
) {
    val shape = segmentShape(index = index, count = count)
    val shapes = ListItemDefaults.shapes(
        shape = shape,
        selectedShape = RoundedCornerShape(OuterCorner),
        pressedShape = shape,
        focusedShape = shape,
        hoveredShape = shape,
    )

    when (entry) {
        is EpisodeListEntry.Season -> SeasonRow(
            label = seasonLabel(meta, entry.season),
            name = entry.name?.takeIf { entry.season > 0 && !isGenericSeasonName(it, entry.season) },
            posterUrl = entry.posterUrl ?: meta.seasonPosters[entry.season],
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
                imageCandidates = listOf(episode.thumbnail, meta.background) + meta.extraArtwork + listOf(meta.poster),
                status = status,
                airDate = if (status == EpisodeStatus.Unaired) computeAirDateText(episode.released, todayIsoDate) else null,
                progress = watchState.inProgress?.progressFraction,
                selected = if (currentEpisodeId != null) {
                    episode.id == currentEpisodeId
                } else {
                    watchState.inProgress != null
                },
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
    name: String?,
    posterUrl: String?,
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
        modifier = modifier.fillMaxWidth().onSecondaryClick(onLongClick),
        onLongClick = onLongClick,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        leadingContent = posterUrl?.let { url ->
            {
                ShapedArtworkImage(
                    candidates = listOf(url),
                    contentDescription = null,
                    letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .width(SeasonPosterWidth)
                        .aspectRatio(2f / 3f)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        },
        // A season with a name of its own shows it under "Season 2", as an episode shows its title under S1E2.
        overlineContent = name?.let {
            {
                FindableText(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FindableText(
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
        FindableText(
            text = name ?: label,
            maxLines = if (name != null) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// TMDB names most seasons only "Season 2", in whatever language, which says nothing the row's
// own label does not. Only a name of its own, such as "The Long Night", is shown.
internal fun isGenericSeasonName(name: String, season: Int): Boolean =
    GenericSeasonName.matchEntire(name.trim())?.groupValues?.get(1)?.toIntOrNull() == season

private val GenericSeasonName = Regex("""\p{L}+\.?\s*(\d+)""")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodeRow(
    episode: MetaVideo,
    imageCandidates: List<String?>,
    status: EpisodeStatus?,
    airDate: String?,
    progress: Float?,
    selected: Boolean,
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

    val clipShape = if (selected) RoundedCornerShape(OuterCorner) else shape
    Box(modifier = modifier.fillMaxWidth().clip(clipShape)) {
        SegmentedListItem(
            selected = selected,
            onClick = onClick,
            shapes = shapes,
            modifier = Modifier.fillMaxWidth().onSecondaryClick(onLongClick),
            onLongClick = onLongClick,
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            leadingContent = {
                EpisodeThumbnail(
                    imageCandidates = imageCandidates,
                    status = status,
                    blurArtwork = blurArtwork,
                )
            },
            overlineContent = episodeCode?.let { code ->
                {
                    FindableText(
                        text = code,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            },
            supportingContent = airDate?.let { date ->
                {
                    FindableText(
                        text = date,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
        ) {
            FindableText(
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
    imageCandidates: List<String?>,
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
        ShapedArtworkImage(
            candidates = imageCandidates,
            contentDescription = null,
            letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .matchParentSize()
                .then(if (blurArtwork) Modifier.blur(12.dp) else Modifier),
        )
        StatusOverlay(
            visible = status != null,
            icon = (status ?: EpisodeStatus.Watched).icon,
            contentDescription = status?.let { stringResource(it.label) },
            badgeSize = EpisodeStatusSize,
        )
    }
}

private enum class EpisodeStatus(val icon: ImageVector, val label: StringResource) {
    Watched(Icons.Rounded.Check, Res.string.episodes_cd_watched),
    Unaired(Icons.Rounded.Timer, Res.string.player_next_episode_unaired),
}

private val EpisodeThumbnailWidth = 128.dp
private val SeasonPosterWidth = 40.dp
private val SeasonItemWidth = 136.dp
private val EpisodeStatusSize = 36.dp
private val EpisodeProgressHeight = 4.dp
