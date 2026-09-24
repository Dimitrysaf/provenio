package com.nuvio.app.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nuvio.app.core.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.core.notifications.EpisodeReleaseNotificationsUiState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_close
import nuvio.composeapp.generated.resources.settings_notifications_disabled_in_app
import nuvio.composeapp.generated.resources.settings_notifications_episode_release_alerts
import nuvio.composeapp.generated.resources.settings_notifications_episode_release_alerts_description
import nuvio.composeapp.generated.resources.settings_notifications_permission_disabled
import nuvio.composeapp.generated.resources.settings_notifications_scheduled_count
import nuvio.composeapp.generated.resources.settings_notifications_section_alerts
import nuvio.composeapp.generated.resources.settings_notifications_section_test
import nuvio.composeapp.generated.resources.settings_notifications_sending_test
import nuvio.composeapp.generated.resources.settings_notifications_test_for_title
import nuvio.composeapp.generated.resources.settings_notifications_test_requires_saved_show
import nuvio.composeapp.generated.resources.settings_notifications_test_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.notificationsSettingsContent(
    isTablet: Boolean,
    uiState: EpisodeReleaseNotificationsUiState,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_notifications_section_alerts),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_notifications_episode_release_alerts),
                    description = stringResource(Res.string.settings_notifications_episode_release_alerts_description),
                    checked = { uiState.isEnabled },
                    enabled = !uiState.isLoading,
                    onCheckedChange = EpisodeReleaseNotificationsRepository::setEnabled,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_notifications_section_test),
            isTablet = isTablet,
        ) {
            // What a test did is a reply to pressing the row, so it is held until the send
            // settles and then said once, rather than sitting under the list forever.
            var awaitingResult by remember { mutableStateOf(false) }
            var resultMessage by remember { mutableStateOf<String?>(null) }
            val permissionWarning = if (uiState.permissionGranted) {
                null
            } else {
                stringResource(Res.string.settings_notifications_permission_disabled)
            }

            LaunchedEffect(uiState.isSendingTest, uiState.statusMessage, uiState.errorMessage) {
                if (!awaitingResult || uiState.isSendingTest) return@LaunchedEffect
                val lines = listOfNotNull(
                    uiState.errorMessage,
                    permissionWarning,
                    uiState.statusMessage,
                )
                if (lines.isEmpty()) return@LaunchedEffect
                resultMessage = lines.joinToString("\n\n")
                awaitingResult = false
            }

            val targetLine = uiState.testTargetTitle?.let { title ->
                stringResource(Res.string.settings_notifications_test_for_title, title)
            } ?: stringResource(Res.string.settings_notifications_test_requires_saved_show)
            val stateLine = if (uiState.isEnabled) {
                stringResource(Res.string.settings_notifications_scheduled_count, uiState.scheduledCount)
            } else {
                stringResource(Res.string.settings_notifications_disabled_in_app)
            }

            SettingsList {
                // The row is the action. A separate button under it would have said the same
                // thing twice, once as a label and once as a control.
                navigationRow(
                    title = if (uiState.isSendingTest) {
                        stringResource(Res.string.settings_notifications_sending_test)
                    } else {
                        stringResource(Res.string.settings_notifications_test_title)
                    },
                    description = "$targetLine\n$stateLine",
                    enabled = !uiState.isSendingTest &&
                        !uiState.isLoading &&
                        uiState.testTargetTitle != null,
                    onClick = {
                        awaitingResult = true
                        EpisodeReleaseNotificationsRepository.sendTestNotification()
                    },
                )
            }

            resultMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = { resultMessage = null },
                    title = { Text(stringResource(Res.string.settings_notifications_test_title)) },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { resultMessage = null }) {
                            Text(stringResource(Res.string.action_close))
                        }
                    },
                )
            }
        }
    }
}
