package io.github.dimitrysaf.provenio.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import co.touchlab.kermit.Logger
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import kotlin.math.roundToInt

private val log = Logger.withTag("Fullscreen")

/** Borderless fullscreen over the window's screen; leaving brings the borders back at the earlier size, or [defaultSize] if that no longer fits. */
internal class WindowFullscreen(private val defaultSize: DpSize) {
    private val os = System.getProperty("os.name").orEmpty().lowercase()
    private val isWindows = os.startsWith("windows")
    private val isMac = os.contains("mac")

    private var applied = false
    private var windowed: Rectangle? = null
    private var wasMaximized = false
    private var windowsStyle: Long? = null

    fun apply(window: Frame, state: WindowState, fullscreen: Boolean) {
        // A minimized window is left as it is; the change waits until it is back on screen.
        if (state.isMinimized || fullscreen == applied) return
        log.i { "Fullscreen $fullscreen: ${window.bounds}, placement ${state.placement}" }
        applied = fullscreen
        if (fullscreen) enter(window, state) else leave(window, state)
    }

    private fun enter(window: Frame, state: WindowState) {
        wasMaximized = state.placement == WindowPlacement.Maximized
        windowed = window.bounds
        if (isWindows) {
            state.placement = WindowPlacement.Floating
            windowsStyle = WindowsBorders.remove(window)
            window.bounds = window.graphicsConfiguration.bounds
            window.toFront()
        } else {
            // Linux asks the window manager for fullscreen, which takes the borders away; macOS uses its own.
            state.placement = WindowPlacement.Fullscreen
        }
    }

    private fun leave(window: Frame, state: WindowState) {
        if (isWindows) {
            windowsStyle?.let { WindowsBorders.restore(window, it) }
            windowsStyle = null
        } else {
            release(window)
        }
        val bounds = windowedBounds(usableArea(window))
        // macOS animates out of its own fullscreen, so it only gets the size through Compose.
        if (!isMac) window.bounds = bounds
        state.placement = if (wasMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
        if (!wasMaximized) {
            state.size = DpSize(bounds.width.dp, bounds.height.dp)
            state.position = WindowPosition(bounds.x.dp, bounds.y.dp)
        }
    }

    // Compose leaves fullscreen through the screen the window reports now, which on Linux need not be the one holding it.
    private fun release(window: Window) {
        runCatching { GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices }.getOrDefault(emptyArray())
            .filter { it.fullScreenWindow === window }
            .forEach { it.fullScreenWindow = null }
    }

    private fun usableArea(window: Window): Rectangle {
        val gc = window.graphicsConfiguration
        val screen = gc.bounds
        val insets = runCatching { Toolkit.getDefaultToolkit().getScreenInsets(gc) }.getOrNull() ?: return screen
        return Rectangle(
            screen.x + insets.left,
            screen.y + insets.top,
            screen.width - insets.left - insets.right,
            screen.height - insets.top - insets.bottom,
        )
    }

    // The size from before fullscreen while it fits the screen; otherwise the default size, centred, and never larger than the screen.
    private fun windowedBounds(usable: Rectangle): Rectangle {
        val remembered = windowed
        if (remembered != null && remembered.width <= usable.width && remembered.height <= usable.height) {
            return if (usable.contains(remembered)) remembered else centred(remembered.width, remembered.height, usable)
        }
        val width = defaultSize.width.value.roundToInt().coerceAtMost(usable.width)
        val height = defaultSize.height.value.roundToInt().coerceAtMost(usable.height)
        return centred(width, height, usable)
    }

    private fun centred(width: Int, height: Int, usable: Rectangle) =
        Rectangle(usable.x + (usable.width - width) / 2, usable.y + (usable.height - height) / 2, width, height)
}

/** The title bar and frame of a Windows window, taken off and put back in place, without recreating the window. */
private object WindowsBorders {
    private const val GwlStyle = -16
    private const val WsCaption = 0x00C00000L
    private const val WsThickFrame = 0x00040000L
    private const val SwpFrameChanged = 0x0020
    private const val SwpUnchanged = 0x0001 or 0x0002 or 0x0004 or 0x0010 or 0x0200

    @Suppress("FunctionName")
    interface User32 : Library {
        fun GetWindowLongPtrW(hWnd: Pointer, index: Int): Long
        fun SetWindowLongPtrW(hWnd: Pointer, index: Int, value: Long): Long
        fun SetWindowPos(hWnd: Pointer, after: Pointer?, x: Int, y: Int, width: Int, height: Int, flags: Int): Boolean
    }

    private val user32: User32? by lazy { runCatching { Native.load("user32", User32::class.java) }.getOrNull() }

    /** Takes the borders off [window], returning its style to put back; null if that failed. */
    fun remove(window: Window): Long? {
        val api = user32 ?: return null
        return runCatching {
            val handle = Native.getWindowPointer(window) ?: return null
            val style = api.GetWindowLongPtrW(handle, GwlStyle)
            api.SetWindowLongPtrW(handle, GwlStyle, style and (WsCaption or WsThickFrame).inv())
            api.SetWindowPos(handle, null, 0, 0, 0, 0, SwpUnchanged or SwpFrameChanged)
            style
        }.onFailure { log.w(it) { "Couldn't take the window's borders off" } }.getOrNull()
    }

    fun restore(window: Window, style: Long) {
        val api = user32 ?: return
        runCatching {
            val handle = Native.getWindowPointer(window) ?: return
            api.SetWindowLongPtrW(handle, GwlStyle, style)
            api.SetWindowPos(handle, null, 0, 0, 0, 0, SwpUnchanged or SwpFrameChanged)
        }.onFailure { log.w(it) { "Couldn't put the window's borders back" } }
    }
}
