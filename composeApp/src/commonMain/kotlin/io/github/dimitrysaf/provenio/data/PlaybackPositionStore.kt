package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.PlaybackPosition
import io.github.dimitrysaf.provenio.db.ProvenioDatabase

/**
 * Where each title was left off, written by the player as it plays.
 *
 * Distinct from Simkl's own notion of a resume point: that one needs a sign-in, covers
 * only titles Simkl matched, and updates when a sync runs. This one is local, immediate,
 * and works for anything that can be played.
 */
class PlaybackPositionStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).playbackPositionQueries

    fun all(): List<PlaybackPosition> = queries.selectAll().executeAsList()

    fun save(
        videoId: String,
        positionMillis: Long,
        durationMillis: Long,
        nowMillis: Long,
        streamId: String? = null,
    ) {
        queries.save(
            videoId = videoId,
            positionMillis = positionMillis,
            durationMillis = durationMillis,
            updatedAtMillis = nowMillis,
            streamId = streamId,
        )
    }

    fun delete(videoId: String) = queries.delete(videoId)

    fun clear() = queries.clearAll()
}
