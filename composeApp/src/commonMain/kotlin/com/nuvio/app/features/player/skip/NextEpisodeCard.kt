package com.nuvio.app.features.player.skip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.components.PlatformBackHandler
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_episode_code_full
import nuvio.composeapp.generated.resources.player_next_episode_thumbnail
import org.jetbrains.compose.resources.stringResource

// The next episode as a large thumbnail on the right; tap plays it, swipe right dismisses it.
@Composable
fun NextEpisodeCard(
    nextEpisode: NextEpisodeInfo?,
    visible: Boolean,
    isLoading: Boolean,
    blurred: Boolean,
    onPlayNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 240.dp,
) {
    if (nextEpisode == null) return

    PlatformBackHandler(enabled = visible, onBack = onDismiss)
    val isPlayable = nextEpisode.hasAired
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
        visible = visible,
        enter = slideInHorizontally(animationSpec = tween(260), initialOffsetX = { it / 2 }) +
            fadeIn(animationSpec = tween(220)),
        exit = slideOutHorizontally(animationSpec = tween(200), targetOffsetX = { it / 2 }) +
            fadeOut(animationSpec = tween(160)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .aspectRatio(16f / 9f)
                .graphicsLayer { translationX = animatedOffsetX }
                .clip(MaterialTheme.shapes.large)
                .background(Color.Black)
                .pointerInput(nextEpisode.videoId, visible, dismissThreshold) {
                    if (!visible) return@pointerInput
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
                    if (visible) dismiss { currentOnDismiss(); true }
                }
                .clickable(enabled = visible && isPlayable && !isLoading) { onPlayNext() },
        ) {
            AsyncImage(
                model = nextEpisode.thumbnail,
                contentDescription = stringResource(Res.string.player_next_episode_thumbnail),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurred) Modifier.blur(18.dp) else Modifier),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.4f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
            )
            if (isLoading) {
                NuvioLoadingIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
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
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                )
                Text(
                    text = if (isPlayable) {
                        nextEpisode.title
                    } else {
                        nextEpisode.unairedMessage?.takeIf { it.isNotBlank() } ?: nextEpisode.title
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
