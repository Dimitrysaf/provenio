package io.github.dimitrysaf.provenio.core.diagnostics

import android.content.Context
import android.os.Build
import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object AppLogs {
    actual val isSupported: Boolean = true

    private const val MaxLogcatBytes = 32 * 1024 * 1024
    private const val KeptCrashes = 5

    private var appContext: Context? = null

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        recordCrashes(crashDirectory(context.applicationContext))
    }

    actual suspend fun collect(): AppLogBundle? = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext null
        val files = linkedMapOf<String, ByteArray>()
        files["provenio.log"] = (deviceSummary(context) + "\n").encodeToByteArray() + logcat()
        crashDirectory(context).listFiles().orEmpty()
            .filter { it.isFile }
            .sortedBy { it.name }
            .forEach { crash -> files["crashes/${crash.name}"] = crash.readBytes() }
        bundle(files)
    }

    // The app's own log buffer at every level: its messages, the engine's, the player's and any crash.
    private fun logcat(): ByteArray = runCatching {
        val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-b", "main,system,crash")
            .redirectErrorStream(true)
            .start()
        val output = ByteArrayOutputStream()
        process.inputStream.use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (output.size() < MaxLogcatBytes) output.write(buffer, 0, read)
            }
        }
        process.waitFor()
        output.toByteArray()
    }.getOrElse { error -> "Could not read the log: ${error.message}\n".encodeToByteArray() }

    private fun deviceSummary(context: Context): String {
        val packageInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        return listOf(
            "Provenio ${AppVersionConfig.VERSION_NAME} (${AppVersionConfig.BUILD_COMMIT})",
            "Package ${context.packageName} ${packageInfo?.versionName.orEmpty()}",
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Device ${Build.MANUFACTURER} ${Build.MODEL}",
            "ABIs ${Build.SUPPORTED_ABIS.joinToString()}",
            "Saved ${timestamp("yyyy-MM-dd HH:mm:ss Z")}",
        ).joinToString("\n")
    }

    // Logcat after a restart no longer holds a crash, so each one is also written to a file.
    private fun recordCrashes(directory: File) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                directory.mkdirs()
                File(directory, "crash-${timestamp("yyyyMMdd-HHmmss")}.txt")
                    .writeText("Thread ${thread.name}\n${error.stackTraceToString()}")
                directory.listFiles().orEmpty()
                    .sortedByDescending { it.name }
                    .drop(KeptCrashes)
                    .forEach { it.delete() }
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun crashDirectory(context: Context): File = File(context.filesDir, "logs/crashes")
}

internal fun bundle(files: Map<String, ByteArray>): AppLogBundle {
    val stamp = timestamp("yyyyMMdd-HHmmss")
    files.entries.singleOrNull()?.let { (_, bytes) ->
        return AppLogBundle("$AppLogBaseName-$stamp.log", "text/plain", bytes)
    }
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        files.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return AppLogBundle("$AppLogBaseName-$stamp.zip", "application/zip", output.toByteArray())
}

private fun timestamp(pattern: String): String = SimpleDateFormat(pattern, Locale.US).format(Date())
