package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.simkl.SimklEntry

/** What the last sync knew, so the next one can decide whether to run at all. */
data class SyncCheckpoint(
    val activitiesAll: String?,
    val lastSyncedAtMillis: Long,
    val initialSyncDone: Boolean,
)

class SimklLibraryStore(driver: SqlDriver) {

    private val database = ProvenioDatabase(driver)
    private val sync = database.simklLibraryQueries

    fun checkpoint(): SyncCheckpoint = sync.selectSync().executeAsOneOrNull()?.let { row ->
        SyncCheckpoint(
            activitiesAll = row.activitiesAll,
            lastSyncedAtMillis = row.lastSyncedAtMillis,
            initialSyncDone = row.initialSyncDone != 0L,
        )
    } ?: SyncCheckpoint(null, 0L, false)

    fun saveCheckpoint(checkpoint: SyncCheckpoint) {
        sync.upsertSync(
            activitiesAll = checkpoint.activitiesAll,
            lastSyncedAtMillis = checkpoint.lastSyncedAtMillis,
            initialSyncDone = if (checkpoint.initialSyncDone) 1L else 0L,
        )
    }

    fun itemsWithStatus(status: String): List<SimklItem> =
        sync.selectByStatus(status).executeAsList()

    /**
     * Writes a delta.
     *
     * An entry whose status is gone has been removed from the user's lists, so the row goes
     * with it rather than lingering as a stale shelf card.
     */
    fun apply(entries: List<SimklEntry>, mediaTypeOf: (SimklEntry) -> String) {
        sync.transaction {
            entries.forEach { entry ->
                val id = entry.media?.ids?.simkl ?: return@forEach
                val status = entry.status
                if (status.isNullOrBlank()) {
                    sync.deleteItem(id)
                    return@forEach
                }
                sync.upsertItem(
                    simklId = id,
                    mediaType = mediaTypeOf(entry),
                    status = status,
                    title = entry.media?.title.orEmpty(),
                    year = entry.media?.year?.toLong(),
                    poster = entry.media?.poster,
                    imdbId = entry.media?.ids?.imdb,
                    watchedEpisodes = entry.watchedEpisodes.toLong(),
                    totalEpisodes = entry.totalEpisodes.toLong(),
                    lastWatchedAt = entry.lastWatchedAt,
                    nextToWatch = entry.nextToWatch,
                )
            }
        }
    }

    fun clear() = sync.clearAll()
}
