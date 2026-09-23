package com.nuvio.app.core.playback

internal data class SubtitleLoadingProgress(
    val total: Int,
    val completed: Int = 0,
    val addonName: String? = null,
)
