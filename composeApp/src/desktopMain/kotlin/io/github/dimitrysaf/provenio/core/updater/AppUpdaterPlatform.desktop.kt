package io.github.dimitrysaf.provenio.core.updater

import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import io.github.dimitrysaf.provenio.desktop.UpdateFailedArgument
import io.github.dimitrysaf.provenio.desktop.UpdateFileArgument
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
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
            val target = updateFile(assetName)
            if (target.exists()) {
                onProgress(target.length(), target.length())
                return@runCatching target.absolutePath
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

            check(partial.renameTo(target)) { "Could not save ${target.name}" }
            target.absolutePath
        }
    }

    actual fun completedUpdatePath(fileName: String): String? =
        updateFile(fileName).takeIf { it.exists() }?.absolutePath

    actual fun hasPartialUpdate(fileName: String): Boolean {
        val file = updateFile(fileName)
        return File(file.parentFile, file.name + PartialSuffix).exists()
    }

    actual fun clearUpdateFiles(keepFileName: String?) {
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

    private fun updatesDirectory(): File {
        val base = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let { File(it, "Provenio") }
            ?: File(System.getProperty("user.home"), ".provenio")
        return File(base, "updates").apply { mkdirs() }
    }

    private fun updateFile(fileName: String): File =
        File(updatesDirectory(), fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_"))

    actual fun canRequestPackageInstalls(): Boolean = true

    actual fun openUnknownSourcesSettings() = Unit

    actual fun installDownloadedApk(path: String): Result<Unit> = runCatching {
        val file = File(path)
        check(file.exists()) { "The downloaded update is missing" }
        if (isWindows) {
            val script = writeWindowsUpdateScript(file)
            ProcessBuilder("wscript", "//B", "//Nologo", script.absolutePath)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            exitProcess(0)
        }
        Desktop.getDesktop().open(file)
    }

    private fun writeWindowsUpdateScript(msi: File): File {
        val appExe = System.getProperty("jpackage.app-path")
            ?.let(::File)
            ?.takeIf { it.isFile }
        val installDir = appExe?.parentFile?.absolutePath
        val install = buildString {
            append("msiexec /i ${vbsQuoted(msi.absolutePath)} /qn /norestart")
            if (installDir != null) append(" INSTALLDIR=${vbsQuoted(installDir)}")
        }
        val lines = buildList {
            add("Set shell = CreateObject(\"WScript.Shell\")")
            add("WScript.Sleep 2000")
            add("code = shell.Run(${vbsString(install)}, 0, True)")
            if (appExe != null) {
                val launch = vbsQuoted(appExe.absolutePath)
                add("If code = 0 Or code = 1641 Or code = 3010 Then")
                add("    shell.Run ${vbsString(launch)}, 1, False")
                add("Else")
                add("    shell.Run ${vbsString("$launch $UpdateFailedArgument")} & code & ${vbsString(" " + vbsQuoted(UpdateFileArgument + msi.absolutePath))}, 1, False")
                add("End If")
            }
        }
        return File(msi.parentFile, "install-update.vbs").apply {
            val text = lines.joinToString("\r\n", postfix = "\r\n")
            writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE))
        }
    }

    private fun vbsString(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private fun vbsQuoted(path: String): String = "\"$path\""

    private const val PartialSuffix = ".part"
    private const val HttpRangeNotSatisfiable = 416
}
