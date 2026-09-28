package io.github.dimitrysaf.provenio.desktop

import com.sun.jna.Pointer
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.UIManager
import kotlin.random.Random

private const val PortalBusName = "org.freedesktop.portal.Desktop"
private const val PortalObjectPath = "/org/freedesktop/portal/desktop"

internal sealed interface FolderChoice {
    data class Chosen(val folder: File) : FolderChoice
    data object Cancelled : FolderChoice
}

internal object DesktopFolderChooser {
    private val osName = System.getProperty("os.name").orEmpty().lowercase()

    fun chooseFolder(title: String, initial: File?): FolderChoice {
        if (osName.contains("linux")) {
            runCatching { PortalFolderChooser.choose(title) }.getOrNull()?.let { return it }
        }
        return if (osName.contains("mac")) chooseWithMacDialog(title, initial) else chooseWithSystemLookAndFeel(title, initial)
    }

    private fun chooseWithMacDialog(title: String, initial: File?): FolderChoice {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        try {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            initial?.let { dialog.directory = it.absolutePath }
            dialog.isVisible = true
            val directory = dialog.directory ?: return FolderChoice.Cancelled
            val file = dialog.file ?: return FolderChoice.Cancelled
            return FolderChoice.Chosen(File(directory, file))
        } finally {
            System.setProperty("apple.awt.fileDialogForDirectories", "false")
        }
    }

    private fun chooseWithSystemLookAndFeel(title: String, initial: File?): FolderChoice {
        val previous = UIManager.getLookAndFeel()
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
        try {
            val chooser = JFileChooser(initial).apply {
                dialogTitle = title
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                isAcceptAllFileFilterUsed = false
            }
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return FolderChoice.Cancelled
            return chooser.selectedFile?.let(FolderChoice::Chosen) ?: FolderChoice.Cancelled
        } finally {
            runCatching { UIManager.setLookAndFeel(previous) }
        }
    }
}

private object PortalFolderChooser {
    private val glib: GLib get() = DesktopDbus.glib
    private val gio: Gio get() = DesktopDbus.gio
    private val gobject: GObject get() = DesktopDbus.gobject

    fun choose(title: String): FolderChoice? {
        val connection = gio.g_bus_get_sync(GBusTypeSession, null, null) ?: return null
        val context = glib.g_main_context_new()
        glib.g_main_context_push_thread_default(context)
        try {
            val uniqueName = gio.g_dbus_connection_get_unique_name(connection) ?: return null
            val token = "provenio${Random.nextInt(0, Int.MAX_VALUE)}"
            val requestPath = "/org/freedesktop/portal/desktop/request/${uniqueName.removePrefix(":").replace('.', '_')}/$token"
            var response: String? = null
            val callback = object : SignalCallback {
                override fun invoke(
                    connection: Pointer?,
                    senderName: String?,
                    objectPath: String?,
                    interfaceName: String?,
                    signalName: String?,
                    parameters: Pointer?,
                    userData: Pointer?,
                ) {
                    response = parameters?.let(DesktopDbus::printVariant) ?: ""
                }
            }
            val subscription = gio.g_dbus_connection_signal_subscribe(
                connection,
                PortalBusName,
                "org.freedesktop.portal.Request",
                "Response",
                requestPath,
                null,
                0,
                callback,
                null,
                null,
            )
            try {
                val parameters = glib.g_variant_parse(
                    null,
                    "('', ${title.toGVariantString()}, {'handle_token': <${token.toGVariantString()}>, 'directory': <true>, 'modal': <true>})",
                    null,
                    null,
                    null,
                ) ?: return null
                val reply = gio.g_dbus_connection_call_sync(
                    connection,
                    PortalBusName,
                    PortalObjectPath,
                    "org.freedesktop.portal.FileChooser",
                    "OpenFile",
                    parameters,
                    null,
                    0,
                    -1,
                    null,
                    null,
                ) ?: return null
                glib.g_variant_unref(reply)
                while (response == null) {
                    glib.g_main_context_iteration(context, true)
                }
            } finally {
                gio.g_dbus_connection_signal_unsubscribe(connection, subscription)
            }
            return parseResponse(response.orEmpty())
        } finally {
            glib.g_main_context_pop_thread_default(context)
            glib.g_main_context_unref(context)
            gobject.g_object_unref(connection)
        }
    }

    private fun parseResponse(text: String): FolderChoice {
        val code = Regex("""^\(\s*(?:uint32\s+)?(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        if (code != 0) return FolderChoice.Cancelled
        val uri = Regex("""'uris':\s*<\['([^']+)'""").find(text)?.groupValues?.get(1) ?: return FolderChoice.Cancelled
        val path = runCatching { File(URI(uri)) }.getOrNull() ?: return FolderChoice.Cancelled
        return FolderChoice.Chosen(path)
    }
}
