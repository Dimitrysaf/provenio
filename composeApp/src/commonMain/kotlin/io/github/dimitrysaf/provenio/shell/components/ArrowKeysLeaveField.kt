package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager

/**
 * For a single-line text field: the arrow keys move on to what is around it, as they do on a TV,
 * instead of being kept by the field. Up and down always leave, as a single line has nowhere to
 * go; left and right leave while the field is empty, and move the cursor once there is text. A
 * remote passing over a field would otherwise stop there, with the arrows seeming to do nothing.
 *
 * With keys, focus lands on the field only when up, down or Tab moves it there, or when asked for
 * on purpose, see [TextFieldEntry]. Opening a page or a sheet, or coming into it from the side,
 * passes over the field to what comes after it, as landing on it would bring up the keyboard.
 */
fun Modifier.arrowKeysLeaveField(isEmpty: () -> Boolean): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val sheetFocus = LocalSheetFocus.current
    val latestIsEmpty by rememberUpdatedState(isEmpty)
    var focused by remember { mutableStateOf(false) }
    focusProperties {
        val sheetAllows = sheetFocus?.headerFocusable ?: true
        val keysAllow = inputModeManager.inputMode != InputMode.Keyboard || focused || TextFieldEntry.allowed
        canFocus = sheetAllows && keysAllow
    }.onFocusChanged { focused = it.isFocused }.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val direction = when (event.key) {
            Key.DirectionUp -> FocusDirection.Up
            Key.DirectionDown -> FocusDirection.Down
            Key.DirectionLeft -> FocusDirection.Left.takeIf { latestIsEmpty() }
            Key.DirectionRight -> FocusDirection.Right.takeIf { latestIsEmpty() }
            else -> null
        } ?: return@onPreviewKeyEvent false
        focusManager.moveFocus(direction)
    }
}

/**
 * Whether a text field may take focus now, with keys: the key last pressed moves focus up or
 * down, or on with Tab, or focus is being asked for on purpose, as Ctrl+K asks for search.
 */
internal object TextFieldEntry {
    private var lastKeyMovesVertically = false
    private var asked = false

    val allowed: Boolean get() = lastKeyMovesVertically || asked

    /** Notes each key pressed, from the app's root and from each sheet's. */
    fun pressed(key: Key) {
        lastKeyMovesVertically = key in VerticalKeys
    }

    /** Focuses a text field on purpose. */
    fun <T> asking(request: () -> T): T {
        asked = true
        try {
            return request()
        } finally {
            asked = false
        }
    }
}

private val VerticalKeys = setOf(Key.DirectionUp, Key.DirectionDown, Key.Tab)
