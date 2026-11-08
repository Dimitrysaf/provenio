package io.github.dimitrysaf.provenio.core.metadata

internal fun MetaTrailer.youtubePlaybackUrl(): String =
    key.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        ?: "https://www.youtube.com/watch?v=$key"

/** The trailer's YouTube still, for keys that are plain video ids rather than full URLs. */
internal fun MetaTrailer.youtubeThumbnailUrl(): String? =
    key.takeIf { it.isNotBlank() && !it.startsWith("http") }
        ?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
