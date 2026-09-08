package io.github.dimitrysaf.provenio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberUrlOpener(): (String) -> Unit = remember {
    { url: String ->
        try {
            val desktop = Desktop.getDesktop()
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        } catch (failure: Exception) {
            // Headless session or no registered browser; nothing to fall back to.
        }
    }
}
