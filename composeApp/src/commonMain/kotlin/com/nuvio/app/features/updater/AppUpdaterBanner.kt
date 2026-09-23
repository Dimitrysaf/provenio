package com.nuvio.app.features.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.shell.theme.nuvio
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_continue
import nuvio.composeapp.generated.resources.action_install
import nuvio.composeapp.generated.resources.action_later
import nuvio.composeapp.generated.resources.action_retry
import nuvio.composeapp.generated.resources.action_update
import nuvio.composeapp.generated.resources.updates_debug_test_complete
import nuvio.composeapp.generated.resources.updates_downloading_progress
import nuvio.composeapp.generated.resources.updates_message_allow_installs
import nuvio.composeapp.generated.resources.updates_message_ready
import nuvio.composeapp.generated.resources.updates_no_release_notes
import nuvio.composeapp.generated.resources.updates_preparing_download
import nuvio.composeapp.generated.resources.updates_title_allow_installs
import nuvio.composeapp.generated.resources.updates_title_available
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.updater.AppUpdaterPlatform

@Composable
fun AppUpdaterHost(
    controller: AppUpdaterController,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!AppFeaturePolicy.inAppUpdaterEnabled || !AppUpdaterPlatform.isSupported) {
        content()
        return
    }

    val state by controller.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(controller) {
        controller.ensureAutoCheckStarted()
    }

    val update = state.update

    Box(modifier = modifier) { content() }

    if (state.showDialog && update != null) {
        AppUpdateDialog(
            state = state,
            update = update,
            onDownload = controller::downloadUpdate,
            onInstall = controller::installDownloadedUpdate,
            onDismiss = controller::dismissDialog,
        )
    }

    if (state.showUnknownSourcesDialog) {
        UnknownSourcesDialog(
            onContinue = controller::resumeInstallation,
            onDismiss = controller::dismissDialog,
        )
    }
}

/**
 * A basic dialog: 28dp container on surface container high, a 24dp secondary-tinted icon, the
 * headline and supporting text, and text-button actions. Supplying an icon centre-aligns the
 * header, which is what the spec asks for.
 *
 * m3.material.io/components/dialogs/specs
 */
@Composable
private fun AppUpdateDialog(
    state: AppUpdaterUiState,
    update: AppUpdate,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = (state.downloadProgress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180),
        label = "updateProgress",
    )
    val debugTestComplete = state.isDebugTest && !state.isDownloading && !state.isUpdateAvailable
    val isReady = state.downloadedApkPath != null
    val updateLabel = listOfNotNull(
        update.tag,
        update.assetSizeBytes?.let(::formatFileSize),
    ).joinToString(separator = " • ")

    AlertDialog(
        // A download in progress has no sensible cancel, so the dialog holds until it finishes.
        onDismissRequest = { if (!state.isDownloading) onDismiss() },
        icon = {
            Icon(
                imageVector = if (debugTestComplete || isReady) {
                    Icons.Outlined.CheckCircle
                } else {
                    Icons.Outlined.CloudDownload
                },
                contentDescription = null,
            )
        },
        title = {
            Text(
                text = when {
                    debugTestComplete -> stringResource(Res.string.updates_debug_test_complete)
                    isReady -> stringResource(Res.string.updates_message_ready)
                    else -> stringResource(Res.string.updates_title_available)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = updateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (state.isDownloading) {
                    Text(
                        text = if (state.downloadProgress != null) {
                            stringResource(
                                Res.string.updates_downloading_progress,
                                (state.downloadProgress * 100).toInt().coerceIn(0, 100),
                            )
                        } else {
                            stringResource(Res.string.updates_preparing_download)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.downloadProgress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    // The notes were behind an info button on the banner; in a dialog there is
                    // room to just show them.
                    Text(
                        text = update.notes.ifBlank {
                            stringResource(Res.string.updates_no_release_notes)
                        },
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (!state.isDownloading && !debugTestComplete) {
                TextButton(
                    onClick = if (isReady) onInstall else onDownload,
                    enabled = isReady || state.isUpdateAvailable,
                ) {
                    Text(
                        text = when {
                            isReady -> stringResource(Res.string.action_install)
                            state.errorMessage != null -> stringResource(Res.string.action_retry)
                            else -> stringResource(Res.string.action_update)
                        },
                    )
                }
            }
        },
        dismissButton = {
            if (!state.isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_later))
                }
            }
        },
    )
}

@Composable
private fun UnknownSourcesDialog(
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Also a basic dialog rather than a hand-built Surface: 28dp corner, surface container high
    // and the 24dp padding rhythm all come from AlertDialog's own defaults.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.updates_title_allow_installs)) },
        text = { Text(stringResource(Res.string.updates_message_allow_installs)) },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(Res.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_later))
            }
        },
    )
}
