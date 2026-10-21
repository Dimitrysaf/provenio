package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_copy
import provenio.composeapp.generated.resources.action_paste
import provenio.composeapp.generated.resources.text_copied
import kotlin.math.roundToInt

/**
 * The menu of a block of text or a text field for a remote or a keyboard, which cannot select
 * text the way a finger or a mouse does: Copy takes the whole text, and a field also has Paste.
 * It opens under the text, where [textActionsMenu] marks it.
 */
@Stable
internal class TextActionsMenuState {
    var open by mutableStateOf(false)
    internal var bounds: Rect? = null
}

@Composable
internal fun rememberTextActionsMenu(): TextActionsMenuState = remember { TextActionsMenuState() }

/**
 * Records where the text is, and opens [menu] on the Menu key or Shift+F10, and on OK too when
 * [openOnOk]; a text field keeps OK for typing.
 */
internal fun Modifier.textActionsMenu(menu: TextActionsMenuState, openOnOk: Boolean = false): Modifier = composed {
    val hintToken = remember { Any() }
    DisposableEffect(hintToken) {
        onDispose { KeyActionHint.hide(hintToken) }
    }
    Modifier
        .onGloballyPositioned { menu.bounds = it.boundsInWindow() }
        .onFocusChanged { if (it.isFocused) KeyActionHint.show(hintToken) else KeyActionHint.hide(hintToken) }
        .onPreviewKeyEvent { event ->
            val opens = event.opensMenu || (openOnOk && event.isOkKey)
            if (!opens) return@onPreviewKeyEvent false
            // Opened on release, so the press that opened it is over before the menu has focus.
            if (event.type == KeyEventType.KeyUp) menu.open = true
            true
        }
}

/**
 * The menu [textActionsMenu] opens: Copy for [text] unless it is not to be copied, as a password
 * is not, and Paste when there is [onPaste].
 */
@Composable
internal fun TextActionsMenu(
    menu: TextActionsMenuState,
    text: String,
    canCopy: Boolean = true,
    onPaste: ((String) -> Unit)? = null,
) {
    if (!menu.open) return
    val clipboard = LocalClipboardManager.current
    val copied = stringResource(Res.string.text_copied)
    val close = { menu.open = false }
    val firstItem = remember { FocusRequester() }
    val usingKeys = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Popup(
        popupPositionProvider = remember(menu.bounds) { BelowTextPositionProvider(menu.bounds) },
        onDismissRequest = close,
        properties = PopupProperties(focusable = true),
    ) {
        LaunchedEffect(Unit) {
            if (!usingKeys) return@LaunchedEffect
            withFrameNanos {}
            runCatching { firstItem.requestFocus() }
        }
        Surface(
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
        ) {
            Column(modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = 8.dp)) {
                if (canCopy) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_copy)) },
                        enabled = text.isNotEmpty(),
                        onClick = {
                            close()
                            clipboard.setText(AnnotatedString(text))
                            ToastController.show(copied)
                        },
                        modifier = Modifier.focusRequester(firstItem),
                    )
                }
                if (onPaste != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_paste)) },
                        enabled = clipboard.hasText(),
                        onClick = {
                            close()
                            clipboard.getText()?.text?.let(onPaste)
                        },
                        modifier = if (canCopy) Modifier else Modifier.focusRequester(firstItem),
                    )
                }
            }
        }
    }
}

private val KeyEvent.opensMenu: Boolean
    get() = key == Key.Menu || (key == Key.F10 && isShiftPressed)

/** Under the text, or above it where there is no room below, kept inside the window. */
private class BelowTextPositionProvider(private val text: Rect?) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val left = text?.left?.roundToInt() ?: anchorBounds.left
        val top = text?.top?.roundToInt() ?: anchorBounds.top
        val bottom = text?.bottom?.roundToInt() ?: anchorBounds.bottom
        val below = bottom + MenuGapPx
        val above = top - MenuGapPx - popupContentSize.height
        val y = if (below + popupContentSize.height <= windowSize.height || above < 0) below else above
        return IntOffset(
            x = left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y = y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

private const val MenuGapPx = 8
