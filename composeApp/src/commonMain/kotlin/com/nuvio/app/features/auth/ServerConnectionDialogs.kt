package com.nuvio.app.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.network.ServerConfiguration
import com.nuvio.app.core.network.ServerDiscoveryFailure
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.server_connect_action
import nuvio.composeapp.generated.resources.server_connect_checking
import nuvio.composeapp.generated.resources.server_connect_subtitle
import nuvio.composeapp.generated.resources.server_connect_title
import nuvio.composeapp.generated.resources.server_connect_url_placeholder
import nuvio.composeapp.generated.resources.server_error_auth
import nuvio.composeapp.generated.resources.server_error_connection
import nuvio.composeapp.generated.resources.server_error_http
import nuvio.composeapp.generated.resources.server_error_invalid_document
import nuvio.composeapp.generated.resources.server_error_invalid_url
import nuvio.composeapp.generated.resources.server_error_missing_configuration
import nuvio.composeapp.generated.resources.server_error_not_self_hosted
import nuvio.composeapp.generated.resources.server_error_official_active
import nuvio.composeapp.generated.resources.server_error_official_available
import nuvio.composeapp.generated.resources.server_error_response_too_large
import nuvio.composeapp.generated.resources.server_error_restart
import nuvio.composeapp.generated.resources.server_error_save
import nuvio.composeapp.generated.resources.server_error_session_clear
import nuvio.composeapp.generated.resources.server_error_service
import nuvio.composeapp.generated.resources.server_error_version
import nuvio.composeapp.generated.resources.server_menu_change_custom
import nuvio.composeapp.generated.resources.server_menu_content_description
import nuvio.composeapp.generated.resources.server_menu_custom
import nuvio.composeapp.generated.resources.server_menu_official
import nuvio.composeapp.generated.resources.server_official_action
import nuvio.composeapp.generated.resources.server_official_body
import nuvio.composeapp.generated.resources.server_official_title
import nuvio.composeapp.generated.resources.server_review_description
import nuvio.composeapp.generated.resources.server_review_key_discovered
import nuvio.composeapp.generated.resources.server_review_key_label
import nuvio.composeapp.generated.resources.server_review_title
import nuvio.composeapp.generated.resources.server_review_trust
import nuvio.composeapp.generated.resources.server_review_verified_label
import nuvio.composeapp.generated.resources.server_switching
import nuvio.composeapp.generated.resources.server_warning_credentials
import nuvio.composeapp.generated.resources.server_warning_http
import nuvio.composeapp.generated.resources.server_warning_private
import nuvio.composeapp.generated.resources.server_warning_public
import nuvio.composeapp.generated.resources.server_warning_public_http
import nuvio.composeapp.generated.resources.server_warning_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ServerConnectionMenu(
    activeServer: ServerConfiguration,
    onUseOfficial: () -> Unit,
    onConnectCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(Res.string.server_menu_content_description),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.server_menu_official)) },
                enabled = activeServer.isCustom,
                onClick = {
                    expanded = false
                    onUseOfficial()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (activeServer.isCustom) {
                                Res.string.server_menu_change_custom
                            } else {
                                Res.string.server_menu_custom
                            },
                        ),
                    )
                },
                onClick = {
                    expanded = false
                    onConnectCustom()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerConnectionSheet(
    state: ServerConnectionUiState,
    onDiscover: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by rememberSaveable { mutableStateOf("") }
    val error = serverDiscoveryError(state)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.server_connect_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.server_connect_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isDiscovering,
                singleLine = true,
                label = { Text(stringResource(Res.string.server_connect_url_placeholder)) },
                isError = error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (url.isNotBlank()) onDiscover(url) },
                ),
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onDiscover(url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = url.isNotBlank() && !state.isDiscovering,
            ) {
                if (state.isDiscovering) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(stringResource(Res.string.server_connect_checking))
                    }
                } else {
                    Text(stringResource(Res.string.server_connect_action))
                }
            }
        }
    }
}

@Composable
internal fun ServerTrustDialog(
    server: ServerConfiguration,
    isSwitching: Boolean,
    switchFailure: ServerSwitchFailure?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSwitching) onDismiss() },
        title = { Text(stringResource(Res.string.server_review_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.server_review_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                ServerDetail(
                    label = stringResource(Res.string.server_review_verified_label),
                    value = server.backendUrl,
                )
                ServerDetail(
                    label = stringResource(Res.string.server_review_key_label),
                    value = stringResource(Res.string.server_review_key_discovered),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.server_warning_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when {
                            !server.isSecure && server.isPublicHost -> {
                                stringResource(Res.string.server_warning_public_http)
                            }
                            !server.isSecure -> stringResource(Res.string.server_warning_http)
                            server.isPublicHost -> stringResource(Res.string.server_warning_public)
                            else -> stringResource(Res.string.server_warning_private)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = stringResource(Res.string.server_warning_credentials),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                if (switchFailure != null) {
                    Text(
                        text = serverSwitchError(switchFailure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSwitching) {
                if (isSwitching) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(stringResource(Res.string.server_switching))
                    }
                } else {
                    Text(stringResource(Res.string.server_review_trust))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSwitching) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun OfficialServerDialog(
    isSwitching: Boolean,
    switchFailure: ServerSwitchFailure?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSwitching) onDismiss() },
        title = { Text(stringResource(Res.string.server_official_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.server_official_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (switchFailure != null) {
                    Text(
                        text = serverSwitchError(switchFailure),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSwitching) {
                if (isSwitching) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(stringResource(Res.string.server_switching))
                    }
                } else {
                    Text(stringResource(Res.string.server_official_action))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSwitching) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ServerDetail(
    label: String,
    value: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun serverDiscoveryError(state: ServerConnectionUiState): String? {
    return when (state.failure) {
        ServerDiscoveryFailure.InvalidUrl -> stringResource(Res.string.server_error_invalid_url)
        ServerDiscoveryFailure.OfficialServer -> stringResource(
            if (state.activeServer.isCustom) {
                Res.string.server_error_official_available
            } else {
                Res.string.server_error_official_active
            },
        )
        ServerDiscoveryFailure.ConnectionFailed -> stringResource(Res.string.server_error_connection)
        ServerDiscoveryFailure.HttpError -> stringResource(
            Res.string.server_error_http,
            state.statusCode ?: 0,
        )
        ServerDiscoveryFailure.ResponseTooLarge -> stringResource(Res.string.server_error_response_too_large)
        ServerDiscoveryFailure.InvalidDocument -> stringResource(Res.string.server_error_invalid_document)
        ServerDiscoveryFailure.UnsupportedVersion -> stringResource(Res.string.server_error_version)
        ServerDiscoveryFailure.WrongService -> stringResource(Res.string.server_error_service)
        ServerDiscoveryFailure.NotSelfHosted -> stringResource(Res.string.server_error_not_self_hosted)
        ServerDiscoveryFailure.MissingConfiguration -> stringResource(Res.string.server_error_missing_configuration)
        ServerDiscoveryFailure.UnsupportedAuthentication -> stringResource(Res.string.server_error_auth)
        null -> null
    }
}

@Composable
private fun serverSwitchError(failure: ServerSwitchFailure): String =
    when (failure) {
        ServerSwitchFailure.SessionClear -> stringResource(Res.string.server_error_session_clear)
        ServerSwitchFailure.Save -> stringResource(Res.string.server_error_save)
        ServerSwitchFailure.Restart -> stringResource(Res.string.server_error_restart)
    }
