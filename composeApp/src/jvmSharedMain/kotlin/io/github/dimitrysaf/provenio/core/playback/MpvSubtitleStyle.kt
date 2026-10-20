package io.github.dimitrysaf.provenio.core.playback

import kotlin.math.roundToInt

/**
 * The mpv properties that draw plain-text subtitles in this style, as name and value, for any
 * mpv: libmpv on Android and the desktop player alike. Styled ASS subtitles keep their own looks.
 * Provenio draws plain-text subtitles itself where it can, see [mpvAppDrawnSubtitleText]; these
 * are for when mpv has to, such as on an mpv too old to say what a track holds.
 *
 * [font] is the family to set them in, or null to keep mpv's own.
 *
 * mpv draws a shadow only down and to the right, with a sharp edge, so the shadow's angle and
 * softness have no say here. In its outline mode the shadow takes the back colour, which has no
 * other use there, so that colour carries the shadow's when one is on. A background box without
 * an outline is its other mode, which has no shadow, as before.
 */
fun SubtitleStyleState.mpvSubtitleProperties(font: String?): List<Pair<String, String>> {
    val fontSize = mpvFontSize()
    val borderStyle = mpvBorderStyle()
    val shadowShown = shadowEnabled && borderStyle == MpvOutlineAndShadow
    return buildList {
        add("sub-ass-override" to "no")
        add("sub-color" to textColor.toMpvColor())
        add("sub-back-color" to (if (shadowShown) shadowColor else backgroundColor).toMpvColor())
        add("sub-outline-color" to outlineColor.toMpvColor())
        add("sub-border-color" to outlineColor.toMpvColor())
        add("sub-border-style" to borderStyle)
        add("sub-bold" to if (bold) "yes" else "no")
        add("sub-font-size" to fontSize.toString())
        add("sub-outline-size" to mpvOutlineSize().toString())
        add("sub-border-size" to mpvOutlineSize().toString())
        add("sub-shadow-offset" to if (shadowShown) (fontSize * shadowOffsetFraction).roundToInt().coerceAtLeast(1).toString() else "0")
        add("sub-pos" to (100 - bottomOffset / 10).coerceIn(0, 100).toString())
        add("sub-filter-sdh" to if (stripSdh) "yes" else "no")
        add("sub-filter-sdh-harder" to if (stripSdh) "yes" else "no")
        if (font != null) add("sub-font" to font)
    }
}

private fun Long.toMpvColor(): String {
    val argb = this and 0xFFFFFFFFL
    val alpha = (argb ushr 24) and 0xFF
    val red = (argb ushr 16) and 0xFF
    val green = (argb ushr 8) and 0xFF
    val blue = argb and 0xFF
    return "#%02X%02X%02X%02X".format(alpha, red, green, blue)
}

private fun SubtitleStyleState.mpvFontSize(): Int =
    (fontSizeSp * MpvFontSizeScale).toInt().coerceIn(MpvFontSizeMin, MpvFontSizeMax)

private fun SubtitleStyleState.mpvOutlineSize(): Int =
    if (!outlineEnabled) 0 else (outlineWidth * MpvOutlineSizeScale).toInt().coerceAtLeast(1)

private fun SubtitleStyleState.mpvBorderStyle(): String =
    if (!outlineEnabled && ((backgroundColor ushr 24) and 0xFF) > 0L) {
        "opaque-box"
    } else {
        MpvOutlineAndShadow
    }

private const val MpvOutlineAndShadow = "outline-and-shadow"
private const val MpvFontSizeScale = 55.0 / 18.0
private const val MpvFontSizeMin = 36
private const val MpvFontSizeMax = 122
private const val MpvOutlineSizeScale = 1.5

/**
 * The line mpv's plain-text subtitle shows now, for Provenio to draw in the player's own layer,
 * where every part of the style applies; mpv can only put a sharp shadow down and to the right.
 * [readProperty] reads one of mpv's properties, and [setVisible] shows or hides mpv's own drawing
 * of them, which it is told only when that changes, through [state].
 *
 * Styled ASS and picture subtitles stay mpv's, so this is null for them and mpv draws them.
 */
fun mpvAppDrawnSubtitleText(
    state: MpvAppDrawnSubtitleState,
    readProperty: (String) -> String?,
    setVisible: (Boolean) -> Unit,
): String? {
    val codec = readProperty("current-tracks/sub/codec")?.lowercase()
    val drawnHere = codec != null && codec in MpvPlainTextSubtitleCodecs
    if (drawnHere != state.drawnHere) {
        state.drawnHere = drawnHere
        setVisible(!drawnHere)
    }
    if (!drawnHere) return null
    return readProperty("sub-text/ass") ?: readProperty("sub-text").orEmpty()
}

/** Whether Provenio last drew mpv's subtitles itself, so mpv's own drawing is hidden. */
class MpvAppDrawnSubtitleState {
    @Volatile
    var drawnHere: Boolean = false
}

/** The codecs mpv reports for subtitles that are only text, with no looks of their own. */
private val MpvPlainTextSubtitleCodecs = setOf(
    "subrip",
    "srt",
    "webvtt",
    "vtt",
    "mov_text",
    "text",
    "microdvd",
    "mpl2",
    "subviewer",
    "subviewer1",
    "vplayer",
    "realtext",
    "sami",
    "pjs",
    "jacosub",
    "stl",
)
