package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.components.skeleton
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun HeroArtworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    portraitFallbackUrl: String? = null,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
        val fallbackUrl = portraitFallbackUrl?.takeIf { it.isNotBlank() && it != url }
        var useFallback by remember(url, fallbackUrl) { mutableStateOf(false) }
        val activeUrl = if (useFallback && fallbackUrl != null) fallbackUrl else url
        var imageRatio by remember(activeUrl) { mutableStateOf<Float?>(null) }
        var settled by remember(activeUrl) { mutableStateOf(false) }
        val letterbox = imageRatio?.let { heroArtworkMismatched(it, containerRatio) } == true
        val skeletonAlpha by animateFloatAsState(
            targetValue = if (settled) 0f else 1f,
            animationSpec = tween(durationMillis = 260),
            label = "hero_artwork_skeleton",
        )

        if (letterbox) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            )
        }
        AsyncImage(
            model = activeUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (letterbox) ContentScale.Fit else ContentScale.Crop,
            alignment = if (letterbox) Alignment.Center else alignment,
            colorFilter = colorFilter,
            onSuccess = { state ->
                val ratio = state.painter.intrinsicSize
                    .takeIf { it.isSpecified && it.width > 0f && it.height > 0f }
                    ?.let { it.width / it.height }
                if (ratio != null && fallbackUrl != null && !useFallback && heroArtworkMismatched(ratio, containerRatio)) {
                    useFallback = true
                } else {
                    imageRatio = ratio
                    settled = true
                }
            },
            onError = {
                if (fallbackUrl != null && !useFallback) {
                    useFallback = true
                } else {
                    settled = true
                }
            },
        )
        if (skeletonAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = skeletonAlpha }
                    .skeleton(RectangleShape),
            )
        }
    }
}

internal fun heroArtworkMismatched(imageRatio: Float, containerRatio: Float): Boolean {
    val factor = max(imageRatio, containerRatio) / min(imageRatio, containerRatio)
    val orientationsDiffer = (imageRatio < 1f) != (containerRatio < 1f)
    return (orientationsDiffer && factor > HeroArtworkOrientationTolerance) || factor > HeroArtworkMaxCropFactor
}

private const val HeroArtworkOrientationTolerance = 1.35f

private const val HeroArtworkMaxCropFactor = 2.2f
