package io.github.dimitrysaf.provenio.core.build

internal actual val isIos: Boolean = false
internal actual val supportsPosterNavigationMotion: Boolean = true
internal actual val isDesktop: Boolean = true
internal actual val isMacOs: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
internal actual val supportsVolumeStabilizer: Boolean = true
internal actual val volumeStabilizerNeedsMpv: Boolean = false
internal actual val supportsSubtitleAutoSync: Boolean = true

internal actual val supportsSubtitleFonts: Boolean = true

internal actual val supportsSubtitleShadow: Boolean = true

// Plain-text subtitles are drawn by Provenio over mpv's picture, so the shadow can do all it can.
internal actual val supportsSubtitleShadowAngle: Boolean = true

internal actual val supportsSubtitleShadowSoftness: Boolean = true
