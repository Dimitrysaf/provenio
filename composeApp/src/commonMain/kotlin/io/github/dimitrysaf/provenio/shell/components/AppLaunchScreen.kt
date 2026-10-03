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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.startup.AppStartupState
import kotlinx.coroutines.delay

@Composable
fun AppLaunchScreen(
    logo: Painter,
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
                SpinningLaunchLogo(logo = logo)
            }
        }
    }
}

@Composable
private fun SpinningLaunchLogo(logo: Painter) {
    val rotation by rememberInfiniteTransition(label = "launch_logo").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LaunchLogoTurnMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "launch_logo_rotation",
    )
    Image(
        painter = logo,
        contentDescription = null,
        modifier = Modifier
            .size(LaunchLogoSize)
            .graphicsLayer {
                transformOrigin = LaunchLogoPivot
                rotationZ = rotation
            },
    )
}

private val LaunchLogoSize = 288.dp

private val LaunchLogoPivot = TransformOrigin(pivotFractionX = 397f / 1080f, pivotFractionY = 363f / 1080f)

private const val LaunchLogoTurnMillis = 1_400

private const val MaxLaunchScreenMillis = 6_000L
