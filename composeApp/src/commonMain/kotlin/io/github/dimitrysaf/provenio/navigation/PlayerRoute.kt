package io.github.dimitrysaf.provenio.navigation

import kotlinx.serialization.Serializable

/**
 * Playback of one URL. The player knows nothing else about where it came from — [mediaType]
 * through [resumeProgressPercent] exist for exactly one purpose, reporting progress to
 * Simkl, and stay null whenever the title opening this route has no Simkl-trackable id.
 * [streamId] is the same idea for the local resume point: this stream's own identity, so
 * the player can remember it alongside the position.
 */
@Serializable
data class PlayerRoute(
    val url: String,
    val title: String? = null,
    val videoId: String? = null,
    val episodeTitle: String? = null,
    val mediaType: String? = null,
    val imdbId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val resumeProgressPercent: Float? = null,
    val streamId: String? = null,
)
