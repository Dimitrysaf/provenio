package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.core.playback.PlayerAudioLevelSample
import io.github.dimitrysaf.provenio.core.playback.AudioTrack
import io.github.dimitrysaf.provenio.core.playback.PlayerResizeMode
import io.github.dimitrysaf.provenio.core.playback.SubtitleTrack
import io.github.dimitrysaf.provenio.core.playback.PlayerNowPlayingInfo
import io.github.dimitrysaf.provenio.core.playback.PlayerPlaybackSnapshot
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import io.github.dimitrysaf.provenio.core.playback.SubtitleStyleState

interface PlayerEngineController {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(offsetMs: Long)
    fun retry()
    fun setPlaybackSpeed(speed: Float)
    fun setMuted(muted: Boolean) {}
    /**
     * Amplifies the player's own output above full volume: 1 leaves it as it is, 2 doubles it.
     * Engines that cannot amplify ignore it.
     */
    fun setVolumeBoost(gain: Float) {}

    /**
     * Evens out loudness, lifting quiet dialogue and holding back loud scenes. Engines that cannot
     * ignore it.
     */
    fun setVolumeStabilizer(enabled: Boolean) {}

    /**
     * Scales the player's own output from 1, as it is, down to 0, silent, for the sleep timer's
     * fade. It is separate from the boost and from the device volume.
     */
    fun setFadeVolume(fraction: Float) {}

    /**
     * How loud the sound is now and where in the video, for automatic subtitle sync. The first
     * call starts measuring. Null while paused, before anything is measured, or where the engine
     * cannot measure.
     */
    fun sampleAudioLevel(): PlayerAudioLevelSample? = null
    fun getAudioTracks(): List<AudioTrack>
    fun getSubtitleTracks(): List<SubtitleTrack>
    fun applyAudioLanguagePreferences(languages: List<String>)
    fun selectAudioTrack(index: Int)
    fun selectSubtitleTrack(index: Int)
    fun setSubtitleUri(url: String)
    fun clearExternalSubtitle()
    fun clearExternalSubtitleAndSelect(trackIndex: Int)
    fun applySubtitleStyle(style: SubtitleStyleState) {}
    fun applySubtitlePreferences(
        preferredLanguage: String,
        secondaryPreferredLanguage: String? = null,
        useForcedSubtitles: Boolean,
        autoSelectionApplied: Boolean,
        hasActiveSubtitle: Boolean,
        useCustomSubtitles: Boolean = false,
    ) {}
    fun setSubtitleDelayMs(delayMs: Int) {}
    fun configureIosVideoOutput(settings: PlayerSettingsUiState) {}
    fun updateNowPlayingMetadata(info: PlayerNowPlayingInfo) {}
    fun clearNowPlayingInfo() {}
}

internal fun sanitizePlaybackHeaders(headers: Map<String, String>?): Map<String, String> {
    val rawHeaders = headers ?: return emptyMap()
    if (rawHeaders.isEmpty()) return emptyMap()

    val sanitized = LinkedHashMap<String, String>(rawHeaders.size)
    rawHeaders.forEach { (rawKey, rawValue) ->
        val key = rawKey.trim()
        val value = rawValue.trim()
        if (key.isEmpty() || value.isEmpty()) return@forEach
        if (key.equals("Range", ignoreCase = true)) return@forEach
        sanitized[key] = value
    }
    return sanitized
}

internal fun sanitizePlaybackResponseHeaders(headers: Map<String, String>?): Map<String, String> {
    val rawHeaders = headers ?: return emptyMap()
    if (rawHeaders.isEmpty()) return emptyMap()

    val sanitized = LinkedHashMap<String, String>(rawHeaders.size)
    rawHeaders.forEach { (rawKey, rawValue) ->
        val key = rawKey.trim()
        val value = rawValue.trim()
        if (key.isEmpty() || value.isEmpty()) return@forEach
        sanitized[key] = value
    }
    return sanitized
}

@Composable
expect fun PlatformPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String? = null,
    sourceHeaders: Map<String, String> = emptyMap(),
    sourceResponseHeaders: Map<String, String> = emptyMap(),
    externalSubtitles: List<io.github.dimitrysaf.provenio.core.streams.StreamSubtitle> = emptyList(),
    streamType: String? = null,
    useYoutubeChunkedPlayback: Boolean = false,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true,
    initialPositionMs: Long? = null,
    initialPositionRequestKey: String? = null,
    resizeMode: PlayerResizeMode = PlayerResizeMode.Fit,
    useNativeController: Boolean = false,
    onInitialPositionHandled: (key: String, handled: Boolean) -> Unit = { _, _ -> },
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
)
