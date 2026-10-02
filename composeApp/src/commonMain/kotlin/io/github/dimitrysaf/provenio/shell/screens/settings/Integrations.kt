package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import provenio.composeapp.generated.resources.compose_settings_page_debrid
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import provenio.composeapp.generated.resources.compose_settings_page_streaming_availability
import provenio.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import provenio.composeapp.generated.resources.settings_integrations_mdblist_description
import provenio.composeapp.generated.resources.settings_integrations_debrid_description
import provenio.composeapp.generated.resources.settings_integrations_section_title
import provenio.composeapp.generated.resources.settings_integrations_streaming_availability_description
import provenio.composeapp.generated.resources.settings_integrations_tmdb_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.integrationsContent(
    isTablet: Boolean,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
    onStreamingAvailabilityClick: () -> Unit,
    onDebridClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_title),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_tmdb_enrichment),
                    description = stringResource(Res.string.settings_integrations_tmdb_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Tmdb),
                    opensPage = true,
                    onClick = onTmdbClick,
                )
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_mdblist_ratings),
                    description = stringResource(Res.string.settings_integrations_mdblist_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.MdbList),
                    opensPage = true,
                    onClick = onMdbListClick,
                )
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_streaming_availability),
                    description = stringResource(Res.string.settings_integrations_streaming_availability_description),
                    onClick = onStreamingAvailabilityClick,
                )
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_debrid),
                    description = stringResource(Res.string.settings_integrations_debrid_description),
                    opensPage = true,
                    onClick = onDebridClick,
                )
            }
        }
    }
}
