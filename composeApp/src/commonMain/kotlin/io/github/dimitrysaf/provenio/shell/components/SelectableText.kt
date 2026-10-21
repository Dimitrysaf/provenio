package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.dp

/**
 * Text that a finger or a mouse can select and copy. A remote or the arrow keys cannot select
 * part of it, so with [copyText] the block is one item for them instead, which OK or Menu opens a
 * menu on to copy it whole, or the parts in [moreCopies]. Without, it has no stop of its own: a
 * selection container takes focus to hear Ctrl+C, and that focus shows nothing, so moving through
 * a page with keys would otherwise pause on blocks of text with nothing lit.
 */
@Composable
fun SelectableText(
    modifier: Modifier = Modifier,
    copyText: String? = null,
    moreCopies: List<TextCopy> = emptyList(),
    content: @Composable () -> Unit,
) {
    val inputModeManager = LocalInputModeManager.current
    val selection = Modifier.focusProperties { canFocus = inputModeManager.inputMode != InputMode.Keyboard }
    if (copyText.isNullOrEmpty()) {
        SelectionContainer(modifier = modifier.then(selection), content = content)
        return
    }
    val menu = rememberTextActionsMenu()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .focusProperties { canFocus = inputModeManager.inputMode == InputMode.Keyboard }
            .textActionsMenu(menu, openOnOk = true)
            .indication(interactionSource, ripple(focusRingShape = SelectableTextShape))
            .focusable(interactionSource = interactionSource),
    ) {
        SelectionContainer(modifier = selection, content = content)
    }
    TextActionsMenu(menu, text = copyText, moreCopies = moreCopies)
}

private val SelectableTextShape = RoundedCornerShape(8.dp)
