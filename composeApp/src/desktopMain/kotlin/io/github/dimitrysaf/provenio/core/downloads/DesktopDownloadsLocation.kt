package io.github.dimitrysaf.provenio.core.downloads

import io.github.dimitrysaf.provenio.desktop.SharedPreferences
import java.awt.Desktop
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.JFileChooser

// Where desktop downloads go: a Provenio folder in the user's Videos by default, or a folder they chose.
internal object DesktopDownloadsLocation {
    private const val DirectoryKey = "download_directory"
    private const val FolderName = "Provenio"

    private var preferences: SharedPreferences? = null

    fun initialize(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    fun directory(): File =
        preferences?.getString(DirectoryKey, null)?.takeIf { it.isNotBlank() }?.let(::File) ?: defaultDirectory()

    // Shows the system folder picker; true when a new folder was chosen.
    fun choose(): Boolean {
        val chooser = JFileChooser(directory().takeIf { it.isDirectory } ?: directory().parentFile).apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return false
        val chosen = chooser.selectedFile ?: return false
        preferences?.edit()?.putString(DirectoryKey, chosen.absolutePath)?.apply()
        return true
    }

    fun open(): Boolean {
        val folder = directory()
        if (!folder.isDirectory && !folder.mkdirs()) return false
        val desktop = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
        if (desktop && runCatching { Desktop.getDesktop().open(folder) }.isSuccess) return true
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val command = when {
            os.contains("win") -> listOf("explorer", folder.absolutePath)
            os.contains("mac") -> listOf("open", folder.absolutePath)
            else -> listOf("xdg-open", folder.absolutePath)
        }
        return runCatching { ProcessBuilder(command).start() }.isSuccess
    }

    private fun defaultDirectory(): File {
        val home = File(System.getProperty("user.home"))
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val videos = when {
            os.contains("mac") -> File(home, "Movies")
            os.contains("win") -> File(home, "Videos")
            else -> linuxVideosDirectory() ?: File(home, "Videos")
        }
        val parent = if (videos.isDirectory) videos else File(home, "Downloads")
        return File(parent, FolderName)
    }

    // The Videos folder under its localized name, as the desktop's user directories name it.
    private fun linuxVideosDirectory(): File? = runCatching {
        val process = ProcessBuilder("xdg-user-dir", "VIDEOS").redirectErrorStream(true).start()
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroy()
            return@runCatching null
        }
        process.inputStream.bufferedReader().readText().trim()
            .takeIf { it.isNotEmpty() && it != System.getProperty("user.home") }
            ?.let(::File)
    }.getOrNull()
}
