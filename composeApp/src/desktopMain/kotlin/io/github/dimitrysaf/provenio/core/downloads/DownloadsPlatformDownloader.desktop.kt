package io.github.dimitrysaf.provenio.core.downloads

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Downloads straight into the chosen folder, resuming from the partial file the way Android does.
internal actual object DownloadsPlatformDownloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
        onPaused: () -> Unit,
    ): DownloadsTaskHandle {
        val job = scope.launch {
            val directory = DesktopDownloadsLocation.directory()
            val destination = File(directory, request.destinationFileName)
            try {
                DownloadSubtitles.prepare(request.item, destination.toURI().toString())
                var lastProgressAt = 0L
                val partial = if (destination.isFile) destination else transferDownload(
                    item = request.item,
                    directory = directory,
                    validator = null,
                    onHeaders = { _, _ -> },
                    onProgress = { bytes, total ->
                        val now = System.currentTimeMillis()
                        if (now - lastProgressAt >= 1_000L || bytes == total) {
                            lastProgressAt = now
                            scope.launch { onProgress(bytes, total) }
                        }
                    },
                )
                val bytes = withContext(Dispatchers.IO) {
                    if (partial != destination) {
                        Files.move(partial.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                    destination.length()
                }
                onSuccess(destination.toURI().toString(), bytes)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onFailure(error.message.orEmpty())
            }
        }
        return object : DownloadsTaskHandle {
            override fun cancel() = job.cancel()
        }
    }

    // Nothing carries on after the app closes, so a download that was running is paused.
    actual fun restoreItem(item: DownloadItem): DownloadItem =
        if (item.status == DownloadStatus.Downloading) item.copy(status = DownloadStatus.Paused) else item

    actual fun removeFile(localFileUri: String?): Boolean =
        localFileUri?.let { runCatching { File(URI(it)).delete() }.getOrDefault(false) } ?: false

    actual fun removePartialFile(destinationFileName: String): Boolean =
        File(DesktopDownloadsLocation.directory(), "$destinationFileName.part").delete()

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? =
        localFileUri?.takeIf { uri -> runCatching { File(URI(uri)).isFile }.getOrDefault(false) }
            ?: File(DesktopDownloadsLocation.directory(), destinationFileName).takeIf { it.isFile }?.toURI()?.toString()

    actual fun openDownloadsDirectory(): Boolean = DesktopDownloadsLocation.open()
}
