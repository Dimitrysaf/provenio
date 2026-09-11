package io.github.dimitrysaf.provenio.navigation

import kotlinx.serialization.Serializable

/**
 * Playback of one URL. The player knows nothing else about where it came from — [mediaType]
 * through [resumeProgressPercent] exist for exactly one purpose, reporting progress to
 * Simkl, and stay null whenever the title opening this route has no Simkl-trackable id.
 */
@Serializable
data class PlayerRoute(
    val url: String,
    val title: String? = null,
    val episodeTitle: String? = null,
    val mediaType: String? = null,
    val imdbId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val resumeProgressPercent: Float? = null,
)
