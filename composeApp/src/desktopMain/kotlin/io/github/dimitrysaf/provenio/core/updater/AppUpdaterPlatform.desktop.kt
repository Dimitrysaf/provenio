package io.github.dimitrysaf.provenio.core.updater

import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import io.github.dimitrysaf.provenio.desktop.Context
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.prefs.Preferences
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.updates_checksum_mismatch

private const val IgnoredTagKey = "updater_ignored_tag"
private const val WindowsAssetName = "Provenio-Setup.exe"

actual object AppUpdaterPlatform {
    private val osName = System.getProperty("os.name").orEmpty().lowercase()
    private val isWindows = osName.startsWith("windows")
    private val preferences: Preferences = Preferences.userRoot().node("io/github/dimitrysaf/provenio")

    actual val isSupported: Boolean = isWindows
    actual val isDebugBuild: Boolean = AppVersionConfig.RELEASE_CHANNEL != "stable"

    actual fun getSupportedAbis(): List<String> = if (isWindows) listOf(WindowsAssetName) else emptyList()

    actual fun getIgnoredTag(): String? = preferences.get(IgnoredTagKey, null)

    actual fun setIgnoredTag(tag: String?) {
        if (tag == null) preferences.remove(IgnoredTagKey) else preferences.put(IgnoredTagKey, tag)
    }

    actual suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        sha256: String?,
        onVerifying: () -> Unit,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val target = updateFile(assetName)
            if (target.exists()) {
                onVerifying()
                if (target.matchesSha256(sha256)) return@runCatching target.absolutePath
                target.delete()
            }
            clearUpdateFiles(target.name)
            val partial = File(target.parentFile, target.name + PartialSuffix)

            var attempt = 0
            while (true) {
                attempt += 1
                val resumeFrom = partial.takeIf { it.exists() }?.length() ?: 0L
                val connection = openFollowingRedirects(assetUrl, resumeFrom)
                try {
                    val code = connection.responseCode
                    if (code == HttpRangeNotSatisfiable && attempt == 1) {
                        partial.delete()
                        continue
                    }
                    check(code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL) { "HTTP $code" }
                    val appending = code == HttpURLConnection.HTTP_PARTIAL && resumeFrom > 0L
                    val alreadyDownloaded = if (appending) resumeFrom else 0L
                    val total = connection.contentLengthLong.takeIf { it > 0 }?.let { it + alreadyDownloaded }
                    connection.inputStream.use { input ->
                        FileOutputStream(partial, appending).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var downloaded = alreadyDownloaded
                            onProgress(downloaded, total)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                onProgress(downloaded, total)
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                break
            }

            onVerifying()
            if (!partial.matchesSha256(sha256)) {
                partial.delete()
                error(getString(Res.string.updates_checksum_mismatch))
            }
            check(partial.renameTo(target)) { "Could not save ${target.name}" }
            target.absolutePath
        }
    }

    actual fun hasPartialUpdate(fileName: String): Boolean {
        val file = updateFile(fileName)
        return File(file.parentFile, file.name + PartialSuffix).exists()
    }

    actual fun clearUpdateFiles(keepFileName: String?) {
        legacyUpdatesDirectory()?.takeIf { it.isDirectory }?.deleteRecursively()
        val keep = keepFileName?.let { updateFile(it).name }
        updatesDirectory().listFiles()?.forEach { file ->
            if (keep == null || (file.name != keep && file.name != keep + PartialSuffix)) {
                file.delete()
            }
        }
    }

    private fun openFollowingRedirects(url: String, resumeFrom: Long): HttpURLConnection {
        var connection = open(url, resumeFrom)
        var redirects = 0
        while (connection.responseCode in 300..399 && redirects < 5) {
            val location = connection.getHeaderField("Location") ?: break
            connection.disconnect()
            connection = open(location, resumeFrom)
            redirects++
        }
        return connection
    }

    private fun open(url: String, resumeFrom: Long): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            if (resumeFrom > 0L) setRequestProperty("Range", "bytes=$resumeFrom-")
        }

    /** Folder for downloaded updates, kept with the app's cache rather than in the install folder. */
    private fun updatesDirectory(): File = File(Context.app.cacheDir, "updates").apply { mkdirs() }

    /** Where older versions saved updates, inside the install folder. */
    private fun legacyUpdatesDirectory(): File? =
        System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { File(File(it, "Provenio"), "updates") }

    private fun updateFile(fileName: String): File =
        File(updatesDirectory(), fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_"))

    actual fun canRequestPackageInstalls(): Boolean = true

    actual fun openUnknownSourcesSettings() = Unit

    /**
     * Runs the downloaded installer silently. It waits for the app to close, replaces the app's
     * files in place, shows its own progress and opens the app again when it is done.
     */
    actual fun installDownloadedApk(path: String): Result<Unit> = runCatching {
        val file = File(path)
        check(file.exists()) { "The downloaded update is missing" }
        if (isWindows) {
            ProcessBuilder(file.absolutePath, "/SILENT", "/NORESTART", "/CLOSEAPPLICATIONS", "/RELAUNCH=1")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            exitProcess(0)
        }
        Desktop.getDesktop().open(file)
    }

    private const val PartialSuffix = ".part"
    private const val HttpRangeNotSatisfiable = 416
}
