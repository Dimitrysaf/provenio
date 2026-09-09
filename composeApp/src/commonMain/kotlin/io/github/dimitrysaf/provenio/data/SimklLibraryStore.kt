package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.simkl.SimklEntry

/**
 * What the last sync knew, so the next one can decide whether to run at all.
 *
 * [dataVersion] is what shape of data that sync asked Simkl for, separate from whether it
 * has ever completed at all — see [io.github.dimitrysaf.provenio.simkl.SimklSync] for why
 * this can force a full re-sync on its own.
 */
data class SyncCheckpoint(
    val activitiesAll: String?,
    val lastSyncedAtMillis: Long,
    val initialSyncDone: Boolean,
    val dataVersion: Int = 0,
)

class SimklLibraryStore(driver: SqlDriver) {

    private val database = ProvenioDatabase(driver)
    private val sync = database.simklLibraryQueries
    private val episodes = database.simklWatchedEpisodeQueries

    fun checkpoint(): SyncCheckpoint = sync.selectSync().executeAsOneOrNull()?.let { row ->
        SyncCheckpoint(
            activitiesAll = row.activitiesAll,
            lastSyncedAtMillis = row.lastSyncedAtMillis,
            initialSyncDone = row.initialSyncDone != 0L,
            dataVersion = row.dataVersion.toInt(),
        )
    } ?: SyncCheckpoint(null, 0L, false)

    fun saveCheckpoint(checkpoint: SyncCheckpoint) {
        sync.upsertSync(
            activitiesAll = checkpoint.activitiesAll,
            lastSyncedAtMillis = checkpoint.lastSyncedAtMillis,
            initialSyncDone = if (checkpoint.initialSyncDone) 1L else 0L,
            dataVersion = checkpoint.dataVersion.toLong(),
        )
    }

    fun itemsWithStatus(status: String): List<SimklItem> =
        sync.selectByStatus(status).executeAsList()

    /** The synced library row for one title, regardless of its list status. */
    fun itemByImdbId(imdbId: String): SimklItem? =
        sync.selectByImdbId(imdbId).executeAsOneOrNull()

    /** Every (season, episode) Simkl has recorded as watched for one show. */
    fun watchedEpisodes(simklId: Long): Set<Pair<Int, Int>> =
        episodes.selectForShow(simklId).executeAsList()
            .map { it.season.toInt() to it.episode.toInt() }
            .toSet()

    /**
     * Writes a delta.
     *
     * An entry whose status is gone has been removed from the user's lists, so the row goes
     * with it rather than lingering as a stale shelf card. Each entry's watched-episode rows
     * are replaced wholesale rather than merged, because Simkl's `seasons` array is that
     * show's full current watched list, not new watches since the last sync.
     */
    fun apply(entries: List<SimklEntry>, mediaTypeOf: (SimklEntry) -> String) {
        sync.transaction {
            entries.forEach { entry ->
                val id = entry.media?.ids?.simkl ?: return@forEach
                val status = entry.status
                if (status.isNullOrBlank()) {
                    sync.deleteItem(id)
                    episodes.deleteForShow(id)
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

                episodes.deleteForShow(id)
                entry.seasons.orEmpty().forEach { season ->
                    season.episodes.forEach { episode ->
                        episodes.insert(id, season.number.toLong(), episode.number.toLong())
                    }
                }
            }
        }
    }

    fun clear() {
        sync.clearAll()
        episodes.clearAll()
    }
}
