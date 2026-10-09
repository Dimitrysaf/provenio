package io.github.dimitrysaf.provenio.core.watch.watching.sync


data class ProgressSyncRecord(
    val contentId: String,
    val contentType: String,
    val videoId: String,
    val season: Int? = null,
    val episode: Int? = null,
    val position: Long = 0L,
    val duration: Long = 0L,
    val lastWatched: Long = 0L,
    val progressKey: String = "",
)

data class ProgressDeltaEvent(
    val eventId: Long,
    val operation: String,
    val progressKey: String,
    val contentId: String = "",
    val contentType: String = "",
    val videoId: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val position: Long = 0L,
    val duration: Long = 0L,
    val lastWatched: Long = 0L,
)

