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
 * Explicit, local overrides of watched state, by video id (e.g. `"tt0108778:1:1"`).
 *
 * Simkl's synced library only reports a watched/total count and a next-to-watch marker per
 * show, never a full per-episode list, so the details page derives a default tick state
 * from that (see `DetailPage.kt`'s `simklWatchedIds`). [overrides] is layered on top of
 * that default: a video id present here always wins, in either direction, over the
 * Simkl-derived guess. An id absent here means "no local opinion", not "not watched" — so
 * un-watching an episode Simkl otherwise infers as watched is recorded the same way as
 * watching one Simkl has not caught up to yet.
 *
 * Setting an override is applied here first, so the UI updates immediately, and mirrored
 * to Simkl's history endpoints in the background on a best-effort basis when signed in.
 */
object EpisodeWatchedRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = SimklClient()

    private val _overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val overrides: StateFlow<Map<String, Boolean>> = _overrides.asStateFlow()

    private var store: EpisodeWatchedStore? = null
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { EpisodeWatchedStore(createDatabaseDriver()) }.getOrNull()
        _overrides.value = store
            ?.let { runCatching { it.all() }.getOrNull() }
            .orEmpty()
            .associate { it.videoId to (it.watched != 0L) }
    }

    /**
     * Records that [video] is watched, or not, overriding whatever the Simkl-derived
     * default would otherwise show for it. [showId] is the title's own id (an imdb id for
     * every addon this app talks to), used both as the local grouping key and, when Simkl
     * can be reached, as the id Simkl matches the episode against.
     */
    fun setWatched(showId: String, video: Video, watched: Boolean) {
        val videoId = video.id
        _overrides.value = _overrides.value + (videoId to watched)

        val season = video.season
        val episode = video.episode
        store?.let { s ->
            runCatching {
                s.setWatched(videoId, showId, season ?: 0, episode ?: 0, watched, currentTimeMillis())
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
                    if (watched) {
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
        _overrides.value = emptyMap()
    }
}
