package io.github.dimitrysaf.provenio.shell.screens.player

import platform.UIKit.UIViewController

/**
 * Bridge interface for the MPV player.
 * Swift side implements this and registers a factory at app startup.
 */
interface PlayerBridge {
    fun createPlayerViewController(): UIViewController
    fun loadFile(url: String)
    fun loadFileWithAudio(
        videoUrl: String,
        audioUrl: String?,
        headersJson: String?,
        subtitlesJson: String? = null
    )
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(offsetMs: Long)
    fun retry()
    fun updateNowPlayingMetadata(
        title: String,
        subtitle: String?,
        artworkUrl: String?,
    )
    fun clearNowPlayingInfo()
    fun configureVideoOutput(
        hardwareDecoder: String,
        targetColorspaceHint: Boolean,
        toneMapping: String,
        hdrComputePeak: Boolean,
        targetPrimaries: String,
        targetTransfer: String,
        extendedDynamicRange: Boolean,
        deband: Boolean,
        interpolation: Boolean,
        brightness: Int,
        contrast: Int,
        saturation: Int,
        gamma: Int,
    )
    fun configureAudioOutput(audioOutput: String)
    fun setPlaybackSpeed(speed: Float)
    fun setMuted(muted: Boolean)
    fun setResizeMode(mode: Int) // 0=Fit, 1=Fill, 2=Zoom
    fun syncVideoSurfaceLayout(width: Double, height: Double)
    fun getAudioTrackCount(): Int
    fun getAudioTrackIndex(at: Int): Int
    fun getAudioTrackId(at: Int): String
    fun getAudioTrackLabel(at: Int): String
    fun getAudioTrackLang(at: Int): String
    fun isAudioTrackSelected(at: Int): Boolean
    fun getSubtitleTrackCount(): Int
    fun getSubtitleTrackIndex(at: Int): Int
    fun getSubtitleTrackId(at: Int): String
    fun getSubtitleTrackLabel(at: Int): String
    fun getSubtitleTrackLang(at: Int): String
    fun isSubtitleTrackSelected(at: Int): Boolean
    fun selectAudioTrack(trackId: Int)
    fun applyAudioLanguagePreferences(languages: List<String>)
    fun selectSubtitleTrack(trackId: Int)
    fun setSubtitleUrl(url: String)
    fun clearExternalSubtitle()
    fun clearExternalSubtitleAndSelect(trackId: Int)
    fun setSubtitleDelayMs(delayMs: Int)
    fun applySubtitleStyle(
        textColor: String,
        backgroundColor: String,
        outlineColor: String,
        outlineSize: Float,
        bold: Boolean,
        fontSize: Float,
        subPos: Int,
        stripSdh: Boolean,
    )
    fun getIsLoading(): Boolean
    fun getIsPlaying(): Boolean
    fun getIsEnded(): Boolean
    fun getDurationMs(): Long
    fun getPositionMs(): Long
    fun getBufferedMs(): Long
    fun getPlaybackSpeed(): Float
    fun getErrorMessage(): String
    fun destroy()
}

/**
 * Registry for the player bridge factory.
 * Swift calls [registerFactory] during app startup before Compose is initialized.
 */
object PlayerBridgeFactory {
    private var factoryRef: PlayerBridgeCreator? = null

    fun registerFactory(creator: PlayerBridgeCreator) {
        this.factoryRef = creator
    }

    fun create(): PlayerBridge? = factoryRef?.createBridge()

    val isRegistered: Boolean get() = factoryRef != null
}

/**
 * Interface for creating bridge instances.
 * Swift implements this to provide the factory.
 */
interface PlayerBridgeCreator {
    fun createBridge(): PlayerBridge
}
