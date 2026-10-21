package io.github.dimitrysaf.provenio.shell.screens.p2p

import io.github.dimitrysaf.provenio.shell.components.arrowKeyScrollContent
import io.github.dimitrysaf.provenio.shell.components.arrowKeyScrollHost
import io.github.dimitrysaf.provenio.shell.components.rememberArrowKeyScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.p2p_consent_body
import provenio.composeapp.generated.resources.p2p_consent_cancel
import provenio.composeapp.generated.resources.p2p_consent_enable
import provenio.composeapp.generated.resources.p2p_consent_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun P2pConsentDialog(
    onEnableP2p: () -> Unit,
    onDismiss: () -> Unit,
) {
    val bodyScroll = rememberScrollState()
    val arrowKeyScroll = rememberArrowKeyScroll()
    AlertDialog(
        onDismissRequest = onDismiss,
        // The buttons are all focus can be on; up and down past them scroll the text.
        modifier = Modifier.arrowKeyScrollHost(arrowKeyScroll),
        title = { Text(stringResource(Res.string.p2p_consent_title)) },
        text = {
            Text(
                text = stringResource(Res.string.p2p_consent_body),
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .arrowKeyScrollContent(arrowKeyScroll, bodyScroll)
                    .verticalScroll(bodyScroll),
            )
        },
        confirmButton = {
            TextButton(onClick = onEnableP2p) {
                Text(stringResource(Res.string.p2p_consent_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.p2p_consent_cancel))
            }
        },
    )
}
