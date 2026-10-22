package io.github.dimitrysaf.provenio.core.stremio

import java.io.File

internal actual object StremioImportPlatform {
    actual fun savedAuthKeys(): List<String> = runCatching(::findSavedAuthKeys).getOrDefault(emptyList())

    actual fun inflate(data: ByteArray): ByteArray? = inflateZlib(data)
}

private const val MaxStorageFileBytes = 64L * 1024 * 1024
private const val MaxSearchDepth = 7

// Stremio 4 keeps its web storage under "Smart Code ltd", Stremio 5 under a folder of its own name.
private val stremioFolderNames = listOf("stremio", "smart code ltd")

// Stremio 4 saves the sign-in as `"authKey":"…"`, Stremio 5 as `"auth":{"key":"…"`.
private val authKeyPatterns = listOf(
    Regex(""""authKey"\s*:\s*"([A-Za-z0-9_-]{16,128})""""),
    Regex(""""auth"\s*:\s*\{\s*"key"\s*:\s*"([A-Za-z0-9_-]{16,128})""""),
)

/**
 * The Stremio app is a web page in an embedded browser, which keeps the page's storage, sign-in
 * included, in LevelDB files. Their records are read as text, as the browser wrote them (Latin-1
 * or UTF-16), without opening the database, so files of an older or newer browser read the same.
 */
private fun findSavedAuthKeys(): List<String> {
    val files = stremioFolders()
        .flatMap(::localStorageFolders)
        .distinct()
        .flatMap { folder ->
            folder.listFiles { file ->
                file.isFile && file.length() in 1..MaxStorageFileBytes &&
                    (file.name.endsWith(".log") || file.name.endsWith(".ldb"))
            }.orEmpty().asList()
        }
        .sortedByDescending(File::lastModified)
    val keys = linkedSetOf<String>()
    files.forEach { file -> runCatching { keys += authKeysIn(file.readBytes()) } }
    return keys.toList()
}

private fun stremioFolders(): List<File> {
    val home = File(System.getProperty("user.home").orEmpty())
    val roots = listOfNotNull(
        System.getenv("APPDATA")?.let { File(it) },
        System.getenv("LOCALAPPDATA")?.let { File(it) },
        File(home, "Library/Application Support"),
        System.getenv("XDG_DATA_HOME")?.let { File(it) } ?: File(home, ".local/share"),
        System.getenv("XDG_CONFIG_HOME")?.let { File(it) } ?: File(home, ".config"),
    )
    val named = roots.filter(File::isDirectory).flatMap { root ->
        root.listFiles { file -> file.isDirectory && file.name.lowercase() in stremioFolderNames }.orEmpty().asList()
    }
    // The Flathub build of Stremio keeps everything inside its sandbox's own folder.
    val flatpak = File(home, ".var/app/com.stremio.Stremio").takeIf(File::isDirectory)
    return named + listOfNotNull(flatpak)
}

private fun localStorageFolders(root: File): List<File> =
    root.walkTopDown()
        .maxDepth(MaxSearchDepth)
        .onEnter { folder -> folder.name !in skippedFolderNames }
        .filter { it.isDirectory && it.name == "leveldb" && it.parentFile?.name == "Local Storage" }
        .toList()

private val skippedFolderNames = setOf("Cache", "Code Cache", "GPUCache", "Service Worker", "blob_storage", "stremio-cache")

private fun authKeysIn(bytes: ByteArray): List<String> {
    val texts = listOf(
        String(bytes, Charsets.ISO_8859_1),
        String(bytes, Charsets.UTF_16LE),
        String(bytes, 1, bytes.size - 1, Charsets.UTF_16LE),
    )
    // Later records in a file are newer, so the last match comes first.
    return texts
        .flatMap { text -> authKeyPatterns.flatMap { pattern -> pattern.findAll(text).map { it.range.first to it.groupValues[1] }.toList() } }
        .sortedByDescending { it.first }
        .map { it.second }
}
