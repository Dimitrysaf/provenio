package io.github.dimitrysaf.provenio.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DefaultWindowExceptionHandlerFactory
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory

/**
 * Skips frames that touch a popup layer Compose already closed mid-frame, and keeps the default
 * error dialog for everything else.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal object AppWindowExceptionHandlerFactory : WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: java.awt.Window): WindowExceptionHandler {
        val fallback = DefaultWindowExceptionHandlerFactory.exceptionHandler(window)
        return WindowExceptionHandler { throwable ->
            if (throwable.isDisposedLayerRace()) {
                System.err.println("Skipped a frame after a closed popup: ${throwable.message}")
                window.repaint()
            } else {
                fallback.onException(throwable)
            }
        }
    }

    private fun Throwable.isDisposedLayerRace(): Boolean =
        (this is IllegalArgumentException || this is IllegalStateException) && message in DisposedLayerMessages

    private val DisposedLayerMessages = setOf(
        "RootNodeOwner is already disposed",
        "AttachedComposeSceneLayer is closed",
    )
}
