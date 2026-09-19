package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.simkl.SIMKL_AUTOMATIC_REFRESH_INTERVAL_MINUTES
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.settings_simkl_sync_info_activity
import nuvio.composeapp.generated.resources.settings_simkl_sync_info_description
import nuvio.composeapp.generated.resources.settings_simkl_sync_info_docs
import nuvio.composeapp.generated.resources.settings_simkl_sync_info_library_statuses
import nuvio.composeapp.generated.resources.settings_simkl_sync_info_manual
import nuvio.composeapp.generated.resources.settings_simkl_sync_info_title
import nuvio.composeapp.generated.resources.settings_trakt_failed_open_browser
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SimklSyncInfoDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var browserError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_simkl_sync_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        Res.string.settings_simkl_sync_info_description,
                        SIMKL_AUTOMATIC_REFRESH_INTERVAL_MINUTES,
                    ),
                )
                Text(stringResource(Res.string.settings_simkl_sync_info_activity))
                Text(stringResource(Res.string.settings_simkl_sync_info_manual))
                Text(stringResource(Res.string.settings_simkl_sync_info_library_statuses))
                if (browserError) {
                    Text(
                        text = stringResource(Res.string.settings_trakt_failed_open_browser),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    browserError = false
                    runCatching { uriHandler.openUri(SIMKL_SYNC_GUIDE_URL) }
                        .onFailure { browserError = true }
                },
            ) {
                Text(stringResource(Res.string.settings_simkl_sync_info_docs))
            }
        },
    )
}

private const val SIMKL_SYNC_GUIDE_URL = "https://api.simkl.org/guides/sync"
