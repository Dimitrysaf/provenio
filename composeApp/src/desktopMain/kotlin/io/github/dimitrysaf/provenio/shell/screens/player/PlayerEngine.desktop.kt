package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.dimitrysaf.provenio.core.playback.AudioTrack
import io.github.dimitrysaf.provenio.core.playback.PlayerPlaybackSnapshot
import io.github.dimitrysaf.provenio.core.playback.PlayerResizeMode
import io.github.dimitrysaf.provenio.core.playback.SubtitleTrack
import io.github.dimitrysaf.provenio.core.streams.StreamSubtitle
import io.github.dimitrysaf.provenio.desktop.mpv.MpvPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Playback through libmpv; see [MpvPlayer]. */
@Composable
actual fun PlatformPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    sourceHeaders: Map<String, String>,
    sourceResponseHeaders: Map<String, String>,
    externalSubtitles: List<StreamSubtitle>,
    streamType: String?,
    useYoutubeChunkedPlayback: Boolean,
    modifier: Modifier,
    playWhenReady: Boolean,
    initialPositionMs: Long?,
    initialPositionRequestKey: String?,
    resizeMode: PlayerResizeMode,
    useNativeController: Boolean,
    onInitialPositionHandled: (key: String, handled: Boolean) -> Unit,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
) {
    val player = remember { MpvPlayer.create() }
    val currentOnError by rememberUpdatedState(onError)
    val currentOnSnapshot by rememberUpdatedState(onSnapshot)
    val currentOnInitialPositionHandled by rememberUpdatedState(onInitialPositionHandled)

    if (player == null) {
        LaunchedEffect(Unit) { currentOnError("Video playback needs libmpv, which could not be loaded.") }
        Box(modifier.fillMaxSize().background(Color.Black))
        return
    }

    DisposableEffect(player) {
        player.onEndFileError = { message -> currentOnError(message) }
        onDispose { player.release() }
    }

    LaunchedEffect(player) {
        onControllerReady(MpvPlayerController(player))
        while (isActive) {
            currentOnSnapshot(withContext(Dispatchers.IO) { player.snapshot() })
            delay(250)
        }
    }

    LaunchedEffect(player, sourceUrl, sourceAudioUrl, sourceHeaders, externalSubtitles) {
        val requestKey = initialPositionRequestKey
        player.onFileLoaded = { requestKey?.let { currentOnInitialPositionHandled(it, true) } }
        withContext(Dispatchers.IO) {
            player.load(
                url = sourceUrl,
                audioUrl = sourceAudioUrl,
                headers = sanitizePlaybackHeaders(sourceHeaders),
                subtitles = externalSubtitles.map { it.url to (it.name ?: it.language) },
                startPositionMs = initialPositionMs,
            )
        }
    }

    LaunchedEffect(player, playWhenReady) { player.setPaused(!playWhenReady) }
    LaunchedEffect(player, resizeMode) { player.setResizeMode(resizeMode) }

    val frame by player.frame.collectAsState()
    Canvas(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged(player::setTargetSize),
    ) {
        val image = frame ?: return@Canvas
        // mpv already letterboxed the frame for this surface's shape; it only needs scaling up.
        drawImage(
            image = image,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}

private class MpvPlayerController(private val player: MpvPlayer) : PlayerEngineController {
    override fun play() = player.setPaused(false)
    override fun pause() = player.setPaused(true)
    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    override fun seekBy(offsetMs: Long) = player.seekBy(offsetMs)
    override fun retry() = player.reload()
    override fun setPlaybackSpeed(speed: Float) = player.setSpeed(speed)
    override fun setMuted(muted: Boolean) = player.setMuted(muted)
    override fun getAudioTracks(): List<AudioTrack> = player.audioTracks()
    override fun getSubtitleTracks(): List<SubtitleTrack> = player.subtitleTracks()
    override fun applyAudioLanguagePreferences(languages: List<String>) = player.setPreferredAudioLanguages(languages)
    override fun selectAudioTrack(index: Int) = player.selectAudioTrack(index)
    override fun selectSubtitleTrack(index: Int) = player.selectSubtitleTrack(index)
    override fun setSubtitleUri(url: String) = player.addSubtitle(url)
    override fun clearExternalSubtitle() = player.removeExternalSubtitles()

    override fun clearExternalSubtitleAndSelect(trackIndex: Int) {
        player.removeExternalSubtitles()
        player.selectSubtitleTrack(trackIndex)
    }

    override fun applySubtitlePreferences(
        preferredLanguage: String,
        secondaryPreferredLanguage: String?,
        useForcedSubtitles: Boolean,
        autoSelectionApplied: Boolean,
        hasActiveSubtitle: Boolean,
        useCustomSubtitles: Boolean,
    ) {
        player.setPreferredSubtitleLanguages(listOfNotNull(preferredLanguage, secondaryPreferredLanguage))
    }

    override fun setSubtitleDelayMs(delayMs: Int) = player.setSubtitleDelayMs(delayMs)
}
