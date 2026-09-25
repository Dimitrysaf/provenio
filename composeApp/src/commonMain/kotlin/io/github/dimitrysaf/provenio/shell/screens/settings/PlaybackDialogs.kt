package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.playback.AndroidPlaybackEngine
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerApp
import io.github.dimitrysaf.provenio.shell.screens.player.formatPlaybackSpeedLabel
import io.github.dimitrysaf.provenio.shell.screens.player.isTransparentArgb
import io.github.dimitrysaf.provenio.core.streams.StreamAutoPlayMode
import io.github.dimitrysaf.provenio.core.streams.StreamAutoPlaySource
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal data class LanguageSelectionOption(
    val value: String?,
    val label: String,
    val description: String? = null,
)

@Composable
internal fun PlayerPreferenceDialog(
    isExternal: Boolean,
    onPreferenceSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_player_preference),
        description = stringResource(Res.string.settings_playback_player_preference_description),
        options = listOf(
            SingleChoiceOption(
                value = false,
                label = stringResource(Res.string.settings_playback_player_preference_internal),
            ),
            SingleChoiceOption(
                value = true,
                label = stringResource(Res.string.settings_playback_player_preference_external),
            ),
        ),
        isSelected = { it == isExternal },
        onSelected = onPreferenceSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ExternalPlayerSelectionDialog(
    players: List<ExternalPlayerApp>,
    selectedPlayerId: String?,
    onPlayerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_external_player_app),
        // With nothing installed there is no list to show, so the sheet says so instead.
        description = stringResource(Res.string.settings_playback_external_player_none_available)
            .takeIf { players.isEmpty() },
        options = players.map { player ->
            SingleChoiceOption(value = player.id, label = player.name)
        },
        isSelected = { it == selectedPlayerId },
        onSelected = onPlayerSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun LanguageSelectionDialog(
    title: String,
    options: List<LanguageSelectionOption>,
    selectedValue: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = title,
        options = options.map { option ->
            SingleChoiceOption(
                value = option.value,
                label = option.label,
                supportingText = option.description?.takeIf { it.isNotBlank() },
            )
        },
        isSelected = { it == selectedValue },
        onSelected = onSelect,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun ReuseCacheDurationDialog(
    selectedHours: Int,
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_last_link_cache_duration),
        options = listOf(1, 2, 3, 6, 12, 24, 48, 72, 168).map { hours ->
            SingleChoiceOption(value = hours, label = formatReuseCacheDuration(hours))
        },
        isSelected = { it == selectedHours },
        onSelected = onDurationSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun DecoderPriorityDialog(
    selectedPriority: Int,
    onPrioritySelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_decoder_priority),
        options = listOf(
            0 to Res.string.settings_playback_decoder_device_only,
            1 to Res.string.settings_playback_decoder_prefer_device,
            2 to Res.string.settings_playback_decoder_prefer_app,
        ).map { (priority, labelRes) ->
            SingleChoiceOption(value = priority, label = stringResource(labelRes))
        },
        isSelected = { it == selectedPriority },
        onSelected = onPrioritySelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun PlaybackEngineDialog(
    selectedEngine: AndroidPlaybackEngine,
    onEngineSelected: (AndroidPlaybackEngine) -> Unit,
    onDismiss: () -> Unit,
) {
    val descriptions = mapOf(
        AndroidPlaybackEngine.Auto to Res.string.settings_playback_engine_auto_description,
        AndroidPlaybackEngine.ExoPlayer to Res.string.settings_playback_engine_exoplayer_description,
        AndroidPlaybackEngine.Libmpv to Res.string.settings_playback_engine_libmpv_description,
    )
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_engine),
        options = AndroidPlaybackEngine.entries.map { engine ->
            SingleChoiceOption(
                value = engine,
                label = engine.label,
                supportingText = stringResource(descriptions.getValue(engine)),
            )
        },
        isSelected = { it == selectedEngine },
        onSelected = onEngineSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun <T> IosEnumSelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    description: @Composable (T) -> String? = { null },
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = title,
        options = options.map { option ->
            SingleChoiceOption(
                value = option,
                label = label(option),
                supportingText = description(option),
            )
        },
        isSelected = { it == selected },
        onSelected = onSelect,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun HoldToSpeedValueDialog(
    selectedSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_hold_speed),
        options = listOf(1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f).map { speed ->
            SingleChoiceOption(value = speed, label = formatPlaybackSpeedLabel(speed))
        },
        isSelected = { it == selectedSpeed },
        onSelected = onSpeedSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun LibassRenderTypeDialog(
    selectedRenderType: String,
    onRenderTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_render_type),
        options = listOf(
            "OVERLAY_OPEN_GL" to Res.string.settings_playback_render_type_overlay_opengl,
            "OVERLAY_CANVAS" to Res.string.settings_playback_render_type_overlay_canvas,
            "EFFECTS_OPEN_GL" to Res.string.settings_playback_render_type_effects_opengl,
            "EFFECTS_CANVAS" to Res.string.settings_playback_render_type_effects_canvas,
            "CUES" to Res.string.settings_playback_render_type_cues,
        ).map { (renderType, labelRes) ->
            SingleChoiceOption(value = renderType, label = stringResource(labelRes))
        },
        isSelected = { it == selectedRenderType },
        onSelected = onRenderTypeSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun SubtitleColorDialog(
    title: String,
    colors: List<Long>,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = title,
        options = colors.map { color ->
            SingleChoiceOption(
                value = color,
                label = subtitleColorLabel(color),
                leadingContent = {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = MaterialTheme.shapes.small,
                        // A fully transparent swatch would be invisible, so it shows the surface
                        // it sits on and relies on its outline to read as a swatch at all.
                        color = if (color.isTransparentArgb()) MaterialTheme.colorScheme.surface else Color(color),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {}
                },
            )
        },
        isSelected = { it == selectedColor },
        onSelected = onColorSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun StreamAutoPlayModeDialog(
    selectedMode: StreamAutoPlayMode,
    onModeSelected: (StreamAutoPlayMode) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_stream_selection_mode),
        options = listOf(
            Triple(
                StreamAutoPlayMode.MANUAL,
                Res.string.settings_playback_stream_selection_mode_manual,
                Res.string.settings_playback_stream_selection_mode_manual_description,
            ),
            Triple(
                StreamAutoPlayMode.FIRST_STREAM,
                Res.string.settings_playback_stream_selection_mode_first_stream,
                Res.string.settings_playback_stream_selection_mode_first_stream_description,
            ),
            Triple(
                StreamAutoPlayMode.REGEX_MATCH,
                Res.string.settings_playback_stream_selection_mode_regex,
                Res.string.settings_playback_stream_selection_mode_regex_description,
            ),
        ).map { (mode, labelRes, descriptionRes) ->
            SingleChoiceOption(
                value = mode,
                label = stringResource(labelRes),
                supportingText = stringResource(descriptionRes),
            )
        },
        isSelected = { it == selectedMode },
        onSelected = onModeSelected,
        onDismiss = onDismiss,
    )
}

@Composable
internal fun StreamAutoPlaySourceDialog(
    pluginsEnabled: Boolean,
    selectedSource: StreamAutoPlaySource,
    onSourceSelected: (StreamAutoPlaySource) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = buildList {
        add(
            Triple(
                StreamAutoPlaySource.ALL_SOURCES,
                if (pluginsEnabled) {
                    Res.string.settings_playback_source_scope_all_sources
                } else {
                    Res.string.settings_playback_source_scope_all_addons
                },
                if (pluginsEnabled) {
                    Res.string.settings_playback_source_scope_all_sources_description
                } else {
                    Res.string.settings_playback_source_scope_all_addons_description
                },
            ),
        )
        add(
            Triple(
                StreamAutoPlaySource.INSTALLED_ADDONS_ONLY,
                Res.string.settings_playback_source_scope_installed_addons_only,
                Res.string.settings_playback_source_scope_installed_addons_only_description,
            ),
        )
        if (pluginsEnabled) {
            add(
                Triple(
                    StreamAutoPlaySource.ENABLED_PLUGINS_ONLY,
                    Res.string.settings_playback_source_scope_enabled_plugins_only,
                    Res.string.settings_playback_source_scope_enabled_plugins_only_description,
                ),
            )
        }
    }

    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_source_scope),
        options = options.map { (source, labelRes, descriptionRes) ->
            SingleChoiceOption(
                value = source,
                label = stringResource(labelRes),
                supportingText = stringResource(descriptionRes),
            )
        },
        isSelected = { it == selectedSource },
        onSelected = onSourceSelected,
        onDismiss = onDismiss,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun StreamAutoPlayRegexDialog(
    initialRegex: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var regex by remember(initialRegex) { mutableStateOf(initialRegex) }
    var regexError by remember { mutableStateOf<String?>(null) }

    val invalidRegexPattern = stringResource(Res.string.settings_playback_invalid_regex_pattern)
    val presets = listOf(
        stringResource(Res.string.settings_playback_regex_preset_any_1080p) to "(2160p|4k|1080p)",
        stringResource(Res.string.settings_playback_regex_preset_quality_4k_remux) to "(2160p|4k|remux)",
        stringResource(Res.string.settings_playback_regex_preset_quality_1080p_standard) to "(1080p|full\\s*hd)",
        stringResource(Res.string.settings_playback_regex_preset_quality_720p_smaller) to "(720p|webrip|web-dl)",
        stringResource(Res.string.settings_playback_regex_preset_web_sources) to "(web[-\\s]?dl|webrip)",
        stringResource(Res.string.settings_playback_regex_preset_bluray_quality) to "(bluray|b[dr]rip|remux)",
        stringResource(Res.string.settings_playback_regex_preset_hevc_x265) to "(hevc|x265|h\\.265)",
        stringResource(Res.string.settings_playback_regex_preset_avc_x264) to "(x264|h\\.264|avc)",
        stringResource(Res.string.settings_playback_regex_preset_hdr_dolby_vision) to "(hdr|hdr10\\+?|dv|dolby\\s*vision)",
        stringResource(Res.string.settings_playback_regex_preset_dolby_atmos_dts) to "(atmos|truehd|dts[-\\s]?hd|dtsx?)",
        stringResource(Res.string.settings_playback_regex_preset_english) to "(\\beng\\b|english)",
        stringResource(Res.string.settings_playback_regex_preset_no_cam_ts) to "^(?!.*\\b(cam|hdcam|ts|telesync)\\b).*$",
        stringResource(Res.string.settings_playback_regex_preset_no_remux_hdr) to "(?is)^(?!.*\\b(hdr|hdr10|dv|dolby|vision|hevc|remux|2160p)\\b).+$",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_playback_regex_pattern)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.settings_playback_regex_matches_against))
                Text(
                    text = stringResource(Res.string.settings_playback_presets),
                    style = MaterialTheme.typography.titleSmall,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        count = presets.size,
                        key = { presets[it].first },
                    ) { index ->
                        val (label, pattern) = presets[index]
                        FilterChip(
                            selected = regex == pattern,
                            onClick = {
                                regex = pattern
                                regexError = null
                            },
                            label = { Text(label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = regex,
                    onValueChange = {
                        regex = it
                        regexError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.settings_playback_regex_pattern)) },
                    placeholder = { Text(stringResource(Res.string.settings_playback_regex_placeholder)) },
                    isError = regexError != null,
                    supportingText = regexError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = regex.trim()
                if (value.isNotEmpty() && runCatching { Regex(value, RegexOption.IGNORE_CASE) }.isFailure) {
                    regexError = invalidRegexPattern
                    return@TextButton
                }
                onSave(value)
            }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    regex = ""
                    regexError = null
                }) {
                    Text(stringResource(Res.string.action_clear))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        },
    )
}

@Composable
internal fun AnimeSkipClientIdDialog(
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_playback_anime_skip_client_id)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.settings_playback_anime_skip_client_id_description))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.settings_playback_anime_skip_client_id)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value.trim()) }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@Composable
internal fun IntroDbApiKeyDialog(
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf(initialValue) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val invalidKeyMessage = stringResource(Res.string.settings_playback_introdb_invalid_key)

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        title = { Text(stringResource(Res.string.settings_playback_introdb_api_key)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.settings_playback_introdb_api_key_description))
                SettingsSecretTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        errorMessage = null
                    },
                    label = stringResource(Res.string.settings_playback_introdb_api_key),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null,
                )
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = value.trim()
                    when {
                        trimmed.isEmpty() -> onSave(trimmed)
                        trimmed == initialValue -> onDismiss()
                        else -> {
                            isVerifying = true
                            errorMessage = null
                            scope.launch {
                                val isValid = io.github.dimitrysaf.provenio.core.playback.skip.SkipIntroRepository
                                    .verifyIntroDbApiKey(trimmed)
                                isVerifying = false
                                if (isValid) onSave(trimmed) else errorMessage = invalidKeyMessage
                            }
                        }
                    }
                },
                enabled = !isVerifying,
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun NextEpisodeThresholdModeDialog(
    selected: io.github.dimitrysaf.provenio.core.playback.skip.NextEpisodeThresholdMode,
    onSelect: (io.github.dimitrysaf.provenio.core.playback.skip.NextEpisodeThresholdMode) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_playback_threshold_mode),
        options = io.github.dimitrysaf.provenio.core.playback.skip.NextEpisodeThresholdMode.entries.map { mode ->
            SingleChoiceOption(value = mode, label = stringResource(mode.labelRes))
        },
        isSelected = { it == selected },
        onSelected = onSelect,
        onDismiss = onDismiss,
    )
}
