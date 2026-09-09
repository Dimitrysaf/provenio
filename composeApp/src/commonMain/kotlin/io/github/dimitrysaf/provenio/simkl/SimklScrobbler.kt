package io.github.dimitrysaf.provenio.simkl

import io.github.dimitrysaf.provenio.player.ScrobbleTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reports playback progress to Simkl's Scrobble API, so a title watched here shows up as
 * in-progress (or watched, past 80%) on Simkl and can be resumed from any of Simkl's other
 * connected apps. Every call is fire-and-forget: playback must never wait on, or be
 * disrupted by, a request to Simkl.
 */
object SimklScrobbler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = SimklClient()

    fun start(target: ScrobbleTarget, progressPercent: Float) =
        send(target, progressPercent, client::scrobbleStart)

    fun pause(target: ScrobbleTarget, progressPercent: Float) =
        send(target, progressPercent, client::scrobblePause)

    fun stop(target: ScrobbleTarget, progressPercent: Float) =
        send(target, progressPercent, client::scrobbleStop)

    private inline fun send(
        target: ScrobbleTarget,
        progressPercent: Float,
        crossinline call: suspend (String, SimklScrobbleRequest) -> SimklScrobbleResponse?,
    ) {
        val token = SimklRepository.accessToken ?: return
        val request = target.toScrobbleRequest(progressPercent) ?: return
        scope.launch { runCatching { call(token, request) } }
    }

    /**
     * Null for a series episode with no season or episode number — a special, or a thin
     * addon listing — since Simkl has nothing to match a scrobble against without one.
     */
    private fun ScrobbleTarget.toScrobbleRequest(progressPercent: Float): SimklScrobbleRequest? {
        val progress = progressPercent.coerceIn(0f, 100f)
        return if (mediaType == "movie") {
            movieScrobbleRequest(imdbId, progress)
        } else {
            val season = season ?: return null
            val episode = episode ?: return null
            singleEpisodeScrobbleRequest(imdbId, season, episode, progress)
        }
    }
}
