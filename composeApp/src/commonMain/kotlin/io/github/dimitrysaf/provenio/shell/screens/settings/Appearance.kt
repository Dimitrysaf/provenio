package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.theme.labelRes
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.collections_header
import provenio.composeapp.generated.resources.compose_settings_page_continue_watching
import provenio.composeapp.generated.resources.compose_settings_page_homescreen
import provenio.composeapp.generated.resources.compose_settings_page_meta_screen
import provenio.composeapp.generated.resources.compose_settings_page_poster_customization
import provenio.composeapp.generated.resources.compose_settings_page_streams
import provenio.composeapp.generated.resources.settings_appearance_app_language
import provenio.composeapp.generated.resources.settings_appearance_app_language_sheet_title
import provenio.composeapp.generated.resources.settings_appearance_app_icon
import provenio.composeapp.generated.resources.settings_appearance_amoled_black
import provenio.composeapp.generated.resources.settings_appearance_amoled_description
import provenio.composeapp.generated.resources.settings_appearance_continue_watching_description
import provenio.composeapp.generated.resources.settings_appearance_poster_customization_description
import provenio.composeapp.generated.resources.settings_appearance_section_detail_page
import provenio.composeapp.generated.resources.settings_appearance_section_display
import provenio.composeapp.generated.resources.settings_appearance_section_home
import provenio.composeapp.generated.resources.settings_appearance_section_streams
import provenio.composeapp.generated.resources.settings_content_discovery_collections_description
import provenio.composeapp.generated.resources.settings_content_discovery_homescreen_description
import provenio.composeapp.generated.resources.settings_content_discovery_meta_screen_description
import provenio.composeapp.generated.resources.compose_settings_root_streams_description
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.AppIconSettingsState
import io.github.dimitrysaf.provenio.core.settings.AppLanguage
import io.github.dimitrysaf.provenio.core.settings.labelResource

internal fun LazyListScope.appearanceSettingsContent(
    isTablet: Boolean,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    appIconState: AppIconSettingsState,
    onAppIconSelected: (AppIconOption) -> Unit,
    onAppIconFailureDismissed: () -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    onHomescreenClick: () -> Unit,
    onMetaScreenClick: () -> Unit,
    onStreamsClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    onContinueWatchingClick: () -> Unit,
    onPosterCustomizationClick: () -> Unit,
) {
    item {
        var showLanguageSheet by remember { mutableStateOf(false) }
        var showAppIconPicker by remember { mutableStateOf(false) }
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_display),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_appearance_amoled_black),
                    description = stringResource(Res.string.settings_appearance_amoled_description),
                    checked = { amoledEnabled },
                    onCheckedChange = onAmoledToggle,
                )
                navigationRow(
                    title = stringResource(Res.string.settings_appearance_app_icon),
                    description = stringResource(appIconState.selected.labelResource),
                    enabled = appIconState.pending == null,
                    trailingContent = {
                        AppIconThumbnail(
                            icon = appIconState.selected,
                            modifier = Modifier.size(if (isTablet) 44.dp else 40.dp),
                            cornerRadius = if (isTablet) 11.dp else 10.dp,
                        )
                    },
                    onClick = {
                        onAppIconFailureDismissed()
                        showAppIconPicker = true
                    },
                )
                navigationRow(
                    title = stringResource(Res.string.settings_appearance_app_language),
                    description = stringResource(selectedAppLanguage.labelRes),
                    onClick = { showLanguageSheet = true },
                )
            }
        }

        if (showLanguageSheet) {
            AppearanceLanguageBottomSheet(
                selectedLanguage = selectedAppLanguage,
                onLanguageSelected = onAppLanguageSelected,
                onDismiss = { showLanguageSheet = false },
            )
        }

        if (showAppIconPicker) {
            AppIconPicker(
                state = appIconState,
                onSelected = onAppIconSelected,
                onDismiss = {
                    onAppIconFailureDismissed()
                    showAppIconPicker = false
                },
            )
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_home),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_homescreen),
                    description = stringResource(Res.string.settings_content_discovery_homescreen_description),
                    onClick = onHomescreenClick,
                )
                navigationRow(
                    title = stringResource(Res.string.collections_header),
                    description = stringResource(Res.string.settings_content_discovery_collections_description),
                    onClick = onCollectionsClick,
                )
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_continue_watching),
                    description = stringResource(Res.string.settings_appearance_continue_watching_description),
                    onClick = onContinueWatchingClick,
                )
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_poster_customization),
                    description = stringResource(Res.string.settings_appearance_poster_customization_description),
                    onClick = onPosterCustomizationClick,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_streams),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_streams),
                    description = stringResource(Res.string.compose_settings_root_streams_description),
                    onClick = onStreamsClick,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_detail_page),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.compose_settings_page_meta_screen),
                    description = stringResource(Res.string.settings_content_discovery_meta_screen_description),
                    onClick = onMetaScreenClick,
                )
            }
        }
    }
}

@Composable
private fun AppearanceLanguageBottomSheet(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_appearance_app_language_sheet_title),
        options = AppLanguage.entries.map { language ->
            SingleChoiceOption(value = language, label = stringResource(language.labelRes))
        },
        isSelected = { it == selectedLanguage },
        onSelected = onLanguageSelected,
        onDismiss = onDismiss,
    )
}
