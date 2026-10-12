package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.search.SearchHistoryRepository
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_content_discovery_section_search
import provenio.composeapp.generated.resources.settings_content_discovery_recent_searches
import provenio.composeapp.generated.resources.settings_content_discovery_recent_searches_description
import org.jetbrains.compose.resources.stringResource

/** The Interface page's Search section: whether recent searches are saved. */
internal fun LazyListScope.searchSettingsContent(isTablet: Boolean) {
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
}
