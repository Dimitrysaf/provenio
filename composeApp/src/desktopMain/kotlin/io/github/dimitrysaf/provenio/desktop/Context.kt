package io.github.dimitrysaf.provenio.desktop

import java.io.File

/**
 * The part of Android's Context the platform storage relies on, for the desktop app.
 *
 * Directories follow the XDG base directory spec, so under Flatpak they land in the app's own
 * ~/.var/app/<id> tree and are removed with it. On Windows the cache goes to
 * %LOCALAPPDATA%\Provenio\Cache instead, where the system's own cleanup tools look for it. The cache
 * and the data can also be moved to folders the user picks, see [DesktopStorageLocations].
 */
class Context private constructor(
    val filesDir: File,
    val cacheDir: File,
    val configDir: File,
) {
    val applicationContext: Context get() = this

    /** Android's no-backup directory; the desktop has no backup to keep it out of. */
    val noBackupFilesDir: File get() = filesDir

    fun getSharedPreferences(name: String, @Suppress("UNUSED_PARAMETER") mode: Int): SharedPreferences =
        SharedPreferences.open(File(configDir, "preferences"), name)

    companion object {
        const val MODE_PRIVATE: Int = 0

        val app: Context by lazy {
            val home = System.getProperty("user.home")
            fun xdg(variable: String, fallback: String): File =
                File(System.getenv(variable)?.takeIf { it.isNotBlank() } ?: "$home/$fallback", APP_DIRECTORY)
            val linuxStyleCache = xdg("XDG_CACHE_HOME", ".cache")
            val defaultCache = windowsCacheDirectory() ?: linuxStyleCache
            val folders = DesktopStorageLocations.resolve(
                defaultFiles = xdg("XDG_DATA_HOME", ".local/share"),
                defaultConfig = xdg("XDG_CONFIG_HOME", ".config"),
                defaultCache = defaultCache,
            )
            Context(
                filesDir = folders.filesDir,
                cacheDir = folders.cacheDir,
                configDir = folders.configDir,
            ).also { context ->
                listOf(context.filesDir, context.cacheDir, context.configDir).forEach { it.mkdirs() }
                if (defaultCache != linuxStyleCache && folders.cacheDir != linuxStyleCache) removeOldCache(linuxStyleCache)
            }
        }

        /** Windows' own place for an app's cache, unless XDG_CACHE_HOME asks for another; null elsewhere. */
        private fun windowsCacheDirectory(): File? {
            if (!System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")) return null
            if (!System.getenv("XDG_CACHE_HOME").isNullOrBlank()) return null
            val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
                ?: "${System.getProperty("user.home")}\\AppData\\Local"
            return File(File(localAppData, WINDOWS_APP_DIRECTORY), "Cache")
        }

        /** Deletes the cache older versions kept in ~/.cache on Windows, in the background since it can be large. */
        private fun removeOldCache(directory: File) {
            if (!directory.isDirectory) return
            Thread({ runCatching { directory.deleteRecursively() } }, "old-cache-cleanup").apply {
                isDaemon = true
                start()
            }
        }

        private const val APP_DIRECTORY = "provenio"
        private const val WINDOWS_APP_DIRECTORY = "Provenio"
    }
}
