package com.nuvio.app.shell.screens.details

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.kmpalette.extensions.painter.rememberPainterDominantColorState
import com.kmpalette.rememberDominantColorState
import com.nuvio.app.core.metadata.MetaScreenBackgroundMode

// The page colour for the dominant colour mode, taken from the hero image once it has loaded.
@Composable
internal fun rememberDominantBackdropColor(
    enabled: Boolean,
    imageBitmap: ImageBitmap?,
    painter: Painter?,
): Color {
    val colorScheme = MaterialTheme.colorScheme
    val imageBitmapColorState = rememberDominantColorState(
        defaultColor = colorScheme.background,
        defaultOnColor = colorScheme.onBackground,
    )
    val painterColorState = rememberPainterDominantColorState(
        defaultColor = colorScheme.background,
        defaultOnColor = colorScheme.onBackground,
    )
    LaunchedEffect(enabled, imageBitmap, painter) {
        if (enabled) {
            when {
                imageBitmap != null -> runCatching { imageBitmapColorState.updateFrom(imageBitmap) }
                painter != null -> runCatching { painterColorState.updateFrom(painter) }
            }
        }
    }
    val extractedColor = if (imageBitmap != null) imageBitmapColorState.color else painterColorState.color
    val targetColor = if (enabled) {
        colorScheme.background.blendTowards(extractedColor, fraction = 0.42f)
    } else {
        colorScheme.background
    }
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        label = "detail_dominant_backdrop_color",
    )
    return color
}

// Paints behind the list: a blurred backdrop for cinematic mode, a flat colour for dominant mode.
@Composable
internal fun DetailBackdrop(
    mode: MetaScreenBackgroundMode,
    backdropUrl: String?,
    visible: Boolean,
    dominantColor: Color,
) {
    val background = MaterialTheme.colorScheme.background
    when (mode) {
        MetaScreenBackgroundMode.Normal -> Unit
        MetaScreenBackgroundMode.Cinematic -> if (visible && backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(30.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background.copy(alpha = 0.92f)),
            )
        }
        MetaScreenBackgroundMode.DominantColor -> if (visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dominantColor),
            )
        }
    }
}

// Fades the bottom edge of the hero into the backdrop, following the list as it scrolls.
@Composable
internal fun DetailHeroFade(
    color: Color,
    widthFraction: Float,
    heroHeightPx: IntState,
    scrollOffsetPx: () -> Float,
) {
    Box(
        modifier = Modifier
            .zIndex(0.5f)
            .fillMaxWidth(widthFraction)
            .height(132.dp)
            .graphicsLayer {
                translationY = heroHeightPx.intValue.toFloat() - scrollOffsetPx()
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.98f),
                        color.copy(alpha = 0.84f),
                        color.copy(alpha = 0.52f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

private fun Color.blendTowards(target: Color, fraction: Float): Color {
    val clamped = fraction.coerceIn(0f, 1f)
    return Color(
        red = red + (target.red - red) * clamped,
        green = green + (target.green - green) * clamped,
        blue = blue + (target.blue - blue) * clamped,
        alpha = alpha + (target.alpha - alpha) * clamped,
    )
}
