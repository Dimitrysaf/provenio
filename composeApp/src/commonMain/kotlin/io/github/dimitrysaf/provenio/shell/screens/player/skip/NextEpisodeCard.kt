package io.github.dimitrysaf.provenio.shell.screens.player.skip

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.core.playback.skip.NextEpisodeInfo
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.SmallLoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.shell.components.shapedClickable
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_player_episode_code_full
import provenio.composeapp.generated.resources.player_next_episode_thumbnail
import provenio.composeapp.generated.resources.player_play_next

private enum class NextEpisodeMode { Button, Card }

/**
 * The way to the next episode. While the controls show, a Play next button sits above the
 * progress bar; once the card's time comes, the same container grows into the episode's card,
 * sized and shaped by the Card Style settings, with its title under the thumbnail the way cards
 * carry it. Tap plays the episode; swiping the card right dismisses it.
 */
@Composable
fun NextEpisodeCard(
    nextEpisode: NextEpisodeInfo?,
    visible: Boolean,
    isLoading: Boolean,
    blurred: Boolean,
    onPlayNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showPlayNextButton: Boolean = false,
    countdown: Int? = null,
) {
    if (nextEpisode == null) return

    val isPlayable = nextEpisode.hasAired
    val mode = when {
        visible -> NextEpisodeMode.Card
        (showPlayNextButton || isLoading || countdown != null) && isPlayable -> NextEpisodeMode.Button
        else -> null
    }
    PlatformBackHandler(enabled = mode == NextEpisodeMode.Card, onBack = onDismiss)

    val cardStyle = rememberPosterCardStyleUiState()
    val cardCorner = cardStyle.cornerRadiusDp.dp
    val corner by animateDpAsState(
        targetValue = if (mode == NextEpisodeMode.Card) cardCorner else PillCorner,
        animationSpec = tween(MorphMillis),
        label = "next_episode_corner",
    )
    val shape = RoundedCornerShape(corner)

    var dragOffsetX by remember(nextEpisode.videoId) { mutableFloatStateOf(0f) }
    var dragging by remember(nextEpisode.videoId) { mutableStateOf(false) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = if (dragging) snap() else tween(160),
        label = "next_episode_swipe",
    )
    val dismissThreshold = with(LocalDensity.current) { 60.dp.toPx() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(nextEpisode.videoId, visible) {
        if (visible) {
            dragging = false
            dragOffsetX = 0f
        }
    }

    AnimatedVisibility(
        visible = mode != null,
        enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.9f),
        exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.9f),
        modifier = modifier,
    ) {
        val shownMode = mode ?: NextEpisodeMode.Button
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = animatedOffsetX }
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .pointerInput(nextEpisode.videoId, shownMode, dismissThreshold) {
                    if (shownMode != NextEpisodeMode.Card) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragOffsetX = animatedOffsetX
                            dragging = true
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragOffsetX = (dragOffsetX + amount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            dragging = false
                            if (dragOffsetX >= dismissThreshold) {
                                currentOnDismiss()
                            } else {
                                dragOffsetX = 0f
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            dragOffsetX = 0f
                        },
                    )
                }
                .semantics {
                    if (shownMode == NextEpisodeMode.Card) dismiss { currentOnDismiss(); true }
                }
                .shapedClickable(shape, enabled = isPlayable && !isLoading) { onPlayNext() },
        ) {
            AnimatedContent(
                targetState = shownMode,
                transitionSpec = {
                    (fadeIn(tween(MorphMillis / 2, delayMillis = MorphMillis / 3)) togetherWith
                        fadeOut(tween(MorphMillis / 3))) using SizeTransform(clip = true) { _, _ -> tween(MorphMillis) }
                },
                contentAlignment = Alignment.BottomEnd,
                label = "next_episode_morph",
            ) { target ->
                when (target) {
                    NextEpisodeMode.Button -> PlayNextButtonContent(isLoading = isLoading, countdown = countdown)
                    NextEpisodeMode.Card -> NextEpisodeCardContent(
                        nextEpisode = nextEpisode,
                        isLoading = isLoading,
                        countdown = countdown,
                        blurred = blurred,
                        showTitle = !cardStyle.hideLabelsEnabled,
                        widthDp = landscapePosterWidth(cardStyle.widthDp).value,
                        cornerRadius = cardCorner,
                    )
                }
            }
        }
    }
}

/** The small form: an icon and Play next, a pill above the progress bar; the icon turns into a spinner while the next episode loads. */
@Composable
private fun PlayNextButtonContent(isLoading: Boolean, countdown: Int?) {
    Row(
        modifier = Modifier.padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (countdown != null) {
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                CountdownNumber(
                    value = countdown,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else if (isLoading) {
            SmallLoadingSpinner(
                size = 20.dp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(1.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(Res.string.player_play_next),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The full form: the thumbnail edge to edge at the top of the container, then the episode code
 * and title beneath it on the container's own background, as Card Style lays out a card.
 */
@Composable
private fun NextEpisodeCardContent(
    nextEpisode: NextEpisodeInfo,
    isLoading: Boolean,
    countdown: Int?,
    blurred: Boolean,
    showTitle: Boolean,
    widthDp: Float,
    cornerRadius: Dp,
) {
    val isPlayable = nextEpisode.hasAired
    Column(modifier = Modifier.width(widthDp.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(
                    if (showTitle) {
                        RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
                    } else {
                        RoundedCornerShape(cornerRadius)
                    },
                )
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            AsyncImage(
                model = nextEpisode.thumbnail,
                contentDescription = stringResource(Res.string.player_next_episode_thumbnail),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurred) Modifier.blur(18.dp) else Modifier),
                contentScale = ContentScale.Crop,
            )
            if (countdown != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CountdownNumber(
                        value = countdown,
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingSpinner(color = Color.White, size = 40.dp)
                }
            }
        }
        if (showTitle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(
                        Res.string.compose_player_episode_code_full,
                        nextEpisode.season,
                        nextEpisode.episode,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = if (isPlayable) {
                        nextEpisode.title
                    } else {
                        nextEpisode.unairedMessage?.takeIf { it.isNotBlank() } ?: nextEpisode.title
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One second of the countdown: the number grows from small to large and fades out as it goes. */
@Composable
private fun CountdownNumber(value: Int, color: Color, style: TextStyle) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(value) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = CountdownStepMillis, easing = LinearEasing))
    }
    Text(
        text = value.toString(),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            val scale = 0.6f + p
            scaleX = scale
            scaleY = scale
            alpha = if (p < 0.35f) 1f else (1f - (p - 0.35f) / 0.65f).coerceIn(0f, 1f)
        },
    )
}

private const val CountdownStepMillis = 1000
private val PillCorner = 24.dp
private const val MorphMillis = 320
