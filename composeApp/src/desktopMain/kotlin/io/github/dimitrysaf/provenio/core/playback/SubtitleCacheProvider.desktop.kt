package io.github.dimitrysaf.provenio.core.playback

// There is no external player on desktop yet, so nothing needs subtitles copied locally.
actual object SubtitleCacheProvider {
    actual suspend fun cacheForExternalPlayer(subtitles: List<SubtitleInput>): List<SubtitleInput>? = subtitles
}
