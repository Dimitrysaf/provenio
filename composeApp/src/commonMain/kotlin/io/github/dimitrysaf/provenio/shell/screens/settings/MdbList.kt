package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListMetadataService
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettingsRepository
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_save
import provenio.composeapp.generated.resources.settings_debrid_not_set
import provenio.composeapp.generated.resources.settings_mdb_add_api_key_first
import provenio.composeapp.generated.resources.settings_mdb_api_key_description
import provenio.composeapp.generated.resources.settings_mdb_api_key_label
import provenio.composeapp.generated.resources.settings_mdb_api_key_saved
import provenio.composeapp.generated.resources.settings_mdb_api_key_title
import provenio.composeapp.generated.resources.settings_mdb_enable_ratings
import provenio.composeapp.generated.resources.settings_mdb_enable_ratings_description
import provenio.composeapp.generated.resources.settings_mdb_section_api_key
import provenio.composeapp.generated.resources.settings_mdb_section_rating_providers
import provenio.composeapp.generated.resources.settings_mdb_section_title
import provenio.composeapp.generated.resources.source_audience_score
import provenio.composeapp.generated.resources.source_imdb
import provenio.composeapp.generated.resources.source_letterboxd
import provenio.composeapp.generated.resources.source_mal
import provenio.composeapp.generated.resources.source_metacritic
import provenio.composeapp.generated.resources.source_rotten_tomatoes
import provenio.composeapp.generated.resources.source_tmdb
import provenio.composeapp.generated.resources.source_trakt
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.mdbListSettingsContent(
    isTablet: Boolean,
    settings: MdbListSettings,
) {
    val providerControlsEnabled = settings.enabled && settings.hasApiKey

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_mdb_section_title),
            isTablet = isTablet,
        ) {
            if (!settings.hasApiKey) {
                MdbListInfoRow(text = stringResource(Res.string.settings_mdb_add_api_key_first))
            }
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_mdb_enable_ratings),
                    description = stringResource(Res.string.settings_mdb_enable_ratings_description),
                    checked = { settings.enabled },
                    enabled = settings.hasApiKey,
                    onCheckedChange = MdbListSettingsRepository::setEnabled,
                )
            }
        }
    }

    item {
        var showApiKeyDialog by rememberSaveable { mutableStateOf(false) }

        SettingsSection(
            title = stringResource(Res.string.settings_mdb_section_api_key),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.settings_mdb_api_key_title),
                    description = stringResource(Res.string.settings_mdb_api_key_description),
                    trailingContent = {
                        Text(
                            text = if (settings.hasApiKey) {
                                stringResource(Res.string.settings_mdb_api_key_saved)
                            } else {
                                stringResource(Res.string.settings_debrid_not_set)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = { showApiKeyDialog = true },
                )
            }
        }

        if (showApiKeyDialog) {
            MdbListApiKeyDialog(
                currentValue = settings.apiKey,
                onConfirm = { entered ->
                    MdbListSettingsRepository.setApiKey(entered.trim())
                    showApiKeyDialog = false
                },
                onDismiss = { showApiKeyDialog = false },
            )
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_mdb_section_rating_providers),
            isTablet = isTablet,
        ) {
            SettingsList {
                ProviderRows(
                    settings = settings,
                    controlsEnabled = providerControlsEnabled,
                )
            }
        }
    }
}

// Declares rows, so it must not be skippable: see SettingsListScope.
@Composable
@NonRestartableComposable
private fun SettingsListScope.ProviderRows(
    settings: MdbListSettings,
    controlsEnabled: Boolean,
) {
    val providers = listOf(
        MdbListMetadataService.PROVIDER_IMDB to Res.string.source_imdb,
        MdbListMetadataService.PROVIDER_TMDB to Res.string.source_tmdb,
        MdbListMetadataService.PROVIDER_TOMATOES to Res.string.source_rotten_tomatoes,
        MdbListMetadataService.PROVIDER_METACRITIC to Res.string.source_metacritic,
        MdbListMetadataService.PROVIDER_TRAKT to Res.string.source_trakt,
        MdbListMetadataService.PROVIDER_LETTERBOXD to Res.string.source_letterboxd,
        MdbListMetadataService.PROVIDER_AUDIENCE to Res.string.source_audience_score,
        MdbListMetadataService.PROVIDER_MAL to Res.string.source_mal,
    )

    providers.forEach { (providerId, providerLabelRes) ->
        switchRow(
            title = stringResource(providerLabelRes),
            checked = { settings.isProviderEnabled(providerId) },
            enabled = controlsEnabled,
            onCheckedChange = { checked ->
                MdbListSettingsRepository.setProviderEnabled(providerId, checked)
            },
        )
    }
}

/** Asks for the key, masked, the way the field on the page used to. */
@Composable
private fun MdbListApiKeyDialog(
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_mdb_api_key_title)) },
        text = {
            SettingsSecretTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(Res.string.settings_mdb_api_key_label),
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

/** A note about the section, above the group rather than pretending to be a row of it. */
@Composable
private fun MdbListInfoRow(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
