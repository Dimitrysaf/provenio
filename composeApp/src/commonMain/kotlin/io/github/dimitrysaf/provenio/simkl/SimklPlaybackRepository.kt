package io.github.dimitrysaf.provenio.simkl

/**
 * Live lookups against Simkl's saved playback sessions — the paused, resumable points
 * scrobbling creates. Unlike [SimklSync], nothing here is cached locally: a playback
 * session is inherently short-lived (Simkl clears it the moment the title is watched
 * elsewhere or resumed), so a stale local copy would be actively misleading rather than
 * merely out of date.
 */
object SimklPlaybackRepository {

    private val client = SimklClient()

    /**
     * Every saved pause point, newest first — exactly what Simkl's own docs describe this
     * endpoint for: "Continue Watching" lists. Movies and episodes are separate Simkl
     * endpoints, so both are asked and merged; empty when signed out or unreachable.
     */
    suspend fun continueWatching(): List<SimklPlaybackSession> {
        val token = SimklRepository.accessToken ?: return emptyList()
        return client.playbackSessions(token, "movies").orEmpty() +
            client.playbackSessions(token, "episodes").orEmpty()
    }

    /**
     * The most recent saved pause point for one title, if Simkl has one. Returns null
     * when signed out, unreachable, or the title simply has no paused session.
     */
    suspend fun sessionFor(imdbId: String): SimklPlaybackSession? =
        continueWatching().firstOrNull { it.media?.ids?.imdb == imdbId }
}
