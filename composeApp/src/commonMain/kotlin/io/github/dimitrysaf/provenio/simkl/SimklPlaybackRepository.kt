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
     * The most recent saved pause point for one title, if Simkl has one. Movies and
     * episodes are separate Simkl endpoints, so both are asked and merged; returns null
     * when signed out, unreachable, or the title simply has no paused session.
     */
    suspend fun sessionFor(imdbId: String): SimklPlaybackSession? {
        val token = SimklRepository.accessToken ?: return null
        val sessions = client.playbackSessions(token, "movies").orEmpty() +
            client.playbackSessions(token, "episodes").orEmpty()
        return sessions.firstOrNull { it.media?.ids?.imdb == imdbId }
    }
}
