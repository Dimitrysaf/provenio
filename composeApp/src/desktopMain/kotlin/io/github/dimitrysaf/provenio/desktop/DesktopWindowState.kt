package io.github.dimitrysaf.provenio.desktop

import io.github.dimitrysaf.provenio.shell.components.AppFullscreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the app's windows are doing, for code that on Android would ask the activity. */
object DesktopWindowState {
    private val mainVisible = MutableStateFlow(true)
    private val playerWindowOpen = MutableStateFlow(false)
    private val playerWindowVisible = MutableStateFlow(true)
    private val playerFullscreen = MutableStateFlow(false)
    private val appVisible = MutableStateFlow(true)
    private val playerVisible = MutableStateFlow(true)

    /** False while every window of the app is minimized or hidden. */
    val isVisible: StateFlow<Boolean> = appVisible.asStateFlow()

    /** False while the window the player is drawn in is minimized or hidden. */
    val isPlayerVisible: StateFlow<Boolean> = playerVisible.asStateFlow()

    /**
     * Whether the main window is fullscreen, asked for by F11, the fullscreen button or the player
     * when it is in that window; Main applies it to the window. It is [AppFullscreen]'s state, so
     * there is one value for all of them to change.
     */
    val isFullscreen: StateFlow<Boolean> = AppFullscreen.isFullscreen

    /** Asked for by the player in its own window; that window applies it. */
    val isPlayerFullscreen: StateFlow<Boolean> = playerFullscreen.asStateFlow()

    val isPlayerWindowOpen: Boolean
        get() = playerWindowOpen.value

    fun setVisible(value: Boolean) {
        mainVisible.value = value
        update()
    }

    fun setFullscreen(value: Boolean) {
        AppFullscreen.set(value)
    }

    fun setPlayerWindowOpen(value: Boolean) {
        playerWindowOpen.value = value
        if (!value) playerWindowVisible.value = true
        update()
    }

    fun setPlayerWindowVisible(value: Boolean) {
        playerWindowVisible.value = value
        update()
    }

    fun setPlayerFullscreen(value: Boolean) {
        playerFullscreen.value = value
    }

    fun togglePlayerFullscreen() {
        if (playerWindowOpen.value) {
            playerFullscreen.value = !playerFullscreen.value
        } else {
            AppFullscreen.toggle()
        }
    }

    private fun update() {
        val playerShown = playerWindowOpen.value && playerWindowVisible.value
        appVisible.value = mainVisible.value || playerShown
        playerVisible.value = if (playerWindowOpen.value) playerWindowVisible.value else mainVisible.value
    }
}
