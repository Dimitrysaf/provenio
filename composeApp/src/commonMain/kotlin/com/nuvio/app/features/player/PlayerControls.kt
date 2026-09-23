package com.nuvio.app.features.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.nuvioTypeScale
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

// Torrent figures shown at the top right while a P2P stream plays.
internal data class PlayerTorrentStats(
    val seeds: Int,
    val peers: Int,
    val downloadSpeed: String,
    val downloadedPercent: Int,
)

@Composable
internal fun PlayerControlsShell(
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    resizeMode: PlayerResizeMode,
    isLocked: Boolean,
    showPlaybackControls: Boolean = true,
    onLockToggle: () -> Unit,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onResizeModeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onAudioClick: () -> Unit,
    onVideoSettingsClick: (() -> Unit)? = null,
    onSourcesClick: (() -> Unit)? = null,
    onEpisodesClick: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onCastClick: (() -> Unit)? = null,
    onOpenInExternalPlayer: (() -> Unit)? = null,
    onSubmitIntroClick: (() -> Unit)? = null,
    torrentStats: PlayerTorrentStats? = null,
    parentalWarnings: List<ParentalWarning> = emptyList(),
    showParentalGuide: Boolean = false,
    onParentalGuideAnimationComplete: () -> Unit = {},
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    horizontalSafePadding: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalSafePadding),
        ) {
            PlayerHeader(
                title = title,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                metrics = metrics,
                showDetails = showPlaybackControls,
                torrentStats = torrentStats,
                parentalWarnings = parentalWarnings,
                showParentalGuide = showParentalGuide,
                onParentalGuideAnimationComplete = onParentalGuideAnimationComplete,
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Top))
                    .padding(
                        start = metrics.horizontalPadding,
                        end = metrics.horizontalPadding,
                        top = metrics.verticalPadding / 4,
                    ),
            )

            if (showPlaybackControls) {
                CenterControls(
                    snapshot = playbackSnapshot,
                    metrics = metrics,
                    onSeekBack = onSeekBack,
                    onSeekForward = onSeekForward,
                    onTogglePlayback = onTogglePlayback,
                    onNextEpisode = onNextEpisode,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = metrics.centerLift),
                )

                BottomControls(
                    playbackSnapshot = playbackSnapshot,
                    displayedPositionMs = displayedPositionMs,
                    metrics = metrics,
                    resizeMode = resizeMode,
                    isLocked = isLocked,
                    onScrubChange = onScrubChange,
                    onScrubFinished = onScrubFinished,
                    onResizeModeClick = onResizeModeClick,
                    onSpeedClick = onSpeedClick,
                    onSubtitleClick = onSubtitleClick,
                    onAudioClick = onAudioClick,
                    onCastClick = onCastClick,
                    onLockToggle = onLockToggle,
                    onVideoSettingsClick = onVideoSettingsClick,
                    onOpenInExternalPlayer = onOpenInExternalPlayer,
                    onSubmitIntroClick = onSubmitIntroClick,
                    onSourcesClick = onSourcesClick,
                    onEpisodesClick = onEpisodesClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(bottom = metrics.sliderBottomOffset),
                )
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    metrics: PlayerLayoutMetrics,
    showDetails: Boolean,
    torrentStats: PlayerTorrentStats?,
    parentalWarnings: List<ParentalWarning>,
    showParentalGuide: Boolean,
    onParentalGuideAnimationComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeScale = MaterialTheme.nuvioTypeScale
    val metadataAlpha by animateFloatAsState(
        targetValue = if (!showParentalGuide && showDetails) 1f else 0f,
        animationSpec = tween(durationMillis = if (!showParentalGuide && showDetails) 260 else 160),
        label = "playerHeaderMetadataAlpha",
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (showDetails) {
            NuvioBackButton(
                onClick = onBack,
                containerColor = Color.Black.copy(alpha = 0.35f),
                contentColor = Color.White,
                buttonSize = metrics.headerIconSize + 16.dp,
                iconSize = metrics.headerIconSize,
                contentDescription = stringResource(Res.string.compose_player_close),
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.graphicsLayer { alpha = metadataAlpha },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = typeScale.titleLg.copy(
                        fontSize = metrics.titleSize,
                        lineHeight = metrics.titleSize * 1.16f,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (seasonNumber != null && episodeNumber != null) {
                    Text(
                        text = if (episodeTitle.isNullOrBlank()) {
                            stringResource(Res.string.compose_player_episode_code_full, seasonNumber, episodeNumber)
                        } else {
                            stringResource(
                                Res.string.compose_player_episode_title_format,
                                seasonNumber,
                                episodeNumber,
                                episodeTitle,
                            )
                        },
                        style = typeScale.bodyMd.copy(
                            fontSize = metrics.episodeInfoSize,
                            lineHeight = metrics.episodeInfoSize * 1.3f,
                        ),
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ParentalGuideOverlay(
                warnings = parentalWarnings,
                isVisible = showParentalGuide,
                onAnimationComplete = onParentalGuideAnimationComplete,
                contentPadding = PaddingValues(0.dp),
            )
        }

        if (showDetails && torrentStats != null) {
            TorrentStatsColumn(stats = torrentStats)
        }
    }
}

@Composable
private fun TorrentStatsColumn(stats: PlayerTorrentStats) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(
            stringResource(Res.string.player_torrent_seeds, stats.seeds),
            stringResource(Res.string.player_torrent_peers, stats.peers),
            stats.downloadSpeed,
            stringResource(Res.string.player_torrent_downloaded, "${stats.downloadedPercent}%"),
        ).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CenterControls(
    snapshot: PlayerPlaybackSnapshot,
    metrics: PlayerLayoutMetrics,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNextEpisode: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(metrics.centerGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SideControlButton(
            icon = Icons.Rounded.Replay10,
            contentDescription = stringResource(Res.string.compose_player_seek_back_10),
            metrics = metrics,
            onClick = onSeekBack,
        )
        when {
            snapshot.isEnded && onNextEpisode != null -> SideControlButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(Res.string.player_next_episode),
                metrics = metrics,
                onClick = onNextEpisode,
                padding = metrics.playButtonPadding,
            )
            snapshot.isEnded -> SideControlButton(
                icon = Icons.Rounded.Replay,
                contentDescription = stringResource(Res.string.player_replay),
                metrics = metrics,
                onClick = onTogglePlayback,
                padding = metrics.playButtonPadding,
            )
            else -> PlayPauseControlButton(
                isPlaying = snapshot.isPlaying,
                isBuffering = snapshot.isLoading,
                metrics = metrics,
                onClick = onTogglePlayback,
            )
        }
        SideControlButton(
            icon = Icons.Rounded.Forward10,
            contentDescription = stringResource(Res.string.compose_player_seek_forward_10),
            metrics = metrics,
            onClick = onSeekForward,
        )
    }
}

@Composable
private fun SideControlButton(
    icon: ImageVector,
    contentDescription: String,
    metrics: PlayerLayoutMetrics,
    onClick: () -> Unit,
    padding: Dp = metrics.sideButtonPadding,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(metrics.playIconSize),
        )
    }
}

@Composable
internal fun PlayPauseControlButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    metrics: PlayerLayoutMetrics,
    onClick: () -> Unit,
) {
    val playPausePainter = appIconPainter(
        if (isPlaying) AppIconResource.PlayerPause else AppIconResource.PlayerPlay,
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(metrics.playButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering) {
            NuvioLoadingIndicator(
                color = Color.White,
                modifier = Modifier.size(metrics.playIconSize),
            )
        } else {
            Icon(
                painter = playPausePainter,
                contentDescription = if (isPlaying) {
                    stringResource(Res.string.compose_action_pause)
                } else {
                    stringResource(Res.string.detail_btn_play)
                },
                tint = Color.White,
                modifier = Modifier.size(metrics.playIconSize),
            )
        }
    }
}

@Composable
private fun BottomControls(
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    resizeMode: PlayerResizeMode,
    isLocked: Boolean,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    onResizeModeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onAudioClick: () -> Unit,
    onCastClick: (() -> Unit)?,
    onLockToggle: () -> Unit,
    onVideoSettingsClick: (() -> Unit)?,
    onOpenInExternalPlayer: (() -> Unit)?,
    onSubmitIntroClick: (() -> Unit)?,
    onSourcesClick: (() -> Unit)?,
    onEpisodesClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val resizeLabel = stringResource(resizeMode.labelRes)
    val speedLabel = formatPlaybackSpeedLabel(playbackSnapshot.playbackSpeed)
    val leftActions = buildList {
        add(
            PlayerGroupAction(
                resizeLabel,
                onResizeModeClick,
                painter = appIconPainter(AppIconResource.PlayerAspectRatio),
            ),
        )
        add(PlayerGroupAction(speedLabel, onSpeedClick, label = speedLabel))
        add(
            PlayerGroupAction(
                stringResource(Res.string.compose_player_subs),
                onSubtitleClick,
                painter = appIconPainter(AppIconResource.PlayerSubtitles),
            ),
        )
        add(
            PlayerGroupAction(
                stringResource(Res.string.compose_player_audio),
                onAudioClick,
                painter = appIconPainter(AppIconResource.PlayerAudioFilled),
            ),
        )
        onCastClick?.let {
            add(PlayerGroupAction(stringResource(Res.string.player_cast), it, icon = Icons.Rounded.Cast))
        }
        add(
            PlayerGroupAction(
                if (isLocked) {
                    stringResource(Res.string.compose_player_unlock_controls)
                } else {
                    stringResource(Res.string.compose_player_lock_controls)
                },
                onLockToggle,
                icon = Icons.Rounded.Lock,
            ),
        )
    }
    val rightActions = buildList {
        onSubmitIntroClick?.let {
            add(PlayerGroupAction(stringResource(Res.string.submit_intro_action), it, icon = Icons.Rounded.Flag))
        }
        onOpenInExternalPlayer?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.streams_open_external_player),
                    it,
                    icon = Icons.Filled.SwapHoriz,
                ),
            )
        }
        onVideoSettingsClick?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.player_action_video_settings),
                    it,
                    icon = Icons.Rounded.Build,
                ),
            )
        }
        onSourcesClick?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.compose_player_sources),
                    it,
                    painter = appIconPainter(AppIconResource.PlayerSource),
                ),
            )
        }
        onEpisodesClick?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.compose_player_episodes),
                    it,
                    painter = appIconPainter(AppIconResource.PlayerEpisodes),
                ),
            )
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerTimeLabel(text = formatPlaybackTime(displayedPositionMs), metrics = metrics)
            PlayerTimeLabel(text = formatPlaybackTime(playbackSnapshot.durationMs), metrics = metrics)
        }
        PlayerSeekBar(
            durationMs = playbackSnapshot.durationMs,
            displayedPositionMs = displayedPositionMs,
            bufferedPositionMs = playbackSnapshot.bufferedPositionMs,
            isPlaying = playbackSnapshot.isPlaying,
            metrics = metrics,
            onScrubChange = onScrubChange,
            onScrubFinished = onScrubFinished,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerButtonGroup(actions = leftActions)
            if (rightActions.isNotEmpty()) {
                PlayerButtonGroup(actions = rightActions)
            }
        }
    }
}

@Composable
private fun PlayerTimeLabel(text: String, metrics: PlayerLayoutMetrics) {
    Text(
        text = text,
        style = MaterialTheme.nuvioTypeScale.labelSm.copy(
            fontSize = metrics.timeSize,
            lineHeight = metrics.timeSize * 1.25f,
            fontWeight = FontWeight.Medium,
        ),
        color = Color.White,
    )
}

// One control in a player button group; icon, painter or text is what it shows.
private class PlayerGroupAction(
    val contentDescription: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val painter: Painter? = null,
    val label: String? = null,
)

@Composable
private fun PlayerButtonGroup(
    actions: List<PlayerGroupAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PlayerGroupGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEachIndexed { index, action ->
            Box(
                modifier = Modifier
                    .height(PlayerGroupButtonHeight)
                    .widthIn(min = PlayerGroupButtonMinWidth)
                    .clip(playerGroupShape(index, actions.size))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = action.onClick)
                    .semantics { contentDescription = action.contentDescription }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    action.painter != null -> Icon(
                        painter = action.painter,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(PlayerGroupIconSize),
                    )
                    action.icon != null -> Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(PlayerGroupIconSize),
                    )
                    action.label != null -> Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun playerGroupShape(index: Int, count: Int): RoundedCornerShape = when {
    count == 1 -> RoundedCornerShape(PlayerGroupOuterCorner)
    index == 0 -> RoundedCornerShape(
        topStart = PlayerGroupOuterCorner,
        bottomStart = PlayerGroupOuterCorner,
        topEnd = PlayerGroupInnerCorner,
        bottomEnd = PlayerGroupInnerCorner,
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = PlayerGroupInnerCorner,
        bottomStart = PlayerGroupInnerCorner,
        topEnd = PlayerGroupOuterCorner,
        bottomEnd = PlayerGroupOuterCorner,
    )
    else -> RoundedCornerShape(PlayerGroupInnerCorner)
}

@Composable
internal fun PlayerSeekBar(
    durationMs: Long,
    displayedPositionMs: Long,
    bufferedPositionMs: Long,
    isPlaying: Boolean,
    metrics: PlayerLayoutMetrics,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seekDurationMs = durationMs.coerceAtLeast(1L)
    val seekDescription = stringResource(Res.string.player_seek_position)
    val bufferedFraction = (bufferedPositionMs.toFloat() / seekDurationMs).coerceIn(0f, 1f)
    val activeColor = MaterialTheme.colorScheme.primary
    Slider(
        modifier = modifier
            .fillMaxWidth()
            .height(metrics.sliderTouchHeight)
            .semantics { contentDescription = seekDescription },
        value = displayedPositionMs.coerceIn(0L, seekDurationMs).toFloat(),
        onValueChange = { value -> onScrubChange(value.toLong()) },
        onValueChangeFinished = { onScrubFinished(displayedPositionMs.coerceIn(0L, seekDurationMs)) },
        enabled = durationMs > 0L,
        valueRange = 0f..seekDurationMs.toFloat(),
        thumb = {
            Box(
                modifier = Modifier
                    .size(SeekThumbSize)
                    .background(activeColor, CircleShape),
            )
        },
        track = { sliderState ->
            WavyProgressTrack(
                sliderState = sliderState,
                bufferedFraction = bufferedFraction,
                isPlaying = isPlaying,
                activeColor = activeColor,
            )
        },
    )
}

// Played part waves while playing and flattens when paused; cached part runs ahead of it.
@Composable
private fun WavyProgressTrack(
    sliderState: SliderState,
    bufferedFraction: Float,
    isPlaying: Boolean,
    activeColor: Color,
) {
    val amplitude by animateDpAsState(
        targetValue = if (isPlaying) WaveAmplitude else 0.dp,
        label = "seekWaveAmplitude",
    )
    val phase by rememberInfiniteTransition(label = "seekWave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = LinearEasing)),
        label = "seekWavePhase",
    )
    val range = sliderState.valueRange
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    val playedFraction = ((sliderState.value - range.start) / span).coerceIn(0f, 1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(TrackCanvasHeight),
    ) {
        val strokeWidth = TrackStrokeWidth.toPx()
        val centerY = size.height / 2f
        val playedX = size.width * playedFraction
        val bufferedX = size.width * maxOf(bufferedFraction, playedFraction)
        val amplitudePx = amplitude.toPx()
        val wavelengthPx = WaveLength.toPx()

        drawLine(
            color = Color.White.copy(alpha = 0.24f),
            start = Offset(bufferedX, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (bufferedX > playedX) {
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(playedX, centerY),
                end = Offset(bufferedX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        if (playedX > 0f) {
            val path = Path().apply {
                moveTo(0f, centerY)
                var x = 0f
                while (x < playedX) {
                    x = minOf(x + 2f, playedX)
                    lineTo(x, centerY + amplitudePx * sin(2f * PI.toFloat() * x / wavelengthPx - phase))
                }
            }
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

private val PlayerGroupGap = 2.dp
private val PlayerGroupButtonHeight = 44.dp
private val PlayerGroupButtonMinWidth = 48.dp
private val PlayerGroupIconSize = 20.dp
private val PlayerGroupOuterCorner = 22.dp
private val PlayerGroupInnerCorner = 6.dp
private val SeekThumbSize = 14.dp
private val TrackCanvasHeight = 14.dp
private val TrackStrokeWidth = 4.dp
private val WaveAmplitude = 3.dp
private val WaveLength = 28.dp

@Composable
internal fun LockedPlayerOverlay(
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    horizontalSafePadding: androidx.compose.ui.unit.Dp,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationMs = playbackSnapshot.durationMs.coerceAtLeast(1L)
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
        disabledThumbColor = Color.White,
        disabledActiveTrackColor = Color.White,
        disabledInactiveTrackColor = Color.White.copy(alpha = 0.28f),
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onUnlock),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = stringResource(Res.string.compose_player_unlock_controls),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.compose_player_tap_to_unlock),
                style = MaterialTheme.nuvioTypeScale.bodyMd.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.92f),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = horizontalSafePadding + metrics.horizontalPadding)
                .padding(bottom = metrics.sliderBottomOffset),
        ) {
            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.sliderTouchHeight)
                    .graphicsLayer(scaleY = metrics.sliderScaleY),
                value = displayedPositionMs.coerceIn(0L, durationMs).toFloat(),
                onValueChange = {},
                onValueChangeFinished = {},
                valueRange = 0f..durationMs.toFloat(),
                enabled = false,
                colors = sliderColors,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimePill(text = formatPlaybackTime(displayedPositionMs), fontSize = metrics.timeSize)
                TimePill(text = formatPlaybackTime(durationMs), fontSize = metrics.timeSize)
            }
        }
    }
}

@Composable
private fun TimePill(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.25f,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
        )
    }
}

