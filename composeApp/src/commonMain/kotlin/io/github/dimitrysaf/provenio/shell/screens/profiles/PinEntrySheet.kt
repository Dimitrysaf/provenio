package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.profiles.PinVerifyResult
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** Every PIN in the app is four digits. */
private const val PIN_LENGTH = 4

/**
 * The PIN a locked profile asks for: a dialog with one field that brings up the number keyboard,
 * a button to show or hide what was typed, and what went wrong under the field. The fourth digit
 * checks the PIN, as does pressing OK or the keyboard's done key.
 */
@Composable
fun PinEntrySheet(
    profileName: String,
    onVerify: suspend (String) -> PinVerifyResult,
    onDismiss: () -> Unit,
    onVerified: ((String) -> Unit)? = null,
    onForgotPin: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    fun submit(entered: String) {
        if (entered.length != PIN_LENGTH || isVerifying) return
        isVerifying = true
        scope.launch {
            val result = onVerify(entered)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (result.unlocked) {
                onVerified?.invoke(entered)
            } else {
                failure = result.message
                    ?: if (result.retryAfterSeconds > 0) {
                        getString(Res.string.pin_locked_try_again, result.retryAfterSeconds)
                    } else {
                        getString(Res.string.pin_incorrect)
                    }
                pin = ""
            }
            isVerifying = false
        }
    }

    val toggleLabel = stringResource(if (visible) Res.string.pin_hide else Res.string.pin_show)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null) },
        title = { Text(stringResource(Res.string.pin_enter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = profileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        val digits = value.filter(Char::isDigit).take(PIN_LENGTH)
                        pin = digits
                        if (digits.isNotEmpty()) failure = null
                        if (digits.length == PIN_LENGTH) submit(digits)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = !isVerifying,
                    singleLine = true,
                    isError = failure != null,
                    supportingText = failure?.let { message -> { Text(message) } },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit(pin) }),
                    trailingIcon = {
                        WithTooltip(toggleLabel) {
                            IconButton(onClick = { visible = !visible }) {
                                Icon(
                                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = toggleLabel,
                                )
                            }
                        }
                    },
                )
                if (onForgotPin != null) {
                    TextButton(onClick = onForgotPin) {
                        Text(stringResource(Res.string.pin_forgot))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submit(pin) },
                enabled = pin.length == PIN_LENGTH && !isVerifying,
            ) {
                Text(stringResource(Res.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
