package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import io.github.dimitrysaf.provenio.core.build.isMacOs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class AppShortcutAction {
    Home,
    Search,
    Library,
    Settings,
    Downloads,
    Back,
    KeyboardShortcuts,
}

object AppKeyboardShortcuts {
    private val pending = MutableSharedFlow<AppShortcutAction>(extraBufferCapacity = 8)
    val actions: SharedFlow<AppShortcutAction> = pending.asSharedFlow()

    fun handle(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val action = actionFor(event) ?: return false
        return pending.tryEmit(action)
    }

    private fun actionFor(event: KeyEvent): AppShortcutAction? {
        val command = event.isCtrlPressed || event.isMetaPressed
        return when {
            command && !event.isAltPressed && !event.isShiftPressed -> when (event.key) {
                Key.One -> AppShortcutAction.Home
                Key.Two, Key.F, Key.K -> AppShortcutAction.Search
                Key.Three -> AppShortcutAction.Library
                Key.Four, Key.Comma -> AppShortcutAction.Settings
                Key.J -> AppShortcutAction.Downloads
                Key.Slash -> AppShortcutAction.KeyboardShortcuts
                Key.LeftBracket -> AppShortcutAction.Back
                else -> null
            }
            event.isAltPressed && !command && !event.isShiftPressed && !isMacOs -> when (event.key) {
                Key.DirectionLeft -> AppShortcutAction.Back
                else -> null
            }
            !command && !event.isAltPressed && event.key == Key.F1 -> AppShortcutAction.KeyboardShortcuts
            else -> null
        }
    }
}
