package io.github.dimitrysaf.provenio.core.build

internal expect val isIos: Boolean

internal expect val supportsPosterNavigationMotion: Boolean

internal expect val isDesktop: Boolean

internal expect val isMacOs: Boolean

/** Whether the player can run the volume stabilizer at all. */
internal expect val supportsVolumeStabilizer: Boolean

/** Whether only the mpv engine can run the volume stabilizer here, as on Android before 9. */
internal expect val volumeStabilizerNeedsMpv: Boolean

/** Whether the player can listen to the sound to sync subtitles to the speech. */
internal expect val supportsSubtitleAutoSync: Boolean

/** Whether the player can set subtitles in another kind of font. */
internal expect val supportsSubtitleFonts: Boolean

/** Whether the player can draw a shadow under subtitles. */
internal expect val supportsSubtitleShadow: Boolean

/** Whether that shadow can fall any way, rather than only down and to the right. */
internal expect val supportsSubtitleShadowAngle: Boolean

/** Whether that shadow's edge can be softened. */
internal expect val supportsSubtitleShadowSoftness: Boolean
