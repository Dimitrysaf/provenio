package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun BoxScope.PosterWatchedOverlay(
    isWatched: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = 6.dp,
) {
    AnimatedWatchedBadge(
        isVisible = isWatched,
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(padding),
    )
}
