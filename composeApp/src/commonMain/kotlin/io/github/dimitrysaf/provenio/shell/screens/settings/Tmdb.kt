package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import provenio.composeapp.generated.resources.settings_tmdb_enable_enrichment_description
import provenio.composeapp.generated.resources.settings_tmdb_language_code_label
import provenio.composeapp.generated.resources.settings_tmdb_module_artwork
import provenio.composeapp.generated.resources.settings_tmdb_module_artwork_description
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
import provenio.composeapp.generated.resources.settings_tmdb_section_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.tmdbSettingsContent(
    isTablet: Boolean,
    settings: TmdbSettings,
) {
    val enrichmentControlsEnabled = settings.enabled

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_tmdb_section_title),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_tmdb_enable_enrichment),
                    description = stringResource(Res.string.settings_tmdb_enable_enrichment_description),
                    checked = { settings.enabled },
                    onCheckedChange = TmdbSettingsRepository::setEnabled,
                )
            }
        }
    }

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
