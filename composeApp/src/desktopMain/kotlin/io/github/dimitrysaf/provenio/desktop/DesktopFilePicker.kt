package io.github.dimitrysaf.provenio.desktop

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// The system's own "Open" dialog for a picture: the desktop portal on Linux, which a Flatpak needs
// to see the user's files, and the platform's native dialog otherwise.
internal object DesktopFilePicker {
    private val osName = System.getProperty("os.name").orEmpty().lowercase()
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    fun chooseImage(title: String): File? {
        if (osName.contains("linux")) {
            val options = "'filters': <[('Images', [(uint32 0, '*.jpg'), (uint32 0, '*.jpeg'), (uint32 0, '*.png'), (uint32 0, '*.webp')])]>"
            runCatching { FileChooserPortal.request("OpenFile", title, options) }.getOrNull()?.let { choice ->
                return (choice as? FolderChoice.Chosen)?.folder
            }
        }
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in imageExtensions }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        return File(directory, file)
    }
}
