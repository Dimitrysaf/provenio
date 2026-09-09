package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.EpisodeWatched
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

/**
 * Local, per-episode overrides of watched state.
 *
 * This exists because Simkl's synced library ([SimklLibraryStore]) only carries a
 * watched/total count and a next-to-watch marker per show, not which individual episodes
 * those are. The episode tick boxes in the details page infer a default from that, and a
 * row here overrides the inference in either direction; there is no row for "no opinion".
 */
class EpisodeWatchedStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).episodeWatchedQueries

    fun all(): List<EpisodeWatched> = queries.selectAll().executeAsList()

    fun setWatched(
        videoId: String,
        showId: String,
        season: Int,
        episode: Int,
        watched: Boolean,
        nowMillis: Long,
    ) {
        queries.setWatched(
            videoId = videoId,
            showId = showId,
            season = season.toLong(),
            episode = episode.toLong(),
            watchedAtMillis = nowMillis,
            watched = if (watched) 1L else 0L,
        )
    }

    fun clear() = queries.clearAll()
}
