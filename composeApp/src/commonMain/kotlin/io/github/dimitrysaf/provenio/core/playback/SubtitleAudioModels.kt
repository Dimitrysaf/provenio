package io.github.dimitrysaf.provenio.core.playback

import io.github.dimitrysaf.provenio.core.build.isIos
import kotlin.math.roundToInt

data class AudioTrack(
    val index: Int,
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false,
)

data class SubtitleTrack(
    val index: Int,
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val isForced: Boolean = false,
)

data class AddonSubtitle(
    val id: String,
    val url: String,
    val language: String,
    val display: String,
    val addonName: String? = null,
    val isSelected: Boolean = false,
)

const val SUBTITLE_DELAY_MIN_MS = -60_000

const val SUBTITLE_DELAY_MAX_MS = 60_000

data class SubtitleSyncCue(
    val startTimeMs: Long,
    val endTimeMs: Long = startTimeMs + 5_000L,
    val text: String,
)

internal val subtitleFontSizeRangeSp: IntRange
    get() = if (isIos) 6..40 else 12..40

data class SubtitleStyleState(
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0x00000000L,
    val outlineColor: Long = 0xFF000000,
    val outlineEnabled: Boolean = true,
    val outlineWidth: Int = 2,
    val bold: Boolean = false,
    val fontSizeSp: Int = 18,
    val bottomOffset: Int = 20,
    val stripSdh: Boolean = false,
    val useForcedSubtitles: Boolean = false,
    val showOnlyPreferredLanguages: Boolean = false,
    val fontFamily: SubtitleFontFamily = SubtitleFontFamily.Default,
    val shadowEnabled: Boolean = false,
    val shadowColor: Long = 0xCC000000,
    /** How far the shadow falls from the text, in fortieths of the text's size. */
    val shadowDistance: Int = 3,
    /**
     * Which way the shadow falls, in degrees clockwise from straight right, so 90 is straight
     * down; 45, down and to the right, is where most players put it.
     */
    val shadowAngle: Int = 45,
    /** How soft the shadow's edge is, from 0, sharp, to the top of [SUBTITLE_SHADOW_SOFTNESS_RANGE]. */
    val shadowSoftness: Int = 2,
) {
    companion object {
        val DEFAULT = SubtitleStyleState()
    }
}

/** The kind of typeface subtitles are set in; each player picks its own font of that kind. */
enum class SubtitleFontFamily {
    Default,
    Serif,
    Monospace,
    Condensed,
}

val SUBTITLE_SHADOW_DISTANCE_RANGE: IntRange = 1..10
val SUBTITLE_SHADOW_SOFTNESS_RANGE: IntRange = 0..10
const val SUBTITLE_SHADOW_ANGLE_STEP = 15

/** [degrees] as an angle the shadow can take: within a turn, on a step of [SUBTITLE_SHADOW_ANGLE_STEP]. */
fun normalizeSubtitleShadowAngle(degrees: Int): Int =
    ((degrees.toFloat() / SUBTITLE_SHADOW_ANGLE_STEP).roundToInt() * SUBTITLE_SHADOW_ANGLE_STEP).mod(360)

/** How far the shadow falls, as a share of the text's size. */
val SubtitleStyleState.shadowOffsetFraction: Float
    get() = shadowDistance.coerceIn(SUBTITLE_SHADOW_DISTANCE_RANGE) / 40f

/** How far the shadow's edge is blurred, as a share of the text's size. */
val SubtitleStyleState.shadowBlurFraction: Float
    get() = shadowSoftness.coerceIn(SUBTITLE_SHADOW_SOFTNESS_RANGE) * 0.015f

// Subtitle colours are stored and passed around as ARGB values, never as Compose colours.
fun Long.toStorageHexString(): String =
    "#" + (this and 0xFFFFFFFFL).toString(16).padStart(8, '0').uppercase()

fun subtitleColorFromStorage(value: String?): Long? {
    val normalized = value
        ?.trim()
        ?.removePrefix("#")
        ?.takeIf { it.length == 6 || it.length == 8 }
        ?: return null
    val argb = if (normalized.length == 6) "FF$normalized" else normalized
    return argb.toLongOrNull(16)
}
