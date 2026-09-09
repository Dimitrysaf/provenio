package io.github.dimitrysaf.provenio.watch

import io.github.dimitrysaf.provenio.data.EpisodeWatchedStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import io.github.dimitrysaf.provenio.simkl.SimklClient
import io.github.dimitrysaf.provenio.simkl.SimklRepository
import io.github.dimitrysaf.provenio.simkl.singleEpisodeHistoryRequest
import io.github.dimitrysaf.provenio.stremio.model.Video
import io.github.dimitrysaf.provenio.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Which episodes the user has watched, kept locally by video id (e.g. `"tt0108778:1:1"`).
 *
 * Simkl's synced library only reports a watched/total count per show, never which
 * episodes those are, so the episode tick box in the details page cannot be driven from
 * [io.github.dimitrysaf.provenio.simkl.SimklSync] the way the overall progress bar is.
 * This is the source of truth for that tick box instead. Toggling one is applied here
 * first, so the UI updates immediately, and mirrored to Simkl's history endpoints in the
 * background on a best-effort basis when the user is signed in.
 */
object EpisodeWatchedRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = SimklClient()

    private val _watched = MutableStateFlow<Set<String>>(emptySet())
    val watched: StateFlow<Set<String>> = _watched.asStateFlow()

    private var store: EpisodeWatchedStore? = null
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { EpisodeWatchedStore(createDatabaseDriver()) }.getOrNull()
        _watched.value = store
            ?.let { runCatching { it.all() }.getOrNull() }
            .orEmpty()
            .map { it.videoId }
            .toSet()
    }

    /**
     * Flips [video]'s watched state. [showId] is the title's own id (an imdb id for every
     * addon this app talks to), used both as the local grouping key and, when Simkl can be
     * reached, as the id Simkl matches the episode against.
     */
    fun toggle(showId: String, video: Video) {
        val videoId = video.id
        val nowWatched = videoId !in _watched.value
        _watched.value = if (nowWatched) {
            _watched.value + videoId
        } else {
            _watched.value - videoId
        }

        val season = video.season
        val episode = video.episode
        store?.let { s ->
            runCatching {
                if (nowWatched) {
                    s.markWatched(videoId, showId, season ?: 0, episode ?: 0, currentTimeMillis())
                } else {
                    s.markUnwatched(videoId)
                }
            }
        }

        // Simkl identifies episodes by season and episode number, which specials and a
        // handful of thin addon listings do not carry. Those stay local-only rather than
        // guessing a number that would tick the wrong episode on Simkl.
        val token = SimklRepository.accessToken
        if (token != null && season != null && episode != null) {
            scope.launch {
                val request = singleEpisodeHistoryRequest(showId, season, episode)
                runCatching {
                    if (nowWatched) {
                        client.addToHistory(token, request)
                    } else {
                        client.removeFromHistory(token, request)
                    }
                }
            }
        }
    }

    fun clear() {
        store?.let { runCatching { it.clear() } }
        _watched.value = emptySet()
    }
}
