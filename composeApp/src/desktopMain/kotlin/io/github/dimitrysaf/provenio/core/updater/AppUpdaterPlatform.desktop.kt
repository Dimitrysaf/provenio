package io.github.dimitrysaf.provenio.core.updater

import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import java.awt.Desktop
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.prefs.Preferences
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val IgnoredTagKey = "updater_ignored_tag"
private const val WindowsAssetName = "Provenio.msi"

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
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(downloadDirectory(), assetName)
            val partial = File(target.parentFile, "$assetName.part")
            var connection = URI(assetUrl).toURL().openConnection() as HttpURLConnection
            var redirects = 0
            while (connection.responseCode in 300..399 && redirects < 5) {
                val location = connection.getHeaderField("Location") ?: break
                connection.disconnect()
                connection = URI(location).toURL().openConnection() as HttpURLConnection
                redirects++
            }
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            val total = connection.contentLengthLong.takeIf { it > 0 }
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            connection.disconnect()
            if (target.exists()) target.delete()
            check(partial.renameTo(target)) { "Could not save ${target.name}" }
            target.absolutePath
        }
    }

    actual fun canRequestPackageInstalls(): Boolean = true

    actual fun openUnknownSourcesSettings() = Unit

    actual fun installDownloadedApk(path: String): Result<Unit> = runCatching {
        val file = File(path)
        check(file.exists()) { "The downloaded update is missing" }
        if (isWindows) {
            ProcessBuilder("msiexec", "/i", file.absolutePath, "/passive", "/norestart").start()
            exitProcess(0)
        }
        Desktop.getDesktop().open(file)
    }

    private fun downloadDirectory(): File {
        val home = File(System.getProperty("user.home"))
        val downloads = File(home, "Downloads")
        val directory = if (downloads.isDirectory) downloads else File(System.getProperty("java.io.tmpdir"))
        directory.mkdirs()
        return directory
    }
}
