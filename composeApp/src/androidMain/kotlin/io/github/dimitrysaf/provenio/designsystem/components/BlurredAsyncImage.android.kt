package io.github.dimitrysaf.provenio.designsystem.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [RenderEffect] — what [androidx.compose.ui.draw.blur] runs on — only exists from Android
 * 12 (API 31) on, so that is used here whenever it is actually there. Below it, the same
 * bytes are fetched by hand, decoded and blurred with [RenderScript]'s blur intrinsic —
 * deprecated in favour of RenderEffect, but still the real thing on exactly the versions
 * that do not have RenderEffect to deprecate it in favour of.
 */
@Composable
actual fun BlurredAsyncImage(url: String, modifier: Modifier, contentScale: ContentScale) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.blur(BlurRadius),
            contentScale = contentScale,
        )
        return
    }

    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = null
        bitmap = fetchBlurredBitmap(context, url)
    }

    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        // Nothing decoded yet — a blank surface rather than the sharp original showing
        // even for an instant, since the whole point of this is that it must not.
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest))
    }
}

private suspend fun fetchBlurredBitmap(context: Context, url: String): ImageBitmap? =
    withContext(Dispatchers.IO) {
        val bytes = runCatching { HttpClient().use { it.get(url).readBytes() } }.getOrNull()
            ?: return@withContext null
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null

        // RenderScript's cost scales with pixel count, and a thumbnail about to be blurred
        // this heavily carries no detail worth paying full resolution to blur away.
        val downscaled = Bitmap.createScaledBitmap(
            original,
            (original.width / DownscaleFactor).coerceAtLeast(1),
            (original.height / DownscaleFactor).coerceAtLeast(1),
            true,
        )
        runCatching { blur(context, downscaled) }.getOrNull()?.asImageBitmap()
    }

@Suppress("DEPRECATION")
private fun blur(context: Context, bitmap: Bitmap): Bitmap {
    val rs = RenderScript.create(context)
    try {
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(MaxScriptBlurRadius)
        script.setInput(input)
        script.forEach(output)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        output.copyTo(result)

        script.destroy()
        input.destroy()
        output.destroy()
        return result
    } finally {
        rs.destroy()
    }
}

private val BlurRadius = 18.dp

/** The intrinsic's own ceiling — it throws above this. */
private const val MaxScriptBlurRadius = 25f
private const val DownscaleFactor = 4
