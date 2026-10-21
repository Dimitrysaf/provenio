package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Where focus is in a sheet, for a remote or a keyboard. When the sheet opens, focus goes to the
 * first control of its body rather than its header, and the arrow keys reach the header's close
 * and other buttons from there. Focus stays in the sheet until it closes. With touch nothing is
 * focused, as anywhere else.
 */
@Stable
internal class SheetFocus(headerFocusable: Boolean) {
    /** False until focus has started in the body, so the header is passed over for it. */
    var headerFocusable by mutableStateOf(headerFocusable)
}

internal val LocalSheetFocus = staticCompositionLocalOf<SheetFocus?> { null }

/**
 * How many sheets and menus are open. A screen that keeps focus for itself, as the player does,
 * leaves it to them meanwhile, and does not answer keys that are theirs.
 */
internal object OpenSheets {
    var count by mutableIntStateOf(0)
}

/**
 * Makes [content] a sheet's body for focus: it is given the modifier to put on the sheet's own
 * container, and everything inside that container is the sheet as far as focus goes.
 *
 * [startFocused] is whether the sheet was opened with keys rather than touch or a mouse, read
 * where it was opened from: a sheet in its own window may not know.
 */
@Composable
internal fun SheetFocusScope(
    startFocused: Boolean = openedWithKeys(),
    content: @Composable (Modifier) -> Unit,
) {
    val sheetFocus = remember { SheetFocus(headerFocusable = !startFocused) }
    val requester = remember { FocusRequester() }
    // Opened by holding OK: that hold is still going, and its repeats and release are not for
    // the row focus starts on.
    var ignoringHeldOk by remember { mutableStateOf(startFocused && OkKeyHold.held) }
    LaunchedEffect(sheetFocus) {
        if (sheetFocus.headerFocusable) return@LaunchedEffect
        // A sheet's rows can arrive a few frames after the sheet does, as its lists fill in.
        var focused = false
        for (frame in 0 until InitialFocusFrames) {
            withFrameNanos {}
            if (requester.requestFocus(FocusDirection.Enter)) {
                focused = true
                break
            }
        }
        sheetFocus.headerFocusable = true
        // A sheet whose only controls are in its header starts on them.
        if (!focused) {
            withFrameNanos {}
            requester.requestFocus(FocusDirection.Enter)
        }
    }
    DisposableEffect(sheetFocus) {
        OpenSheets.count++
        onDispose {
            OpenSheets.count--
            FocusMemory.sheetClosed()
        }
    }
    CompositionLocalProvider(LocalSheetFocus provides sheetFocus) {
        // Up and down scroll a sheet of only text, where there is nothing for focus to move to.
        ArrowKeyScrollHost { arrowKeyScroll ->
            content(
                arrowKeyScroll
                    // A sheet can be a window of its own, which the app's root does not see into.
                    .singleKeyPresses()
                    .onPreviewKeyEvent { event ->
                        if (!ignoringHeldOk || !event.isOkKey) return@onPreviewKeyEvent false
                        if (event.type == KeyEventType.KeyUp) {
                            ignoringHeldOk = false
                            OkKeyHold.held = false
                        }
                        true
                    }
                    .focusRequester(requester)
                    .focusProperties {
                        // The arrow and tab keys stay in the sheet; focus can still be cleared or
                        // asked for elsewhere, as when a text field closes its keyboard.
                        onExit = { if (requestedFocusDirection in TrappedDirections) cancelFocusChange() }
                    }
                    .focusGroup(),
            )
        }
    }
}

/** Whether keys, a remote's or a keyboard's, are what is being used now. */
@Composable
internal fun openedWithKeys(): Boolean = LocalInputModeManager.current.inputMode == InputMode.Keyboard

/**
 * For a sheet's header row, or a header button outside one: its controls are reached with the
 * arrow keys, but focus never starts on them while the body has something to focus. The same
 * goes for a text field and the buttons inside it.
 */
internal fun Modifier.sheetHeaderFocus(sheetFocus: SheetFocus?): Modifier =
    if (sheetFocus == null) this else focusProperties { canFocus = sheetFocus.headerFocusable }

/**
 * A scrim that dismisses what it is under when tapped. It is never focused: a remote or a
 * keyboard would land on it, and its OK would close the sheet before anything in it could be
 * reached.
 */
internal fun Modifier.dismissScrim(onDismiss: () -> Unit): Modifier =
    focusProperties { canFocus = false }
        .clickable(interactionSource = null, indication = null, onClick = onDismiss)

private val TrappedDirections = setOf(
    FocusDirection.Up,
    FocusDirection.Down,
    FocusDirection.Left,
    FocusDirection.Right,
    FocusDirection.Next,
    FocusDirection.Previous,
)

/** About half a second at 60 frames a second. */
private const val InitialFocusFrames = 30
