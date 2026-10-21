package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.getValue
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
 */
fun Modifier.arrowKeysLeaveField(isEmpty: () -> Boolean): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val latestIsEmpty by rememberUpdatedState(isEmpty)
    onPreviewKeyEvent { event ->
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
