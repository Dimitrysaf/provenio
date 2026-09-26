package io.github.dimitrysaf.provenio.core.trailer

private val YouTubeVideoUrl = Regex(
    "^https?://(?:(?:www|m|music)\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|live/)|youtu\\.be/)[A-Za-z0-9_-]{6,}.*",
    RegexOption.IGNORE_CASE,
)

// A YouTube page link, which the player resolves to the video's own streams before playing.
fun isYouTubeVideoUrl(url: String?): Boolean = url != null && YouTubeVideoUrl.matches(url.trim())

fun youTubeWatchUrl(videoId: String): String = "https://www.youtube.com/watch?v=${videoId.trim()}"
