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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.episodes_cd_watched
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchedBadge(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(Res.string.episodes_cd_watched),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp),
        )
    }
}

@Composable
fun AnimatedWatchedBadge(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        WatchedBadge()
    }
}

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

/** The scrim and large check of [PosterWatchedOverlay], over whatever box it is put in; [content] replaces the check. */
@Composable
fun BoxScope.CompletedOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    badgeSize: Dp = CompletedBadgeSize,
    checkDescription: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.matchParentSize(),
    ) {
        Box(
            modifier = Modifier.background(Color.Black.copy(alpha = CompletedScrimAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (content != null) {
                    content()
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = checkDescription,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(badgeSize * 0.6f),
                    )
                }
            }
        }
    }
}

/**
 * How far through a title is, as a bar along the bottom of its poster; nothing for null. It is the
 * whole movie or series, unlike Continue Watching, whose bar is the episode or movie playing.
 */
@Composable
fun BoxScope.PosterProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    if (progress == null) return
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .align(Alignment.BottomCenter)
            // Drawn as Continue Watching draws its bar: thin, and close to the card's bottom edge.
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .fillMaxWidth()
            .height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.White.copy(alpha = 0.24f),
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

private val CompletedBadgeSize = 40.dp
private const val CompletedScrimAlpha = 0.45f
