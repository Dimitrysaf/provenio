package io.github.dimitrysaf.provenio.core.trailer

expect object TrailerPlaybackResolver {
    suspend fun resolveFromYouTubeUrl(youtubeUrl: String): TrailerPlaybackSource?
}
