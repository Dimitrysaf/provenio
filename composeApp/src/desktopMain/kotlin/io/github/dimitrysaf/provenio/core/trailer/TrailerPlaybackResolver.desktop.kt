package io.github.dimitrysaf.provenio.core.trailer

actual object TrailerPlaybackResolver {
    actual suspend fun resolveFromYouTubeUrl(youtubeUrl: String): TrailerPlaybackSource? = null
}