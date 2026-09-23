package com.nuvio.app.features.profiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.BottomSheetBodyMargin
import com.nuvio.app.shell.components.NuvioModalBottomSheet
import com.nuvio.app.shell.components.dismissNuvioBottomSheet
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The PIN a locked profile asks for before it opens.
 *
 * A sheet rather than a dialog: the keypad is a task to carry out, not a question to answer, and
 * the sheet gives it the room and the swipe-away every other picker in the app has. A refused PIN
 * is the exception — that one does interrupt, so it comes back as a dialog over the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinEntrySheet(
    profileName: String,
    onVerify: suspend (String) -> PinVerifyResult,
    onDismiss: () -> Unit,
    onVerified: ((String) -> Unit)? = null,
    onForgotPin: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var failure by remember { mutableStateOf<String?>(null) }

    NuvioModalBottomSheet(
        onDismissRequest = {
            scope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(bottom = BottomSheetBodyMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.pin_enter),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = profileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            PinEntryBody(
                verify = onVerify,
                onVerified = { pin -> onVerified?.invoke(pin) },
                onFailed = { message -> failure = message },
            )

            if (onForgotPin != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onForgotPin) {
                    Text(stringResource(Res.string.pin_forgot))
                }
            }
        }
    }

    failure?.let { message ->
        AlertDialog(
            onDismissRequest = { failure = null },
            title = { Text(stringResource(Res.string.pin_enter)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { failure = null }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        )
    }
}
