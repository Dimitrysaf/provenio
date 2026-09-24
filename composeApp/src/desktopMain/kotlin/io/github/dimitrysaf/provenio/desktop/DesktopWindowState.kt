package io.github.dimitrysaf.provenio.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the main window is doing, for code that on Android would ask the activity. */
object DesktopWindowState {
    private val visible = MutableStateFlow(true)
    private val fullscreen = MutableStateFlow(false)

    /** False while the window is minimized. */
    val isVisible: StateFlow<Boolean> = visible.asStateFlow()

    /** Asked for by the player; Main applies it to the window. */
    val isFullscreen: StateFlow<Boolean> = fullscreen.asStateFlow()

    fun setVisible(value: Boolean) {
        visible.value = value
    }

    fun setFullscreen(value: Boolean) {
        fullscreen.value = value
    }
}
