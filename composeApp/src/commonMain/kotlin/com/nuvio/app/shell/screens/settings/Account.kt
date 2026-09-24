package com.nuvio.app.shell.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.build.AppFeaturePolicy
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.compose_settings_page_account
import nuvio.composeapp.generated.resources.auth_account_deletion_failed
import nuvio.composeapp.generated.resources.settings_account_delete_account
import nuvio.composeapp.generated.resources.settings_account_delete_confirm_message
import nuvio.composeapp.generated.resources.settings_account_delete_confirm_title
import nuvio.composeapp.generated.resources.settings_account_email
import nuvio.composeapp.generated.resources.settings_account_not_signed_in
import nuvio.composeapp.generated.resources.settings_account_sign_out
import nuvio.composeapp.generated.resources.settings_account_sign_out_confirm_message
import nuvio.composeapp.generated.resources.settings_account_sign_out_confirm_title
import nuvio.composeapp.generated.resources.settings_account_status
import nuvio.composeapp.generated.resources.settings_account_status_anonymous
import nuvio.composeapp.generated.resources.settings_account_status_signed_in
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.accountSettingsContent(
    isTablet: Boolean,
) {
    item {
        AccountSettingsBody(isTablet = isTablet)
    }
}

/**
 * The account page says what the account is and offers the two ways out of it.
 *
 * What the account is, is read-only, so it is a segmented list like every other settings group.
 * Signing out and deleting are destructive, so they are buttons below the list rather than rows
 * inside it, and each one asks before it does anything.
 */
@Composable
private fun AccountSettingsBody(
    isTablet: Boolean,
) {
    val authState by AuthRepository.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }
    val deleteAccountFallbackMessage = stringResource(Res.string.auth_account_deletion_failed)
    val canDeleteAccount = AppFeaturePolicy.accountDeletionEnabled && authState is AuthState.Authenticated

    val statusLabel = stringResource(Res.string.settings_account_status)
    val emailLabel = stringResource(Res.string.settings_account_email)
    val anonymousValue = stringResource(Res.string.settings_account_status_anonymous)
    val signedInValue = stringResource(Res.string.settings_account_status_signed_in)
    val signedOutValue = stringResource(Res.string.settings_account_not_signed_in)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSection(
            title = stringResource(Res.string.compose_settings_page_account),
            isTablet = isTablet,
        ) {
            SettingsList {
                val state = authState
                if (state is AuthState.Authenticated) {
                    infoRow(
                        title = statusLabel,
                        value = if (state.isAnonymous) anonymousValue else signedInValue,
                    )
                    state.email?.takeUnless { state.isAnonymous }?.let { email ->
                        infoRow(title = emailLabel, value = email)
                    }
                } else {
                    infoRow(title = statusLabel, value = signedOutValue)
                }
            }
        }

        Button(
            onClick = { showSignOutConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(stringResource(Res.string.settings_account_sign_out))
        }

        if (canDeleteAccount) {
            // Deleting is the rarer and the worse of the two, so it takes the quieter container
            // and keeps the error colour only where it carries the warning.
            OutlinedButton(
                onClick = {
                    deleteErrorMessage = null
                    showDeleteConfirm = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(Res.string.settings_account_delete_account))
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(stringResource(Res.string.settings_account_sign_out_confirm_title)) },
            text = { Text(stringResource(Res.string.settings_account_sign_out_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        scope.launch { AuthRepository.signOut() }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.settings_account_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.settings_account_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.settings_account_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletingAccount = true
                        scope.launch {
                            val result = AuthRepository.deleteAccount()
                            isDeletingAccount = false
                            showDeleteConfirm = false
                            deleteErrorMessage = if (result.isSuccess) {
                                null
                            } else {
                                AuthRepository.error.value
                                    ?: result.exceptionOrNull()?.message
                                    ?: deleteAccountFallbackMessage
                            }
                        }
                    },
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.settings_account_delete_account))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !isDeletingAccount,
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    // A failed deletion is a reply to pressing delete, so it is said once and then gone, rather
    // than sitting under the button forever.
    deleteErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = null },
            title = { Text(stringResource(Res.string.auth_account_deletion_failed)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { deleteErrorMessage = null }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        )
    }
}
