package io.github.dimitrysaf.provenio.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import co.touchlab.kermit.Logger
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import kotlin.math.roundToInt

private val log = Logger.withTag("Fullscreen")

/** Borderless fullscreen over the window's screen; leaving brings the borders back at the earlier size, or [defaultSize] if that no longer fits. macOS keeps its own. */
internal class WindowFullscreen(private val defaultSize: DpSize) {
    private val isMac = System.getProperty("os.name").orEmpty().lowercase().contains("mac")

    private var applied = false
    private var windowedBounds: Rectangle? = null
    private var wasMaximized = false
    private var wasResizable = true
    private var macWindowed: Triple<WindowPlacement, DpSize, WindowPosition>? = null

    fun apply(window: Frame, state: WindowState, fullscreen: Boolean) {
        // A minimized window is left as it is; the change waits until it is back on screen.
        if (state.isMinimized || fullscreen == applied) return
        log.i { "Fullscreen $fullscreen: ${window.bounds}, maximized ${window.isMaximized}" }
        applied = fullscreen
        when {
            isMac -> applyMac(state, fullscreen)
            fullscreen -> enter(window)
            else -> leave(window)
        }
    }

    private fun enter(window: Frame) {
        val screen = window.graphicsConfiguration.bounds
        wasMaximized = window.isMaximized
        windowedBounds = window.bounds
        wasResizable = window.isResizable
        window.extendedState = Frame.NORMAL
        // An undecorated, resizable Compose window grows resize handles at its edges.
        window.isResizable = false
        redecorate(window, undecorated = true, bounds = screen)
        window.toFront()
    }

    private fun leave(window: Frame) {
        val gc = window.graphicsConfiguration
        val insets = runCatching { Toolkit.getDefaultToolkit().getScreenInsets(gc) }.getOrNull()
        val screen = gc.bounds
        val usable = Rectangle(
            screen.x + (insets?.left ?: 0),
            screen.y + (insets?.top ?: 0),
            screen.width - (insets?.left ?: 0) - (insets?.right ?: 0),
            screen.height - (insets?.top ?: 0) - (insets?.bottom ?: 0),
        )
        redecorate(window, undecorated = false, bounds = windowedBounds(usable))
        window.isResizable = wasResizable
        if (wasMaximized) window.extendedState = Frame.MAXIMIZED_BOTH
    }

    // The size from before fullscreen while it fits the screen; otherwise the default size, centred, and never larger than the screen.
    private fun windowedBounds(usable: Rectangle): Rectangle {
        val remembered = windowedBounds
        if (remembered != null && remembered.width <= usable.width && remembered.height <= usable.height) {
            return if (usable.contains(remembered)) remembered else centred(remembered.width, remembered.height, usable)
        }
        val width = defaultSize.width.value.roundToInt().coerceAtMost(usable.width)
        val height = defaultSize.height.value.roundToInt().coerceAtMost(usable.height)
        return centred(width, height, usable)
    }

    private fun centred(width: Int, height: Int, usable: Rectangle) =
        Rectangle(usable.x + (usable.width - width) / 2, usable.y + (usable.height - height) / 2, width, height)

    // Decorations only change while the window has no native window, so it is taken down and shown again around the change.
    private fun redecorate(window: Frame, undecorated: Boolean, bounds: Rectangle) {
        if (window.isUndecorated != undecorated) {
            window.isVisible = false
            window.removeNotify()
            window.isUndecorated = undecorated
        }
        window.bounds = bounds
        window.isVisible = true
    }

    private fun applyMac(state: WindowState, fullscreen: Boolean) {
        if (fullscreen) {
            macWindowed = Triple(state.placement, state.size, state.position)
            state.placement = WindowPlacement.Fullscreen
        } else {
            val restored = macWindowed
            macWindowed = null
            state.placement = restored?.first ?: WindowPlacement.Floating
            if (restored != null) {
                state.size = restored.second
                state.position = restored.third
            }
        }
    }

    private val Frame.isMaximized: Boolean get() = extendedState and Frame.MAXIMIZED_BOTH == Frame.MAXIMIZED_BOTH
}
