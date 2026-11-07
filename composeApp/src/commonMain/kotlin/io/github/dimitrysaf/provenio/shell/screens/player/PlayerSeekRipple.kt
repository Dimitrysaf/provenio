package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_player_seek_feedback_backward
import provenio.composeapp.generated.resources.compose_player_seek_feedback_forward

/**
 * The run of seeks on its side of the screen, as YouTube shows it: the side lit up with a curved
 * edge, a wave spreading from its middle at every step, and the icon over the total so far.
 */
@Composable
internal fun BoxScope.PlayerSeekRipple(state: PlayerAccumulatedSeekState?) {
    // The last run stays drawn while it fades out.
    var last by remember { mutableStateOf(state) }
    LaunchedEffect(state) { if (state != null) last = state }
    val shown = state ?: last
    val backward = shown?.direction == PlayerSeekDirection.Backward

    AnimatedVisibility(
        visible = state != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(if (backward) Alignment.CenterStart else Alignment.CenterEnd)
            .fillMaxHeight()
            .fillMaxWidth(PlayerLeftGestureBoundary),
    ) {
        shown ?: return@AnimatedVisibility
        val wave = remember { Animatable(0f) }
        // Every step adds to the total, so a new total is a new tap: the wave starts again.
        LaunchedEffect(shown.amountMs) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(durationMillis = 550, easing = FastOutSlowInEasing))
        }
        val innerEdge = if (backward) {
            RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
        } else {
            RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(innerEdge)
                .background(Color.White.copy(alpha = 0.12f))
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f * (1f - wave.value)),
                        radius = size.maxDimension * wave.value,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (backward) Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    text = stringResource(
                        if (backward) {
                            Res.string.compose_player_seek_feedback_backward
                        } else {
                            Res.string.compose_player_seek_feedback_forward
                        },
                        shown.amountMs / 1000L,
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
