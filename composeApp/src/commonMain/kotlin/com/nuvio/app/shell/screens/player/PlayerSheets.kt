package com.nuvio.app.shell.screens.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.components.NuvioModalBottomSheet
import com.nuvio.app.shell.components.SingleChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption
import com.nuvio.app.shell.components.dismissNuvioBottomSheet
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.core.debrid.DebridSettingsRepository
import com.nuvio.app.core.metadata.MetaDetails
import com.nuvio.app.core.metadata.MetaVideo
import com.nuvio.app.shell.screens.details.components.DetailEpisodeListRow
import com.nuvio.app.shell.screens.details.components.buildEpisodeListEntries
import com.nuvio.app.shell.screens.details.components.episodeWatchState
import com.nuvio.app.shell.screens.details.components.rememberEpisodeSeasonExpansion
import com.nuvio.app.shell.screens.details.components.summarizeEpisodeSeasons
import com.nuvio.app.core.metadata.groupedEpisodesForDisplay
import com.nuvio.app.shell.screens.settings.ListItemBetweenSpace
import com.nuvio.app.shell.screens.settings.segmentShape
import com.nuvio.app.shell.screens.streams.ActiveStreamStore
import com.nuvio.app.shell.screens.streams.LocalStreamSizeLabelFormat
import com.nuvio.app.core.streams.StreamBadgeSettingsRepository
import com.nuvio.app.core.streams.StreamItem
import com.nuvio.app.shell.screens.streams.StreamsEmptyBlock
import com.nuvio.app.shell.screens.streams.StreamsHorizontalPadding
import com.nuvio.app.shell.screens.streams.StreamsPreparingBlock
import com.nuvio.app.core.streams.StreamsUiState
import com.nuvio.app.shell.screens.streams.rememberStreamGroupExpansion
import com.nuvio.app.shell.screens.streams.rememberStreamSizeLabelFormat
import com.nuvio.app.shell.screens.streams.streamGroups
import com.nuvio.app.core.watch.progress.CurrentDateProvider
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import com.nuvio.app.core.playback.AddonSubtitle
import com.nuvio.app.core.playback.AudioTrack
import com.nuvio.app.core.playback.PlayerResizeMode
import com.nuvio.app.core.playback.SUBTITLE_DELAY_MAX_MS
import com.nuvio.app.core.playback.SUBTITLE_DELAY_MIN_MS
import com.nuvio.app.core.playback.SubtitleLanguageItem
import com.nuvio.app.core.playback.SubtitleOffLanguageKey
import com.nuvio.app.core.playback.SubtitleSelectionOption
import com.nuvio.app.core.playback.SubtitleTrack
import com.nuvio.app.core.playback.SubtitleUnknownLanguageKey
import com.nuvio.app.core.playback.buildSubtitleLanguageItems
import com.nuvio.app.core.playback.buildSubtitleSelectionOptions
import com.nuvio.app.core.playback.findSelectedAddon
import com.nuvio.app.core.playback.labelRes

@Composable
internal fun PlayerAudioSheet(
    audioTracks: List<AudioTrack>,
    selectedIndex: Int,
    onTrackSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.compose_player_audio_tracks),
        options = audioTracks.map { track ->
            SingleChoiceOption(
                value = track.index,
                label = localizedTrackDisplayName(track.label, track.language, track.index),
                supportingText = track.language
                    ?.takeIf { it.isNotBlank() && it != "und" }
                    ?.let { languageLabelForCode(it) },
            )
        },
        isSelected = { it == selectedIndex },
        onSelected = onTrackSelected,
        onDismiss = onDismiss,
        description = if (audioTracks.isEmpty()) {
            stringResource(Res.string.compose_player_no_audio_tracks_available)
        } else {
            null
        },
    )
}

internal val PlayerPlaybackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@Composable
internal fun PlayerSpeedSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.player_playback_speed),
        options = PlayerPlaybackSpeeds.map { speed ->
            SingleChoiceOption(value = speed, label = formatPlaybackSpeedLabel(speed))
        },
        isSelected = { abs(it - currentSpeed) < 0.01f },
        onSelected = onSpeedSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun PlayerResizeSheet(
    currentMode: PlayerResizeMode,
    onModeSelected: (PlayerResizeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.player_video_fit),
        options = PlayerResizeMode.entries.map { mode ->
            SingleChoiceOption(value = mode, label = stringResource(mode.labelRes))
        },
        isSelected = { it == currentMode },
        onSelected = onModeSelected,
        onDismiss = onDismiss,
    )
}

// The streams for what is playing, the same segmented list the Streams sheet shows.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerStreamsSheet(
    title: String,
    subtitle: String?,
    streamsUiState: StreamsUiState,
    isStreamSelected: (StreamItem) -> Boolean,
    onStreamSelected: (StreamItem) -> Unit,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    NuvioModalBottomSheet(
        onDismissRequest = { scope.launch { dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss) } },
        sheetState = sheetState,
        fullHeight = true,
    ) {
        PlayerSheetHeader(
            title = title,
            subtitle = subtitle,
            onReload = onReload,
            reloadEnabled = !streamsUiState.isAnyLoading,
        )
        HorizontalDivider()
        PlayerStreamGroupsList(
            streamsUiState = streamsUiState,
            isStreamSelected = isStreamSelected,
            onStreamSelected = onStreamSelected,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

// Episodes as the Details page lists them; picking one shows its streams in the same sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerEpisodesSheet(
    title: String,
    parentMetaId: String,
    parentMetaType: String,
    poster: String?,
    background: String?,
    episodes: List<MetaVideo>,
    currentSeason: Int?,
    currentVideoId: String?,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    blurUnwatchedEpisodes: Boolean,
    episodeStreams: EpisodeStreamsPanelState,
    onEpisodeSelected: (MetaVideo) -> Unit,
    onEpisodeStreamSelected: (StreamItem, MetaVideo) -> Unit,
    onBackToEpisodes: () -> Unit,
    onReloadEpisodeStreams: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val selectedEpisode = episodeStreams.selectedEpisode
    NuvioModalBottomSheet(
        onDismissRequest = { scope.launch { dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss) } },
        sheetState = sheetState,
        fullHeight = true,
    ) {
        if (episodeStreams.showStreams && selectedEpisode != null) {
            PlayerSheetHeader(
                title = selectedEpisode.title,
                subtitle = episodeCodeLabel(selectedEpisode),
                onBack = onBackToEpisodes,
                onReload = onReloadEpisodeStreams,
                reloadEnabled = !episodeStreams.streamsUiState.isAnyLoading,
            )
            HorizontalDivider()
            PlayerStreamGroupsList(
                streamsUiState = episodeStreams.streamsUiState,
                isStreamSelected = { stream -> ActiveStreamStore.isActive(selectedEpisode.id, stream) },
                onStreamSelected = { stream -> onEpisodeStreamSelected(stream, selectedEpisode) },
                modifier = Modifier.weight(1f, fill = false),
            )
        } else {
            PlayerSheetHeader(title = stringResource(Res.string.compose_player_episodes), subtitle = title)
            HorizontalDivider()
            PlayerEpisodeList(
                meta = MetaDetails(
                    id = parentMetaId,
                    type = parentMetaType,
                    name = title,
                    poster = poster,
                    background = background,
                    videos = episodes,
                ),
                currentSeason = currentSeason,
                currentVideoId = currentVideoId,
                progressByVideoId = progressByVideoId,
                watchedKeys = watchedKeys,
                blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                onEpisodeSelected = onEpisodeSelected,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun PlayerEpisodeList(
    meta: MetaDetails,
    currentSeason: Int?,
    currentVideoId: String?,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    blurUnwatchedEpisodes: Boolean,
    onEpisodeSelected: (MetaVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayIsoDate = CurrentDateProvider.todayIsoDate()
    val grouped = remember(meta.videos, meta.type) { meta.groupedEpisodesForDisplay() }
    val summary = remember(meta, grouped, progressByVideoId, watchedKeys, todayIsoDate) {
        summarizeEpisodeSeasons(grouped, todayIsoDate) { episode ->
            episodeWatchState(meta, episode, progressByVideoId, watchedKeys)
        }
    }
    // The season playing now is the one open, falling back to the Details page rule.
    val defaultSeason = currentSeason?.takeIf { it in grouped } ?: summary.defaultSeason
    val expansion = rememberEpisodeSeasonExpansion(meta.id)
    val expandedSeasons = grouped.keys.filter { expansion.isExpanded(it, defaultSeason) }.toSet()
    val entries = remember(grouped, expandedSeasons, summary.completedSeasons) {
        buildEpisodeListEntries(grouped, expandedSeasons, summary.completedSeasons)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 8.dp, bottom = nuvioSafeBottomPadding(16.dp)),
    ) {
        itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
            DetailEpisodeListRow(
                entry = entry,
                index = index,
                count = entries.size,
                meta = meta,
                todayIsoDate = todayIsoDate,
                progressByVideoId = progressByVideoId,
                watchedKeys = watchedKeys,
                blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                onSeasonClick = { season -> expansion.toggle(season, defaultSeason) },
                onSeasonLongPress = null,
                onEpisodeClick = onEpisodeSelected,
                onEpisodeLongPress = null,
                modifier = Modifier
                    .padding(horizontal = StreamsHorizontalPadding)
                    .padding(bottom = if (index == entries.lastIndex) 0.dp else ListItemBetweenSpace)
                    .animateItem(),
                currentEpisodeId = currentVideoId,
            )
        }
    }
}

@Composable
private fun PlayerStreamGroupsList(
    streamsUiState: StreamsUiState,
    isStreamSelected: (StreamItem) -> Boolean,
    onStreamSelected: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val debridSettings by remember {
        DebridSettingsRepository.ensureLoaded()
        DebridSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val badgeSettings by remember {
        StreamBadgeSettingsRepository.ensureLoaded()
        StreamBadgeSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val expansion = rememberStreamGroupExpansion(streamsUiState.requestToken)
    val formatStreamSize = rememberStreamSizeLabelFormat()
    val torrentNotSupportedText = stringResource(Res.string.streams_torrent_not_supported)
    val hasVisibleRows = streamsUiState.groups.any { it.streams.isNotEmpty() || it.isLoading }

    CompositionLocalProvider(LocalStreamSizeLabelFormat provides formatStreamSize) {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = nuvioSafeBottomPadding(16.dp)),
        ) {
            when {
                !hasVisibleRows && streamsUiState.isAnyLoading -> {
                    item(key = "player_streams_preparing") { StreamsPreparingBlock() }
                }
                !hasVisibleRows -> {
                    item(key = "player_streams_empty") { StreamsEmptyBlock(reason = streamsUiState.emptyStateReason) }
                }
                else -> streamGroups(
                    groups = streamsUiState.groups,
                    expansion = expansion,
                    debridEnabled = debridSettings.canResolvePlayableLinks,
                    appendInstantServiceToDefaultName = debridSettings.canResolvePlayableLinks &&
                        !debridSettings.hasCustomStreamFormatting,
                    showFileSizeBadges = badgeSettings.showFileSizeBadges,
                    showAddonLogo = badgeSettings.showAddonLogo,
                    badgePlacement = badgeSettings.badgePlacement,
                    isStreamSelected = isStreamSelected,
                    torrentNotSupportedText = torrentNotSupportedText,
                    onStreamSelected = onStreamSelected,
                    onStreamLongPress = {},
                    horizontalPadding = StreamsHorizontalPadding,
                )
            }
        }
    }
}

@Composable
private fun PlayerSheetHeader(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)? = null,
    onReload: (() -> Unit)? = null,
    reloadEnabled: Boolean = true,
    onFetch: (() -> Unit)? = null,
    fetching: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (onBack == null) StreamsHorizontalPadding else 4.dp, end = 8.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onReload != null) {
            IconButton(onClick = onReload, enabled = reloadEnabled) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.streams_refresh),
                )
            }
        }
        if (onFetch != null) {
            // The fetch button turns into a progress indicator while fetching.
            Crossfade(targetState = fetching, label = "player_sheet_fetch") { isFetching ->
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    if (isFetching) {
                        NuvioLoadingIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = onFetch) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = stringResource(Res.string.compose_player_fetch_subtitles),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun episodeCodeLabel(episode: MetaVideo): String? {
    val season = episode.season ?: return null
    val number = episode.episode ?: return null
    return stringResource(Res.string.compose_player_episode_code_full, season, number)
}

// Subtitles as one segmented list: off, then each language folding open to its tracks, then delay.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayerSubtitlesSheet(
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleIndex: Int,
    addonSubtitles: List<AddonSubtitle>,
    selectedAddonSubtitleId: String?,
    isLoadingAddonSubtitles: Boolean,
    preferredSubtitleLanguage: String,
    secondaryPreferredSubtitleLanguage: String?,
    showOnlyPreferredLanguages: Boolean,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleDelayMs: Int,
    onBuiltInTrackSelected: (Int) -> Unit,
    onAddonSubtitleSelected: (AddonSubtitle) -> Unit,
    onFetchAddonSubtitles: () -> Unit,
    onSubtitleDelayChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val effectiveSelectedAddon = selectedAddonSubtitle ?: addonSubtitles.findSelectedAddon(selectedAddonSubtitleId)
    val playbackLanguageKey = selectedSubtitleLanguageKey(
        subtitleTracks = subtitleTracks,
        selectedSubtitleIndex = selectedSubtitleIndex,
        selectedAddonSubtitle = effectiveSelectedAddon,
    )
    val playbackOptionId = selectedSubtitleOptionId(
        subtitleTracks = subtitleTracks,
        selectedSubtitleIndex = selectedSubtitleIndex,
        selectedAddonSubtitle = effectiveSelectedAddon,
    )
    val languages = buildSubtitleLanguageItems(
        subtitleTracks = subtitleTracks,
        addonSubtitles = addonSubtitles,
        preferredLanguage = preferredSubtitleLanguage,
        secondaryPreferredLanguage = secondaryPreferredSubtitleLanguage,
        showOnlyPreferredLanguages = showOnlyPreferredLanguages,
        selectedLanguageKey = playbackLanguageKey,
    ).filter { it.key != SubtitleOffLanguageKey }
    var expandedLanguages by remember { mutableStateOf(setOf(playbackLanguageKey)) }

    val entries = buildList {
        add(SubtitleSheetEntry.Off)
        languages.forEach { language ->
            val expanded = language.key in expandedLanguages
            add(SubtitleSheetEntry.Language(language, expanded))
            if (expanded) {
                buildSubtitleSelectionOptions(language.key, subtitleTracks, addonSubtitles)
                    .forEach { add(SubtitleSheetEntry.Option(it)) }
            }
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = { scope.launch { dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss) } },
        sheetState = sheetState,
        fullHeight = true,
    ) {
        PlayerSheetHeader(
            title = stringResource(Res.string.compose_player_subtitles),
            subtitle = null,
            onFetch = onFetchAddonSubtitles,
            fetching = isLoadingAddonSubtitles,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            contentPadding = PaddingValues(
                start = StreamsHorizontalPadding,
                end = StreamsHorizontalPadding,
                top = 8.dp,
                bottom = nuvioSafeBottomPadding(16.dp),
            ),
        ) {
            itemsIndexed(entries, key = { index, entry -> entry.key(index) }) { index, entry ->
                val shapes = playerSegmentShapes(index, entries.size)
                val rowModifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (index == entries.lastIndex) 0.dp else ListItemBetweenSpace)
                when (entry) {
                    SubtitleSheetEntry.Off -> SegmentedListItem(
                        selected = playbackLanguageKey == SubtitleOffLanguageKey,
                        onClick = { onBuiltInTrackSelected(-1) },
                        shapes = shapes,
                        modifier = rowModifier,
                        colors = playerSheetRowColors(),
                    ) {
                        Text(stringResource(Res.string.compose_player_none))
                    }
                    is SubtitleSheetEntry.Language -> SubtitleLanguageHeader(
                        item = entry.item,
                        expanded = entry.expanded,
                        shapes = shapes,
                        onClick = {
                            expandedLanguages = if (entry.expanded) {
                                expandedLanguages - entry.item.key
                            } else {
                                expandedLanguages + entry.item.key
                            }
                        },
                        modifier = rowModifier,
                    )
                    is SubtitleSheetEntry.Option -> SubtitleOptionItem(
                        option = entry.option,
                        selected = entry.option.id == playbackOptionId,
                        shapes = shapes,
                        onClick = {
                            when (val option = entry.option) {
                                is SubtitleSelectionOption.BuiltIn -> onBuiltInTrackSelected(option.track.index)
                                is SubtitleSelectionOption.Addon -> onAddonSubtitleSelected(option.subtitle)
                            }
                        },
                        modifier = rowModifier,
                    )
                }
            }

            item(key = "subtitle_delay") {
                SubtitleDelayRow(
                    delayMs = subtitleDelayMs,
                    onDelayChanged = onSubtitleDelayChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        }
    }
}

private sealed interface SubtitleSheetEntry {
    fun key(index: Int): String

    data object Off : SubtitleSheetEntry {
        override fun key(index: Int) = "off"
    }

    data class Language(val item: SubtitleLanguageItem, val expanded: Boolean) : SubtitleSheetEntry {
        override fun key(index: Int) = "language:${item.key}"
    }

    data class Option(val option: SubtitleSelectionOption) : SubtitleSheetEntry {
        override fun key(index: Int) = "option:${option.id}:$index"
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SubtitleLanguageHeader(
    item: SubtitleLanguageItem,
    expanded: Boolean,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "subtitle_language_chevron")
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(
            text = if (item.key == SubtitleUnknownLanguageKey) {
                stringResource(Res.string.subtitle_language_unknown)
            } else {
                languageLabelForCode(item.key)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SubtitleOptionItem(
    option: SubtitleSelectionOption,
    selected: Boolean,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source: String
    val title: String
    val detail: String?
    when (option) {
        is SubtitleSelectionOption.BuiltIn -> {
            source = stringResource(Res.string.compose_player_built_in)
            title = localizedTrackDisplayName(option.track.label, option.track.language, option.track.index)
            detail = if (option.track.isForced) stringResource(Res.string.settings_playback_option_forced) else null
        }
        is SubtitleSelectionOption.Addon -> {
            source = option.subtitle.addonName ?: stringResource(Res.string.addon_title)
            title = languageLabelForCode(option.subtitle.language)
            detail = option.subtitle.display.takeIf { it.isNotBlank() && it != title }
        }
    }
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        colors = playerSheetRowColors(),
        overlineContent = { Text(source, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = detail?.let { { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } },
    ) {
        Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SubtitleDelayRow(
    delayMs: Int,
    onDelayChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = segmentShape(0, 1)
    Box(modifier = modifier) {
        SegmentedListItem(
            onClick = { onDelayChanged(0) },
            shapes = ListItemDefaults.shapes(
                shape = shape,
                selectedShape = shape,
                pressedShape = shape,
                focusedShape = shape,
                hoveredShape = shape,
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = playerSheetRowColors(),
            supportingContent = { Text(formatSubtitleDelay(delayMs)) },
            trailingContent = {
                Row {
                    IconButton(
                        onClick = {
                            onDelayChanged((delayMs - SUBTITLE_DELAY_STEP_MS).coerceAtLeast(SUBTITLE_DELAY_MIN_MS))
                        },
                    ) {
                        Icon(imageVector = Icons.Rounded.Remove, contentDescription = null)
                    }
                    IconButton(
                        onClick = {
                            onDelayChanged((delayMs + SUBTITLE_DELAY_STEP_MS).coerceAtMost(SUBTITLE_DELAY_MAX_MS))
                        },
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                    }
                }
            },
        ) {
            Text(stringResource(Res.string.compose_player_subtitle_delay))
        }
    }
}

private fun formatSubtitleDelay(delayMs: Int): String {
    val sign = when {
        delayMs > 0 -> "+"
        delayMs < 0 -> "-"
        else -> ""
    }
    val absolute = abs(delayMs)
    return "$sign${absolute / 1000}.${(absolute % 1000) / 100} s"
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun playerSegmentShapes(index: Int, count: Int): ListItemShapes {
    val shape = segmentShape(index = index, count = count)
    return ListItemDefaults.shapes(
        shape = shape,
        selectedShape = shape,
        pressedShape = shape,
        focusedShape = shape,
        hoveredShape = shape,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun playerSheetRowColors() = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
)

data class EpisodeStreamsPanelState(
    val showStreams: Boolean = false,
    val selectedEpisode: MetaVideo? = null,
    val streamsUiState: StreamsUiState = StreamsUiState(),
)

internal fun StreamItem.isCurrentPlayerStream(
    currentUrl: String?,
    currentName: String?,
): Boolean {
    if (!currentUrl.isNullOrBlank() && playableDirectUrl == currentUrl) return true
    return !currentName.isNullOrBlank() && streamLabel.equals(currentName, ignoreCase = true) &&
        playableDirectUrl == currentUrl
}
