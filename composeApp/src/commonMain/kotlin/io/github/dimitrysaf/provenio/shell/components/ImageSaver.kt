package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable

internal class ImageSaveRequest(
    val url: String,
    val fileName: String,
) {
    val mimeType: String
        get() = when (fileName.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
}

@Composable
internal expect fun rememberImageSaver(): (ImageSaveRequest) -> Unit
