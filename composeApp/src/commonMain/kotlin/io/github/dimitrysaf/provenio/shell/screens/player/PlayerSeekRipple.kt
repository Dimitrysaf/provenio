package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_player_seek_feedback_backward
import provenio.composeapp.generated.resources.compose_player_seek_feedback_forward

/**
 * The run of seeks on its side of the screen, as YouTube shows it: the side lit softly up to a
 * curved edge, a wave spreading at every step, three arrows lighting in turn, and the total.
 * With [compact], where the seek buttons sit at the bottom rather than mid-screen, it is a round
 * badge on that side instead of the whole side.
 */
@Composable
internal fun BoxScope.PlayerSeekRipple(state: PlayerAccumulatedSeekState?, compact: Boolean) {
    // The last run stays drawn while it fades out.
    var last by remember { mutableStateOf(state) }
    LaunchedEffect(state) { if (state != null) last = state }
    val shown = state ?: last
    val backward = shown?.direction == PlayerSeekDirection.Backward

    AnimatedVisibility(
        visible = state != null,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(250)),
        modifier = Modifier
            .align(if (backward) Alignment.CenterStart else Alignment.CenterEnd)
            .then(
                if (compact) {
                    Modifier.padding(horizontal = CompactRippleInset).size(CompactRippleSize)
                } else {
                    Modifier.fillMaxHeight().fillMaxWidth(PlayerLeftGestureBoundary)
                },
            ),
    ) {
        shown ?: return@AnimatedVisibility
        val wave = remember { Animatable(0f) }
        // Every step adds to the total, so a new total is a new tap: the wave starts again.
        LaunchedEffect(shown.amountMs) {
            wave.snapTo(0f)
            wave.animateTo(1f, tween(durationMillis = 450, easing = LinearOutSlowInEasing))
        }
        val litArrow by rememberInfiniteTransition(label = "seek_arrows").animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 750, easing = LinearEasing)),
            label = "seek_arrow",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(if (compact) CircleShape else remember(backward) { sideArcShape(backward) })
                // A badge sits over the picture rather than over a whole side, so it darkens it.
                .background(if (compact) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f))
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f * (1f - wave.value)),
                        radius = size.width * 0.6f * wave.value,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Mirrored for going back, so the arrows point and light up towards the left.
                Row(
                    modifier = Modifier.graphicsLayer { scaleX = if (backward) -1f else 1f },
                    horizontalArrangement = Arrangement.spacedBy((-8).dp),
                ) {
                    repeat(3) { index ->
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (litArrow.toInt() == index) 1f else 0.4f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
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
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private val CompactRippleSize = 128.dp
private val CompactRippleInset = 96.dp

// A slice of a large oval: flat against the screen's edge, curved where it meets the middle.
private fun sideArcShape(backward: Boolean) = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    addOval(
        if (backward) {
            Rect(-width, -height * 0.25f, width, height * 1.25f)
        } else {
            Rect(0f, -height * 0.25f, width * 2f, height * 1.25f)
        },
    )
}
