package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.focus.FocusEnterExitScope
import androidx.compose.runtime.staticCompositionLocalOf
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

/** The rail's selected destination, where focus goes on leaving a row of the page to the left. */
internal val LocalRailFocus = staticCompositionLocalOf<FocusRequester?> { null }

/**
 * For a row of a grid: leaving it to the left goes to the rail, beside it. Searched for, focus
 * could land on the end of the row above instead, once it had been on the rail for a moment.
 */
internal fun Modifier.leftLeavesToRail(): Modifier = composed {
    val rail = LocalRailFocus.current ?: return@composed Modifier
    focusProperties {
        onExit = { if (requestedFocusDirection == FocusDirection.Left) rail.toRail(this) }
    }.focusGroup()
}

private fun FocusRequester.toRail(scope: FocusEnterExitScope) {
    scope.cancelFocusChange()
    runCatching { requestFocus() }
}

/**
 * For a row of items that scrolls sideways: moving up or down into it lands on the item last
 * focused in it, or else on the first one shown, rather than on whichever happens to be above or
 * below the item left. Leaving it to the left goes to the rail, see [leftLeavesToRail].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.rowFocusEntry(): Modifier = composed {
    val row = remember { FocusRequester() }
    val rail = LocalRailFocus.current
    focusProperties {
        onEnter = {
            if (requestedFocusDirection == FocusDirection.Up || requestedFocusDirection == FocusDirection.Down) {
                row.requestFocus(FocusDirection.Enter)
            }
        }
        onExit = { if (requestedFocusDirection == FocusDirection.Left) rail?.toRail(this) }
    }
        .focusGroup()
        .focusRequester(row)
        .focusRestorer()
}
