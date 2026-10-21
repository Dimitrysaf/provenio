package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.diagnostics.AppCrash
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogBundle
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveFailure
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveResult
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogs
import io.github.dimitrysaf.provenio.shell.components.SelectableText
import io.github.dimitrysaf.provenio.shell.components.ToastController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_close
import provenio.composeapp.generated.resources.action_retry
import provenio.composeapp.generated.resources.crash_prompt_message
import provenio.composeapp.generated.resources.crash_prompt_not_now
import provenio.composeapp.generated.resources.crash_prompt_title
import provenio.composeapp.generated.resources.settings_save_logs_collecting
import provenio.composeapp.generated.resources.settings_save_logs_copy_crash
import provenio.composeapp.generated.resources.settings_save_logs_description
import provenio.composeapp.generated.resources.settings_save_logs_detail
import provenio.composeapp.generated.resources.settings_save_logs_empty
import provenio.composeapp.generated.resources.settings_save_logs_empty_file_deleted
import provenio.composeapp.generated.resources.settings_save_logs_empty_file_left
import provenio.composeapp.generated.resources.settings_save_logs_failed
import provenio.composeapp.generated.resources.settings_save_logs_last_crash
import provenio.composeapp.generated.resources.settings_save_logs_lost
import provenio.composeapp.generated.resources.settings_save_logs_nothing
import provenio.composeapp.generated.resources.settings_save_logs_saved
import provenio.composeapp.generated.resources.settings_save_logs_title
import provenio.composeapp.generated.resources.settings_save_logs_write_failed

internal enum class SaveLogsState {
    IDLE,
    COLLECTING,
    SAVED,
    FAILED,
}

/**
 * Gathering the logs and saving them to a file the user picks, from the Settings row or from the
 * question asked after a crash. A save that fails says why, see [LogSaveFailedDialog].
 */
@Stable
internal class LogSaving(private val scope: CoroutineScope) {
    internal var state by mutableStateOf(SaveLogsState.IDLE)
    var failure by mutableStateOf<AppLogSaveResult.Failed?>(null)
    internal var saver: (AppLogBundle) -> Unit = {}
    internal var savedText = ""

    val collecting: Boolean get() = state == SaveLogsState.COLLECTING

    fun save() {
        state = SaveLogsState.COLLECTING
        scope.launch {
            val bundle = runCatching { AppLogs.collect() }.getOrNull()
            if (bundle == null || bundle.bytes.isEmpty()) {
                finish(AppLogSaveResult.Failed(AppLogSaveFailure.NothingToSave))
            } else {
                saver(bundle)
            }
        }
    }

    internal fun finish(result: AppLogSaveResult) {
        state = when (result) {
            AppLogSaveResult.Saved -> SaveLogsState.SAVED
            AppLogSaveResult.Cancelled -> SaveLogsState.IDLE
            is AppLogSaveResult.Failed -> SaveLogsState.FAILED
        }
        failure = result as? AppLogSaveResult.Failed
        if (result == AppLogSaveResult.Saved) ToastController.show(savedText)
    }
}

@Composable
internal fun rememberLogSaving(): LogSaving {
    val scope = rememberCoroutineScope()
    val saving = remember { LogSaving(scope) }
    saving.savedText = stringResource(Res.string.settings_save_logs_saved)
    saving.saver = rememberAppLogSaver { result -> saving.finish(result) }
    return saving
}

@Composable
internal fun rememberSaveLogsRow(): SaveLogsRowState {
    val saving = rememberLogSaving()
    LogSaveFailedDialog(saving)
    val description = when (saving.state) {
        SaveLogsState.IDLE -> stringResource(Res.string.settings_save_logs_description)
        SaveLogsState.COLLECTING -> stringResource(Res.string.settings_save_logs_collecting)
        SaveLogsState.SAVED -> stringResource(Res.string.settings_save_logs_saved)
        SaveLogsState.FAILED -> stringResource(Res.string.settings_save_logs_failed)
    }
    return SaveLogsRowState(
        title = stringResource(Res.string.settings_save_logs_title),
        description = description,
        enabled = !saving.collecting,
        onClick = saving::save,
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

/**
 * Why the logs could not be saved, and the last crash as the system recorded it: when the file is
 * no use, the crash can still be read and copied here.
 */
@Composable
internal fun LogSaveFailedDialog(saving: LogSaving) {
    val failure = saving.failure ?: return
    var crash by remember(failure) { mutableStateOf<AppCrash?>(null) }
    LaunchedEffect(failure) { crash = runCatching { AppLogs.latestCrash() }.getOrNull() }
    val close = { saving.failure = null }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(stringResource(Res.string.settings_save_logs_failed)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(
                        when (failure.reason) {
                            AppLogSaveFailure.NothingToSave -> Res.string.settings_save_logs_nothing
                            AppLogSaveFailure.Lost -> Res.string.settings_save_logs_lost
                            AppLogSaveFailure.WriteFailed -> Res.string.settings_save_logs_write_failed
                            AppLogSaveFailure.Empty -> Res.string.settings_save_logs_empty
                        },
                    ),
                )
                failure.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                    Text(
                        text = stringResource(Res.string.settings_save_logs_detail, detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (failure.reason != AppLogSaveFailure.NothingToSave) {
                    Text(
                        stringResource(
                            if (failure.emptyFileLeft) {
                                Res.string.settings_save_logs_empty_file_left
                            } else {
                                Res.string.settings_save_logs_empty_file_deleted
                            },
                        ),
                    )
                }
                crash?.let { recorded ->
                    Text(
                        text = stringResource(Res.string.settings_save_logs_last_crash),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    CrashReport(recorded.report)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    close()
                    saving.save()
                },
            ) {
                Text(stringResource(Res.string.action_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = close) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

/**
 * Asked once after the app crashed: whether to save the logs, with what went wrong shown under
 * the question. Either answer counts as having been told.
 */
@Composable
internal fun CrashPrompt() {
    if (!AppLogs.isSupported) return
    var crash by remember { mutableStateOf<AppCrash?>(null) }
    LaunchedEffect(Unit) { crash = runCatching { AppLogs.unseenCrash() }.getOrNull() }
    val saving = rememberLogSaving()
    LogSaveFailedDialog(saving)
    val shown = crash ?: return
    val answer = { save: Boolean ->
        AppLogs.markCrashesSeen()
        crash = null
        if (save) saving.save()
    }
    AlertDialog(
        onDismissRequest = { answer(false) },
        title = { Text(stringResource(Res.string.crash_prompt_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.crash_prompt_message))
                CrashReport(shown.report)
            }
        },
        confirmButton = {
            TextButton(onClick = { answer(true) }) {
                Text(stringResource(Res.string.settings_save_logs_title))
            }
        },
        dismissButton = {
            TextButton(onClick = { answer(false) }) {
                Text(stringResource(Res.string.crash_prompt_not_now))
            }
        },
    )
}

/** A crash report as recorded, to read and to copy. */
@Composable
private fun CrashReport(report: String) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectableText(copyText = report) {
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                )
            }
        }
        TextButton(onClick = { clipboardManager.setText(AnnotatedString(report)) }) {
            Text(stringResource(Res.string.settings_save_logs_copy_crash))
        }
    }
}
