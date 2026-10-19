package io.github.dimitrysaf.provenio.desktop

import java.io.File
import java.nio.file.Files
import java.util.Properties

/**
 * Where the desktop app keeps its cache and its data when the user moved them in Storage. The
 * choice lives in a small file in the default config folder, which never moves itself, and the
 * folders' contents move the next time the app starts, before anything opens them.
 */
internal object DesktopStorageLocations {
    /** The folders the app uses this run. */
    class Folders(val filesDir: File, val configDir: File, val cacheDir: File)

    private var settingsFile: File? = null
    private var current: Folders? = null

    /** Works out this run's folders from the defaults and any choice, finishing a pending move first. */
    fun resolve(defaultFiles: File, defaultConfig: File, defaultCache: File): Folders {
        val file = File(defaultConfig, FileName).also { settingsFile = it }
        val properties = load(file)
        val dataRoot = properties.getProperty(DataKey)?.takeIf { it.isNotBlank() }?.let(::File)
        val folders = Folders(
            filesDir = dataRoot?.let { File(it, FilesFolder) } ?: defaultFiles,
            configDir = dataRoot?.let { File(it, ConfigFolder) } ?: defaultConfig,
            cacheDir = properties.getProperty(CacheKey)?.takeIf { it.isNotBlank() }?.let(::File) ?: defaultCache,
        )
        val pending = listOf(MoveCacheFromKey, MoveFilesFromKey, MoveConfigFromKey).filter { properties.containsKey(it) }
        if (pending.isNotEmpty()) {
            properties.getProperty(MoveCacheFromKey)?.let { moveContents(File(it), folders.cacheDir) }
            properties.getProperty(MoveFilesFromKey)?.let { moveContents(File(it), folders.filesDir) }
            properties.getProperty(MoveConfigFromKey)?.let { moveContents(File(it), folders.configDir, keep = FileName) }
            pending.forEach(properties::remove)
            save(file, properties)
        }
        current = folders
        return folders
    }

    /** The folder the cache is in, or the one it moves to on the next start. */
    fun cacheRoot(): File? {
        val properties = settingsFile?.let(::load) ?: return current?.cacheDir
        return properties.getProperty(CacheKey)?.takeIf { it.isNotBlank() }?.let(::File) ?: current?.cacheDir
    }

    /** The folder the app's data is in, or the one it moves to on the next start. */
    fun dataRoot(): File? {
        val properties = settingsFile?.let(::load) ?: return current?.filesDir
        return properties.getProperty(DataKey)?.takeIf { it.isNotBlank() }?.let(::File) ?: current?.filesDir
    }

    /** Moves the cache into [root] on the next start. */
    fun moveCache(root: File) {
        val file = settingsFile ?: return
        val folders = current ?: return
        val properties = load(file)
        properties.setProperty(CacheKey, root.absolutePath)
        properties.setProperty(MoveCacheFromKey, folders.cacheDir.absolutePath)
        save(file, properties)
    }

    /** Moves the app's data, its files and its settings, into [root] on the next start. */
    fun moveData(root: File) {
        val file = settingsFile ?: return
        val folders = current ?: return
        val properties = load(file)
        properties.setProperty(DataKey, root.absolutePath)
        properties.setProperty(MoveFilesFromKey, folders.filesDir.absolutePath)
        properties.setProperty(MoveConfigFromKey, folders.configDir.absolutePath)
        save(file, properties)
    }

    /**
     * Moves everything in [from] into [to], renaming where both are on one disk and copying where
     * they are not. [keep] stays behind, and so does anything [to] is inside of.
     */
    private fun moveContents(from: File, to: File, keep: String? = null) {
        if (!from.isDirectory) return
        val target = to.canonicalFile
        if (from.canonicalFile == target) return
        target.mkdirs()
        from.listFiles()?.forEach { child ->
            if (child.name == keep) return@forEach
            val canonical = child.canonicalFile
            if (target.path.startsWith(canonical.path)) return@forEach
            val destination = File(target, child.name)
            val renamed = !destination.exists() &&
                runCatching { Files.move(child.toPath(), destination.toPath()) }.isSuccess
            if (!renamed) {
                runCatching {
                    child.copyRecursively(destination, overwrite = true)
                    child.deleteRecursively()
                }
            }
        }
    }

    private fun load(file: File): Properties = Properties().apply {
        if (file.isFile) runCatching { file.inputStream().use(::load) }
    }

    private fun save(file: File, properties: Properties) {
        runCatching {
            file.parentFile?.mkdirs()
            file.outputStream().use { properties.store(it, null) }
        }
    }

    private const val FileName = "locations.properties"
    private const val CacheKey = "cache"
    private const val DataKey = "data"
    private const val MoveCacheFromKey = "move.cache.from"
    private const val MoveFilesFromKey = "move.files.from"
    private const val MoveConfigFromKey = "move.config.from"
    private const val FilesFolder = "files"
    private const val ConfigFolder = "config"
}
