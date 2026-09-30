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
import provenio.composeapp.generated.resources.settings_appearance_color_palette
import provenio.composeapp.generated.resources.settings_appearance_display_scale
import provenio.composeapp.generated.resources.settings_appearance_display_scale_automatic
import provenio.composeapp.generated.resources.settings_appearance_display_scale_automatic_description
import provenio.composeapp.generated.resources.settings_appearance_amoled_black
import provenio.composeapp.generated.resources.settings_appearance_amoled_description
import provenio.composeapp.generated.resources.settings_appearance_theme
import provenio.composeapp.generated.resources.settings_appearance_theme_dark
import provenio.composeapp.generated.resources.settings_appearance_theme_light
import provenio.composeapp.generated.resources.settings_appearance_theme_system
import provenio.composeapp.generated.resources.settings_appearance_theme_system_description
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.AppIconSettingsState
import io.github.dimitrysaf.provenio.core.settings.AppLanguage
import io.github.dimitrysaf.provenio.core.settings.ColorPalette
import io.github.dimitrysaf.provenio.core.settings.DisplayScaleOption
import io.github.dimitrysaf.provenio.core.settings.displayScaleSettingSupported
import io.github.dimitrysaf.provenio.core.settings.ThemeMode
import io.github.dimitrysaf.provenio.core.settings.labelResource

internal fun LazyListScope.appearanceSettingsContent(
    isTablet: Boolean,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    colorPalette: ColorPalette,
    dynamicColorAvailable: Boolean,
    onColorPaletteSelected: (ColorPalette) -> Unit,
    displayScale: DisplayScaleOption,
    onDisplayScaleSelected: (DisplayScaleOption) -> Unit,
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
        var showThemeSheet by remember { mutableStateOf(false) }
        var showColorPaletteSheet by remember { mutableStateOf(false) }
        var showDisplayScaleSheet by remember { mutableStateOf(false) }
        val effectiveColorPalette = colorPalette.effective(dynamicColorAvailable)
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
                    enabled = themeMode != ThemeMode.LIGHT,
                    onCheckedChange = onAmoledToggle,
                )
                navigationRow(
                    title = stringResource(Res.string.settings_appearance_theme),
                    description = stringResource(themeMode.labelRes),
                    onClick = { showThemeSheet = true },
                )
                navigationRow(
                    title = stringResource(Res.string.settings_appearance_color_palette),
                    description = effectiveColorPalette.label(),
                    trailingContent = {
                        ColorPaletteSwatch(
                            palette = effectiveColorPalette,
                            size = if (isTablet) 44.dp else 40.dp,
                        )
                    },
                    onClick = { showColorPaletteSheet = true },
                )
                if (displayScaleSettingSupported) {
                    navigationRow(
                        title = stringResource(Res.string.settings_appearance_display_scale),
                        description = displayScale.label(),
                        onClick = { showDisplayScaleSheet = true },
                    )
                }
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

        if (showThemeSheet) {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.settings_appearance_theme),
                options = ThemeMode.entries.map { mode ->
                    SingleChoiceOption(
                        value = mode,
                        label = stringResource(mode.labelRes),
                        supportingText = if (mode == ThemeMode.SYSTEM) {
                            stringResource(Res.string.settings_appearance_theme_system_description)
                        } else {
                            null
                        },
                    )
                },
                isSelected = { it == themeMode },
                onSelected = onThemeModeSelected,
                onDismiss = { showThemeSheet = false },
            )
        }

        if (showColorPaletteSheet) {
            ColorPaletteBottomSheet(
                selected = effectiveColorPalette,
                dynamicColorAvailable = dynamicColorAvailable,
                onSelected = onColorPaletteSelected,
                onDismiss = { showColorPaletteSheet = false },
            )
        }

        if (showDisplayScaleSheet) {
            SingleChoiceBottomSheet(
                title = stringResource(Res.string.settings_appearance_display_scale),
                options = DisplayScaleOption.entries.map { option ->
                    SingleChoiceOption(
                        value = option,
                        label = option.label(),
                        supportingText = if (option == DisplayScaleOption.AUTOMATIC) {
                            stringResource(Res.string.settings_appearance_display_scale_automatic_description)
                        } else {
                            null
                        },
                    )
                },
                isSelected = { it == displayScale },
                onSelected = onDisplayScaleSelected,
                onDismiss = { showDisplayScaleSheet = false },
            )
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

private val ThemeMode.labelRes: StringResource
    get() = when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_appearance_theme_system
        ThemeMode.LIGHT -> Res.string.settings_appearance_theme_light
        ThemeMode.DARK -> Res.string.settings_appearance_theme_dark
    }

@Composable
private fun DisplayScaleOption.label(): String = when (val value = scale) {
    null -> stringResource(Res.string.settings_appearance_display_scale_automatic)
    else -> "${(value * 100).toInt()}%"
}
