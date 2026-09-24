package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.search.SearchHistoryRepository
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_settings_page_addons
import provenio.composeapp.generated.resources.compose_settings_page_plugins
import provenio.composeapp.generated.resources.settings_content_discovery_addons_description
import provenio.composeapp.generated.resources.settings_content_discovery_addons_description_appstore
import provenio.composeapp.generated.resources.settings_content_discovery_plugins_description
import provenio.composeapp.generated.resources.settings_content_discovery_section_sources
import provenio.composeapp.generated.resources.settings_content_discovery_section_search
import provenio.composeapp.generated.resources.settings_content_discovery_recent_searches
import provenio.composeapp.generated.resources.settings_content_discovery_recent_searches_description
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.contentDiscoveryContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
) {
    item {
        val recentSearchesEnabled by remember {
            SearchHistoryRepository.ensureLoaded()
            SearchHistoryRepository.enabled
        }.collectAsStateWithLifecycle()

        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_search),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_content_discovery_recent_searches),
                    description = stringResource(Res.string.settings_content_discovery_recent_searches_description),
                    checked = { recentSearchesEnabled },
                    onCheckedChange = SearchHistoryRepository::setEnabled,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_content_discovery_section_sources),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_addons),
                    description = stringResource(
                        if (AppFeaturePolicy.personalMediaAddonCopyEnabled) {
                            Res.string.settings_content_discovery_addons_description_appstore
                        } else {
                            Res.string.settings_content_discovery_addons_description
                        },
                    ),
                    onClick = onAddonsClick,
                )
                if (showPluginsEntry) {
                    navigationRow(
                        title = stringResource(Res.string.compose_settings_page_plugins),
                        description = stringResource(Res.string.settings_content_discovery_plugins_description),
                        onClick = onPluginsClick,
                    )
                }
            }
        }
    }
}
