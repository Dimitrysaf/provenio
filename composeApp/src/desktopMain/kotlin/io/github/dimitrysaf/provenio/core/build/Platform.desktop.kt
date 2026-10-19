package io.github.dimitrysaf.provenio.core.build

internal actual val isIos: Boolean = false
internal actual val supportsPosterNavigationMotion: Boolean = true
internal actual val isDesktop: Boolean = true
internal actual val isMacOs: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
internal actual val supportsVolumeStabilizer: Boolean = true
internal actual val volumeStabilizerNeedsMpv: Boolean = false
internal actual val supportsSubtitleAutoSync: Boolean = true
