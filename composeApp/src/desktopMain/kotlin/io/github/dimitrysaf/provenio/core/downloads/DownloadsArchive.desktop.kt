package io.github.dimitrysaf.provenio.core.downloads

import java.io.File

internal actual object DownloadsArchive {
    private const val RecordSuffix = ".json"

    actual fun save(fileName: String, record: String) {
        val directory = recordDirectory()
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
        runCatching { recordFile(recordDirectory(), fileName).delete() }
    }

    actual fun records(): List<String> =
        recordDirectory().listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(RecordSuffix) }
            .mapNotNull { runCatching { it.readText() }.getOrNull() }

    actual fun mediaFileUris(): List<String> =
        DesktopDownloadsLocation.directory().listFiles().orEmpty()
            .filter { it.isFile && it.extension.lowercase() in VideoExtensions }
            .map { it.toURI().toString() }

    private fun recordDirectory(): File = File(DesktopDownloadsLocation.directory(), ".provenio")

    private fun recordFile(directory: File, fileName: String): File =
        File(directory, File(fileName).name + RecordSuffix)
}

private val VideoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "mpg", "mpeg", "wmv", "flv")
