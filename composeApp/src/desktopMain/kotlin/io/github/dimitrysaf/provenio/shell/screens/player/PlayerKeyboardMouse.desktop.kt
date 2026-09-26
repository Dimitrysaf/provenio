package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import io.github.dimitrysaf.provenio.desktop.DesktopWindowState
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

private val BlankCursor by lazy {
    PointerIcon(
        Toolkit.getDefaultToolkit().createCustomCursor(
            BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
            Point(0, 0),
            "provenio-blank",
        ),
    )
}

internal actual fun togglePlayerFullscreen(): Boolean {
    DesktopWindowState.setFullscreen(!DesktopWindowState.isFullscreen.value)
    return true
}

internal actual fun Modifier.playerCursorHidden(hidden: Boolean): Modifier =
    if (hidden) pointerHoverIcon(BlankCursor) else this
