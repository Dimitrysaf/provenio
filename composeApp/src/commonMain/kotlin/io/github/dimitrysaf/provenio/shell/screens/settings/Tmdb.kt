package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbApiKey
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_save
import provenio.composeapp.generated.resources.settings_debrid_not_set
import provenio.composeapp.generated.resources.settings_tmdb_api_key_built_in
import provenio.composeapp.generated.resources.settings_tmdb_api_key_description
import provenio.composeapp.generated.resources.settings_tmdb_api_key_label
import provenio.composeapp.generated.resources.settings_tmdb_api_key_required
import provenio.composeapp.generated.resources.settings_tmdb_api_key_saved
import provenio.composeapp.generated.resources.settings_tmdb_api_key_title
import provenio.composeapp.generated.resources.settings_tmdb_get_api_key
import provenio.composeapp.generated.resources.settings_tmdb_get_api_key_description
import provenio.composeapp.generated.resources.settings_tmdb_section_api_key
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.shell.components.TextPromptDialog
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettings
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.tmdb.normalizeLanguage
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_tmdb_enable_enrichment
import provenio.composeapp.generated.resources.settings_tmdb_language_code_label
import provenio.composeapp.generated.resources.settings_tmdb_module_artwork
import provenio.composeapp.generated.resources.settings_tmdb_module_artwork_description
import provenio.composeapp.generated.resources.settings_tmdb_prefer_addon_artwork
import provenio.composeapp.generated.resources.settings_tmdb_prefer_addon_artwork_description
import provenio.composeapp.generated.resources.settings_tmdb_module_basic_info
import provenio.composeapp.generated.resources.settings_tmdb_module_basic_info_description
import provenio.composeapp.generated.resources.settings_tmdb_module_collections
import provenio.composeapp.generated.resources.settings_tmdb_module_collections_description
import provenio.composeapp.generated.resources.settings_tmdb_module_credits
import provenio.composeapp.generated.resources.settings_tmdb_module_credits_description
import provenio.composeapp.generated.resources.settings_tmdb_module_details
import provenio.composeapp.generated.resources.settings_tmdb_module_details_description
import provenio.composeapp.generated.resources.settings_tmdb_module_episodes
import provenio.composeapp.generated.resources.settings_tmdb_module_episodes_description
import provenio.composeapp.generated.resources.settings_tmdb_module_more_like_this
import provenio.composeapp.generated.resources.settings_tmdb_module_more_like_this_description
import provenio.composeapp.generated.resources.settings_tmdb_module_networks
import provenio.composeapp.generated.resources.settings_tmdb_module_networks_description
import provenio.composeapp.generated.resources.settings_tmdb_module_production_companies
import provenio.composeapp.generated.resources.settings_tmdb_module_production_companies_description
import provenio.composeapp.generated.resources.settings_tmdb_module_release_dates
import provenio.composeapp.generated.resources.settings_tmdb_module_release_dates_description
import provenio.composeapp.generated.resources.settings_tmdb_module_season_posters
import provenio.composeapp.generated.resources.settings_tmdb_module_season_posters_description
import provenio.composeapp.generated.resources.settings_tmdb_module_trailers
import provenio.composeapp.generated.resources.settings_tmdb_module_trailers_description
import provenio.composeapp.generated.resources.settings_tmdb_preferred_language
import provenio.composeapp.generated.resources.settings_tmdb_preferred_language_description
import provenio.composeapp.generated.resources.settings_tmdb_section_localization
import provenio.composeapp.generated.resources.settings_tmdb_section_modules
import org.jetbrains.compose.resources.stringResource

private const val TmdbApiKeyUrl = "https://www.themoviedb.org/settings/api"

@Composable
private fun TmdbApiKeyDialog(
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(currentValue) }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_tmdb_api_key_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSecretTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(Res.string.settings_tmdb_api_key_label),
                )
                TextButton(
                    onClick = { uriHandler.openUri(TmdbApiKeyUrl) },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(TmdbApiKeyUrl.removePrefix("https://www."))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp),
                    )
                }
            }
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

internal fun LazyListScope.tmdbSettingsContent(
    isTablet: Boolean,
    settings: TmdbSettings,
) {
    val enrichmentControlsEnabled = settings.enabled

    item {
        SettingsMainSwitch(
            title = stringResource(Res.string.settings_tmdb_enable_enrichment),
            checked = settings.enabled,
            onCheckedChange = TmdbSettingsRepository::setEnabled,
        )
    }

    item {
        var showApiKeyDialog by rememberSaveable { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current
        val hasUserKey = settings.apiKey.isNotBlank()

        SettingsSection(
            title = stringResource(Res.string.settings_tmdb_section_api_key),
            isTablet = isTablet,
        ) {
            if (!hasUserKey && !TmdbApiKey.hasBuiltInKey) {
                Text(
                    text = stringResource(Res.string.settings_tmdb_api_key_required),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.settings_tmdb_api_key_title),
                    description = stringResource(Res.string.settings_tmdb_api_key_description),
                    trailingContent = {
                        Text(
                            text = when {
                                hasUserKey -> stringResource(Res.string.settings_tmdb_api_key_saved)
                                TmdbApiKey.hasBuiltInKey -> stringResource(Res.string.settings_tmdb_api_key_built_in)
                                else -> stringResource(Res.string.settings_debrid_not_set)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = { showApiKeyDialog = true },
                )
                navigationRow(
                    title = stringResource(Res.string.settings_tmdb_get_api_key),
                    description = stringResource(Res.string.settings_tmdb_get_api_key_description),
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                        )
                    },
                    onClick = { uriHandler.openUri(TmdbApiKeyUrl) },
                )
            }
        }

        if (showApiKeyDialog) {
            TmdbApiKeyDialog(
                currentValue = settings.apiKey,
                onConfirm = { entered ->
                    TmdbSettingsRepository.setApiKey(entered)
                    showApiKeyDialog = false
                },
                onDismiss = { showApiKeyDialog = false },
            )
        }
    }

    // The key stays reachable while enrichment is off; everything below shapes enrichment itself.
    if (!settings.enabled) return
    item {
        var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

        SettingsSection(
            title = stringResource(Res.string.settings_tmdb_section_localization),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.settings_tmdb_preferred_language),
                    description = stringResource(Res.string.settings_tmdb_preferred_language_description),
                    trailingContent = {
                        Text(
                            text = settings.language,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = { showLanguageDialog = true },
                )
            }
        }

        if (showLanguageDialog) {
            TextPromptDialog(
                title = stringResource(Res.string.settings_tmdb_preferred_language),
                label = stringResource(Res.string.settings_tmdb_language_code_label),
                initialValue = settings.language,
                onConfirm = { entered ->
                    TmdbSettingsRepository.setLanguage(normalizeLanguage(entered))
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false },
            )
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tmdb_section_modules),
            isTablet = isTablet,
        ) {
            SettingsList {
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_trailers),
                    description = stringResource(Res.string.settings_tmdb_module_trailers_description),
                    checked = { settings.useTrailers },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseTrailers,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_artwork),
                    description = stringResource(Res.string.settings_tmdb_module_artwork_description),
                    checked = { settings.useArtwork },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseArtwork,
                )
                // Ordering TMDB's artwork against an addon's only matters while TMDB supplies some.
                expandableRows(expanded = settings.useArtwork) {
                    TmdbToggleRow(
                        title = stringResource(Res.string.settings_tmdb_prefer_addon_artwork),
                        description = stringResource(Res.string.settings_tmdb_prefer_addon_artwork_description),
                        checked = { settings.preferAddonArtwork },
                        enabled = enrichmentControlsEnabled,
                        onCheckedChange = TmdbSettingsRepository::setPreferAddonArtwork,
                    )
                }
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_basic_info),
                    description = stringResource(Res.string.settings_tmdb_module_basic_info_description),
                    checked = { settings.useBasicInfo },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseBasicInfo,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_details),
                    description = stringResource(Res.string.settings_tmdb_module_details_description),
                    checked = { settings.useDetails },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseDetails,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_release_dates),
                    description = stringResource(Res.string.settings_tmdb_module_release_dates_description),
                    checked = { settings.useReleaseDates },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseReleaseDates,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_credits),
                    description = stringResource(Res.string.settings_tmdb_module_credits_description),
                    checked = { settings.useCredits },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseCredits,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_production_companies),
                    description = stringResource(Res.string.settings_tmdb_module_production_companies_description),
                    checked = { settings.useProductions },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseProductions,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_networks),
                    description = stringResource(Res.string.settings_tmdb_module_networks_description),
                    checked = { settings.useNetworks },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseNetworks,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_episodes),
                    description = stringResource(Res.string.settings_tmdb_module_episodes_description),
                    checked = { settings.useEpisodes },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseEpisodes,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_season_posters),
                    description = stringResource(Res.string.settings_tmdb_module_season_posters_description),
                    checked = { settings.useSeasonPosters },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseSeasonPosters,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_more_like_this),
                    description = stringResource(Res.string.settings_tmdb_module_more_like_this_description),
                    checked = { settings.useMoreLikeThis },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseMoreLikeThis,
                )
                TmdbToggleRow(
                    title = stringResource(Res.string.settings_tmdb_module_collections),
                    description = stringResource(Res.string.settings_tmdb_module_collections_description),
                    checked = { settings.useCollections },
                    enabled = enrichmentControlsEnabled,
                    onCheckedChange = TmdbSettingsRepository::setUseCollections,
                )
            }
        }
    }
}

// Declares rows, so it must not be skippable: see SettingsListScope.
@Composable
@NonRestartableComposable
private fun SettingsListScope.TmdbToggleRow(
    title: String,
    description: String,
    checked: () -> Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    switchRow(
        title = title,
        description = description,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}
