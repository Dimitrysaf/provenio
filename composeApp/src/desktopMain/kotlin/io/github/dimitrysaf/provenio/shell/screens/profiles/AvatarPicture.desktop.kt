package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.dimitrysaf.provenio.desktop.DesktopFilePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.profile_avatar_choose_picture

@Composable
internal actual fun rememberAvatarPicturePicker(onPicked: (ByteArray?) -> Unit): (() -> Unit)? {
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    return remember {
        {
            scope.launch {
                val title = getString(Res.string.profile_avatar_choose_picture)
                val bytes = withContext(Dispatchers.IO) {
                    DesktopFilePicker.chooseImage(title)?.let { file -> runCatching { file.readBytes() }.getOrNull() }
                }
                currentOnPicked(bytes)
            }
        }
    }
}

internal actual fun squareAvatarJpeg(bytes: ByteArray, size: Int): ByteArray? = runCatching {
    val image = Image.makeFromEncoded(bytes)
    val side = minOf(image.width, image.height)
    if (side <= 0) return@runCatching null
    val surface = Surface.makeRasterN32Premul(size, size)
    surface.canvas.drawImageRect(
        image,
        Rect.makeXYWH(((image.width - side) / 2).toFloat(), ((image.height - side) / 2).toFloat(), side.toFloat(), side.toFloat()),
        Rect.makeWH(size.toFloat(), size.toFloat()),
    )
    surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, JpegQuality)?.bytes
}.getOrNull()

private const val JpegQuality = 85
