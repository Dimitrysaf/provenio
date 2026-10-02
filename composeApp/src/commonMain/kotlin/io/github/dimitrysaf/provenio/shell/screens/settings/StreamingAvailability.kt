package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.streams.availability.StreamingAvailabilitySettings
import io.github.dimitrysaf.provenio.core.streams.availability.StreamingAvailabilitySettingsRepository
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_save
import provenio.composeapp.generated.resources.settings_debrid_not_set
import provenio.composeapp.generated.resources.settings_sa_add_api_key_first
import provenio.composeapp.generated.resources.settings_sa_api_key_description
import provenio.composeapp.generated.resources.settings_sa_api_key_label
import provenio.composeapp.generated.resources.settings_sa_api_key_saved
import provenio.composeapp.generated.resources.settings_sa_api_key_title
import provenio.composeapp.generated.resources.settings_sa_enable
import provenio.composeapp.generated.resources.settings_sa_get_api_key
import provenio.composeapp.generated.resources.settings_sa_get_api_key_description
import provenio.composeapp.generated.resources.settings_sa_section_api_key
import androidx.compose.foundation.layout.Column
import org.jetbrains.compose.resources.stringResource

private const val StreamingAvailabilitySignUpUrl = "https://www.movieofthenight.com/about/api"

internal fun LazyListScope.streamingAvailabilitySettingsContent(
    isTablet: Boolean,
    settings: StreamingAvailabilitySettings,
) {
    item {
        Column {
            if (!settings.hasApiKey) {
                StreamingAvailabilityInfoRow(text = stringResource(Res.string.settings_sa_add_api_key_first))
            }
            SettingsMainSwitch(
                title = stringResource(Res.string.settings_sa_enable),
                checked = settings.enabled,
                // Availability needs a key, so the switch waits for one.
                enabled = settings.hasApiKey,
                onCheckedChange = StreamingAvailabilitySettingsRepository::setEnabled,
            )
        }
    }

    item {
        var showApiKeyDialog by rememberSaveable { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current

        SettingsSection(
            title = stringResource(Res.string.settings_sa_section_api_key),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.settings_sa_api_key_title),
                    description = stringResource(Res.string.settings_sa_api_key_description),
                    trailingContent = {
                        Text(
                            text = if (settings.hasApiKey) {
                                stringResource(Res.string.settings_sa_api_key_saved)
                            } else {
                                stringResource(Res.string.settings_debrid_not_set)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = { showApiKeyDialog = true },
                )
                navigationRow(
                    title = stringResource(Res.string.settings_sa_get_api_key),
                    description = stringResource(Res.string.settings_sa_get_api_key_description),
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                        )
                    },
                    onClick = { uriHandler.openUri(StreamingAvailabilitySignUpUrl) },
                )
            }
        }

        if (showApiKeyDialog) {
            StreamingAvailabilityApiKeyDialog(
                currentValue = settings.apiKey,
                onConfirm = { entered ->
                    StreamingAvailabilitySettingsRepository.setApiKey(entered.trim())
                    showApiKeyDialog = false
                },
                onDismiss = { showApiKeyDialog = false },
            )
        }
    }
}

@Composable
private fun StreamingAvailabilityApiKeyDialog(
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_sa_api_key_title)) },
        text = {
            SettingsSecretTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(Res.string.settings_sa_api_key_label),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(draft) },
                enabled = draft.trim() != currentValue,
            ) {
                Text(stringResource(Res.string.action_save))
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
private fun StreamingAvailabilityInfoRow(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
