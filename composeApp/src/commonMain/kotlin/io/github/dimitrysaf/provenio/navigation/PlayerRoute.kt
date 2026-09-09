package io.github.dimitrysaf.provenio.navigation

import kotlinx.serialization.Serializable

/** Playback of one URL. The player knows nothing else about where it came from. */
@Serializable
data class PlayerRoute(val url: String)
