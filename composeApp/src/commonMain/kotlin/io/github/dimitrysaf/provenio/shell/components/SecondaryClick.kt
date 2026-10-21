package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A right-click, or the Menu key or Shift+F10, runs [action]: the more actions a long press opens.
 * While it has focus from keys it shows how to reach them, see [KeyActionHint].
 */
fun Modifier.onSecondaryClick(action: (() -> Unit)?): Modifier {
    if (action == null) return this
    return composed {
        val latestAction by rememberUpdatedState(action)
        val hintToken = remember { Any() }
        val coordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }
        // From keys the menu has no press to open at, so it opens beside this item.
        val recordAnchor = {
            coordinates[0]?.takeIf { it.isAttached }?.let { LastPressPosition.recordAnchor(it.boundsInWindow()) }
        }
        DisposableEffect(hintToken) {
            onDispose { KeyActionHint.hide(hintToken) }
        }
        // An item with more actions is one a menu is opened from, which focus goes back to.
        restoresFocus().onGloballyPositioned { coordinates[0] = it }.onFocusChanged { state ->
            if (state.isFocused) KeyActionHint.show(hintToken) else KeyActionHint.hide(hintToken)
        }.onPreviewKeyEvent { event ->
            // Seen, not taken: the press itself is the clickable's.
            if (event.isOkKey) {
                OkKeyHold.held = event.type == KeyEventType.KeyDown
                if (event.type == KeyEventType.KeyDown) recordAnchor()
            }
            false
        }.onKeyEvent { event ->
            val opensMenu = event.key == Key.Menu || (event.key == Key.F10 && event.isShiftPressed)
            if (opensMenu && event.type == KeyEventType.KeyDown) {
                recordAnchor()
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

/**
 * Whether OK is held on something whose long press opens more actions. A remote repeats a held
 * key, so the sheet that opens would take the repeats as a press on its first row, and choose it
 * as OK is let go; a sheet opened during the hold ignores OK until it is released.
 */
internal object OkKeyHold {
    var held = false
}

internal val KeyEvent.isOkKey: Boolean
    get() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter
