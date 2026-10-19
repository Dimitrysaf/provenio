package io.github.dimitrysaf.provenio.core.storage

import com.engine.Engine
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/** The bytes the files under this folder take, not following links; 0 when it does not exist. */
internal fun File.sizeOnDisk(): Long {
    if (!exists()) return 0L
    if (Files.isSymbolicLink(toPath())) return 0L
    if (isFile) return diskUsage()
    return walkTopDown()
        .onEnter { !Files.isSymbolicLink(it.toPath()) }
        .filter { Files.isRegularFile(it.toPath(), LinkOption.NOFOLLOW_LINKS) }
        .sumOf { it.diskUsage() }
}

/**
 * The space a file really takes. Torrent files are sparse and report their full size from the
 * start, so the engine, which measures them the same way for the cache limit, is asked.
 */
private fun File.diskUsage(): Long =
    runCatching { Engine.fileDiskUsage(absolutePath) }.getOrNull() ?: length()

/** The length of the file a file: URI points at, or null for any other kind of URI. */
internal fun fileUriLength(uri: String): Long? {
    if (!uri.startsWith("file:")) return null
    return runCatching { File(java.net.URI(uri)).takeIf { it.isFile }?.length() }.getOrNull()
}

/** Deletes everything inside this folder, keeping the folder itself. */
internal fun File.deleteContents() {
    listFiles()?.forEach { runCatching { it.deleteRecursively() } }
}

/**
 * The torrents under a torrent cache's payload folder, which keeps one folder per info hash. Each
 * is named from [titles], or else from the first file or folder inside it.
 */
internal fun cachedTorrentsIn(payloadRoots: List<File>, titles: Map<String, String>): List<CachedTorrent> =
    payloadRoots
        .flatMap { root -> root.listFiles()?.filter { it.isDirectory }.orEmpty() }
        .map { folder ->
            val infoHash = folder.name.lowercase()
            CachedTorrent(
                infoHash = infoHash,
                name = titles[infoHash]
                    ?: folder.listFiles()?.firstOrNull()?.let(::readableTorrentName)
                    ?: infoHash,
                bytes = folder.sizeOnDisk(),
            )
        }
        .filter { it.bytes > 0L }
        .groupBy { it.infoHash }
        .map { (_, copies) -> copies.first().copy(bytes = copies.sumOf { it.bytes }) }
        .sortedByDescending { it.bytes }

/** A release's file or folder name, without a file's extension and with dots and underscores as spaces. */
private fun readableTorrentName(entry: File): String {
    val name = if (entry.isFile) entry.nameWithoutExtension else entry.name
    if (name.contains(' ')) return name
    return name.replace('.', ' ').replace('_', ' ').trim().ifEmpty { entry.name }
}
