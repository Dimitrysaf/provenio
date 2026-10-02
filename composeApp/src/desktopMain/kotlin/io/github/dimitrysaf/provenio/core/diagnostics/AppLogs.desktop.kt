package io.github.dimitrysaf.provenio.core.diagnostics

import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import io.github.dimitrysaf.provenio.desktop.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object AppLogs {
    actual val isSupported: Boolean = true

    private const val CurrentName = "provenio.log"
    private const val KeptSessions = 3
    private const val MaxSessionBytes = 32L * 1024L * 1024L

    private var directory: File? = null
    private var sink: SessionLog? = null

    // Copies everything the app prints, its own logger and any uncaught error included, into this
    // session's file, keeping the files of the sessions before it.
    fun install(context: Context) {
        if (sink != null) return
        val logs = File(context.filesDir, "logs").apply { mkdirs() }
        directory = logs
        runCatching {
            for (index in KeptSessions - 1 downTo 1) {
                val older = File(logs, sessionName(index))
                val newer = File(logs, if (index == 1) CurrentName else sessionName(index - 1))
                if (newer.isFile) {
                    older.delete()
                    newer.renameTo(older)
                }
            }
            val log = SessionLog(FileOutputStream(File(logs, CurrentName)), MaxSessionBytes)
            sink = log
            log.write((summary() + "\n").encodeToByteArray())
            System.setOut(PrintStream(TeeStream(System.out, log), true, Charsets.UTF_8))
            System.setErr(PrintStream(TeeStream(System.err, log), true, Charsets.UTF_8))
        }
    }

    actual suspend fun collect(): AppLogBundle? = withContext(Dispatchers.IO) {
        val logs = directory ?: return@withContext null
        System.out.flush()
        System.err.flush()
        val files = linkedMapOf<String, ByteArray>()
        (listOf(CurrentName) + (1 until KeptSessions).map(::sessionName))
            .map { File(logs, it) }
            .filter { it.isFile && it.length() > 0L }
            .forEach { files[it.name] = it.readBytes() }
        if (files.isEmpty()) return@withContext null
        bundle(files)
    }

    private fun sessionName(index: Int): String = "provenio.$index.log"

    private fun summary(): String = listOf(
        "Provenio ${AppVersionConfig.VERSION_NAME}",
        "${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}",
        "Java ${System.getProperty("java.version")}",
        "Started ${timestamp("yyyy-MM-dd HH:mm:ss Z")}",
    ).joinToString("\n")
}

private class SessionLog(private val output: OutputStream, private val limit: Long) {
    private var written = 0L

    @Synchronized
    fun write(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        if (written >= limit) return
        val allowed = minOf(length.toLong(), limit - written).toInt()
        runCatching {
            output.write(bytes, offset, allowed)
            output.flush()
        }
        written += allowed
    }
}

private class TeeStream(private val original: OutputStream, private val log: SessionLog) : OutputStream() {
    override fun write(byte: Int) {
        original.write(byte)
        log.write(byteArrayOf(byte.toByte()))
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        original.write(bytes, offset, length)
        log.write(bytes, offset, length)
    }

    override fun flush() = original.flush()
}

private fun bundle(files: Map<String, ByteArray>): AppLogBundle {
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
