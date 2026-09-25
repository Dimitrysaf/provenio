package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
import io.github.dimitrysaf.provenio.shell.components.ContentDialog
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.ToastController
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

// Device sync, laid out like a tracker card: the banner, what it syncs, then its devices and actions.
@Composable
internal fun LocalSyncCard(modifier: Modifier = Modifier) {
    val state by LocalSyncRepository.uiState.collectAsStateWithLifecycle()
    var showCodeEntry by rememberSaveable { mutableStateOf(false) }
    var peerToForgetId by rememberSaveable { mutableStateOf<String?>(null) }
    val scan = rememberLocalSyncScanner { code -> code?.let(LocalSyncRepository::join) }
    val now by produceState(EpisodeReleaseDatePlatform.nowEpochMs()) {
        while (true) {
            delay(30_000L)
            value = EpisodeReleaseDatePlatform.nowEpochMs()
        }
    }

    LaunchedEffect(Unit) { LocalSyncRepository.start() }

    // Pairing and failures of something the person asked for are said once, as a snackbar.
    LaunchedEffect(state.activity) {
        val message = when (val activity = state.activity) {
            is LocalSyncActivity.Paired -> getString(Res.string.local_sync_paired, activity.peerName)
            is LocalSyncActivity.Failed -> getString(activity.error.messageRes())
            LocalSyncActivity.Idle -> null
        } ?: return@LaunchedEffect
        ToastController.show(message)
        LocalSyncRepository.clearActivity()
    }

    val forgetLabel = stringResource(Res.string.local_sync_forget)
    SettingsList(modifier = modifier) {
        shapedRow { shape ->
            LocalSyncBanner(shape = shape)
        }
        shapedRow { shape ->
            TrackingFeaturesRow(
                features = LocalSyncFeatures,
                active = state.peers.isNotEmpty(),
                shape = shape,
            )
        }
        state.peers.forEach { peer ->
            val syncing = peer.deviceId in state.syncingPeerIds
            navigationRow(
                title = peer.name,
                description = lastSyncedLabel(peer.lastSyncedAtEpochMs, now),
                icon = Icons.Rounded.Devices,
                enabled = !syncing,
                trailingContent = {
                    if (syncing) {
                        LoadingSpinner(size = 18.dp)
                    } else {
                        IconButton(onClick = { peerToForgetId = peer.deviceId }) {
                            Icon(imageVector = Icons.Rounded.Delete, contentDescription = forgetLabel)
                        }
                    }
                },
                onClick = { LocalSyncRepository.syncNow(peer) },
            )
        }
        navigationRow(
            title = stringResource(Res.string.local_sync_show_code),
            description = stringResource(
                if (state.peers.isEmpty()) Res.string.local_sync_explanation else Res.string.local_sync_show_code_description,
            ),
            icon = Icons.Rounded.QrCode,
            onClick = LocalSyncRepository::startPairing,
        )
        if (scan != null) {
            navigationRow(
                title = stringResource(Res.string.local_sync_scan_code),
                description = stringResource(Res.string.local_sync_scan_code_description),
                icon = Icons.Rounded.QrCodeScanner,
                enabled = !state.joining,
                trailingContent = if (state.joining) {
                    { LoadingSpinner(size = 18.dp) }
                } else {
                    null
                },
                onClick = scan,
            )
        }
        navigationRow(
            title = stringResource(Res.string.local_sync_enter_code),
            description = stringResource(Res.string.local_sync_enter_code_description),
            icon = Icons.Rounded.Keyboard,
            enabled = !state.joining,
            trailingContent = if (state.joining && scan == null) {
                { LoadingSpinner(size = 18.dp) }
            } else {
                null
            },
            onClick = { showCodeEntry = true },
        )
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

private val LocalSyncFeatures = listOf(
    TrackingFeature(Icons.Rounded.BookmarkBorder, Res.string.tracking_feature_lists),
    TrackingFeature(Icons.Rounded.History, Res.string.tracking_feature_watched),
    TrackingFeature(Icons.Rounded.PlayCircle, Res.string.tracking_feature_progress),
    TrackingFeature(Icons.Rounded.Extension, Res.string.tracking_feature_addons),
    TrackingFeature(Icons.Rounded.Tune, Res.string.tracking_feature_settings),
)

// The same place and height as a tracker's artwork, drawn from the Material You scheme instead of a brand.
@Composable
private fun LocalSyncBanner(shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            imageVector = Icons.Rounded.Devices,
            contentDescription = null,
            tint = colors.onPrimaryContainer.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(132.dp),
        )
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Devices,
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(Res.string.local_sync_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun lastSyncedLabel(lastSyncedAtEpochMs: Long?, nowEpochMs: Long): String {
    lastSyncedAtEpochMs ?: return stringResource(Res.string.local_sync_never_synced)
    val minutes = ((nowEpochMs - lastSyncedAtEpochMs) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> stringResource(Res.string.local_sync_synced_just_now)
        minutes < 60L -> stringResource(Res.string.local_sync_synced_minutes, minutes.toInt())
        minutes < 24L * 60L -> stringResource(Res.string.local_sync_synced_hours, (minutes / 60L).toInt())
        else -> stringResource(Res.string.local_sync_synced_days, (minutes / (24L * 60L)).toInt())
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
