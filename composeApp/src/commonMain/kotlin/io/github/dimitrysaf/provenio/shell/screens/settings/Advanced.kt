package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Science
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingEnrichmentCache
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_advanced_clear_cw_cache
import provenio.composeapp.generated.resources.settings_advanced_clear_cw_cache_done
import provenio.composeapp.generated.resources.settings_advanced_clear_cw_cache_subtitle
import provenio.composeapp.generated.resources.settings_advanced_remember_last_profile
import provenio.composeapp.generated.resources.settings_advanced_remember_last_profile_description
import provenio.composeapp.generated.resources.settings_advanced_section_cache
import provenio.composeapp.generated.resources.settings_advanced_section_startup
import provenio.composeapp.generated.resources.settings_advanced_section_trakt
import provenio.composeapp.generated.resources.settings_advanced_section_updates
import provenio.composeapp.generated.resources.settings_tracking_show_trakt
import provenio.composeapp.generated.resources.settings_tracking_show_trakt_connected
import provenio.composeapp.generated.resources.settings_tracking_show_trakt_description
import provenio.composeapp.generated.resources.updates_debug_test_description
import provenio.composeapp.generated.resources.updates_debug_test_title
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository

internal fun LazyListScope.advancedSettingsContent(
    isTablet: Boolean,
    rememberLastProfileEnabled: Boolean,
    traktConnected: Boolean,
    traktEnabled: Boolean,
    onTestUpdateBannerClick: (() -> Unit)?,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_startup),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_advanced_remember_last_profile),
                    description = stringResource(Res.string.settings_advanced_remember_last_profile_description),
                    checked = { rememberLastProfileEnabled },
                    onCheckedChange = ProfileRepository::setRememberLastProfileEnabled,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_trakt),
            isTablet = isTablet,
        ) {
            val traktDescription = listOfNotNull(
                stringResource(Res.string.settings_tracking_show_trakt_description),
                stringResource(Res.string.settings_tracking_show_trakt_connected).takeIf { traktConnected },
            ).joinToString("\n")
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_tracking_show_trakt),
                    description = traktDescription,
                    icon = Icons.Rounded.Science,
                    // A live Trakt connection is never hidden, whatever the switch says.
                    checked = { traktEnabled || traktConnected },
                    enabled = !traktConnected,
                    onCheckedChange = TrackingSettingsRepository::setTraktEnabled,
                )
            }
        }
    }
    if (onTestUpdateBannerClick != null) {
        item {
            SettingsSection(
                title = stringResource(Res.string.settings_advanced_section_updates),
                isTablet = isTablet,
            ) {
                SettingsList {
                    navigationRow(
                        title = stringResource(Res.string.updates_debug_test_title),
                        description = stringResource(Res.string.updates_debug_test_description),
                        icon = Icons.Rounded.BugReport,
                        onClick = onTestUpdateBannerClick,
                    )
                }
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_advanced_section_cache),
            isTablet = isTablet,
        ) {
            SettingsList {
                val scope = rememberCoroutineScope()
                var cleared by rememberSaveable { mutableStateOf(false) }
                navigationRow(
                    title = stringResource(Res.string.settings_advanced_clear_cw_cache),
                    description = if (cleared) {
                        stringResource(Res.string.settings_advanced_clear_cw_cache_done)
                    } else {
                        stringResource(Res.string.settings_advanced_clear_cw_cache_subtitle)
                    },
                    onClick = {
                        if (!cleared) {
                            ContinueWatchingEnrichmentCache.clearAll(ProfileRepository.activeProfileId)
                            cleared = true
                            scope.launch {
                                WatchProgressRepository.clearLocalAndForceSnapshotRefreshFromServer(
                                    ProfileRepository.activeProfileId,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}
