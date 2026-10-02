package io.github.dimitrysaf.provenio.desktop

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

internal sealed interface SaveChoice {
    data class Chosen(val file: File) : SaveChoice
    data object Cancelled : SaveChoice
}

// The system's own "Save as" dialog: the desktop portal on Linux, the platform's native dialog otherwise.
internal object DesktopFileSaver {
    private val osName = System.getProperty("os.name").orEmpty().lowercase()

    fun chooseSaveFile(title: String, suggestedName: String): SaveChoice {
        if (osName.contains("linux")) {
            val options = "'current_name': <${suggestedName.toGVariantString()}>"
            runCatching { FileChooserPortal.request("SaveFile", title, options) }.getOrNull()?.let { choice ->
                return when (choice) {
                    is FolderChoice.Chosen -> SaveChoice.Chosen(choice.folder)
                    FolderChoice.Cancelled -> SaveChoice.Cancelled
                }
            }
        }
        return chooseWithNativeDialog(title, suggestedName)
    }

    // Windows' and macOS's own save dialogs; on Linux without the portal, GTK's.
    private fun chooseWithNativeDialog(title: String, suggestedName: String): SaveChoice {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        dialog.file = suggestedName
        dialog.isVisible = true
        val directory = dialog.directory ?: return SaveChoice.Cancelled
        val file = dialog.file ?: return SaveChoice.Cancelled
        return SaveChoice.Chosen(File(directory, file))
    }
}
