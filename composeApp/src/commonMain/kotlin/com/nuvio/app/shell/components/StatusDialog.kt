package com.nuvio.app.shell.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_ok
import org.jetbrains.compose.resources.stringResource

/**
 * The app's confirmation and status dialog.
 *
 * Built on the dialog component rather than on a bare surface with the parts placed by hand: the
 * headline, the supporting text and the actions are the dialog's own slots, so they take its
 * spec's placement, tone, shape and text button treatment. A busy dialog shows the indicator as
 * the dialog's icon and withholds its actions, since neither is answerable yet.
 *
 * m3.material.io/components/dialogs/specs
 */
@Composable
fun NuvioStatusModal(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isBusy: Boolean = false,
    confirmText: String = stringResource(Res.string.action_ok),
    dismissText: String? = null,
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    if (!isVisible) return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!isBusy) {
                onDismiss?.invoke() ?: onConfirm()
            }
        },
        icon = if (isBusy) {
            { NuvioLoadingIndicator() }
        } else {
            null
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) {
                Text(confirmText)
            }
        },
        dismissButton = if (!isBusy && dismissText != null && onDismiss != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text(dismissText)
                }
            }
        } else {
            null
        },
    )
}
