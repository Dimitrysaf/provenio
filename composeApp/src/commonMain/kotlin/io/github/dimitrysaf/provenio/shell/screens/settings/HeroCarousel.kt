package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsItem
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_homescreen_hero_source
import provenio.composeapp.generated.resources.settings_homescreen_not_in_hero
import provenio.composeapp.generated.resources.settings_homescreen_section_hero_sources
import provenio.composeapp.generated.resources.settings_homescreen_show_hero

internal fun LazyListScope.heroCarouselSettingsContent(
    isTablet: Boolean,
    heroEnabled: Boolean,
    items: List<HomeCatalogSettingsItem>,
) {
    item {
        HomeLayoutPreview(highlight = HomePreviewSection.Hero)
    }
    item {
        SettingsMainSwitch(
            title = stringResource(Res.string.settings_homescreen_show_hero),
            checked = heroEnabled,
            onCheckedChange = HomeCatalogSettingsRepository::setHeroEnabled,
        )
    }
    item {
        val catalogs = items.filter { !it.isCollection }
        if (heroEnabled && catalogs.isNotEmpty()) {
            val selectedCount = catalogs.count { it.heroSourceEnabled }
            val limit = HomeCatalogSettingsRepository.HERO_SOURCE_SELECTION_LIMIT
            SettingsSection(
                title = "${stringResource(Res.string.settings_homescreen_section_hero_sources)} ($selectedCount/$limit)",
                isTablet = isTablet,
            ) {
                val shownDescription = stringResource(Res.string.settings_homescreen_hero_source)
                val hiddenDescription = stringResource(Res.string.settings_homescreen_not_in_hero)
                SettingsList {
                    catalogs.forEach { item ->
                        val shown = item.heroSourceEnabled
                        navigationRow(
                            title = item.displayTitle,
                            description = item.addonName,
                            // No "limit reached" note: the heading already reads n/n, and a
                            // catalog that cannot be shown is disabled.
                            enabled = shown || selectedCount < limit,
                            trailingContent = {
                                Icon(
                                    imageVector = if (shown) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = if (shown) shownDescription else hiddenDescription,
                                    tint = if (shown) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            onClick = { HomeCatalogSettingsRepository.setHeroSourceEnabled(item.key, !shown) },
                        )
                    }
                }
            }
        }
    }
}
