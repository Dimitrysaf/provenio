package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.build.isIos
import io.github.dimitrysaf.provenio.core.playback.AndroidPlaybackEngine
import io.github.dimitrysaf.provenio.core.playback.AvailableLanguageOptions
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import io.github.dimitrysaf.provenio.core.playback.SubtitleLanguageOption
import io.github.dimitrysaf.provenio.core.playback.SubtitleStyleState
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
            SubtitlePreview(style = settings.subtitleStyle)
            SubtitleLanguageGroup(isTablet = isTablet, settings = settings)
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
 * same colours, outline, weight and background, at the size and height the player would use.
 *
 * The player sets text at a fixed size in sp over a full-screen landscape frame, so the preview
 * scales that size by how much smaller its frame is than [PlayerReferenceHeight]. The line's
 * height follows ExoPlayer's: a base bottom padding of two-thirds of its default, plus the offset
 * in thousandths of the frame.
 */
@Composable
private fun SubtitlePreview(style: SubtitleStyleState) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = SubtitlePreviewMaxWidth)
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(OuterCorner + 8.dp))
                .background(PreviewSceneBrush),
        ) {
            val scale = maxHeight / PlayerReferenceHeight
            val bottomPadding = maxHeight * (
                SubtitleBaseBottomFraction + (style.bottomOffset / 1000f).coerceIn(0f, 0.2f)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = bottomPadding),
            ) {
                PreviewSubtitleText(style = style, fontScale = scale)
            }
        }
    }
}

@Composable
private fun PreviewSubtitleText(style: SubtitleStyleState, fontScale: Float) {
    val text = stringResource(Res.string.settings_subtitles_preview_sample)
    val fontSize = (style.fontSizeSp * fontScale).sp
    val baseStyle = TextStyle(
        fontSize = fontSize,
        lineHeight = fontSize * 1.2f,
        fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
    val background = Color(style.backgroundColor)
    Box(
        modifier = Modifier
            .background(background)
            .padding(horizontal = 4.dp),
    ) {
        if (style.outlineEnabled) {
            // The outline is the text stroked behind the fill, the way an outline edge is drawn.
            val strokeWidth = with(LocalDensity.current) { fontSize.toPx() } * OutlineFraction
            Text(
                text = text,
                style = baseStyle.copy(
                    color = Color(style.outlineColor),
                    drawStyle = Stroke(width = strokeWidth, join = StrokeJoin.Round),
                ),
            )
        }
        Text(text = text, style = baseStyle.copy(color = Color(style.textColor)))
    }
}

/** A full-screen landscape frame on a phone is about this tall. */
private val PlayerReferenceHeight: Dp = 412.dp

/** Wide enough to read on a tablet without the frame dwarfing the settings under it. */
private val SubtitlePreviewMaxWidth: Dp = 640.dp

/** `SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION` (0.08) times two-thirds, as the player sets it. */
private const val SubtitleBaseBottomFraction = 0.08f * 2f / 3f

/** How thick an outline edge is relative to the text. */
private const val OutlineFraction = 0.12f

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
    var showLibassRenderTypeDialog by remember { mutableStateOf(false) }
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
                valueText = stringResource(Res.string.compose_player_font_size_value, subtitleStyle.fontSizeSp),
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
                valueText = subtitleStyle.bottomOffset.toString(),
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
