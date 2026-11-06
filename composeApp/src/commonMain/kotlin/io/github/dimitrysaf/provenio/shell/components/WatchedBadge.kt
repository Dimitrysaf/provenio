package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.episodes_cd_watched
import org.jetbrains.compose.resources.stringResource

/**
 * A finished title: its artwork dimmed under a see-through scrim, with a large check in the middle.
 * The App Icon sheet marks its chosen icon the same way, so "done" and "chosen" read alike.
 */
@Composable
fun BoxScope.PosterWatchedOverlay(
    isWatched: Boolean,
    modifier: Modifier = Modifier,
) {
    CompletedOverlay(
        visible = isWatched,
        modifier = modifier,
        checkDescription = stringResource(Res.string.episodes_cd_watched),
    )
}

/**
 * The scrim and large check of [PosterWatchedOverlay], over whatever box it is put in; [content]
 * replaces the check. The same mark as [StatusOverlay], with the check as its icon.
 */
@Composable
fun BoxScope.CompletedOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    badgeSize: Dp = CompletedBadgeSize,
    checkDescription: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    StatusOverlay(
        visible = visible,
        icon = Icons.Default.Check,
        contentDescription = checkDescription,
        modifier = modifier,
        badgeSize = badgeSize,
        content = content,
    )
}

/**
 * The one status mark over any artwork, poster, episode still or season poster alike: the whole
 * picture dimmed, a darker circle in the middle, and the state's [icon] in it, such as a check
 * for watched or a timer for not aired yet. [content] replaces the icon.
 */
@Composable
fun BoxScope.StatusOverlay(
    visible: Boolean,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    badgeSize: Dp = CompletedBadgeSize,
    iconTint: Color = Color.White,
    content: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.matchParentSize(),
    ) {
        Box(
            modifier = Modifier.background(Color.Black.copy(alpha = StatusScrimAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = StatusBadgeAlpha)),
                contentAlignment = Alignment.Center,
            ) {
                if (content != null) {
                    content()
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = iconTint,
                        modifier = Modifier.size(badgeSize * 0.6f),
                    )
                }
            }
        }
    }
}

/**
 * How far through a title is, as a bar along the bottom of its poster; nothing for null. It is the
 * whole movie or series, unlike Continue Watching, whose bar is the episode or movie playing, and
 * it is drawn the way Continue Watching's posters draw theirs: edge to edge along the bottom.
 */
@Composable
fun BoxScope.PosterProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    if (progress == null) return
    val windowWidthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp().value }
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(posterProgressHeight(windowWidthDp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.White.copy(alpha = 0.24f),
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** How thick a poster's progress bar is, in a window [windowWidthDp] wide. */
internal fun posterProgressHeight(windowWidthDp: Float): Dp = when {
    windowWidthDp >= 1440f -> 6.dp
    windowWidthDp >= 1024f -> 5.dp
    else -> 4.dp
}

private val CompletedBadgeSize = 40.dp
private const val StatusScrimAlpha = 0.45f
// Darker than the scrim around it, so the circle reads on any picture.
private const val StatusBadgeAlpha = 0.6f
