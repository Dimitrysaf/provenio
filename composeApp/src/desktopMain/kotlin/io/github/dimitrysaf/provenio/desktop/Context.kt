package io.github.dimitrysaf.provenio.desktop

import java.io.File

/**
 * The part of Android's Context the platform storage relies on, for the desktop app.
 *
 * Directories follow the XDG base directory spec, so under Flatpak they land in the app's own
 * ~/.var/app/<id> tree and are removed with it.
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
            Context(
                filesDir = xdg("XDG_DATA_HOME", ".local/share"),
                cacheDir = xdg("XDG_CACHE_HOME", ".cache"),
                configDir = xdg("XDG_CONFIG_HOME", ".config"),
            ).also { context ->
                listOf(context.filesDir, context.cacheDir, context.configDir).forEach { it.mkdirs() }
            }
        }

        private const val APP_DIRECTORY = "provenio"
    }
}
