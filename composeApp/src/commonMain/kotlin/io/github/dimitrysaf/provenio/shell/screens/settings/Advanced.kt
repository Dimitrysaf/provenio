package io.github.dimitrysaf.provenio.shell.screens.settings

import provenio.composeapp.generated.resources.settings_advanced_show_onboarding_description
import provenio.composeapp.generated.resources.settings_advanced_show_onboarding
import io.github.dimitrysaf.provenio.shell.screens.onboarding.OnboardingPreview
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository
import provenio.composeapp.generated.resources.settings_advanced_show_imports
import provenio.composeapp.generated.resources.settings_advanced_show_imports_description
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_advanced_remember_last_profile
import provenio.composeapp.generated.resources.settings_advanced_remember_last_profile_description
import provenio.composeapp.generated.resources.settings_advanced_section_startup
import provenio.composeapp.generated.resources.settings_advanced_section_updates
import provenio.composeapp.generated.resources.updates_debug_test_description
import provenio.composeapp.generated.resources.updates_debug_test_title
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.advancedSettingsContent(
    isTablet: Boolean,
    rememberLastProfileEnabled: Boolean,
    showImports: Boolean,
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
                navigationRow(
                    title = stringResource(Res.string.settings_advanced_show_onboarding),
                    description = stringResource(Res.string.settings_advanced_show_onboarding_description),
                    icon = Icons.Rounded.RocketLaunch,
                    onClick = OnboardingPreview::open,
                )
                switchRow(
                    title = stringResource(Res.string.settings_advanced_show_imports),
                    description = stringResource(Res.string.settings_advanced_show_imports_description),
                    checked = { showImports },
                    onCheckedChange = ThemeSettingsRepository::setShowImports,
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
}
