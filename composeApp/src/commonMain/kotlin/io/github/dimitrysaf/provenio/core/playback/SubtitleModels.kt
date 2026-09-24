package io.github.dimitrysaf.provenio.core.playback

internal data class SubtitleLoadingProgress(
    val total: Int,
    val completed: Int = 0,
    val addonName: String? = null,
)
