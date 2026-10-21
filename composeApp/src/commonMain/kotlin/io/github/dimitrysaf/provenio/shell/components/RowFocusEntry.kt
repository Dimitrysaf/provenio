package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer

/**
 * For a row of items that scrolls sideways: moving up or down into it lands on the item last
 * focused in it, or else on the first one shown, rather than on whichever happens to be above or
 * below the item left.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.rowFocusEntry(): Modifier = composed {
    val row = remember { FocusRequester() }
    focusProperties {
        onEnter = {
            if (requestedFocusDirection == FocusDirection.Up || requestedFocusDirection == FocusDirection.Down) {
                row.requestFocus(FocusDirection.Enter)
            }
        }
    }
        .focusGroup()
        .focusRequester(row)
        .focusRestorer()
}
