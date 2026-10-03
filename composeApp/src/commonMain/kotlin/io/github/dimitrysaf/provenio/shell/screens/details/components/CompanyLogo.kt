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
    var boxed by remember(url) { mutableStateOf<Boolean?>(null) }
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alpha = if (boxed == null) 0f else 1f,
        colorFilter = if (boxed == false) ColorFilter.tint(LocalContentColor.current) else null,
        onSuccess = { state -> boxed = runCatching { state.painter.hasOpaqueEdges() }.getOrDefault(false) },
        onError = { boxed = true },
    )
}

private fun Painter.hasOpaqueEdges(): Boolean {
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
        with(this@hasOpaqueEdges) { draw(size) }
    }
    val pixels = bitmap.toPixelMap()
    var opaque = 0
    var total = 0
    fun sample(x: Int, y: Int) {
        total++
        if (pixels[x, y].alpha > OpaqueAlpha) opaque++
    }
    for (x in 0 until width) {
        sample(x, 0)
        sample(x, height - 1)
    }
    for (y in 1 until height - 1) {
        sample(0, y)
        sample(width - 1, y)
    }
    return opaque >= total * BoxedEdgeShare
}

private const val LogoSampleSize = 64f
private const val OpaqueAlpha = 0.9f
private const val BoxedEdgeShare = 0.6f
