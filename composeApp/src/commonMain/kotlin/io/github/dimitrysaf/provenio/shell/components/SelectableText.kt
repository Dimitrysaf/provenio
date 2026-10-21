package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/**
 * Text that a finger or a mouse can select and copy, with no stop of its own for a remote or the
 * arrow keys. A selection container takes focus to hear Ctrl+C, and that focus shows nothing, so
 * moving through a page with keys would otherwise pause on blocks of text with nothing lit.
 */
@Composable
fun SelectableText(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val inputModeManager = LocalInputModeManager.current
    SelectionContainer(
        modifier = modifier.focusProperties { canFocus = inputModeManager.inputMode != InputMode.Keyboard },
        content = content,
    )
}
