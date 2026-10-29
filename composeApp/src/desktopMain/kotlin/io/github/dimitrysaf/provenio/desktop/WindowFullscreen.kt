package io.github.dimitrysaf.provenio.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState

/**
 * Turns a window's fullscreen on and off, and brings back how the window was before. Compose
 * records the fullscreen size as the window's size, so setting it back to floating alone kept a
 * screen-sized window on Linux that looked as if fullscreen had never ended.
 */
internal class WindowFullscreen {
    private class Windowed(val placement: WindowPlacement, val size: DpSize, val position: WindowPosition)

    private var windowed: Windowed? = null

    fun apply(state: WindowState, fullscreen: Boolean) {
        if (fullscreen) {
            if (state.placement != WindowPlacement.Fullscreen) {
                windowed = Windowed(state.placement, state.size, state.position)
            }
            state.placement = WindowPlacement.Fullscreen
        } else {
            val restored = windowed
            windowed = null
            state.placement = restored?.placement ?: WindowPlacement.Floating
            if (restored != null) {
                state.size = restored.size
                state.position = restored.position
            }
        }
    }
}
