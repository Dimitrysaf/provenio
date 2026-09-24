package io.github.dimitrysaf.provenio.core.trailer

actual object TrailerPlaybackResolver {
    private val extractor by lazy { InAppYouTubeExtractor() }

    actual suspend fun resolveFromYouTubeUrl(youtubeUrl: String): TrailerPlaybackSource? {
        if (youtubeUrl.isBlank()) return null
        return extractor.extractPlaybackSource(youtubeUrl)
    }
}