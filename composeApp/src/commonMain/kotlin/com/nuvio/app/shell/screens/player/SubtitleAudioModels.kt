package com.nuvio.app.shell.screens.player

import androidx.compose.runtime.Composable
import com.nuvio.app.core.build.isIos
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_track_number
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.playback.AddonSubtitle
import com.nuvio.app.core.playback.AudioTrack
import com.nuvio.app.core.playback.SubtitleSyncCue
import com.nuvio.app.core.playback.SubtitleTrack
import com.nuvio.app.core.playback.SubtitleStyleState

const val SUBTITLE_DELAY_STEP_MS = 100

const val SUBTITLE_AUTO_SYNC_REACTION_COMPENSATION_MS = 300L

data class SubtitleAutoSyncUiState(
    val capturedPositionMs: Long? = null,
    val cues: List<SubtitleSyncCue> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

val SubtitleColorSwatches: List<Long> = listOf(
    0xFFFFFFFF,
    0xFFFFD700,
    0xFF00E5FF,
    0xFFFF5C5C,
    0xFF00FF88,
    0xFF9B59B6,
    0xFFF97316,
    0xFF22C55E,
    0xFF3B82F6,
    0xFF000000,
)

val SubtitleBackgroundColorSwatches: List<Long> = listOf(
    0x00000000L,
    0x8C000000,
    0xB8111827,
    0xAD7F1D1D,
    0xAD064E3B,
    0xAD1E3A8A,
)

fun Long.isTransparentArgb(): Boolean = ((this ushr 24) and 0xFFL) == 0L

data class SubtitleAudioUiState(
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val addonSubtitles: List<AddonSubtitle> = emptyList(),
    val isLoadingAddonSubtitles: Boolean = false,
    val addonSubtitleError: String? = null,
    val selectedAudioIndex: Int = -1,
    val selectedSubtitleIndex: Int = -1,
    val selectedAddonSubtitleId: String? = null,
    val useCustomSubtitles: Boolean = false,
    val subtitleStyle: SubtitleStyleState = SubtitleStyleState.DEFAULT,
    val showAudioModal: Boolean = false,
    val showSubtitleModal: Boolean = false,
)

@Composable
fun localizedTrackDisplayName(label: String?, language: String?, index: Int): String {
    if (!label.isNullOrBlank()) return label
    if (!language.isNullOrBlank()) return languageLabelForCode(language)
    return stringResource(Res.string.compose_player_track_number, index + 1)
}
