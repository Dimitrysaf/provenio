package io.github.dimitrysaf.provenio.core.playback

import io.github.dimitrysaf.provenio.core.build.isIos

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
) {
    companion object {
        val DEFAULT = SubtitleStyleState()
    }
}

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
