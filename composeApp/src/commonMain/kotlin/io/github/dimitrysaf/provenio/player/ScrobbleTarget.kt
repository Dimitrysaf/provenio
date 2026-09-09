package io.github.dimitrysaf.provenio.player

/**
 * What [PlayerScreen] is playing, for a backend that can report progress to Simkl. Null
 * out of a route entirely rather than filling this with blanks when the title has no
 * Simkl-trackable id, or nothing about where it came from tracks progress at all (an
 * external player, which this app never sees playback events from).
 */
data class ScrobbleTarget(
    /** "movie" or "series", matching the addon protocol's own type strings. */
    val mediaType: String,
    val imdbId: String,
    val season: Int?,
    val episode: Int?,
    /** Where Simkl says this title was left off, 0-100, to seek to once playback starts. */
    val resumeProgressPercent: Float?,
)
