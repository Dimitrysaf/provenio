package io.github.dimitrysaf.provenio.core.playback

/**
 * The font the desktop player sets subtitles in for [SubtitleFontFamily], which the settings
 * preview shows too. The default is mpv's own, so going back to it undoes another choice. Linux
 * asks fontconfig by kind, which always finds something; Windows has no such names, so each kind
 * is a font that comes with it.
 */
internal fun SubtitleFontFamily.desktopSubtitleFont(): String {
    val windows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
    return when (this) {
        SubtitleFontFamily.Default -> "sans-serif"
        SubtitleFontFamily.Serif -> if (windows) "Georgia" else "serif"
        SubtitleFontFamily.Monospace -> if (windows) "Consolas" else "monospace"
        SubtitleFontFamily.Condensed -> if (windows) "Arial Narrow" else "DejaVu Sans Condensed"
    }
}
