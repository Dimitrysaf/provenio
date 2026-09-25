package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncActivity
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncError
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncPeer
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncRepository
import io.github.dimitrysaf.provenio.shell.components.ContentDialog
import io.github.dimitrysaf.provenio.shell.components.ToastController
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_close
import provenio.composeapp.generated.resources.local_sync_code_label
import provenio.composeapp.generated.resources.local_sync_connect
import provenio.composeapp.generated.resources.local_sync_copy_code
import provenio.composeapp.generated.resources.local_sync_done
import provenio.composeapp.generated.resources.local_sync_enter_code
import provenio.composeapp.generated.resources.local_sync_enter_code_description
import provenio.composeapp.generated.resources.local_sync_error_code
import provenio.composeapp.generated.resources.local_sync_error_failed
import provenio.composeapp.generated.resources.local_sync_error_rejected
import provenio.composeapp.generated.resources.local_sync_error_unreachable
import provenio.composeapp.generated.resources.local_sync_error_wifi
import provenio.composeapp.generated.resources.local_sync_explanation
import provenio.composeapp.generated.resources.local_sync_forget
import provenio.composeapp.generated.resources.local_sync_forget_description
import provenio.composeapp.generated.resources.local_sync_forget_title
import provenio.composeapp.generated.resources.local_sync_no_devices
import provenio.composeapp.generated.resources.local_sync_no_devices_description
import provenio.composeapp.generated.resources.local_sync_paired_devices
import provenio.composeapp.generated.resources.local_sync_pairing_hint
import provenio.composeapp.generated.resources.local_sync_pairing_title
import provenio.composeapp.generated.resources.local_sync_scan_code
import provenio.composeapp.generated.resources.local_sync_scan_code_description
import provenio.composeapp.generated.resources.local_sync_show_code
import provenio.composeapp.generated.resources.local_sync_show_code_description
import provenio.composeapp.generated.resources.local_sync_syncing
import provenio.composeapp.generated.resources.local_sync_syncing_with
import provenio.composeapp.generated.resources.local_sync_tap_to_sync
import provenio.composeapp.generated.resources.local_sync_this_device
import provenio.composeapp.generated.resources.local_sync_waiting
import provenio.composeapp.generated.resources.local_sync_what_syncs
import provenio.composeapp.generated.resources.local_sync_what_syncs_description

internal fun LazyListScope.localSyncSettingsContent(isTablet: Boolean) {
    // One item, so the page keeps listening for as long as it is open rather than as long as a row is on screen.
    item {
        LocalSyncPage(isTablet = isTablet)
    }
}

@Composable
private fun LocalSyncPage(isTablet: Boolean) {
    val state by LocalSyncRepository.uiState.collectAsStateWithLifecycle()
    var showCodeEntry by rememberSaveable { mutableStateOf(false) }
    var peerToForgetId by rememberSaveable { mutableStateOf<String?>(null) }
    val scan = rememberLocalSyncScanner { code -> code?.let(LocalSyncRepository::join) }

    DisposableEffect(Unit) {
        LocalSyncRepository.open()
        onDispose { LocalSyncRepository.close() }
    }

    // A finished sync or a failure is said once, as a snackbar, rather than left on the page.
    LaunchedEffect(state.activity) {
        val message = when (val activity = state.activity) {
            is LocalSyncActivity.Synced -> getString(Res.string.local_sync_done, activity.peerName, activity.changeCount)
            is LocalSyncActivity.Failed -> getString(activity.error.messageRes())
            LocalSyncActivity.Idle,
            is LocalSyncActivity.Syncing,
            -> null
        } ?: return@LaunchedEffect
        ToastController.show(message)
        LocalSyncRepository.clearActivity()
    }

    Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 24.dp)) {
        SettingsSection(
            title = stringResource(Res.string.local_sync_this_device),
            isTablet = isTablet,
        ) {
            SettingsList {
                infoRow(
                    title = state.deviceName,
                    description = stringResource(Res.string.local_sync_explanation),
                )
                navigationRow(
                    title = stringResource(Res.string.local_sync_show_code),
                    description = stringResource(Res.string.local_sync_show_code_description),
                    icon = Icons.Rounded.QrCode,
                    onClick = LocalSyncRepository::startPairing,
                )
                if (scan != null) {
                    navigationRow(
                        title = stringResource(Res.string.local_sync_scan_code),
                        description = stringResource(Res.string.local_sync_scan_code_description),
                        icon = Icons.Rounded.QrCodeScanner,
                        onClick = scan,
                    )
                }
                navigationRow(
                    title = stringResource(Res.string.local_sync_enter_code),
                    description = stringResource(Res.string.local_sync_enter_code_description),
                    icon = Icons.Rounded.Keyboard,
                    onClick = { showCodeEntry = true },
                )
            }
        }

        val syncing = state.activity as? LocalSyncActivity.Syncing
        if (syncing != null) {
            LocalSyncProgress(peerName = syncing.peerName)
        }

        SettingsSection(
            title = stringResource(Res.string.local_sync_paired_devices),
            isTablet = isTablet,
        ) {
            val tapToSync = stringResource(Res.string.local_sync_tap_to_sync)
            val forgetLabel = stringResource(Res.string.local_sync_forget)
            SettingsList {
                if (state.peers.isEmpty()) {
                    infoRow(
                        title = stringResource(Res.string.local_sync_no_devices),
                        description = stringResource(Res.string.local_sync_no_devices_description),
                    )
                }
                state.peers.forEach { peer ->
                    navigationRow(
                        title = peer.name,
                        description = tapToSync,
                        icon = Icons.Rounded.Devices,
                        enabled = syncing == null,
                        trailingContent = {
                            IconButton(onClick = { peerToForgetId = peer.deviceId }) {
                                Icon(imageVector = Icons.Rounded.Delete, contentDescription = forgetLabel)
                            }
                        },
                        onClick = { LocalSyncRepository.syncWith(peer) },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.local_sync_what_syncs),
            isTablet = isTablet,
        ) {
            SettingsList {
                infoRow(
                    title = stringResource(Res.string.local_sync_what_syncs),
                    description = stringResource(Res.string.local_sync_what_syncs_description),
                )
            }
        }
    }

    state.pairingCode?.let { code ->
        LocalSyncPairingDialog(
            code = code,
            qr = state.pairingQr,
            onDismiss = LocalSyncRepository::stopPairing,
        )
    }

    if (showCodeEntry) {
        LocalSyncCodeDialog(
            onConnect = { code ->
                showCodeEntry = false
                LocalSyncRepository.join(code)
            },
            onDismiss = { showCodeEntry = false },
        )
    }

    state.peers.firstOrNull { it.deviceId == peerToForgetId }?.let { peer ->
        LocalSyncForgetDialog(
            peer = peer,
            onConfirm = {
                peerToForgetId = null
                LocalSyncRepository.forget(peer)
            },
            onDismiss = { peerToForgetId = null },
        )
    }
}

@Composable
private fun LocalSyncProgress(peerName: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = peerName?.let { stringResource(Res.string.local_sync_syncing_with, it) }
                    ?: stringResource(Res.string.local_sync_syncing),
                style = MaterialTheme.typography.titleSmall,
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LocalSyncPairingDialog(
    code: String,
    qr: List<BooleanArray>?,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    ContentDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.local_sync_pairing_title),
        buttons = {
            TextButton(onClick = { clipboardManager.setText(AnnotatedString(code)) }) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Text(
                    text = stringResource(Res.string.local_sync_copy_code),
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    ) {
        Text(stringResource(Res.string.local_sync_pairing_hint))
        if (qr != null) {
            LocalSyncQrCode(
                matrix = qr,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.local_sync_waiting),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Dark modules on white whatever the theme, since that is what camera scanners read best. */
@Composable
private fun LocalSyncQrCode(
    matrix: List<BooleanArray>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
    ) {
        Canvas(
            modifier = Modifier
                .padding(16.dp)
                .size(208.dp),
        ) {
            val cells = matrix.size.coerceAtLeast(1)
            val cell = size.minDimension / cells
            matrix.forEachIndexed { y, row ->
                row.forEachIndexed { x, dark ->
                    if (dark) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cell, y * cell),
                            size = Size(cell, cell),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalSyncCodeDialog(
    onConnect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.local_sync_enter_code)) },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(stringResource(Res.string.local_sync_code_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(code) },
                enabled = code.isNotBlank(),
            ) {
                Text(stringResource(Res.string.local_sync_connect))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun LocalSyncForgetDialog(
    peer: LocalSyncPeer,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.local_sync_forget_title, peer.name)) },
        text = { Text(stringResource(Res.string.local_sync_forget_description)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(Res.string.local_sync_forget))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

private fun LocalSyncError.messageRes() = when (this) {
    LocalSyncError.NOT_ON_WIFI -> Res.string.local_sync_error_wifi
    LocalSyncError.UNREACHABLE -> Res.string.local_sync_error_unreachable
    LocalSyncError.REJECTED -> Res.string.local_sync_error_rejected
    LocalSyncError.INVALID_CODE -> Res.string.local_sync_error_code
    LocalSyncError.FAILED -> Res.string.local_sync_error_failed
}

/** Opens the camera to scan a pairing code, or null where there is no scanner. */
@Composable
internal expect fun rememberLocalSyncScanner(onResult: (String?) -> Unit): (() -> Unit)?
