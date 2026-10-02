package io.github.dimitrysaf.provenio.core.metadata.tmdb

data class TmdbSettings(
    val enabled: Boolean = false,
    val language: String = "en",
    val apiKey: String = "",
    val useTrailers: Boolean = true,
    val useArtwork: Boolean = true,
    /**
     * An addon's own poster and main artwork stay first; TMDB's artwork is added after them rather
     * than replacing them.
     */
    val preferAddonArtwork: Boolean = false,
    val useBasicInfo: Boolean = true,
    val useDetails: Boolean = true,
    val useReleaseDates: Boolean = false,
    val useCredits: Boolean = true,
    val useProductions: Boolean = true,
    val useNetworks: Boolean = true,
    val useEpisodes: Boolean = true,
    val useSeasonPosters: Boolean = true,
    val useMoreLikeThis: Boolean = true,
    val useCollections: Boolean = true,
)
