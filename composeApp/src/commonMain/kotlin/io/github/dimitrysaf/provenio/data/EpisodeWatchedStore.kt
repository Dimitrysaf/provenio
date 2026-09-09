package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.EpisodeWatched
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

/**
 * Local record of which episodes have been watched, by video id.
 *
 * This exists because Simkl's synced library ([SimklLibraryStore]) only carries a
 * watched/total count per show, not which individual episodes those are. The episode tick
 * boxes in the details page read and write here directly; syncing the change to Simkl is a
 * separate, best-effort step layered on top.
 */
class EpisodeWatchedStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).episodeWatchedQueries

    fun all(): List<EpisodeWatched> = queries.selectAll().executeAsList()

    fun markWatched(videoId: String, showId: String, season: Int, episode: Int, nowMillis: Long) {
        queries.markWatched(videoId, showId, season.toLong(), episode.toLong(), nowMillis)
    }

    fun markUnwatched(videoId: String) = queries.markUnwatched(videoId)

    fun clear() = queries.clearAll()
}
