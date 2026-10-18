package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The player's main button icon. Play and pause morph into each other, the triangle splitting
 * into the two bars and back; any other icon is drawn as it is.
 */
@Composable
internal fun PlayerPrimaryIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    if (icon == Icons.Rounded.Pause || icon == Icons.Rounded.PlayArrow) {
        PlayPauseIcon(
            playing = icon == Icons.Rounded.Pause,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier,
        )
    } else {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = modifier)
    }
}

/**
 * Play while [playing] is false, pause while it is true, and a morph between them when it
 * changes: the triangle's two halves slide apart and square off into the pause bars.
 */
@Composable
internal fun PlayPauseIcon(
    playing: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
) {
    val progress by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = tween(durationMillis = MorphMillis, easing = FastOutSlowInEasing),
        label = "playPauseMorph",
    )
    Canvas(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
    ) {
        val scale = size.minDimension / ViewportSize
        val left = size.width / 2f - ViewportSize * scale / 2f
        val top = size.height / 2f - ViewportSize * scale / 2f
        val path = Path()
        path.addMorph(PlayLeftHalf, PauseLeftBar, progress, scale, left, top)
        path.addMorph(PlayRightHalf, PauseRightBar, progress, scale, left, top)
        drawMorph(path, tint, scale)
    }
}

/** Fills the shapes and traces them with a round join, which softens their corners like the rounded icons'. */
private fun DrawScope.drawMorph(path: Path, tint: Color, scale: Float) {
    drawPath(path, tint, style = Fill)
    drawPath(path, tint, style = Stroke(width = CornerSoftening * scale, join = StrokeJoin.Round))
}

/** Adds the four-cornered shape [progress] of the way from [from] to [to], in the 24-unit viewport. */
private fun Path.addMorph(
    from: List<Offset>,
    to: List<Offset>,
    progress: Float,
    scale: Float,
    left: Float,
    top: Float,
) {
    from.indices.forEach { index ->
        val a = from[index]
        val b = to[index]
        val x = left + (a.x + (b.x - a.x) * progress) * scale
        val y = top + (a.y + (b.y - a.y) * progress) * scale
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private const val ViewportSize = 24f
private const val MorphMillis = 260
private const val CornerSoftening = 1.6f

// The play triangle from (8,5.6) to (18.6,12) to (8,18.4), cut down the middle into two halves,
// each with four corners so it can turn into a bar. The right half's tip is two corners at one point.
private val PlayLeftHalf = listOf(Offset(8f, 5.6f), Offset(13.3f, 8.8f), Offset(13.3f, 15.2f), Offset(8f, 18.4f))
private val PlayRightHalf = listOf(Offset(13.3f, 8.8f), Offset(18.6f, 12f), Offset(18.6f, 12f), Offset(13.3f, 15.2f))
private val PauseLeftBar = listOf(Offset(6.8f, 5.8f), Offset(9.8f, 5.8f), Offset(9.8f, 18.2f), Offset(6.8f, 18.2f))
private val PauseRightBar = listOf(Offset(14.2f, 5.8f), Offset(17.2f, 5.8f), Offset(17.2f, 18.2f), Offset(14.2f, 18.2f))
