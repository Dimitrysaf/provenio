package com.nuvio.app.shell.screens.settings

import com.nuvio.app.core.build.AppFeaturePolicy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import com.nuvio.app.shell.components.MultiChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.playback.AndroidLibmpvVideoOutput
import com.nuvio.app.core.playback.AndroidPlaybackEngine
import com.nuvio.app.core.playback.AudioLanguageOption
import com.nuvio.app.core.playback.AvailableLanguageOptions
import com.nuvio.app.core.playback.ExternalPlayerPlatform
import com.nuvio.app.core.playback.IosAudioOutputMode
import com.nuvio.app.core.playback.IosHardwareDecoderMode
import com.nuvio.app.shell.screens.player.localizedLabel
import com.nuvio.app.core.playback.IosTargetPrimaries
import com.nuvio.app.core.playback.IosTargetTransfer
import com.nuvio.app.core.playback.PlayerSettingsRepository
import com.nuvio.app.core.playback.STREAM_AUTO_PLAY_TIMEOUT_VALUES
import com.nuvio.app.shell.screens.player.SubtitleBackgroundColorSwatches
import com.nuvio.app.shell.screens.player.SubtitleColorSwatches
import com.nuvio.app.core.playback.SubtitleLanguageOption
import com.nuvio.app.shell.screens.player.formatPlaybackSpeedLabel
import com.nuvio.app.shell.screens.player.languageLabelForCode
import com.nuvio.app.core.playback.subtitleFontSizeRangeSp
import com.nuvio.app.shell.screens.player.isTransparentArgb
import com.nuvio.app.core.playback.toStorageHexString
import com.nuvio.app.shell.screens.p2p.P2pConsentDialog
import com.nuvio.app.core.p2p.P2pCacheClearResult
import com.nuvio.app.core.p2p.P2pCacheSize
import com.nuvio.app.core.p2p.P2pSettingsRepository
import com.nuvio.app.core.p2p.P2pStreamingEngine
import com.nuvio.app.core.p2p.P2pStreamingState
import com.nuvio.app.core.p2p.P2pTorrentProfile
import com.nuvio.app.core.plugins.PluginsUiState
import com.nuvio.app.core.plugins.PluginRepository
import com.nuvio.app.core.streams.StreamAutoPlayMode
import com.nuvio.app.core.streams.StreamAutoPlaySource
import com.nuvio.app.isIos
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.playbackSettingsContent(
    isTablet: Boolean,
    showLoadingOverlay: Boolean,
    holdToSpeedEnabled: Boolean,
    holdToSpeedValue: Float,
    touchGesturesEnabled: Boolean,
    preferredAudioLanguage: String,
    secondaryPreferredAudioLanguage: String?,
    preferredSubtitleLanguage: String,
    secondaryPreferredSubtitleLanguage: String?,
    streamReuseLastLinkEnabled: Boolean,
    streamReuseLastLinkCacheHours: Int,
    androidPlaybackEngine: AndroidPlaybackEngine,
    androidLibmpvVideoOutput: AndroidLibmpvVideoOutput,
    androidLibmpvHardwareDecodingEnabled: Boolean,
    androidLibmpvYuv420pEnabled: Boolean,
    decoderPriority: Int,
    mapDV7ToHevc: Boolean,
    tunnelingEnabled: Boolean,
    useLibass: Boolean,
    libassRenderType: String,
) {
    item {
        PlaybackSettingsSection(
            isTablet = isTablet,
            showLoadingOverlay = showLoadingOverlay,
            holdToSpeedEnabled = holdToSpeedEnabled,
            holdToSpeedValue = holdToSpeedValue,
            touchGesturesEnabled = touchGesturesEnabled,
            preferredAudioLanguage = preferredAudioLanguage,
            secondaryPreferredAudioLanguage = secondaryPreferredAudioLanguage,
            preferredSubtitleLanguage = preferredSubtitleLanguage,
            secondaryPreferredSubtitleLanguage = secondaryPreferredSubtitleLanguage,
            streamReuseLastLinkEnabled = streamReuseLastLinkEnabled,
            streamReuseLastLinkCacheHours = streamReuseLastLinkCacheHours,
            androidPlaybackEngine = androidPlaybackEngine,
            androidLibmpvVideoOutput = androidLibmpvVideoOutput,
            androidLibmpvHardwareDecodingEnabled = androidLibmpvHardwareDecodingEnabled,
            androidLibmpvYuv420pEnabled = androidLibmpvYuv420pEnabled,
            decoderPriority = decoderPriority,
            mapDV7ToHevc = mapDV7ToHevc,
            tunnelingEnabled = tunnelingEnabled,
            useLibass = useLibass,
            libassRenderType = libassRenderType,
        )
    }
}

@Composable
private fun p2pProfileLabel(profile: P2pTorrentProfile): String = when (profile) {
    P2pTorrentProfile.SOFT -> stringResource(Res.string.settings_p2p_profile_soft)
    P2pTorrentProfile.BALANCED -> stringResource(Res.string.settings_p2p_profile_balanced)
    P2pTorrentProfile.FAST -> stringResource(Res.string.settings_p2p_profile_fast)
}

@Composable
private fun p2pCacheSizeLabel(size: P2pCacheSize): String = when (size) {
    P2pCacheSize.NONE -> stringResource(Res.string.settings_p2p_cache_none)
    P2pCacheSize.GB_2 -> stringResource(Res.string.settings_p2p_cache_2_gb)
    P2pCacheSize.GB_5 -> stringResource(Res.string.settings_p2p_cache_5_gb)
    P2pCacheSize.GB_10 -> stringResource(Res.string.settings_p2p_cache_10_gb)
}

private fun formatP2pCacheBytes(bytes: Long): String {
    val gibibyte = 1024.0 * 1024.0 * 1024.0
    val mebibyte = 1024.0 * 1024.0
    return if (bytes >= gibibyte) {
        "${kotlin.math.round(bytes / gibibyte * 10.0) / 10.0} GB"
    } else {
        "${kotlin.math.round(bytes / mebibyte * 10.0) / 10.0} MB"
    }
}

@Composable
internal fun subtitleColorLabel(color: Long): String {
    return if (color.isTransparentArgb()) {
        stringResource(Res.string.settings_playback_subtitle_color_transparent)
    } else {
        color.toStorageHexString()
    }
}

@Composable
private fun PlaybackSettingsSection(
    isTablet: Boolean,
    showLoadingOverlay: Boolean,
    holdToSpeedEnabled: Boolean,
    holdToSpeedValue: Float,
    touchGesturesEnabled: Boolean,
    preferredAudioLanguage: String,
    secondaryPreferredAudioLanguage: String?,
    preferredSubtitleLanguage: String,
    secondaryPreferredSubtitleLanguage: String?,
    streamReuseLastLinkEnabled: Boolean,
    streamReuseLastLinkCacheHours: Int,
    androidPlaybackEngine: AndroidPlaybackEngine,
    androidLibmpvVideoOutput: AndroidLibmpvVideoOutput,
    androidLibmpvHardwareDecodingEnabled: Boolean,
    androidLibmpvYuv420pEnabled: Boolean,
    decoderPriority: Int,
    mapDV7ToHevc: Boolean,
    tunnelingEnabled: Boolean,
    useLibass: Boolean,
    libassRenderType: String,
) {
    var showPreferredAudioDialog by remember { mutableStateOf(false) }
    var showSecondaryAudioDialog by remember { mutableStateOf(false) }
    var showPreferredSubtitleDialog by remember { mutableStateOf(false) }
    var showSecondarySubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleTextColorDialog by remember { mutableStateOf(false) }
    var showSubtitleBackgroundColorDialog by remember { mutableStateOf(false) }
    var showSubtitleOutlineColorDialog by remember { mutableStateOf(false) }
    var showExternalPlayerDialog by remember { mutableStateOf(false) }
    var showExternalPlayerAppDialog by remember { mutableStateOf(false) }
    var showReuseCacheDurationDialog by remember { mutableStateOf(false) }
    var showPlaybackEngineDialog by remember { mutableStateOf(false) }
    var showLibmpvVideoOutputDialog by remember { mutableStateOf(false) }
    var showDecoderPriorityDialog by remember { mutableStateOf(false) }
    var showHoldToSpeedValueDialog by remember { mutableStateOf(false) }
    var showIosAudioOutputDialog by remember { mutableStateOf(false) }
    var showIosHardwareDecoderDialog by remember { mutableStateOf(false) }
    var showIosTargetPrimariesDialog by remember { mutableStateOf(false) }
    var showIosTargetTransferDialog by remember { mutableStateOf(false) }
    var showLibassRenderTypeDialog by remember { mutableStateOf(false) }
    var showAutoPlayModeDialog by remember { mutableStateOf(false) }
    var showAutoPlaySourceDialog by remember { mutableStateOf(false) }
    var showAutoPlayAddonSelectionDialog by remember { mutableStateOf(false) }
    var showAutoPlayPluginSelectionDialog by remember { mutableStateOf(false) }
    var showAutoPlayRegexDialog by remember { mutableStateOf(false) }
    var showP2pConsentDialog by remember { mutableStateOf(false) }
    var showP2pProfileDialog by remember { mutableStateOf(false) }
    var showP2pCacheSizeDialog by remember { mutableStateOf(false) }
    var p2pCacheClearResult by remember { mutableStateOf<P2pCacheClearResult?>(null) }
    var p2pCacheClearFailed by remember { mutableStateOf(false) }
    val pluginsEnabled = AppFeaturePolicy.pluginsEnabled
    val autoPlayPlayerSettings by PlayerSettingsRepository.uiState.collectAsStateWithLifecycle()
    val p2pSettings by remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val p2pCacheState by P2pStreamingEngine.cacheState.collectAsStateWithLifecycle()
    val p2pStreamingState by P2pStreamingEngine.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val availableExternalPlayers = ExternalPlayerPlatform.availablePlayers()
    val selectedExternalPlayer = availableExternalPlayers.firstOrNull {
        it.id == autoPlayPlayerSettings.externalPlayerId
    }
    val addonUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val pluginUiState = if (pluginsEnabled) {
        val state by PluginRepository.uiState.collectAsStateWithLifecycle()
        state
    } else {
        PluginsUiState(pluginsEnabled = false)
    }
    val hapticFeedback = LocalHapticFeedback.current
    val sectionSpacing = if (isTablet) 18.dp else 12.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
    ) {
        SettingsSection(
            title = stringResource(Res.string.settings_playback_section_player),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_playback_show_loading_overlay),
                    description = stringResource(Res.string.settings_playback_show_loading_overlay_description),
                    checked = { showLoadingOverlay },
                    onCheckedChange = PlayerSettingsRepository::setShowLoadingOverlay,
                )
                switchRow(
                    title = stringResource(Res.string.playback_show_loading_status),
                    description = stringResource(Res.string.playback_show_loading_status_sub),
                    checked = { autoPlayPlayerSettings.showPlayerLoadingStatus },
                    onCheckedChange = PlayerSettingsRepository::setShowPlayerLoadingStatus,
                )
                switchRow(
                    title = stringResource(Res.string.settings_playback_pause_overlay),
                    description = stringResource(Res.string.settings_playback_pause_overlay_description),
                    checked = { autoPlayPlayerSettings.pauseOverlayEnabled },
                    onCheckedChange = PlayerSettingsRepository::setPauseOverlayEnabled,
                )
                switchRow(
                    title = stringResource(Res.string.settings_playback_parental_guide),
                    description = stringResource(Res.string.settings_playback_parental_guide_description),
                    checked = { autoPlayPlayerSettings.showParentalGuide },
                    onCheckedChange = PlayerSettingsRepository::setShowParentalGuide,
                )
                // Player preference picker: Internal / External
                navigationRow(
                    title = stringResource(Res.string.settings_playback_player_preference),
                    description = if (autoPlayPlayerSettings.externalPlayerEnabled) {
                        stringResource(Res.string.settings_playback_player_preference_external)
                    } else {
                        stringResource(Res.string.settings_playback_player_preference_internal)
                    },
                    onClick = { showExternalPlayerDialog = true },
                )
                if (isIos && autoPlayPlayerSettings.externalPlayerEnabled) {
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_external_player_app),
                        description = selectedExternalPlayer?.name
                            ?: if (availableExternalPlayers.isEmpty()) {
                                stringResource(Res.string.settings_playback_external_player_none_available)
                            } else {
                                stringResource(Res.string.settings_playback_not_set)
                            },
                        onClick = { showExternalPlayerAppDialog = true },
                    )
                }
                if (!isIos && autoPlayPlayerSettings.externalPlayerEnabled) {
                    switchRow(
                        title = stringResource(Res.string.settings_playback_external_player_forward_subtitles),
                        description = stringResource(Res.string.settings_playback_external_player_forward_subtitles_description),
                        checked = { autoPlayPlayerSettings.externalPlayerForwardSubtitles },
                        onCheckedChange = PlayerSettingsRepository::setExternalPlayerForwardSubtitles,
                    )
                    switchRow(
                        title = stringResource(Res.string.settings_playback_external_player_send_skip_segments),
                        description = stringResource(Res.string.settings_playback_external_player_send_skip_segments_description),
                        checked = { autoPlayPlayerSettings.externalPlayerSendSkipSegments },
                        onCheckedChange = PlayerSettingsRepository::setExternalPlayerSendSkipSegments,
                    )
                }
                switchRow(
                    title = stringResource(Res.string.settings_playback_touch_gestures),
                    description = stringResource(Res.string.settings_playback_touch_gestures_description),
                    checked = { touchGesturesEnabled },
                    enabled = !autoPlayPlayerSettings.externalPlayerEnabled,
                    onCheckedChange = PlayerSettingsRepository::setTouchGesturesEnabled,
                )
                switchRow(
                    title = stringResource(Res.string.settings_playback_hold_to_speed),
                    description = stringResource(Res.string.settings_playback_hold_to_speed_description),
                    checked = { holdToSpeedEnabled },
                    enabled = !autoPlayPlayerSettings.externalPlayerEnabled,
                    onCheckedChange = PlayerSettingsRepository::setHoldToSpeedEnabled,
                )
                if (holdToSpeedEnabled && !autoPlayPlayerSettings.externalPlayerEnabled) {
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_hold_speed),
                        description = formatPlaybackSpeedLabel(holdToSpeedValue),
                        onClick = { showHoldToSpeedValueDialog = true },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.settings_playback_section_subtitle_audio),
            isTablet = isTablet,
        ) {
            // Subtitle/Audio settings enable/disable logic:
            // Internal: everything enabled
            // External + forwarding enabled: subtitle language pickers enabled, other subtitle options disabled
            // External + forwarding disabled: entire subtitle section disabled
            // External: audio language pickers always disabled (external player manages audio tracks)
            val isExternalPlayer = autoPlayPlayerSettings.externalPlayerEnabled
            val isForwardingSubtitles = autoPlayPlayerSettings.externalPlayerForwardSubtitles
            val audioLanguageEnabled = !isExternalPlayer
            val subtitleLanguageEnabled = !isExternalPlayer || isForwardingSubtitles
            val otherSubtitleOptionsEnabled = !isExternalPlayer

            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.settings_playback_preferred_audio_language),
                    description = when (preferredAudioLanguage) {
                        AudioLanguageOption.DEFAULT -> stringResource(Res.string.settings_playback_option_default)
                        AudioLanguageOption.DEVICE -> stringResource(Res.string.settings_playback_option_device_language)
                        AudioLanguageOption.ORIGINAL -> stringResource(Res.string.settings_playback_option_original)
                        else -> languageLabelForCode(preferredAudioLanguage)
                    },
                    enabled = audioLanguageEnabled,
                    onClick = { showPreferredAudioDialog = true },
                )
                navigationRow(
                    title = stringResource(Res.string.settings_playback_secondary_audio_language),
                    description = languageLabelForCode(secondaryPreferredAudioLanguage),
                    enabled = audioLanguageEnabled,
                    onClick = { showSecondaryAudioDialog = true },
                )
                navigationRow(
                    title = stringResource(Res.string.settings_playback_preferred_subtitle_language),
                    description = when (preferredSubtitleLanguage) {
                        SubtitleLanguageOption.NONE -> stringResource(Res.string.settings_playback_option_none)
                        SubtitleLanguageOption.DEVICE -> stringResource(Res.string.settings_playback_option_device_language)
                        SubtitleLanguageOption.FORCED -> stringResource(Res.string.settings_playback_option_forced)
                        else -> languageLabelForCode(preferredSubtitleLanguage)
                    },
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
                    checked = { autoPlayPlayerSettings.subtitleStyle.stripSdh },
                    enabled = otherSubtitleOptionsEnabled,
                    onCheckedChange = { enabled ->
                        PlayerSettingsRepository.setSubtitleStyle(
                            autoPlayPlayerSettings.subtitleStyle.copy(stripSdh = enabled),
                        )
                    },
                )
                switchRow(
                    title = stringResource(Res.string.settings_playback_subtitle_use_forced),
                    description = stringResource(Res.string.settings_playback_subtitle_use_forced_description),
                    checked = { autoPlayPlayerSettings.subtitleStyle.useForcedSubtitles },
                    enabled = otherSubtitleOptionsEnabled,
                    onCheckedChange = { enabled ->
                        PlayerSettingsRepository.setSubtitleStyle(
                            autoPlayPlayerSettings.subtitleStyle.copy(useForcedSubtitles = enabled),
                        )
                    },
                )
                switchRow(
                    title = stringResource(Res.string.settings_playback_subtitle_show_preferred_only),
                    description = stringResource(Res.string.settings_playback_subtitle_show_preferred_only_description),
                    checked = { autoPlayPlayerSettings.subtitleStyle.showOnlyPreferredLanguages },
                    enabled = otherSubtitleOptionsEnabled,
                    onCheckedChange = { enabled ->
                        PlayerSettingsRepository.setSubtitleStyle(
                            autoPlayPlayerSettings.subtitleStyle.copy(showOnlyPreferredLanguages = enabled),
                        )
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.settings_playback_section_subtitle_rendering),
            isTablet = isTablet,
        ) {
            val subtitleRenderingEnabled = !autoPlayPlayerSettings.externalPlayerEnabled
            SettingsList {
                val subtitleStyle = autoPlayPlayerSettings.subtitleStyle
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

        if (P2pSettingsRepository.isVisible) {
            SettingsSection(
                title = stringResource(Res.string.settings_playback_section_p2p),
                isTablet = isTablet,
            ) {
                SettingsList {
                    switchRow(
                        title = stringResource(Res.string.settings_p2p_title),
                        description = stringResource(Res.string.settings_p2p_subtitle),
                        checked = { p2pSettings.p2pEnabled },
                        onCheckedChange = { enabled ->
                            if (enabled && !p2pSettings.p2pEnabled) {
                                showP2pConsentDialog = true
                            } else {
                                P2pSettingsRepository.setP2pEnabled(enabled)
                            }
                        },
                    )
                    switchRow(
                        title = stringResource(Res.string.settings_p2p_hide_stats_title),
                        description = stringResource(Res.string.settings_p2p_hide_stats_subtitle),
                        checked = { p2pSettings.hideTorrentStats },
                        onCheckedChange = P2pSettingsRepository::setHideTorrentStats,
                    )
                    navigationRow(
                        title = stringResource(Res.string.settings_p2p_profile_title),
                        description = p2pProfileLabel(p2pSettings.torrentProfile),
                        onClick = { showP2pProfileDialog = true },
                    )
                    navigationRow(
                        title = stringResource(Res.string.settings_p2p_cache_size_title),
                        description = p2pCacheSizeLabel(p2pSettings.cacheSize),
                        onClick = { showP2pCacheSizeDialog = true },
                    )
                    val cacheClearAvailable = p2pStreamingState !is P2pStreamingState.Connecting &&
                        p2pStreamingState !is P2pStreamingState.Streaming &&
                        !p2pCacheState.isClearing
                    navigationRow(
                        title = stringResource(Res.string.settings_p2p_clear_cache_title),
                        description = when {
                            p2pCacheState.isClearing ->
                                stringResource(Res.string.settings_p2p_clear_cache_clearing)
                            !cacheClearAvailable ->
                                stringResource(Res.string.settings_p2p_clear_cache_playback_active)
                            p2pCacheClearFailed ->
                                stringResource(Res.string.settings_p2p_clear_cache_failed)
                            p2pCacheClearResult != null -> stringResource(
                                Res.string.settings_p2p_clear_cache_done,
                                formatP2pCacheBytes(p2pCacheClearResult!!.reclaimedBytes),
                            )
                            !p2pCacheState.hasMeasurement ->
                                stringResource(Res.string.settings_p2p_clear_cache_usage_pending)
                            else -> stringResource(
                                Res.string.settings_p2p_clear_cache_usage,
                                formatP2pCacheBytes(p2pCacheState.usedBytes),
                            )
                        },
                        enabled = cacheClearAvailable,
                        onClick = {
                            p2pCacheClearResult = null
                            p2pCacheClearFailed = false
                            coroutineScope.launch {
                                runCatching { P2pStreamingEngine.clearCache() }
                                    .onSuccess { p2pCacheClearResult = it }
                                    .onFailure { p2pCacheClearFailed = true }
                            }
                        },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.settings_playback_section_stream_selection),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_playback_reuse_last_link),
                    description = stringResource(Res.string.settings_playback_reuse_last_link_description),
                    checked = { streamReuseLastLinkEnabled },
                    onCheckedChange = PlayerSettingsRepository::setStreamReuseLastLinkEnabled,
                )
                if (streamReuseLastLinkEnabled) {
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_last_link_cache_duration),
                        description = formatReuseCacheDuration(streamReuseLastLinkCacheHours),
                        onClick = { showReuseCacheDurationDialog = true },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.settings_playback_section_stream_auto_play),
            isTablet = isTablet,
        ) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.settings_playback_stream_selection_mode),
                    description = stringResource(autoPlayPlayerSettings.streamAutoPlayMode.labelRes),
                    onClick = { showAutoPlayModeDialog = true },
                )
                if (autoPlayPlayerSettings.streamAutoPlayMode == StreamAutoPlayMode.REGEX_MATCH) {
                    val notSetLabel = stringResource(Res.string.settings_playback_not_set)
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_regex_pattern),
                        description = autoPlayPlayerSettings.streamAutoPlayRegex.ifBlank { notSetLabel },
                        onClick = { showAutoPlayRegexDialog = true },
                    )
                }
                val timeoutSec = autoPlayPlayerSettings.streamAutoPlayTimeoutSeconds
                val timeoutLabel = when (timeoutSec) {
                    0 -> stringResource(Res.string.settings_playback_timeout_instant)
                    Int.MAX_VALUE -> stringResource(Res.string.settings_playback_timeout_unlimited)
                    else -> stringResource(Res.string.settings_playback_timeout_seconds, timeoutSec)
                }
                shapedRow { shape ->
                SettingsSliderContainer(shape = shape, enabled = true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isTablet) 18.dp else 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_playback_stream_timeout),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.settings_playback_stream_timeout_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ValueBox(text = timeoutLabel, modifier = Modifier.wrapContentWidth())
                    }
                    val timeoutIndex = STREAM_AUTO_PLAY_TIMEOUT_VALUES.indexOf(timeoutSec)
                        .coerceAtLeast(0)
                    val maxIndex = (STREAM_AUTO_PLAY_TIMEOUT_VALUES.size - 1).toFloat()
                    var sliderValue by remember(timeoutIndex) { mutableFloatStateOf(timeoutIndex.toFloat()) }
                    var lastHapticStep by remember(timeoutIndex) { mutableStateOf(timeoutIndex.toFloat()) }
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            val snapped = snapToStep(it, 1f)
                            sliderValue = snapped

                            if (snapped != lastHapticStep) {
                                lastHapticStep = snapped
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onValueChangeFinished = {
                            val index = sliderValue.toInt().coerceIn(0, STREAM_AUTO_PLAY_TIMEOUT_VALUES.size - 1)
                            PlayerSettingsRepository.setStreamAutoPlayTimeoutSeconds(STREAM_AUTO_PLAY_TIMEOUT_VALUES[index])
                        },
                        valueRange = 0f..maxIndex,
                        steps = calculateSteps(0f, maxIndex, 1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                }
                }
                navigationRow(
                    title = stringResource(Res.string.settings_playback_source_scope),
                    description = stringResource(autoPlayPlayerSettings.streamAutoPlaySource.labelRes(pluginsEnabled)),
                    onClick = { showAutoPlaySourceDialog = true },
                )
                if (autoPlayPlayerSettings.streamAutoPlaySource != StreamAutoPlaySource.ENABLED_PLUGINS_ONLY) {
                    val addonSubtitle = if (autoPlayPlayerSettings.streamAutoPlaySelectedAddons.isEmpty()) {
                        stringResource(Res.string.settings_playback_all_addons)
                    } else {
                        stringResource(
                            Res.string.settings_playback_selected_count,
                            autoPlayPlayerSettings.streamAutoPlaySelectedAddons.size,
                        )
                    }
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_allowed_addons),
                        description = addonSubtitle,
                        onClick = { showAutoPlayAddonSelectionDialog = true },
                    )
                }
                if (pluginsEnabled && autoPlayPlayerSettings.streamAutoPlaySource != StreamAutoPlaySource.INSTALLED_ADDONS_ONLY) {
                    val pluginSubtitle = if (autoPlayPlayerSettings.streamAutoPlaySelectedPlugins.isEmpty()) {
                        stringResource(Res.string.settings_playback_all_plugins)
                    } else {
                        stringResource(
                            Res.string.settings_playback_selected_count,
                            autoPlayPlayerSettings.streamAutoPlaySelectedPlugins.size,
                        )
                    }
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_allowed_plugins),
                        description = pluginSubtitle,
                        onClick = { showAutoPlayPluginSelectionDialog = true },
                    )
                }
            }
        }

        if (!isIos) {
            val decoderEnabled = !autoPlayPlayerSettings.externalPlayerEnabled
            val exoOptionsEnabled = decoderEnabled && androidPlaybackEngine != AndroidPlaybackEngine.Libmpv
            val libmpvOptionsVisible = androidPlaybackEngine != AndroidPlaybackEngine.ExoPlayer
            val libmpvOptionsEnabled = decoderEnabled && libmpvOptionsVisible
            SettingsSection(
                title = stringResource(Res.string.settings_playback_section_decoder),
                isTablet = isTablet,
            ) {
                SettingsList {
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_engine),
                        description = androidPlaybackEngine.label,
                        enabled = decoderEnabled,
                        onClick = { showPlaybackEngineDialog = true },
                    )
                    if (libmpvOptionsVisible) {
                        navigationRow(
                            title = stringResource(Res.string.settings_playback_libmpv_video_output),
                            description = androidLibmpvVideoOutput.label,
                            enabled = libmpvOptionsEnabled,
                            onClick = { showLibmpvVideoOutputDialog = true },
                        )
                        switchRow(
                            title = stringResource(Res.string.settings_playback_libmpv_hardware_decoding),
                            description = stringResource(Res.string.settings_playback_libmpv_hardware_decoding_description),
                            checked = { androidLibmpvHardwareDecodingEnabled },
                            enabled = libmpvOptionsEnabled,
                            onCheckedChange = PlayerSettingsRepository::setAndroidLibmpvHardwareDecodingEnabled,
                        )
                        switchRow(
                            title = stringResource(Res.string.settings_playback_libmpv_yuv420p),
                            description = stringResource(Res.string.settings_playback_libmpv_yuv420p_description),
                            checked = { androidLibmpvYuv420pEnabled },
                            enabled = libmpvOptionsEnabled,
                            onCheckedChange = PlayerSettingsRepository::setAndroidLibmpvYuv420pEnabled,
                        )
                    }
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_decoder_priority),
                        description = decoderPriorityLabel(decoderPriority),
                        enabled = exoOptionsEnabled,
                        onClick = { showDecoderPriorityDialog = true },
                    )
                    switchRow(
                        title = stringResource(Res.string.settings_playback_map_dv7_to_hevc),
                        description = stringResource(Res.string.settings_playback_map_dv7_to_hevc_description),
                        checked = { mapDV7ToHevc },
                        enabled = exoOptionsEnabled,
                        onCheckedChange = PlayerSettingsRepository::setMapDV7ToHevc,
                    )
                    switchRow(
                        title = stringResource(Res.string.settings_playback_tunneled_playback),
                        description = stringResource(Res.string.settings_playback_tunneled_playback_description),
                        checked = { tunnelingEnabled },
                        enabled = exoOptionsEnabled,
                        onCheckedChange = PlayerSettingsRepository::setTunnelingEnabled,
                    )
                }
            }
        }

        if (isIos) {
            SettingsSection(
                title = stringResource(Res.string.settings_playback_ios_audio_output_section),
                isTablet = isTablet,
            ) {
                SettingsList {
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_ios_audio_output),
                        description = autoPlayPlayerSettings.iosAudioOutputMode.label,
                        onClick = { showIosAudioOutputDialog = true },
                    )
                }
            }

            SettingsSection(
                title = stringResource(Res.string.settings_playback_ios_video_output),
                isTablet = isTablet,
            ) {
                SettingsList {
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_ios_hardware_decoder),
                        description = autoPlayPlayerSettings.iosHardwareDecoderMode.localizedLabel(),
                        onClick = { showIosHardwareDecoderDialog = true },
                    )
                    switchRow(
                        title = stringResource(Res.string.settings_playback_ios_extended_dynamic_range),
                        description = stringResource(Res.string.settings_playback_ios_extended_dynamic_range_desc),
                        checked = { autoPlayPlayerSettings.iosExtendedDynamicRangeEnabled },
                        onCheckedChange = PlayerSettingsRepository::setIosExtendedDynamicRangeEnabled,
                    )
                    switchRow(
                        title = stringResource(Res.string.settings_playback_ios_display_color_hint),
                        description = stringResource(Res.string.settings_playback_ios_display_color_hint_desc),
                        checked = { autoPlayPlayerSettings.iosTargetColorspaceHintEnabled },
                        onCheckedChange = PlayerSettingsRepository::setIosTargetColorspaceHintEnabled,
                    )
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_ios_target_primaries),
                        description = autoPlayPlayerSettings.iosTargetPrimaries.label,
                        onClick = { showIosTargetPrimariesDialog = true },
                    )
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_ios_target_transfer),
                        description = autoPlayPlayerSettings.iosTargetTransfer.label,
                        onClick = { showIosTargetTransferDialog = true },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.settings_playback_section_skip_segments),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_playback_skip_intro_outro_recap),
                    description = stringResource(Res.string.settings_playback_skip_intro_outro_recap_description),
                    checked = { autoPlayPlayerSettings.skipIntroEnabled },
                    onCheckedChange = PlayerSettingsRepository::setSkipIntroEnabled,
                )
                switchRow(
                    title = stringResource(Res.string.settings_playback_anime_skip),
                    description = stringResource(Res.string.settings_playback_anime_skip_description),
                    checked = { autoPlayPlayerSettings.animeSkipEnabled },
                    onCheckedChange = PlayerSettingsRepository::setAnimeSkipEnabled,
                )
                if (autoPlayPlayerSettings.animeSkipEnabled) {
                    var showAnimeSkipClientIdDialog by remember { mutableStateOf(false) }
                    val notSetLabel = stringResource(Res.string.settings_playback_not_set)
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_anime_skip_client_id),
                        description = autoPlayPlayerSettings.animeSkipClientId.ifBlank { notSetLabel },
                        onClick = { showAnimeSkipClientIdDialog = true },
                    )
                    if (showAnimeSkipClientIdDialog) {
                        AnimeSkipClientIdDialog(
                            initialValue = autoPlayPlayerSettings.animeSkipClientId,
                            onSave = {
                                PlayerSettingsRepository.setAnimeSkipClientId(it)
                                showAnimeSkipClientIdDialog = false
                            },
                            onDismiss = { showAnimeSkipClientIdDialog = false },
                        )
                    }
                }
                switchRow(
                    title = stringResource(Res.string.settings_playback_intro_submit_enabled),
                    description = stringResource(Res.string.settings_playback_intro_submit_enabled_description),
                    checked = { autoPlayPlayerSettings.introSubmitEnabled },
                    onCheckedChange = PlayerSettingsRepository::setIntroSubmitEnabled,
                )
                if (autoPlayPlayerSettings.introSubmitEnabled) {
                    var showIntroDbApiKeyDialog by remember { mutableStateOf(false) }
                    val notSetLabel = stringResource(Res.string.settings_playback_not_set)
                    navigationRow(
                        title = stringResource(Res.string.settings_playback_introdb_api_key),
                        description = autoPlayPlayerSettings.introDbApiKey.ifBlank { notSetLabel },
                        onClick = { showIntroDbApiKeyDialog = true },
                    )
                    if (showIntroDbApiKeyDialog) {
                        IntroDbApiKeyDialog(
                            initialValue = autoPlayPlayerSettings.introDbApiKey,
                            onSave = {
                                PlayerSettingsRepository.setIntroDbApiKey(it)
                                showIntroDbApiKeyDialog = false
                            },
                            onDismiss = { showIntroDbApiKeyDialog = false },
                        )
                    }
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.settings_playback_section_next_episode),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_playback_auto_play_next_episode),
                    description = stringResource(Res.string.settings_playback_auto_play_next_episode_description),
                    checked = { autoPlayPlayerSettings.streamAutoPlayNextEpisodeEnabled },
                    onCheckedChange = PlayerSettingsRepository::setStreamAutoPlayNextEpisodeEnabled,
                )
                if (autoPlayPlayerSettings.streamAutoPlayNextEpisodeEnabled &&
                    autoPlayPlayerSettings.streamAutoPlayMode == StreamAutoPlayMode.MANUAL) {
                    switchRow(
                        title = stringResource(Res.string.settings_playback_auto_play_next_episode_fallback),
                        description = stringResource(Res.string.settings_playback_auto_play_next_episode_fallback_description),
                        checked = { autoPlayPlayerSettings.streamAutoPlayNextEpisodeFallbackEnabled },
                        onCheckedChange = PlayerSettingsRepository::setStreamAutoPlayNextEpisodeFallbackEnabled,
                    )
                }
                switchRow(
                    title = stringResource(Res.string.settings_playback_prefer_binge_group),
                    description = stringResource(Res.string.settings_playback_prefer_binge_group_description),
                    checked = { autoPlayPlayerSettings.streamAutoPlayPreferBingeGroup },
                    onCheckedChange = PlayerSettingsRepository::setStreamAutoPlayPreferBingeGroup,
                )
                if (autoPlayPlayerSettings.streamAutoPlayPreferBingeGroup) {
                    switchRow(
                        title = stringResource(Res.string.settings_playback_reuse_binge_group),
                        description = stringResource(Res.string.settings_playback_reuse_binge_group_description),
                        checked = { autoPlayPlayerSettings.streamAutoPlayReuseBingeGroup },
                        onCheckedChange = PlayerSettingsRepository::setStreamAutoPlayReuseBingeGroup,
                    )
                }
                var showThresholdModeDialog by remember { mutableStateOf(false) }
                navigationRow(
                    title = stringResource(Res.string.settings_playback_threshold_mode),
                    description = stringResource(autoPlayPlayerSettings.nextEpisodeThresholdMode.labelRes),
                    onClick = { showThresholdModeDialog = true },
                )
                if (showThresholdModeDialog) {
                    NextEpisodeThresholdModeDialog(
                        selected = autoPlayPlayerSettings.nextEpisodeThresholdMode,
                        onSelect = {
                            PlayerSettingsRepository.setNextEpisodeThresholdMode(it)
                        },
                        onDismiss = { showThresholdModeDialog = false },
                    )
                }
                when (autoPlayPlayerSettings.nextEpisodeThresholdMode) {
                    com.nuvio.app.core.playback.skip.NextEpisodeThresholdMode.PERCENTAGE -> {
                        val thresholdPercent = autoPlayPlayerSettings.nextEpisodeThresholdPercent
                        shapedRow { shape ->
                        SettingsSliderContainer(shape = shape, enabled = true) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isTablet) 18.dp else 16.dp, vertical = 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = stringResource(Res.string.settings_playback_threshold_percentage),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(Res.string.settings_playback_threshold_percentage_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                ValueBox(text = stringResource(
                                    Res.string.settings_playback_threshold_percentage_value,
                                    formatStep(thresholdPercent)), modifier = Modifier.wrapContentWidth())
                            }
                            var sliderValue by remember(thresholdPercent) { mutableFloatStateOf(thresholdPercent) }
                            var lastHapticPercent by remember(thresholdPercent) { mutableStateOf(thresholdPercent) }
                            Slider(
                                value = sliderValue,
                                onValueChange = {
                                    val snapped = snapToStep(it, 0.5f)
                                    sliderValue = snapped

                                    if (snapped != lastHapticPercent) {
                                        lastHapticPercent = snapped
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onValueChangeFinished = {
                                    PlayerSettingsRepository.setNextEpisodeThresholdPercent(sliderValue)
                                },
                                valueRange = 97f..100f,
                                steps = calculateSteps(97f, 100f, 0.5f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        }
                        }
                    }
                    com.nuvio.app.core.playback.skip.NextEpisodeThresholdMode.MINUTES_BEFORE_END -> {
                        val thresholdMinutes = autoPlayPlayerSettings.nextEpisodeThresholdMinutesBeforeEnd
                        shapedRow { shape ->
                        SettingsSliderContainer(shape = shape, enabled = true) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isTablet) 18.dp else 16.dp, vertical = 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = stringResource(Res.string.settings_playback_minutes_before_end),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(Res.string.settings_playback_minutes_before_end_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                ValueBox(text = stringResource(
                                        Res.string.settings_playback_minutes_value,
                                        formatStep(thresholdMinutes)), modifier = Modifier.wrapContentWidth())
                            }
                            var sliderValue by remember(thresholdMinutes) { mutableFloatStateOf(thresholdMinutes) }
                            var lastHapticMin by remember(thresholdMinutes) { mutableStateOf(thresholdMinutes) }
                            Slider(
                                value = sliderValue,
                                onValueChange = {
                                    val snapped = snapToStep(it, 0.5f)
                                    sliderValue = snapped

                                    if (snapped != lastHapticMin) {
                                        lastHapticMin = snapped
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onValueChangeFinished = {
                                    PlayerSettingsRepository.setNextEpisodeThresholdMinutesBeforeEnd(sliderValue)
                                },
                                valueRange = 0f..3.5f,
                                steps = calculateSteps(0f, 3.5f, 0.5f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        }
                        }
                    }
                }
            }
        }
    }

    if (showPreferredAudioDialog) {
        val originalHint = stringResource(Res.string.settings_playback_option_original_hint)
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_playback_preferred_audio_language),
            options = listOf(
                LanguageSelectionOption(AudioLanguageOption.DEFAULT, stringResource(Res.string.settings_playback_option_default)),
                LanguageSelectionOption(AudioLanguageOption.DEVICE, stringResource(Res.string.settings_playback_option_device_language)),
                LanguageSelectionOption(AudioLanguageOption.ORIGINAL, stringResource(Res.string.settings_playback_option_original), description = originalHint),
            ) + AvailableLanguageOptions.map { option ->
                LanguageSelectionOption(option.code, stringResource(option.labelRes))
            },
            selectedValue = preferredAudioLanguage,
            onSelect = { value ->
                PlayerSettingsRepository.setPreferredAudioLanguage(value ?: AudioLanguageOption.DEVICE)
            },
            onDismiss = { showPreferredAudioDialog = false },
        )
    }

    if (showP2pProfileDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_p2p_profile_title),
            options = P2pTorrentProfile.entries,
            selected = p2pSettings.torrentProfile,
            label = { p2pProfileLabel(it) },
            description = { profile ->
                when (profile) {
                    P2pTorrentProfile.SOFT ->
                        stringResource(Res.string.settings_p2p_profile_soft_description)
                    P2pTorrentProfile.BALANCED ->
                        stringResource(Res.string.settings_p2p_profile_balanced_description)
                    P2pTorrentProfile.FAST ->
                        stringResource(Res.string.settings_p2p_profile_fast_description)
                }
            },
            onSelect = { profile ->
                P2pSettingsRepository.setTorrentProfile(profile)
                showP2pProfileDialog = false
            },
            onDismiss = { showP2pProfileDialog = false },
        )
    }

    if (showP2pCacheSizeDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_p2p_cache_size_title),
            options = P2pCacheSize.entries,
            selected = p2pSettings.cacheSize,
            label = { p2pCacheSizeLabel(it) },
            onSelect = { size ->
                P2pSettingsRepository.setCacheSize(size)
                p2pCacheClearResult = null
                showP2pCacheSizeDialog = false
            },
            onDismiss = { showP2pCacheSizeDialog = false },
        )
    }

    if (showSecondaryAudioDialog) {
        val originalHint = stringResource(Res.string.settings_playback_option_original_hint)
        LanguageSelectionDialog(
            title = stringResource(Res.string.settings_playback_secondary_audio_language),
            options = listOf(
                LanguageSelectionOption(null, stringResource(Res.string.settings_playback_option_none)),
                LanguageSelectionOption(AudioLanguageOption.ORIGINAL, stringResource(Res.string.settings_playback_option_original), description = originalHint),
            ) + AvailableLanguageOptions.map { option ->
                LanguageSelectionOption(option.code, stringResource(option.labelRes))
            },
            selectedValue = secondaryPreferredAudioLanguage,
            onSelect = { value ->
                PlayerSettingsRepository.setSecondaryPreferredAudioLanguage(value)
            },
            onDismiss = { showSecondaryAudioDialog = false },
        )
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

    if (showSubtitleTextColorDialog) {
        SubtitleColorDialog(
            title = stringResource(Res.string.settings_playback_subtitle_text_color),
            colors = SubtitleColorSwatches,
            selectedColor = autoPlayPlayerSettings.subtitleStyle.textColor,
            onColorSelected = { color ->
                PlayerSettingsRepository.setSubtitleStyle(autoPlayPlayerSettings.subtitleStyle.copy(textColor = color))
            },
            onDismiss = { showSubtitleTextColorDialog = false },
        )
    }

    if (showSubtitleBackgroundColorDialog) {
        SubtitleColorDialog(
            title = stringResource(Res.string.settings_playback_subtitle_background_color),
            colors = SubtitleBackgroundColorSwatches,
            selectedColor = autoPlayPlayerSettings.subtitleStyle.backgroundColor,
            onColorSelected = { color ->
                PlayerSettingsRepository.setSubtitleStyle(autoPlayPlayerSettings.subtitleStyle.copy(backgroundColor = color))
            },
            onDismiss = { showSubtitleBackgroundColorDialog = false },
        )
    }

    if (showSubtitleOutlineColorDialog) {
        SubtitleColorDialog(
            title = stringResource(Res.string.settings_playback_subtitle_outline_color),
            colors = SubtitleColorSwatches,
            selectedColor = autoPlayPlayerSettings.subtitleStyle.outlineColor,
            onColorSelected = { color ->
                PlayerSettingsRepository.setSubtitleStyle(autoPlayPlayerSettings.subtitleStyle.copy(outlineColor = color))
            },
            onDismiss = { showSubtitleOutlineColorDialog = false },
        )
    }

    if (showReuseCacheDurationDialog) {
        ReuseCacheDurationDialog(
            selectedHours = streamReuseLastLinkCacheHours,
            onDurationSelected = { hours ->
                PlayerSettingsRepository.setStreamReuseLastLinkCacheHours(hours)
            },
            onDismiss = { showReuseCacheDurationDialog = false },
        )
    }

    if (showExternalPlayerDialog) {
        PlayerPreferenceDialog(
            isExternal = autoPlayPlayerSettings.externalPlayerEnabled,
            onPreferenceSelected = { external ->
                PlayerSettingsRepository.setExternalPlayerEnabled(external)
            },
            onDismiss = { showExternalPlayerDialog = false },
        )
    }

    if (showExternalPlayerAppDialog) {
        ExternalPlayerSelectionDialog(
            players = availableExternalPlayers,
            selectedPlayerId = autoPlayPlayerSettings.externalPlayerId,
            onPlayerSelected = { playerId ->
                PlayerSettingsRepository.setExternalPlayerId(playerId)
            },
            onDismiss = { showExternalPlayerAppDialog = false },
        )
    }

    if (showP2pConsentDialog) {
        P2pConsentDialog(
            onEnableP2p = {
                P2pSettingsRepository.setP2pEnabled(true)
                showP2pConsentDialog = false
            },
            onDismiss = { showP2pConsentDialog = false },
        )
    }

    if (showDecoderPriorityDialog) {
        DecoderPriorityDialog(
            selectedPriority = decoderPriority,
            onPrioritySelected = { priority ->
                PlayerSettingsRepository.setDecoderPriority(priority)
            },
            onDismiss = { showDecoderPriorityDialog = false },
        )
    }

    if (showPlaybackEngineDialog) {
        PlaybackEngineDialog(
            selectedEngine = androidPlaybackEngine,
            onEngineSelected = { engine ->
                PlayerSettingsRepository.setAndroidPlaybackEngine(engine)
            },
            onDismiss = { showPlaybackEngineDialog = false },
        )
    }

    if (showLibmpvVideoOutputDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_playback_libmpv_video_output_dialog),
            options = AndroidLibmpvVideoOutput.entries,
            selected = androidLibmpvVideoOutput,
            label = { it.label },
            description = { it.description },
            onSelect = {
                PlayerSettingsRepository.setAndroidLibmpvVideoOutput(it)
            },
            onDismiss = { showLibmpvVideoOutputDialog = false },
        )
    }

    if (showHoldToSpeedValueDialog) {
        HoldToSpeedValueDialog(
            selectedSpeed = holdToSpeedValue,
            onSpeedSelected = { speed ->
                PlayerSettingsRepository.setHoldToSpeedValue(speed)
            },
            onDismiss = { showHoldToSpeedValueDialog = false },
        )
    }

    if (showIosHardwareDecoderDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_playback_ios_hw_decoder_dialog),
            options = IosHardwareDecoderMode.entries,
            selected = autoPlayPlayerSettings.iosHardwareDecoderMode,
            label = { it.label },
            onSelect = {
                PlayerSettingsRepository.setIosHardwareDecoderMode(it)
            },
            onDismiss = { showIosHardwareDecoderDialog = false },
        )
    }

    if (showIosAudioOutputDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_playback_ios_audio_output_dialog),
            options = IosAudioOutputMode.selectableEntries,
            selected = autoPlayPlayerSettings.iosAudioOutputMode,
            label = { it.label },
            description = {
                when (it) {
                    IosAudioOutputMode.Auto -> stringResource(Res.string.settings_playback_ios_audio_output_auto_desc)
                    IosAudioOutputMode.AvFoundation -> stringResource(Res.string.settings_playback_ios_audio_output_avfoundation_desc)
                    IosAudioOutputMode.AudioUnit -> stringResource(Res.string.settings_playback_ios_audio_output_audiounit_desc)
                }
            },
            onSelect = {
                PlayerSettingsRepository.setIosAudioOutputMode(it)
            },
            onDismiss = { showIosAudioOutputDialog = false },
        )
    }

    if (showIosTargetPrimariesDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_playback_ios_target_primaries_dialog),
            options = IosTargetPrimaries.entries,
            selected = autoPlayPlayerSettings.iosTargetPrimaries,
            label = { it.label },
            onSelect = {
                PlayerSettingsRepository.setIosTargetPrimaries(it)
            },
            onDismiss = { showIosTargetPrimariesDialog = false },
        )
    }

    if (showIosTargetTransferDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_playback_ios_target_transfer_dialog),
            options = IosTargetTransfer.entries,
            selected = autoPlayPlayerSettings.iosTargetTransfer,
            label = { it.label },
            onSelect = {
                PlayerSettingsRepository.setIosTargetTransfer(it)
            },
            onDismiss = { showIosTargetTransferDialog = false },
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

    if (showAutoPlayModeDialog) {
        StreamAutoPlayModeDialog(
            selectedMode = autoPlayPlayerSettings.streamAutoPlayMode,
            onModeSelected = {
                PlayerSettingsRepository.setStreamAutoPlayMode(it)
            },
            onDismiss = { showAutoPlayModeDialog = false },
        )
    }

    if (showAutoPlaySourceDialog) {
        StreamAutoPlaySourceDialog(
            pluginsEnabled = pluginsEnabled,
            selectedSource = autoPlayPlayerSettings.streamAutoPlaySource,
            onSourceSelected = {
                PlayerSettingsRepository.setStreamAutoPlaySource(it)
            },
            onDismiss = { showAutoPlaySourceDialog = false },
        )
    }

    if (showAutoPlayAddonSelectionDialog) {
        val addonNames = addonUiState.addons
            .enabledAddons()
            .mapNotNull { it.manifest }
            .filter { manifest -> manifest.resources.any { resource -> resource.name == "stream" } }
            .map { it.name }
            .distinct()
            .sorted()
        MultiChoiceBottomSheet(
            title = stringResource(Res.string.settings_playback_allowed_addons),
            allLabel = stringResource(Res.string.settings_playback_all_addons),
            options = addonNames.map { name -> SingleChoiceOption(value = name, label = name) },
            selected = autoPlayPlayerSettings.streamAutoPlaySelectedAddons,
            onSelectionChanged = PlayerSettingsRepository::setStreamAutoPlaySelectedAddons,
            onDismiss = { showAutoPlayAddonSelectionDialog = false },
        )
    }

    if (pluginsEnabled && showAutoPlayPluginSelectionDialog) {
        val pluginNames = pluginUiState.scrapers
            .filter { it.enabled }
            .map { it.name }
            .distinct()
            .sorted()
        MultiChoiceBottomSheet(
            title = stringResource(Res.string.settings_playback_allowed_plugins),
            allLabel = stringResource(Res.string.settings_playback_all_plugins),
            options = pluginNames.map { name -> SingleChoiceOption(value = name, label = name) },
            selected = autoPlayPlayerSettings.streamAutoPlaySelectedPlugins,
            onSelectionChanged = PlayerSettingsRepository::setStreamAutoPlaySelectedPlugins,
            onDismiss = { showAutoPlayPluginSelectionDialog = false },
        )
    }

    if (showAutoPlayRegexDialog) {
        StreamAutoPlayRegexDialog(
            initialRegex = autoPlayPlayerSettings.streamAutoPlayRegex,
            onSave = {
                PlayerSettingsRepository.setStreamAutoPlayRegex(it)
                showAutoPlayRegexDialog = false
            },
            onDismiss = { showAutoPlayRegexDialog = false },
        )
    }
}

@Composable
internal fun formatReuseCacheDuration(hours: Int): String = when {
    hours < 24 && hours == 1 -> stringResource(Res.string.settings_playback_duration_hour_one, hours)
    hours < 24 -> stringResource(Res.string.settings_playback_duration_hours, hours)
    hours % 24 == 0 -> {
        val days = hours / 24
        if (days == 1) stringResource(Res.string.settings_playback_duration_day_one, days)
        else stringResource(Res.string.settings_playback_duration_days, days)
    }
    else -> stringResource(Res.string.settings_playback_duration_hours, hours)
}

private fun decoderPriorityRes(priority: Int): StringResource = when (priority) {
    0 -> Res.string.settings_playback_decoder_device_only
    1 -> Res.string.settings_playback_decoder_prefer_device
    2 -> Res.string.settings_playback_decoder_prefer_app
    else -> Res.string.settings_playback_decoder_prefer_device
}

@Composable
private fun decoderPriorityLabel(priority: Int): String = stringResource(decoderPriorityRes(priority))

internal fun StreamAutoPlaySource.labelRes(pluginsEnabled: Boolean): StringResource = when (this) {
    StreamAutoPlaySource.ALL_SOURCES ->
        if (pluginsEnabled) Res.string.settings_playback_source_scope_all_sources
        else Res.string.settings_playback_source_scope_all_addons
    StreamAutoPlaySource.INSTALLED_ADDONS_ONLY -> Res.string.settings_playback_source_scope_installed_addons_only
    StreamAutoPlaySource.ENABLED_PLUGINS_ONLY -> Res.string.settings_playback_source_scope_enabled_plugins_only
}

internal val StreamAutoPlayMode.labelRes: StringResource
    get() = when (this) {
        StreamAutoPlayMode.MANUAL -> Res.string.settings_playback_stream_selection_mode_manual
        StreamAutoPlayMode.FIRST_STREAM -> Res.string.settings_playback_stream_selection_mode_first_stream
        StreamAutoPlayMode.REGEX_MATCH -> Res.string.settings_playback_stream_selection_mode_regex
    }

internal val com.nuvio.app.core.playback.skip.NextEpisodeThresholdMode.labelRes: StringResource
    get() = when (this) {
        com.nuvio.app.core.playback.skip.NextEpisodeThresholdMode.PERCENTAGE ->
            Res.string.settings_playback_threshold_mode_percentage
        com.nuvio.app.core.playback.skip.NextEpisodeThresholdMode.MINUTES_BEFORE_END ->
            Res.string.settings_playback_threshold_mode_minutes_before_end
    }

private fun libassRenderTypeRes(renderType: String): StringResource = when (renderType) {
    "OVERLAY_OPEN_GL" -> Res.string.settings_playback_render_type_overlay_opengl
    "OVERLAY_CANVAS" -> Res.string.settings_playback_render_type_overlay_canvas
    "EFFECTS_OPEN_GL" -> Res.string.settings_playback_render_type_effects_opengl
    "EFFECTS_CANVAS" -> Res.string.settings_playback_render_type_effects_canvas
    "CUES" -> Res.string.settings_playback_render_type_cues
    else -> Res.string.settings_playback_render_type_cues
}

@Composable
private fun libassRenderTypeLabel(renderType: String): String = stringResource(libassRenderTypeRes(renderType))
