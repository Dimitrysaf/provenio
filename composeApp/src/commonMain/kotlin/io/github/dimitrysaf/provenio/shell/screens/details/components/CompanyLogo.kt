package io.github.dimitrysaf.provenio.shell.screens.details.components

import io.github.dimitrysaf.provenio.shell.components.SkeletonAsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A company's or network's own logo, in its own colours. A see-through logo that would vanish
 * into the theme, a black one in dark mode or a white one in light mode, sits on a small plate of
 * the opposite tone; a logo with a background of its own needs none.
 */
@Composable
internal fun CompanyLogo(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var plate by remember(url, darkSurface) { mutableStateOf<Color?>(null) }
    SkeletonAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier.then(
            plate?.let { color ->
                Modifier.background(color, RoundedCornerShape(LogoPlateCorner)).padding(LogoPlatePadding)
            } ?: Modifier,
        ),
        contentScale = ContentScale.Fit,
        onSuccess = { state ->
            plate = runCatching { state.painter.contrastPlate(darkSurface) }.getOrNull()
        },
    )
}

/** The plate [this] logo needs to be seen on a dark or light surface, or null when it shows fine. */
private fun Painter.contrastPlate(darkSurface: Boolean): Color? {
    val intrinsic = intrinsicSize
    if (intrinsic.isUnspecified || intrinsic.width <= 0f || intrinsic.height <= 0f) return null
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
        with(this@contrastPlate) { draw(size) }
    }
    val pixels = bitmap.toPixelMap()
    val tone = logoTone(width, height) { x, y ->
        val color = pixels[x, y]
        color.alpha to color.luminance()
    } ?: return null
    return when {
        darkSurface && tone < DarkLogoLuminance -> LightPlate
        !darkSurface && tone > LightLogoLuminance -> DarkPlate
        else -> null
    }
}

/**
 * The mean luminance of a logo's drawn pixels, or null when the logo fills its own edges (it
 * brings its own background) or draws nothing. [pixel] gives each pixel's alpha and luminance.
 */
internal fun logoTone(width: Int, height: Int, pixel: (x: Int, y: Int) -> Pair<Float, Float>): Float? {
    var edgeOpaque = 0
    var edgeTotal = 0
    var opaque = 0
    var luminanceSum = 0f
    for (y in 0 until height) {
        for (x in 0 until width) {
            val (alpha, luminance) = pixel(x, y)
            val isOpaque = alpha > OpaqueAlpha
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                edgeTotal++
                if (isOpaque) edgeOpaque++
            }
            if (!isOpaque) continue
            opaque++
            luminanceSum += luminance
        }
    }
    if (opaque == 0 || edgeOpaque >= edgeTotal * BoxedEdgeShare) return null
    return luminanceSum / opaque
}

private val LightPlate = Color(0xFFF2F2F2)
private val DarkPlate = Color(0xFF202020)
private val LogoPlateCorner = 6.dp
private val LogoPlatePadding = 4.dp
private const val LogoSampleSize = 64f
private const val OpaqueAlpha = 0.5f
private const val BoxedEdgeShare = 0.6f
private const val DarkLogoLuminance = 0.3f
private const val LightLogoLuminance = 0.75f
