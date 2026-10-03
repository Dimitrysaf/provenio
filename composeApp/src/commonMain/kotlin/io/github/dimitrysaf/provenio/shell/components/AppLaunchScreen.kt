package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.startup.AppStartupState
import kotlinx.coroutines.delay

@Composable
fun AppLaunchScreen(
    logoColor: Color,
    background: Color,
    content: @Composable () -> Unit,
) {
    val firstScreenReady by AppStartupState.firstScreenReady.collectAsState()
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MaxLaunchScreenMillis)
        timedOut = true
    }
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = !firstScreenReady && !timedOut,
            enter = EnterTransition.None,
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background),
                contentAlignment = Alignment.Center,
            ) {
                LaunchLogo(color = logoColor)
            }
        }
    }
}

@Composable
private fun LaunchLogo(color: Color) {
    val stemAngle by rememberInfiniteTransition(label = "launch_logo").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LaunchLogo.TurnMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "launch_logo_stem",
    )
    val triangle = remember {
        Path().apply {
            moveTo(LaunchLogo.TriangleLeft, LaunchLogo.TriangleTop)
            lineTo(LaunchLogo.TriangleLeft, LaunchLogo.TriangleBottom)
            lineTo(LaunchLogo.TriangleTip, LaunchLogo.CenterY)
            close()
        }
    }
    Canvas(modifier = Modifier.size(LaunchLogoSize)) {
        val unit = size.minDimension / LaunchLogo.Viewport
        scale(scale = unit, pivot = Offset.Zero) {
            val center = Offset(LaunchLogo.CenterX, LaunchLogo.CenterY)
            drawCircle(
                color = color,
                radius = LaunchLogo.RingRadius,
                center = center,
                style = Stroke(width = LaunchLogo.StrokeWidth),
            )
            drawPath(path = triangle, color = color, style = Fill)
            drawPath(
                path = triangle,
                color = color,
                style = Stroke(width = LaunchLogo.TriangleRounding, join = StrokeJoin.Round),
            )
            rotate(degrees = stemAngle, pivot = center) {
                drawLine(
                    color = color,
                    start = Offset(LaunchLogo.StemX, LaunchLogo.CenterY),
                    end = Offset(LaunchLogo.StemX, LaunchLogo.StemEndY),
                    strokeWidth = LaunchLogo.StrokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private val LaunchLogoSize = 288.dp

object LaunchLogo {
    const val Viewport = 1080f
    const val CenterX = 539.5f
    const val CenterY = 466.5f
    const val RingRadius = 144.5f
    const val StrokeWidth = 84f
    const val StemX = CenterX - RingRadius
    const val StemEndY = 757f
    const val TriangleLeft = 518f
    const val TriangleTop = 418f
    const val TriangleBottom = 516f
    const val TriangleTip = 598f
    const val TriangleRounding = 20f
    const val TurnMillis = 1_400
}

private const val MaxLaunchScreenMillis = 6_000L
