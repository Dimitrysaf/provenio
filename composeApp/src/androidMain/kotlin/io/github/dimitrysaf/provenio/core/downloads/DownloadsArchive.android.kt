package io.github.dimitrysaf.provenio.core.downloads

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

internal actual object DownloadsArchive {
    private const val RecordSuffix = ".json"

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun save(fileName: String, record: String) {
        val directory = recordDirectory() ?: return
        runCatching {
            if (!directory.isDirectory) directory.mkdirs()
            val target = recordFile(directory, fileName)
            val temporary = File(directory, ".${target.name}.tmp")
            temporary.writeText(record)
            if (!temporary.renameTo(target)) {
                target.delete()
                temporary.renameTo(target)
            }
        }
    }

    actual fun remove(fileName: String) {
        val directory = recordDirectory() ?: return
        runCatching { recordFile(directory, fileName).delete() }
    }

    actual fun records(): List<String> {
        val directory = recordDirectory() ?: return emptyList()
        return directory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(RecordSuffix) }
            .mapNotNull { runCatching { it.readText() }.getOrNull() }
    }

    actual fun mediaFileUris(): List<String> {
        val context = appContext ?: return emptyList()
        return AndroidDownloadPublisher.directory(context).listFiles().orEmpty()
            .filter { it.isFile && it.extension.lowercase() in VideoExtensions }
            .map { it.toURI().toString() }
    }

    // Documents/Provenio from Android 11, which survives clearing the app's data like Movies/Provenio
    // does. Before that, finished videos live in the app's own storage and go with its data anyway.
    private fun recordDirectory(): File? {
        if (Build.VERSION.SDK_INT < 30) return null
        return File(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Provenio"),
            ".downloads",
        )
    }

    private fun recordFile(directory: File, fileName: String): File =
        File(directory, File(fileName).name + RecordSuffix)
}

private val VideoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "mpg", "mpeg", "wmv", "flv")
