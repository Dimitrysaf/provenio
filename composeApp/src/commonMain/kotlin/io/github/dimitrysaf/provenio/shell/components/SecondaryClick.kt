package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.onSecondaryClick(action: (() -> Unit)?): Modifier {
    if (action == null) return this
    return composed {
        val latestAction by rememberUpdatedState(action)
        onKeyEvent { event ->
            val opensMenu = event.key == Key.Menu || (event.key == Key.F10 && event.isShiftPressed)
            if (opensMenu && event.type == KeyEventType.KeyDown) {
                latestAction()
                true
            } else {
                opensMenu
            }
        }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        latestAction()
                    }
                }
            }
        }
    }
}
