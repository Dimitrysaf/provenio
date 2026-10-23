package io.github.dimitrysaf.provenio.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * Skips frames that touch a popup layer Compose already closed mid-frame. Anything else is a
 * crash: the app shows the error and ends. Compose's own handler would instead ask the window to
 * close, which hides it to the tray, so the process, its torrent engine and the tray icon stayed
 * behind, and the next launch only reached that hidden, broken window.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal object AppWindowExceptionHandlerFactory : WindowExceptionHandlerFactory {
    private val crashed = AtomicBoolean(false)

    override fun exceptionHandler(window: java.awt.Window): WindowExceptionHandler =
        WindowExceptionHandler { throwable ->
            if (throwable.isDisposedLayerRace()) {
                System.err.println("Skipped a frame after a closed popup: ${throwable.message}")
                window.repaint()
            } else {
                crash(window, throwable)
            }
        }

    private fun crash(window: java.awt.Window, throwable: Throwable) {
        // A broken composition keeps failing every frame; only the first failure ends the app.
        if (!crashed.compareAndSet(false, true)) return
        throwable.printStackTrace()
        // Later, so the dialog does not block the frame that failed.
        SwingUtilities.invokeLater {
            runCatching {
                JOptionPane.showMessageDialog(
                    window,
                    throwable.message ?: throwable.toString(),
                    "Provenio",
                    JOptionPane.ERROR_MESSAGE,
                )
            }
            runCatching { P2pStreamingEngine.shutdownAll() }
            SingleInstance.release()
            exitProcess(1)
        }
    }

    private fun Throwable.isDisposedLayerRace(): Boolean =
        (this is IllegalArgumentException || this is IllegalStateException) && message in DisposedLayerMessages

    private val DisposedLayerMessages = setOf(
        "RootNodeOwner is already disposed",
        "AttachedComposeSceneLayer is closed",
    )
}
