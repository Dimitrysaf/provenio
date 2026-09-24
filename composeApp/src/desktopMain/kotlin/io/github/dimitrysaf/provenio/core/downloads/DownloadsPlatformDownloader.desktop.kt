package io.github.dimitrysaf.provenio.core.downloads

import java.io.File
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.downloads_desktop_unavailable

// Downloading to disk is a later phase of the desktop port; a started download fails at once
// with a message rather than sitting at zero.
internal actual object DownloadsPlatformDownloader {
    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
        onPaused: () -> Unit,
    ): DownloadsTaskHandle {
        onFailure(runBlocking { getString(Res.string.downloads_desktop_unavailable) })
        return object : DownloadsTaskHandle {
            override fun cancel() = Unit
        }
    }

    actual fun restoreItem(item: DownloadItem): DownloadItem = item

    actual fun removeFile(localFileUri: String?): Boolean =
        localFileUri?.let { runCatching { File(URI(it)).delete() }.getOrDefault(false) } ?: false

    actual fun removePartialFile(destinationFileName: String): Boolean = false

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? =
        localFileUri?.takeIf { uri -> runCatching { File(URI(uri)).isFile }.getOrDefault(false) }

    actual fun openDownloadsDirectory(): Boolean = false
}
