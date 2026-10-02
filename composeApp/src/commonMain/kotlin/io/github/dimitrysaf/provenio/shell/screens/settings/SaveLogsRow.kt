package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveResult
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogs
import io.github.dimitrysaf.provenio.core.diagnostics.rememberAppLogSaver
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_save_logs_collecting
import provenio.composeapp.generated.resources.settings_save_logs_description
import provenio.composeapp.generated.resources.settings_save_logs_failed
import provenio.composeapp.generated.resources.settings_save_logs_saved
import provenio.composeapp.generated.resources.settings_save_logs_title

private enum class SaveLogsState {
    IDLE,
    COLLECTING,
    SAVED,
    FAILED,
}

@Composable
internal fun rememberSaveLogsRow(): SaveLogsRowState {
    var state by remember { mutableStateOf(SaveLogsState.IDLE) }
    val scope = rememberCoroutineScope()
    val saver = rememberAppLogSaver { result ->
        state = when (result) {
            AppLogSaveResult.SAVED -> SaveLogsState.SAVED
            AppLogSaveResult.CANCELLED -> SaveLogsState.IDLE
            AppLogSaveResult.FAILED -> SaveLogsState.FAILED
        }
    }
    val description = when (state) {
        SaveLogsState.IDLE -> stringResource(Res.string.settings_save_logs_description)
        SaveLogsState.COLLECTING -> stringResource(Res.string.settings_save_logs_collecting)
        SaveLogsState.SAVED -> stringResource(Res.string.settings_save_logs_saved)
        SaveLogsState.FAILED -> stringResource(Res.string.settings_save_logs_failed)
    }
    return SaveLogsRowState(
        title = stringResource(Res.string.settings_save_logs_title),
        description = description,
        enabled = state != SaveLogsState.COLLECTING,
        onClick = {
            state = SaveLogsState.COLLECTING
            scope.launch {
                val bundle = runCatching { AppLogs.collect() }.getOrNull()
                if (bundle == null) {
                    state = SaveLogsState.FAILED
                } else {
                    saver(bundle)
                }
            }
        },
    )
}

internal class SaveLogsRowState(
    val title: String,
    val description: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

internal fun SettingsListScope.saveLogsRow(row: SaveLogsRowState) {
    navigationRow(
        title = row.title,
        description = row.description,
        icon = Icons.Rounded.Description,
        enabled = row.enabled,
        onClick = row.onClick,
    )
}
