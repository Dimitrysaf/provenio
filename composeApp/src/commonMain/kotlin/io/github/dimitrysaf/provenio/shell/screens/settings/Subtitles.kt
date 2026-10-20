package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.build.isIos
import io.github.dimitrysaf.provenio.core.playback.AndroidPlaybackEngine
import io.github.dimitrysaf.provenio.core.playback.AvailableLanguageOptions
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import io.github.dimitrysaf.provenio.core.playback.SubtitleLanguageOption
import io.github.dimitrysaf.provenio.core.playback.SubtitleStyleState
import io.github.dimitrysaf.provenio.core.playback.SUBTITLE_SHADOW_DISTANCE_RANGE
import io.github.dimitrysaf.provenio.core.playback.SUBTITLE_SHADOW_SOFTNESS_RANGE
import io.github.dimitrysaf.provenio.core.playback.SubtitleFontFamily
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.screens.player.SubtitleFrame
import io.github.dimitrysaf.provenio.shell.screens.player.SubtitleShadowColorSwatches
import androidx.compose.ui.text.AnnotatedString
import io.github.dimitrysaf.provenio.core.playback.subtitleFontSizeRangeSp
import io.github.dimitrysaf.provenio.shell.screens.player.SubtitleBackgroundColorSwatches
import io.github.dimitrysaf.provenio.shell.screens.player.SubtitleColorSwatches
import io.github.dimitrysaf.provenio.shell.screens.player.languageLabelForCode
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

internal fun LazyListScope.subtitlesSettingsContent(isTablet: Boolean) {
    item {
        val settings = remember {
            PlayerSettingsRepository.ensureLoaded()
            PlayerSettingsRepository.uiState
        }.collectAsStateWithLifecycle().value
        Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 12.dp)) {
            SubtitlePreview(style = settings.drawnSubtitleStyle())
            SubtitleLanguageGroup(isTablet = isTablet, settings = settings)
            SubtitleTimingGroup(isTablet = isTablet, settings = settings)
            SubtitleRenderingGroup(
                isTablet = isTablet,
                settings = settings,
                androidPlaybackEngine = settings.androidPlaybackEngine,
                useLibass = settings.useLibass,
                libassRenderType = settings.libassRenderType,
            )
        }
    }
}

/** Whether add-on subtitles are matched to the speech in the video on their own. */
@Composable
private fun SubtitleTimingGroup(
    isTablet: Boolean,
    settings: PlayerSettingsUiState,
) {
    val title = stringResource(Res.string.settings_subtitles_auto_sync)
    val block = subtitleAutoSyncBlock(settings)
    var showBlock by remember { mutableStateOf(false) }
    SettingsSection(
        title = stringResource(Res.string.settings_subtitles_section_timing),
        isTablet = isTablet,
    ) {
        SettingsList {
            switchRow(
                title = title,
                description = stringResource(Res.string.settings_subtitles_auto_sync_description),
                checked = { settings.subtitleAutoSyncEnabled && block == null },
                enabled = block == null,
                onDisabledClick = { showBlock = true },
                onCheckedChange = PlayerSettingsRepository::setSubtitleAutoSyncEnabled,
            )
        }
    }
    if (showBlock && block != null) {
        PlayerFeatureBlockDialog(feature = title, block = block, onDismiss = { showBlock = false })
    }
}

/** The preferred subtitle language as the Player page's Subtitles row states it. */
@Composable
internal fun preferredSubtitleLanguageLabel(code: String): String = when (code) {
    SubtitleLanguageOption.NONE -> stringResource(Res.string.settings_playback_option_none)
    SubtitleLanguageOption.DEVICE -> stringResource(Res.string.settings_playback_option_device_language)
    SubtitleLanguageOption.FORCED -> stringResource(Res.string.settings_playback_option_forced)
    else -> languageLabelForCode(code)
}

/**
 * A frame of video with a line of subtitles on it, drawn the way the player draws them: the
 * same font, colours, outline, shadow, weight and background, at the size and height the player
 * would use.
 */
@Composable
private fun SubtitlePreview(style: SubtitleStyleState) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .widthIn(max = SubtitlePreviewMaxWidth)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(OuterCorner + 8.dp))
                .background(PreviewSceneBrush),
        ) {
            SubtitleFrame(
                text = AnnotatedString(stringResource(Res.string.settings_subtitles_preview_sample)),
                style = style,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Wide enough to read on a tablet without the frame dwarfing the settings under it. */
private val SubtitlePreviewMaxWidth: Dp = 640.dp

/**
 * A dusky scene in place of a real frame. Fixed colours, not the theme's: it stands for video,
 * which looks the same in either theme, and the subtitle colours are judged against it.
 */
private val PreviewSceneBrush = Brush.verticalGradient(
    listOf(Color(0xFF3A4A66), Color(0xFF6B5A6E), Color(0xFF2A2633), Color(0xFF121216)),
)

// Which subtitles the player picks, and how it treats them.
@Composable
private fun SubtitleLanguageGroup(
    isTablet: Boolean,
    settings: PlayerSettingsUiState,
) {
    var showPreferredSubtitleDialog by remember { mutableStateOf(false) }
    var showSecondarySubtitleDialog by remember { mutableStateOf(false) }
    val preferredSubtitleLanguage = settings.preferredSubtitleLanguage
    val secondaryPreferredSubtitleLanguage = settings.secondaryPreferredSubtitleLanguage
    // With an external player, the language pickers still apply when subtitles are forwarded to
    // it; the other options only ever apply to the internal player.
    val isExternalPlayer = settings.externalPlayerEnabled
    val subtitleLanguageEnabled = !isExternalPlayer || settings.externalPlayerForwardSubtitles
    val otherSubtitleOptionsEnabled = !isExternalPlayer
    SettingsSection(
        title = stringResource(Res.string.settings_subtitles_section_languages),
        isTablet = isTablet,
    ) {
        SettingsList {
            navigationRow(
                title = stringResource(Res.string.settings_playback_preferred_subtitle_language),
                description = preferredSubtitleLanguageLabel(preferredSubtitleLanguage),
                enabled = subtitleLanguageEnabled,
                onClick = { showPreferredSubtitleDialog = true },
            )
            navigationRow(
                title = stringResource(Res.string.settings_playback_secondary_subtitle_language),
                description = languageLabelForCode(secondaryPreferredSubtitleLanguage),
                enabled = subtitleLanguageEnabled,
                onClick = { showSecondarySubtitleDialog = true },
            )
            switchRow(
                title = stringResource(Res.string.settings_playback_subtitle_strip_sdh),
                description = stringResource(Res.string.settings_playback_subtitle_strip_sdh_description),
                checked = { settings.subtitleStyle.stripSdh },
                enabled = otherSubtitleOptionsEnabled,
                onCheckedChange = { enabled ->
                    PlayerSettingsRepository.setSubtitleStyle(
                        settings.subtitleStyle.copy(stripSdh = enabled),
                    )
                },
            )
            switchRow(
                title = stringResource(Res.string.settings_playback_subtitle_use_forced),
                description = stringResource(Res.string.settings_playback_subtitle_use_forced_description),
                checked = { settings.subtitleStyle.useForcedSubtitles },
                enabled = otherSubtitleOptionsEnabled,
                onCheckedChange = { enabled ->
                    PlayerSettingsRepository.setSubtitleStyle(
                        settings.subtitleStyle.copy(useForcedSubtitles = enabled),
                    )
                },
            )
            switchRow(
                title = stringResource(Res.string.settings_playback_subtitle_show_preferred_only),
                description = stringResource(Res.string.settings_playback_subtitle_show_preferred_only_description),
                checked = { settings.subtitleStyle.showOnlyPreferredLanguages },
                enabled = otherSubtitleOptionsEnabled,
                onCheckedChange = { enabled ->
                    PlayerSettingsRepository.setSubtitleStyle(
                        settings.subtitleStyle.copy(showOnlyPreferredLanguages = enabled),
                    )
                },
            )
        }
    }

    if (showPreferredSubtitleDialog) {
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_playback_preferred_subtitle_language),
            options = listOf(
                LanguageSelectionOption(SubtitleLanguageOption.NONE, stringResource(Res.string.settings_playback_option_none)),
                LanguageSelectionOption(SubtitleLanguageOption.DEVICE, stringResource(Res.string.settings_playback_option_device_language)),
                LanguageSelectionOption(SubtitleLanguageOption.FORCED, stringResource(Res.string.settings_playback_option_forced)),
            ) + AvailableLanguageOptions.map { option ->
                LanguageSelectionOption(option.code, stringResource(option.labelRes))
            },
            selectedValue = preferredSubtitleLanguage,
            onSelect = { value ->
                PlayerSettingsRepository.setPreferredSubtitleLanguage(value ?: SubtitleLanguageOption.NONE)
            },
            onDismiss = { showPreferredSubtitleDialog = false },
        )
    }

    if (showSecondarySubtitleDialog) {
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_playback_secondary_subtitle_language),
            options = listOf(
                LanguageSelectionOption(null, stringResource(Res.string.settings_playback_option_none)),
                LanguageSelectionOption(SubtitleLanguageOption.FORCED, stringResource(Res.string.settings_playback_option_forced)),
            ) + AvailableLanguageOptions.map { option ->
                LanguageSelectionOption(option.code, stringResource(option.labelRes))
            },
            selectedValue = secondaryPreferredSubtitleLanguage,
            onSelect = { value ->
                PlayerSettingsRepository.setSecondaryPreferredSubtitleLanguage(value)
            },
            onDismiss = { showSecondarySubtitleDialog = false },
        )
    }
}

// How the internal player draws subtitles; the preview above reflects every change.
@Composable
private fun SubtitleRenderingGroup(
    isTablet: Boolean,
    settings: PlayerSettingsUiState,
    androidPlaybackEngine: AndroidPlaybackEngine,
    useLibass: Boolean,
    libassRenderType: String,
) {
    var showSubtitleTextColorDialog by remember { mutableStateOf(false) }
    var showSubtitleBackgroundColorDialog by remember { mutableStateOf(false) }
    var showSubtitleOutlineColorDialog by remember { mutableStateOf(false) }
    var showSubtitleFontSheet by remember { mutableStateOf(false) }
    var showSubtitleShadowColorDialog by remember { mutableStateOf(false) }
    var blockedFeature by remember { mutableStateOf<Pair<String, PlayerFeatureBlock>?>(null) }
    var showLibassRenderTypeDialog by remember { mutableStateOf(false) }
    val fontTitle = stringResource(Res.string.settings_subtitles_font)
    val shadowTitle = stringResource(Res.string.settings_subtitles_shadow)
    val shadowDirectionTitle = stringResource(Res.string.settings_subtitles_shadow_direction)
    val shadowSoftnessTitle = stringResource(Res.string.settings_subtitles_shadow_softness)
    val fontBlock = subtitleFontBlock(settings)
    val shadowBlock = subtitleShadowBlock(settings)
    val shadowAngleBlock = subtitleShadowAngleBlock(settings)
    val shadowSoftnessBlock = subtitleShadowSoftnessBlock(settings)
    SettingsSection(
        title = stringResource(Res.string.settings_subtitles_section_style),
        isTablet = isTablet,
    ) {
        val subtitleRenderingEnabled = !settings.externalPlayerEnabled
        SettingsList {
            val subtitleStyle = settings.subtitleStyle
            SettingsSliderRow(
                title = stringResource(Res.string.settings_playback_subtitle_size),
                value = subtitleStyle.fontSizeSp,
                valueText = { stringResource(Res.string.compose_player_font_size_value, it) },
                valueRange = subtitleFontSizeRangeSp,
                step = 2,
                isTablet = isTablet,
                enabled = subtitleRenderingEnabled,
                onValueChange = { value ->
                    PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(fontSizeSp = value))
                },
            )
            SettingsSliderRow(
                title = stringResource(Res.string.settings_playback_subtitle_vertical_offset),
                value = subtitleStyle.bottomOffset,
                valueText = { it.toString() },
                valueRange = 0..200,
                step = 5,
                isTablet = isTablet,
                enabled = subtitleRenderingEnabled,
                onValueChange = { value ->
                    PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(bottomOffset = value))
                },
            )
            switchRow(
                title = stringResource(Res.string.settings_playback_subtitle_bold),
                description = stringResource(Res.string.settings_playback_subtitle_bold_description),
                checked = { subtitleStyle.bold },
                enabled = subtitleRenderingEnabled,
                onCheckedChange = { enabled ->
                    PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(bold = enabled))
                },
            )
            navigationRow(
                title = fontTitle,
                description = subtitleFontLabel(
                    if (fontBlock == null) subtitleStyle.fontFamily else SubtitleFontFamily.Default,
                ),
                enabled = subtitleRenderingEnabled,
                onClick = {
                    if (fontBlock == null) showSubtitleFontSheet = true else blockedFeature = fontTitle to fontBlock
                },
            )
            navigationRow(
                title = stringResource(Res.string.settings_playback_subtitle_text_color),
                description = subtitleColorLabel(subtitleStyle.textColor),
                enabled = subtitleRenderingEnabled,
                onClick = { showSubtitleTextColorDialog = true },
            )
            navigationRow(
                title = stringResource(Res.string.settings_playback_subtitle_background_color),
                description = subtitleColorLabel(subtitleStyle.backgroundColor),
                enabled = subtitleRenderingEnabled,
                onClick = { showSubtitleBackgroundColorDialog = true },
            )
            switchRow(
                title = stringResource(Res.string.settings_playback_subtitle_outline),
                description = stringResource(Res.string.settings_playback_subtitle_outline_description),
                checked = { subtitleStyle.outlineEnabled },
                enabled = subtitleRenderingEnabled,
                onCheckedChange = { enabled ->
                    PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(outlineEnabled = enabled))
                },
            )
            if (subtitleStyle.outlineEnabled) {
                navigationRow(
                    title = stringResource(Res.string.settings_playback_subtitle_outline_color),
                    description = subtitleColorLabel(subtitleStyle.outlineColor),
                    enabled = subtitleRenderingEnabled,
                    onClick = { showSubtitleOutlineColorDialog = true },
                )
            }
            switchRow(
                title = shadowTitle,
                description = stringResource(Res.string.settings_subtitles_shadow_description),
                checked = { subtitleStyle.shadowEnabled && shadowBlock == null },
                enabled = shadowBlock == null,
                onDisabledClick = { shadowBlock?.let { blockedFeature = shadowTitle to it } },
                onCheckedChange = { enabled ->
                    PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(shadowEnabled = enabled))
                },
            )
            expandableRows(expanded = subtitleStyle.shadowEnabled && shadowBlock == null) {
                navigationRow(
                    title = stringResource(Res.string.settings_subtitles_shadow_color),
                    description = subtitleColorLabel(subtitleStyle.shadowColor),
                    onClick = { showSubtitleShadowColorDialog = true },
                )
                SettingsSliderRow(
                    title = stringResource(Res.string.settings_subtitles_shadow_distance),
                    value = subtitleStyle.shadowDistance,
                    valueText = { it.toString() },
                    valueRange = SUBTITLE_SHADOW_DISTANCE_RANGE,
                    step = 1,
                    isTablet = isTablet,
                    onValueChange = { value ->
                        PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(shadowDistance = value))
                    },
                )
                ShadowDirectionRow(
                    title = shadowDirectionTitle,
                    degrees = if (shadowAngleBlock == null) subtitleStyle.shadowAngle else SubtitleStyleState.DEFAULT.shadowAngle,
                    isTablet = isTablet,
                    enabled = shadowAngleBlock == null,
                    onDisabledClick = { shadowAngleBlock?.let { blockedFeature = shadowDirectionTitle to it } },
                    onDegreesChange = { degrees ->
                        PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(shadowAngle = degrees))
                    },
                )
                val sharpLabel = stringResource(Res.string.settings_subtitles_shadow_softness_sharp)
                SettingsSliderRow(
                    title = shadowSoftnessTitle,
                    value = if (shadowSoftnessBlock == null) subtitleStyle.shadowSoftness else 0,
                    valueText = { if (it == 0) sharpLabel else it.toString() },
                    valueRange = SUBTITLE_SHADOW_SOFTNESS_RANGE,
                    step = 1,
                    isTablet = isTablet,
                    enabled = shadowSoftnessBlock == null,
                    onDisabledClick = { shadowSoftnessBlock?.let { blockedFeature = shadowSoftnessTitle to it } },
                    onValueChange = { value ->
                        PlayerSettingsRepository.setSubtitleStyle(subtitleStyle.copy(shadowSoftness = value))
                    },
                )
            }
            val showLibassSettings = !isIos && androidPlaybackEngine != AndroidPlaybackEngine.Libmpv
            if (showLibassSettings) {
                switchRow(
                    title = stringResource(Res.string.settings_playback_enable_libass),
                    description = stringResource(Res.string.settings_playback_enable_libass_description),
                    checked = { useLibass },
                    enabled = subtitleRenderingEnabled,
                    onCheckedChange = PlayerSettingsRepository::setUseLibass,
                )
                if (useLibass) {
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_render_type),
                        description = libassRenderTypeLabel(libassRenderType),
                        enabled = subtitleRenderingEnabled,
                        onClick = { showLibassRenderTypeDialog = true },
                    )
                }
            }
        }
    }

    if (showSubtitleTextColorDialog) {
        SubtitleColorDialog(
            title = stringResource(Res.string.settings_playback_subtitle_text_color),
            colors = SubtitleColorSwatches,
            selectedColor = settings.subtitleStyle.textColor,
            onColorSelected = { color ->
                PlayerSettingsRepository.setSubtitleStyle(settings.subtitleStyle.copy(textColor = color))
            },
            onDismiss = { showSubtitleTextColorDialog = false },
        )
    }

    if (showSubtitleBackgroundColorDialog) {
        SubtitleColorDialog(
            title = stringResource(Res.string.settings_playback_subtitle_background_color),
            colors = SubtitleBackgroundColorSwatches,
            selectedColor = settings.subtitleStyle.backgroundColor,
            onColorSelected = { color ->
                PlayerSettingsRepository.setSubtitleStyle(settings.subtitleStyle.copy(backgroundColor = color))
            },
            onDismiss = { showSubtitleBackgroundColorDialog = false },
        )
    }

    if (showSubtitleOutlineColorDialog) {
        SubtitleColorDialog(
            title = stringResource(Res.string.settings_playback_subtitle_outline_color),
            colors = SubtitleColorSwatches,
            selectedColor = settings.subtitleStyle.outlineColor,
            onColorSelected = { color ->
                PlayerSettingsRepository.setSubtitleStyle(settings.subtitleStyle.copy(outlineColor = color))
            },
            onDismiss = { showSubtitleOutlineColorDialog = false },
        )
    }

    if (showSubtitleFontSheet) {
        SingleChoiceBottomSheet(
            title = fontTitle,
            options = SubtitleFontFamily.entries.map { family ->
                SingleChoiceOption(
                    value = family,
                    label = subtitleFontLabel(family),
                    leadingContent = {
                        Text(
                            text = stringResource(Res.string.settings_subtitles_font_sample),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = subtitleTextFontFamily(family),
                        )
                    },
                )
            },
            isSelected = { it == settings.subtitleStyle.fontFamily },
            onSelected = { family ->
                PlayerSettingsRepository.setSubtitleStyle(settings.subtitleStyle.copy(fontFamily = family))
            },
            onDismiss = { showSubtitleFontSheet = false },
        )
    }

    if (showSubtitleShadowColorDialog) {
        SubtitleColorDialog(
            title = stringResource(Res.string.settings_subtitles_shadow_color),
            colors = SubtitleShadowColorSwatches,
            selectedColor = settings.subtitleStyle.shadowColor,
            onColorSelected = { color ->
                PlayerSettingsRepository.setSubtitleStyle(settings.subtitleStyle.copy(shadowColor = color))
            },
            onDismiss = { showSubtitleShadowColorDialog = false },
        )
    }

    blockedFeature?.let { (feature, block) ->
        PlayerFeatureBlockDialog(feature = feature, block = block, onDismiss = { blockedFeature = null })
    }

    if (showLibassRenderTypeDialog) {
        LibassRenderTypeDialog(
            selectedRenderType = libassRenderType,
            onRenderTypeSelected = { renderType ->
                PlayerSettingsRepository.setLibassRenderType(renderType)
            },
            onDismiss = { showLibassRenderTypeDialog = false },
        )
    }
}

@Composable
private fun subtitleFontLabel(family: SubtitleFontFamily): String = stringResource(
    when (family) {
        SubtitleFontFamily.Default -> Res.string.settings_subtitles_font_default
        SubtitleFontFamily.Serif -> Res.string.settings_subtitles_font_serif
        SubtitleFontFamily.Monospace -> Res.string.settings_subtitles_font_monospace
        SubtitleFontFamily.Condensed -> Res.string.settings_subtitles_font_condensed
    },
)
