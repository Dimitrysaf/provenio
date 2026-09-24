package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_save
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for one piece of text and hands it back.
 *
 * Naming a collection, renaming a folder and pointing at an image are the same interaction in
 * different words, so they are the same dialog: a short, self-contained task with one clear commit
 * point, and a draft that is only written out when it is confirmed.
 */
@Composable
fun TextPromptDialog(
    title: String,
    label: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(Res.string.action_save),
    keyboardType: KeyboardType = KeyboardType.Text,
    allowBlank: Boolean = false,
) {
    var draft by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val canConfirm = allowBlank || draft.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            // Requested from inside the dialog's own content: the dialog composes in a
            // subcomposition, so an effect placed outside it can run before this field exists.
            // Guarded because focus is a convenience here: if the node is not attached yet on
            // some platform, the dialog should still open rather than throw.
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (canConfirm) onConfirm(draft.trim()) },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft.trim()) }, enabled = canConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
