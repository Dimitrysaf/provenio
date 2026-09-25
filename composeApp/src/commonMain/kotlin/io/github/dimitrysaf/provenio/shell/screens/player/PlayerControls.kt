package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoodBad
import androidx.compose.material.icons.rounded.NoAdultContent
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.BackButton
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.theme.typeScale
import kotlinx.coroutines.delay
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin
import io.github.dimitrysaf.provenio.core.playback.ParentalWarning
import io.github.dimitrysaf.provenio.core.playback.PlayerResizeMode
import io.github.dimitrysaf.provenio.core.playback.labelRes
import io.github.dimitrysaf.provenio.core.playback.PlayerPlaybackSnapshot

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
    showPlaybackControls: Boolean = true,
    controlsReady: Boolean = true,
    playbackRequested: Boolean = true,
    hideSeekForward: Boolean = false,
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
    onOpenInExternalPlayer: (() -> Unit)? = null,
    onSubmitIntroClick: (() -> Unit)? = null,
    statusLines: List<String> = emptyList(),
    onStatusClick: (() -> Unit)? = null,
    parentalWarnings: List<ParentalWarning> = emptyList(),
    showParentalGuide: Boolean = false,
    onParentalGuideAnimationComplete: () -> Unit = {},
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showPlaybackControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))),
                )
            }
        }

        PlayerHeader(
            title = title,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            metrics = metrics,
            statusLines = if (showPlaybackControls) statusLines else emptyList(),
            onStatusClick = onStatusClick,
            parentalWarnings = parentalWarnings,
            showParentalGuide = showParentalGuide,
            onParentalGuideAnimationComplete = onParentalGuideAnimationComplete,
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .playerFrameInsets(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                .padding(horizontal = metrics.horizontalPadding)
                .padding(top = metrics.verticalPadding),
        )

        AnimatedVisibility(
            visible = showPlaybackControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CenterControls(
                    snapshot = playbackSnapshot,
                    metrics = metrics,
                    controlsReady = controlsReady,
                    playbackRequested = playbackRequested,
                    hideSeekForward = hideSeekForward,
                    onSeekBack = onSeekBack,
                    onSeekForward = onSeekForward,
                    onTogglePlayback = onTogglePlayback,
                    onNextEpisode = onNextEpisode,
                    modifier = Modifier.align(Alignment.Center),
                )

                BottomControls(
                    playbackSnapshot = playbackSnapshot,
                    displayedPositionMs = displayedPositionMs,
                    metrics = metrics,
                    resizeMode = resizeMode,
                    controlsReady = controlsReady,
                    onScrubChange = onScrubChange,
                    onScrubFinished = onScrubFinished,
                    onResizeModeClick = onResizeModeClick,
                    onSpeedClick = onSpeedClick,
                    onSubtitleClick = onSubtitleClick,
                    onAudioClick = onAudioClick,
                    onLockToggle = onLockToggle,
                    onVideoSettingsClick = onVideoSettingsClick,
                    onOpenInExternalPlayer = onOpenInExternalPlayer,
                    onSubmitIntroClick = onSubmitIntroClick,
                    onSourcesClick = onSourcesClick,
                    onEpisodesClick = onEpisodesClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .playerFrameInsets(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
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
    statusLines: List<String>,
    onStatusClick: (() -> Unit)?,
    parentalWarnings: List<ParentalWarning>,
    showParentalGuide: Boolean,
    onParentalGuideAnimationComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeScale = MaterialTheme.typeScale
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PlayerOverlayBackButton(onClick = onBack, metrics = metrics)

        Column(modifier = Modifier.weight(1f)) {
            // Title and episode line sit centred against the back button, with or without the episode line.
            Column(
                modifier = Modifier.heightIn(min = metrics.headerIconSize + 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
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
            ParentalGuideIcons(
                warnings = parentalWarnings,
                isVisible = showParentalGuide,
                onAnimationComplete = onParentalGuideAnimationComplete,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        PlayerStatusColumn(lines = statusLines, onClick = onStatusClick)
    }
}

@Composable
internal fun PlayerOverlayBackButton(
    onClick: () -> Unit,
    metrics: PlayerLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    BackButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = PlayerScrimColor,
        contentColor = Color.White,
        buttonSize = metrics.headerIconSize + 16.dp,
        iconSize = metrics.headerIconSize,
        contentDescription = stringResource(Res.string.compose_player_close),
    )
}

// Status at the top right, one fact per line: loading phase, seeds, peers, speed, downloaded.
@Composable
internal fun PlayerStatusColumn(
    lines: List<String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (lines.isEmpty()) return
    Column(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            } else {
                Modifier
            },
        ),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
            )
        }
    }
}

// Warnings as icons under the title: each fades in, they hold, then each fades out.
@Composable
private fun ParentalGuideIcons(
    warnings: List<ParentalWarning>,
    isVisible: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return
    val alphas = remember(warnings) { warnings.map { Animatable(0f) } }
    val onComplete by rememberUpdatedState(onAnimationComplete)

    LaunchedEffect(isVisible, warnings) {
        if (!isVisible) {
            alphas.forEach { it.snapTo(0f) }
            return@LaunchedEffect
        }
        alphas.forEach { alpha ->
            alpha.animateTo(1f, tween(ParentalIconFadeMs))
        }
        delay(ParentalIconHoldMs)
        alphas.forEach { alpha ->
            alpha.animateTo(0f, tween(ParentalIconFadeMs))
        }
        onComplete()
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        warnings.forEachIndexed { index, warning ->
            Box(
                modifier = Modifier
                    .alpha(alphas[index].value)
                    .size(28.dp)
                    .background(PlayerScrimColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = warning.icon(),
                    contentDescription = "${warning.label}, ${warning.severity}",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun ParentalWarning.icon(): ImageVector = when (category) {
    "nudity" -> Icons.Rounded.NoAdultContent
    "violence" -> Icons.Rounded.SportsMma
    "profanity" -> Icons.Rounded.Explicit
    "alcohol" -> Icons.Rounded.LocalBar
    "frightening" -> Icons.Rounded.MoodBad
    else -> Icons.Rounded.Warning
}

@Composable
private fun CenterControls(
    snapshot: PlayerPlaybackSnapshot,
    metrics: PlayerLayoutMetrics,
    controlsReady: Boolean,
    playbackRequested: Boolean,
    hideSeekForward: Boolean,
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
        CenterControlButton(
            icon = Icons.Rounded.Replay10,
            contentDescription = stringResource(Res.string.compose_player_seek_back_10),
            metrics = metrics,
            onClick = onSeekBack.takeIf { controlsReady },
        )
        // Square when paused or finished, round while playing or buffering.
        val isRound = !snapshot.isEnded && (snapshot.isLoading || snapshot.isPlaying)
        when {
            snapshot.isEnded && onNextEpisode != null -> PrimaryControlButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(Res.string.player_next_episode),
                metrics = metrics,
                isRound = isRound,
                onClick = onNextEpisode,
            )
            snapshot.isEnded -> PrimaryControlButton(
                icon = Icons.Rounded.Replay,
                contentDescription = stringResource(Res.string.player_replay),
                metrics = metrics,
                isRound = isRound,
                onClick = onTogglePlayback,
            )
            // Loading still pauses and resumes; the ring shows it is waiting for data.
            snapshot.isLoading -> PrimaryControlButton(
                icon = if (playbackRequested) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playbackRequested) {
                    stringResource(Res.string.compose_action_pause)
                } else {
                    stringResource(Res.string.detail_btn_play)
                },
                metrics = metrics,
                isRound = true,
                loading = playbackRequested,
                onClick = onTogglePlayback,
            )
            else -> PrimaryControlButton(
                icon = if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (snapshot.isPlaying) {
                    stringResource(Res.string.compose_action_pause)
                } else {
                    stringResource(Res.string.detail_btn_play)
                },
                metrics = metrics,
                isRound = isRound,
                onClick = onTogglePlayback,
            )
        }
        // An invisible stand-in keeps play centred while the next episode card holds this side.
        Box(modifier = Modifier.alpha(if (hideSeekForward) 0f else 1f)) {
            CenterControlButton(
                icon = Icons.Rounded.Forward10,
                contentDescription = stringResource(Res.string.compose_player_seek_forward_10),
                metrics = metrics,
                onClick = if (controlsReady) { { if (!hideSeekForward) onSeekForward() } } else null,
            )
        }
    }
}

@Composable
private fun CenterControlButton(
    icon: ImageVector,
    contentDescription: String,
    metrics: PlayerLayoutMetrics,
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(PlayerScrimColor)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(metrics.sideButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (onClick != null) Color.White else Color.White.copy(alpha = 0.38f),
            modifier = Modifier.size(metrics.sideIconSize),
        )
    }
}

// Play, pause, replay, next or buffering; the corners morph between round and square.
@Composable
private fun PrimaryControlButton(
    icon: ImageVector?,
    contentDescription: String?,
    metrics: PlayerLayoutMetrics,
    isRound: Boolean,
    onClick: (() -> Unit)?,
    loading: Boolean = false,
) {
    val size = metrics.playIconSize + metrics.playButtonPadding * 2
    val corner by animateDpAsState(
        targetValue = if (isRound) size / 2 else size * PrimarySquareCornerFraction,
        animationSpec = tween(220),
        label = "player_primary_corner",
    )
    val shape = RoundedCornerShape(corner)
    val contentColor = MaterialTheme.colorScheme.onPrimary
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.requiredSize(size + 16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = PlayerScrimColor,
                strokeWidth = 4.dp,
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primary)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (icon == null) {
                LoadingSpinner(
                    color = contentColor,
                    modifier = Modifier.size(metrics.playIconSize),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(metrics.playIconSize),
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    resizeMode: PlayerResizeMode,
    controlsReady: Boolean,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    onResizeModeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onAudioClick: () -> Unit,
    onLockToggle: () -> Unit,
    onVideoSettingsClick: (() -> Unit)?,
    onOpenInExternalPlayer: (() -> Unit)?,
    onSubmitIntroClick: (() -> Unit)?,
    onSourcesClick: (() -> Unit)?,
    onEpisodesClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val speedLabel = formatPlaybackSpeedLabel(playbackSnapshot.playbackSpeed)
    val leftActions = listOf(
        PlayerGroupAction(stringResource(resizeMode.labelRes), onResizeModeClick, icon = Icons.Rounded.AspectRatio),
        // Speed and tracks need a loaded stream, so they wait until it is ready.
        PlayerGroupAction(speedLabel, onSpeedClick.takeIf { controlsReady }, label = speedLabel),
        PlayerGroupAction(
            stringResource(Res.string.compose_player_subs),
            onSubtitleClick.takeIf { controlsReady },
            icon = Icons.Rounded.Subtitles,
        ),
        PlayerGroupAction(
            stringResource(Res.string.compose_player_audio),
            onAudioClick.takeIf { controlsReady },
            icon = Icons.Rounded.Audiotrack,
        ),
        // Casting is not built yet, so the button is shown but does nothing.
        PlayerGroupAction(stringResource(Res.string.player_cast), onClick = null, icon = Icons.Rounded.Cast),
        PlayerGroupAction(
            stringResource(Res.string.compose_player_lock_controls),
            onLockToggle,
            icon = Icons.Rounded.Lock,
        ),
    )
    val rightActions = buildList {
        onSubmitIntroClick?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.submit_intro_action),
                    it.takeIf { controlsReady },
                    icon = Icons.Rounded.Flag,
                ),
            )
        }
        onOpenInExternalPlayer?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.streams_open_external_player),
                    it,
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                ),
            )
        }
        onVideoSettingsClick?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.player_action_video_settings),
                    it.takeIf { controlsReady },
                    icon = Icons.Rounded.Tune,
                ),
            )
        }
        onSourcesClick?.let {
            add(PlayerGroupAction(stringResource(Res.string.compose_player_sources), it, icon = Icons.Rounded.Layers))
        }
        onEpisodesClick?.let {
            add(
                PlayerGroupAction(
                    stringResource(Res.string.compose_player_episodes),
                    it,
                    icon = Icons.Rounded.VideoLibrary,
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
            modifier = Modifier.fillMaxWidth(),
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
        style = MaterialTheme.typeScale.labelSm.copy(
            fontSize = metrics.timeSize,
            lineHeight = metrics.timeSize * 1.25f,
            fontWeight = FontWeight.Medium,
        ),
        color = Color.White,
    )
}

// One control in a player button group; a null click leaves it shown but disabled.
private class PlayerGroupAction(
    val contentDescription: String,
    val onClick: (() -> Unit)?,
    val icon: ImageVector? = null,
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
            val onClick = action.onClick
            Box(
                modifier = Modifier
                    .height(PlayerGroupButtonHeight)
                    .widthIn(min = PlayerGroupButtonMinWidth)
                    .clip(playerGroupShape(index, actions.size))
                    .background(PlayerScrimColor)
                    .clickable(enabled = onClick != null) { onClick?.invoke() }
                    .semantics { contentDescription = action.contentDescription }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val tint = if (onClick != null) Color.White else Color.White.copy(alpha = 0.38f)
                when {
                    action.icon != null -> Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(PlayerGroupIconSize),
                    )
                    action.label != null -> Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
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
        // The slider stretches its thumb slot to the bar's height, so the dot keeps its own size in the middle.
        thumb = {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .requiredSize(SeekThumbSize)
                        .background(activeColor, CircleShape),
                )
            }
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

// Played part waves gently while playing and flattens when paused; cached part runs ahead of it.
@Composable
private fun WavyProgressTrack(
    sliderState: SliderState,
    bufferedFraction: Float,
    isPlaying: Boolean,
    activeColor: Color,
) {
    val amplitude by animateDpAsState(
        targetValue = if (isPlaying) WaveAmplitude else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "seekWaveAmplitude",
    )
    val phase by rememberInfiniteTransition(label = "seekWave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = WavePeriodMs, easing = LinearEasing)),
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
        // M3 leaves a gap on each side of the thumb; round caps reach half a stroke past each end.
        val gapPx = SeekThumbSize.toPx() / 2f + TrackThumbGap.toPx() + strokeWidth / 2f
        val playedEndX = playedX - gapPx
        val restStartX = minOf(playedX + gapPx, size.width)
        val bufferedStartX = maxOf(bufferedX, restStartX)

        if (bufferedStartX < size.width) {
            drawLine(
                color = Color.White.copy(alpha = 0.24f),
                start = Offset(bufferedStartX, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        if (bufferedStartX > restStartX) {
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(restStartX, centerY),
                end = Offset(bufferedStartX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        if (playedEndX > 0f) {
            val waveY = { x: Float -> centerY + amplitudePx * sin(2f * PI.toFloat() * x / wavelengthPx - phase) }
            val path = Path().apply {
                moveTo(0f, waveY(0f))
                var x = 0f
                while (x < playedEndX) {
                    x = minOf(x + 1f, playedEndX)
                    lineTo(x, waveY(x))
                }
            }
            drawPath(
                path = path,
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

// Locked: nothing but an unlock button at the right edge, and only after a tap.
@Composable
internal fun LockedPlayerOverlay(
    metrics: PlayerLayoutMetrics,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .playerFrameInsets(WindowInsetsSides.Horizontal)
                .padding(end = metrics.horizontalPadding)
                .size(metrics.headerIconSize + 24.dp)
                .clip(CircleShape)
                .background(PlayerScrimColor)
                .clickable(onClick = onUnlock),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.LockOpen,
                contentDescription = stringResource(Res.string.compose_player_unlock_controls),
                tint = Color.White,
                modifier = Modifier.size(metrics.headerIconSize),
            )
        }
    }
}

internal val PlayerScrimColor = Color.Black.copy(alpha = 0.5f)

private val PlayerGroupGap = 2.dp
private const val PrimarySquareCornerFraction = 0.28f
private val PlayerGroupButtonHeight = 36.dp
private val PlayerGroupButtonMinWidth = 40.dp
private val PlayerGroupIconSize = 18.dp
private val PlayerGroupOuterCorner = 18.dp
private val PlayerGroupInnerCorner = 4.dp
private val SeekThumbSize = 12.dp
private val TrackCanvasHeight = 12.dp
private val TrackStrokeWidth = 4.dp
private val TrackThumbGap = 4.dp
private val WaveAmplitude = 1.5.dp
private val WaveLength = 40.dp
private const val WavePeriodMs = 2400
private const val ParentalIconFadeMs = 250
private const val ParentalIconHoldMs = 5000L

// Height of the time row, seek bar and button row, for overlays that sit just above them.
internal fun playerBottomControlsHeight(metrics: PlayerLayoutMetrics): Dp =
    PlayerTimeRowApproxHeight + metrics.sliderTouchHeight + PlayerGroupButtonHeight

private val PlayerTimeRowApproxHeight = 18.dp
