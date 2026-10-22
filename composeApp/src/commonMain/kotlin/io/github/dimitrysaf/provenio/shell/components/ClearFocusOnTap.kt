package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * A tap on nothing in particular lets go of what has focus, so a text field gives up the keyboard
 * when tapped away from. Anything pressable keeps its tap: only one that nothing under it took
 * reaches here.
 */
internal fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    pointerInput(focusManager) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}
