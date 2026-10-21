package io.github.dimitrysaf.provenio.core.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
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
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object AppLogs {
    actual val isSupported: Boolean = true

    private const val MaxLogcatBytes = 32 * 1024 * 1024
    private const val KeptCrashes = 5
    private const val MaxTraceChars = 64 * 1024
    private const val SameCrashMillis = 30_000L
    private val CrashReasons = setOf(
        ApplicationExitInfo.REASON_CRASH,
        ApplicationExitInfo.REASON_CRASH_NATIVE,
        ApplicationExitInfo.REASON_ANR,
    )

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
        crashFiles(context).forEach { crash -> files["crashes/${crash.name}"] = crash.readBytes() }
        val exits = systemCrashes(context)
        if (exits.isNotEmpty()) {
            files["crashes/system.txt"] = exits.joinToString("\n\n") { it.report }.encodeToByteArray()
        }
        bundle(files)
    }

    actual suspend fun latestCrash(): AppCrash? = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext null
        crashes(context).maxByOrNull { it.timeMillis }
    }

    actual suspend fun unseenCrash(): AppCrash? = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext null
        val seen = crashesSeenAt(context)
        crashes(context).filter { it.timeMillis > seen }.maxByOrNull { it.timeMillis }
    }

    actual fun markCrashesSeen() {
        val context = appContext ?: return
        runCatching {
            seenFile(context).apply { parentFile?.mkdirs() }.writeText(System.currentTimeMillis().toString())
        }
    }

    // Both the crashes the app caught itself and the ones only the system saw, as a crash in the
    // engine's native code or the app stopping for not responding.
    // A crash the app caught is also one the system saw; the app's own record, with the whole
    // stack trace, is the one kept.
    private fun crashes(context: Context): List<AppCrash> {
        val own = crashFiles(context).map { AppCrash(it.lastModified(), it.readText()) }
        val system = systemCrashes(context).filter { seen ->
            own.none { abs(it.timeMillis - seen.timeMillis) < SameCrashMillis }
        }
        return own + system
    }

    private fun crashFiles(context: Context): List<File> =
        crashDirectory(context).listFiles().orEmpty().filter { it.isFile }.sortedBy { it.name }

    // How the app's last runs ended, as the system recorded it.
    private fun systemCrashes(context: Context): List<AppCrash> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val manager = context.getSystemService(ActivityManager::class.java) ?: return emptyList()
        val exits = runCatching { manager.getHistoricalProcessExitReasons(context.packageName, 0, KeptCrashes) }
            .getOrDefault(emptyList())
        return exits
            .filter { it.reason in CrashReasons }
            .map { exit ->
                val reason = when (exit.reason) {
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "Crashed in native code"
                    ApplicationExitInfo.REASON_ANR -> "Stopped for not responding"
                    else -> "Crashed"
                }
                // Only a hang's trace is text; a native crash's is a binary tombstone.
                val trace = if (exit.reason == ApplicationExitInfo.REASON_ANR) {
                    runCatching { exit.traceInputStream?.use { it.readBytes().decodeToString().take(MaxTraceChars) } }
                        .getOrNull()
                } else {
                    null
                }
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(exit.timestamp))
                AppCrash(
                    timeMillis = exit.timestamp,
                    report = listOfNotNull(
                        "$reason at $time",
                        exit.description?.let { "Description: $it" },
                        "Process ${exit.processName} (pid ${exit.pid}), exit status ${exit.status}",
                        trace,
                    ).joinToString("\n"),
                )
            }
    }

    private fun crashesSeenAt(context: Context): Long =
        runCatching { seenFile(context).readText().trim().toLong() }.getOrDefault(0L)

    private fun seenFile(context: Context): File = File(context.filesDir, "logs/crashes-seen")

    // The app's own log buffer at every level: its messages, the engine's, the player's and any crash.
    private fun logcat(): ByteArray = runCatching {
        // Every level, verbose up, from every buffer the app may read.
        val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "-b", "main,system,crash,events", "*:V")
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
            "Provenio ${AppVersionConfig.VERSION_NAME}",
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
