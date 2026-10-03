package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun CompanyLogo(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var keepColors by remember(url) { mutableStateOf<Boolean?>(null) }
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alpha = if (keepColors == null) 0f else 1f,
        colorFilter = if (keepColors == false) ColorFilter.tint(LocalContentColor.current) else null,
        onSuccess = { state -> keepColors = runCatching { state.painter.needsOriginalColors() }.getOrDefault(false) },
        onError = { keepColors = true },
    )
}

private fun Painter.needsOriginalColors(): Boolean {
    val intrinsic = intrinsicSize
    if (intrinsic.isUnspecified || intrinsic.width <= 0f || intrinsic.height <= 0f) return false
    val scale = min(1f, LogoSampleSize / max(intrinsic.width, intrinsic.height))
    val width = max(2, (intrinsic.width * scale).roundToInt())
    val height = max(2, (intrinsic.height * scale).roundToInt())
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(width.toFloat(), height.toFloat()),
    ) {
        with(this@needsOriginalColors) { draw(size) }
    }
    val pixels = bitmap.toPixelMap()

    var edgeOpaque = 0
    var edgeTotal = 0
    var opaque = 0
    var dark = 0
    var light = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixels[x, y]
            val isOpaque = color.alpha > OpaqueAlpha
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                edgeTotal++
                if (isOpaque) edgeOpaque++
            }
            if (!isOpaque) continue
            opaque++
            val luminance = color.luminance()
            if (luminance < DarkLuminance) dark++
            if (luminance > LightLuminance) light++
        }
    }
    if (edgeOpaque >= edgeTotal * BoxedEdgeShare) return true
    if (opaque == 0) return false
    return dark >= opaque * ToneShare && light >= opaque * ToneShare
}

private const val LogoSampleSize = 64f
private const val OpaqueAlpha = 0.9f
private const val BoxedEdgeShare = 0.6f
private const val DarkLuminance = 0.3f
private const val LightLuminance = 0.7f
private const val ToneShare = 0.1f
