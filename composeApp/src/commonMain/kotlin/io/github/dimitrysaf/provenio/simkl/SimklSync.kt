package io.github.dimitrysaf.provenio.simkl

import io.github.dimitrysaf.provenio.data.SimklLibraryStore
import io.github.dimitrysaf.provenio.data.SyncCheckpoint
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import io.github.dimitrysaf.provenio.db.SimklItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Why a sync was asked for. Manual runs skip the throttle, automatic ones do not. */
enum class SyncTrigger { Startup, Manual }

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data class Failed(val message: String) : SyncState
}

/**
 * Simkl library sync.
 *
 * Written to Simkl's stated rules, which are enforcement rather than advice: breaking them
 * gets the client id suspended.
 *
 *  - `/sync/activities` is fetched first, always, and the sync stops there when nothing has
 *    moved since the saved timestamp.
 *  - The first sync calls `/sync/shows`, `/sync/movies` and `/sync/anime` one after another.
 *    Deliberately sequential. Fanning these out with `async` would be the natural Kotlin
 *    shape and is exactly what Simkl asks callers not to do.
 *  - Every later sync is a single `/sync/all-items?date_from=` carrying the saved timestamp
 *    unchanged.
 *  - Nothing polls. Sync happens on startup or when the user asks, throttled by
 *    [ThrottleMillis], and there is no timer anywhere in this file.
 */
object SimklSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = SimklClient()

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val _watching = MutableStateFlow<List<SimklItem>>(emptyList())
    val watching: StateFlow<List<SimklItem>> = _watching.asStateFlow()

    private val _planToWatch = MutableStateFlow<List<SimklItem>>(emptyList())
    val planToWatch: StateFlow<List<SimklItem>> = _planToWatch.asStateFlow()

    private var store: SimklLibraryStore? = null
    private var loaded = false
    private var job: Job? = null

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { SimklLibraryStore(createDatabaseDriver()) }.getOrNull()
        publish()
    }

    fun sync(trigger: SyncTrigger, nowMillis: Long) {
        val token = SimklRepository.accessToken ?: return
        if (job?.isActive == true) return

        val checkpoint = store?.checkpoint() ?: return
        // Rapid app switching must not turn into a request per switch.
        if (trigger == SyncTrigger.Startup &&
            nowMillis - checkpoint.lastSyncedAtMillis < ThrottleMillis
        ) {
            return
        }

        job = scope.launch { run(token, checkpoint, nowMillis) }
    }

    private suspend fun run(token: String, checkpoint: SyncCheckpoint, nowMillis: Long) {
        _state.value = SyncState.Running

        val activities = client.activities(token)
        if (activities == null) {
            _state.value = SyncState.Failed("Could not reach Simkl.")
            return
        }

        val latest = activities.all
        // Nothing moved. This is the branch that keeps the daily quota near idle.
        if (latest != null && latest == checkpoint.activitiesAll && checkpoint.initialSyncDone) {
            store?.saveCheckpoint(checkpoint.copy(lastSyncedAtMillis = nowMillis))
            _state.value = SyncState.Idle
            return
        }

        val savedDate = checkpoint.activitiesAll
        val ok = if (!checkpoint.initialSyncDone || savedDate == null) {
            initialSync(token)
        } else {
            deltaSync(token, savedDate)
        }

        if (!ok) {
            _state.value = SyncState.Failed("Sync did not finish.")
            return
        }

        store?.saveCheckpoint(
            SyncCheckpoint(
                activitiesAll = latest,
                lastSyncedAtMillis = nowMillis,
                initialSyncDone = true,
            ),
        )
        publish()
        _state.value = SyncState.Idle
    }

    /** Phase 1. One library at a time, never in parallel. */
    private suspend fun initialSync(token: String): Boolean {
        var any = false
        for (type in InitialLibraries) {
            val page = client.library(token, type) ?: continue
            store?.apply(page.all()) { entry -> mediaTypeFor(type, entry) }
            any = true
        }
        return any
    }

    /** Phase 2. One request, delta only, timestamp passed back untouched. */
    private suspend fun deltaSync(token: String, dateFrom: String): Boolean {
        val page = client.changesSince(token, dateFrom) ?: return false
        store?.apply(page.shows) { "shows" }
        store?.apply(page.movies) { "movies" }
        store?.apply(page.anime) { "anime" }
        return true
    }

    private fun mediaTypeFor(library: String, entry: SimklEntry): String =
        if (library == "all") {
            if (entry.isMovie) "movies" else "shows"
        } else {
            library
        }

    private fun publish() {
        val current = store ?: return
        _watching.value = runCatching {
            current.itemsWithStatus(SimklStatus.Watching)
        }.getOrDefault(emptyList())
        _planToWatch.value = runCatching {
            current.itemsWithStatus(SimklStatus.PlanToWatch)
        }.getOrDefault(emptyList())
    }

    fun clear() {
        job?.cancel()
        store?.let { runCatching { it.clear() } }
        store?.saveCheckpoint(SyncCheckpoint(null, 0L, false))
        publish()
        _state.value = SyncState.Idle
    }

    /** Simkl asks for 15 to 30 minutes between automatic checks. */
    private const val ThrottleMillis = 20L * 60L * 1000L

    private val InitialLibraries = listOf("shows", "movies", "anime")
}
