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
 *  - The first sync calls `/sync/all-items/shows`, `/sync/all-items/movies` and
 *    `/sync/all-items/anime` one after another. Deliberately sequential. Fanning these out
 *    with `async` would be the natural Kotlin shape and is exactly what Simkl asks callers
 *    not to do.
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

    private val _completed = MutableStateFlow<List<SimklItem>>(emptyList())
    val completed: StateFlow<List<SimklItem>> = _completed.asStateFlow()

    private var store: SimklLibraryStore? = null
    private var loaded = false
    private var job: Job? = null

    /**
     * Runs a database call, returning null if it fails.
     *
     * Storage is a cache here. Losing it should cost the user their shelves until the next
     * sync, never the app.
     */
    private fun <T> withStore(block: (SimklLibraryStore) -> T): T? {
        val current = store ?: return null
        return runCatching { block(current) }.getOrNull()
    }

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { SimklLibraryStore(createDatabaseDriver()) }.getOrNull()
        publish()
    }

    fun sync(trigger: SyncTrigger, nowMillis: Long) {
        val token = SimklRepository.accessToken ?: return
        if (job?.isActive == true) return

        val checkpoint = withStore { it.checkpoint() } ?: return
        // Rapid app switching must not turn into a request per switch — but a stale data
        // version means a past sync never asked Simkl for a field this version needs, and
        // that backfill must not wait out a 20 minute throttle on every launch until it
        // happens to land outside the window.
        val behindDataVersion = checkpoint.dataVersion < CurrentDataVersion
        if (trigger == SyncTrigger.Startup &&
            !behindDataVersion &&
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
        // A stale data version means a prior sync never asked for a field this version
        // needs — the per-episode watched list, currently — so a show nothing changed
        // about would sit forever without it if this were allowed to skip straight to
        // "nothing moved" below, or to a delta sync that only ever mentions shows that
        // did change.
        val behindDataVersion = checkpoint.dataVersion < CurrentDataVersion

        // Nothing moved. This is the branch that keeps the daily quota near idle.
        if (!behindDataVersion &&
            latest != null &&
            latest == checkpoint.activitiesAll &&
            checkpoint.initialSyncDone
        ) {
            withStore { it.saveCheckpoint(checkpoint.copy(lastSyncedAtMillis = nowMillis)) }
            _state.value = SyncState.Idle
            return
        }

        val savedDate = checkpoint.activitiesAll
        val ok = if (!checkpoint.initialSyncDone || savedDate == null || behindDataVersion) {
            initialSync(token)
        } else {
            deltaSync(token, savedDate)
        }

        if (!ok) {
            _state.value = SyncState.Failed("Sync did not finish.")
            return
        }

        withStore {
            it.saveCheckpoint(
                SyncCheckpoint(
                    activitiesAll = latest,
                    lastSyncedAtMillis = nowMillis,
                    initialSyncDone = true,
                    dataVersion = CurrentDataVersion,
                ),
            )
        }
        publish()
        _state.value = SyncState.Idle
    }

    /** Phase 1. One library at a time, never in parallel. */
    private suspend fun initialSync(token: String): Boolean {
        var any = false
        for (type in InitialLibraries) {
            val page = client.library(token, type) ?: continue
            withStore { it.apply(page.all()) { entry -> mediaTypeFor(type, entry) } }
            any = true
        }
        return any
    }

    /** Phase 2. One request, delta only, timestamp passed back untouched. */
    private suspend fun deltaSync(token: String, dateFrom: String): Boolean {
        val page = client.changesSince(token, dateFrom) ?: return false
        withStore { it.apply(page.shows) { "shows" } }
        withStore { it.apply(page.movies) { "movies" } }
        withStore { it.apply(page.anime) { "anime" } }
        return true
    }

    private fun mediaTypeFor(library: String, entry: SimklEntry): String =
        if (library == "all") {
            if (entry.isMovie) "movies" else "shows"
        } else {
            library
        }

    private fun publish() {
        _watching.value = withStore { it.itemsWithStatus(SimklStatus.Watching) }.orEmpty()
        _planToWatch.value = withStore { it.itemsWithStatus(SimklStatus.PlanToWatch) }.orEmpty()
        _completed.value = withStore { it.itemsWithStatus(SimklStatus.Completed) }.orEmpty()
    }

    /**
     * Moves one title between the user's lists, or takes it out of them when [status] is
     * null.
     *
     * The local copy is not edited directly. This table is keyed by Simkl's own id, which
     * a title being added for the first time does not have here yet, so inventing a row
     * would mean inventing a primary key. A successful write is followed by a manual sync
     * instead, and what lands locally is what Simkl actually recorded.
     */
    suspend fun setListStatus(
        imdbId: String,
        isMovie: Boolean,
        status: String?,
        nowMillis: Long,
    ): Boolean {
        val token = SimklRepository.accessToken ?: return false
        val request = singleTitleListRequest(imdbId, isMovie, status)
        val ok = if (status == null) {
            client.removeFromList(token, request)
        } else {
            client.addToList(token, request)
        }
        if (ok) sync(SyncTrigger.Manual, nowMillis)
        return ok
    }

    /** The synced Simkl row for one title, by its imdb id, or null if Simkl has no record. */
    fun progressFor(imdbId: String): SimklItem? = withStore { it.itemByImdbId(imdbId) }

    /** Every (season, episode) Simkl has recorded as watched for one show. */
    fun watchedEpisodesFor(simklId: Long): Set<Pair<Int, Int>> =
        withStore { it.watchedEpisodes(simklId) }.orEmpty()

    fun clear() {
        job?.cancel()
        withStore { it.clear() }
        withStore { it.saveCheckpoint(SyncCheckpoint(null, 0L, false)) }
        publish()
        _state.value = SyncState.Idle
    }

    /** Simkl asks for 15 to 30 minutes between automatic checks. */
    private const val ThrottleMillis = 20L * 60L * 1000L

    private val InitialLibraries = listOf("shows", "movies", "anime")

    /**
     * Bump this when a sync starts asking Simkl for a field it did not before. A stale
     * [SyncCheckpoint.dataVersion] forces one full [initialSync] even if nothing changed
     * on Simkl's side, which is the only way an existing row picks up a field a delta sync
     * would otherwise never send for it. Current history: 1 adds the per-episode watched
     * list (`extended=full` on every sync request).
     */
    private const val CurrentDataVersion = 1
}
