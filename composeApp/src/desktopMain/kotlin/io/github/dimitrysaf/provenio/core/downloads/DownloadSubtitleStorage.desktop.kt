package io.github.dimitrysaf.provenio.core.downloads

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal actual class DownloadSubtitleStorage actual constructor(localVideoUri: String) {
    private val directory = File(File(URI(localVideoUri)).path + ".subtitles")

    actual fun read(fileName: String): String? =
        runCatching { file(fileName).readText(Charsets.UTF_8) }.getOrNull()

    actual fun write(fileName: String, text: String) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create subtitle directory" }
        val target = file(fileName)
        val temporary = File(directory, ".$fileName.tmp")
        temporary.writeText(text, Charsets.UTF_8)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    actual fun localFileUri(fileName: String): String? =
        file(fileName).takeIf { it.isFile }?.toURI()?.toString()

    actual fun remove() {
        directory.deleteRecursively()
    }

    private fun file(fileName: String): File {
        require(fileName.isNotBlank() && File(fileName).name == fileName && fileName != "." && fileName != "..")
        return File(directory, fileName)
    }
}
