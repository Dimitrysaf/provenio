package com.nuvio.app.core.playback

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
