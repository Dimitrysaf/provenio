package io.github.dimitrysaf.provenio.player

import io.github.dimitrysaf.provenio.core.platform.currentTimeMillis
import io.github.dimitrysaf.provenio.data.PlaybackPositionStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How far into a title playback had got, and how long the title runs. */
data class PlaybackPosition(
    val positionMillis: Long,
    val durationMillis: Long,
    /** When this was last written, so the most recent thing watched can be found. */
    val updatedAtMillis: Long = 0,
    /**
     * The source that was playing — a torrent's info hash, or the playable url itself for
     * anything else, matching [io.github.dimitrysaf.provenio.stremio.SourceOption.streamId].
     * Null for a position saved before this existed, which reads as "no remembered source"
     * and falls back to asking the viewer to pick one.
     */
    val streamId: String? = null,
) {
    /** 0..1, or null when the duration is not known and a fraction would be a guess. */
    val fraction: Float?
        get() = if (durationMillis > 0) {
            (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        } else {
            null
        }
}

/**
 * Resume points, kept locally.
 *
 * The player writes here as it plays; the details page reads it to draw a bar under what
 * is part way through and to start "Watch now" where it was left rather than at zero.
 *
 * Two thresholds decide what is worth keeping. Below [MinimumMillis] nothing has really
 * been watched, so nothing is written — an accidental open should not put a bar on a
 * title. Past [FinishedFraction] the title is over and the row is removed: there is
 * nothing left to resume and a bar pinned at the end is noise.
 */
object PlaybackPositionRepository {

    private var store: PlaybackPositionStore? = null

    private val _positions = MutableStateFlow<Map<String, PlaybackPosition>>(emptyMap())
    val positions: StateFlow<Map<String, PlaybackPosition>> = _positions.asStateFlow()

    fun load() {
        if (store != null) return
        store = runCatching { PlaybackPositionStore(createDatabaseDriver()) }.getOrNull()
        refresh()
    }

    /** What to resume [videoId] at, or null when it has no position worth resuming. */
    fun positionFor(videoId: String?): PlaybackPosition? =
        videoId?.let { _positions.value[it] }

    /**
     * Records progress. Called often while playing, so it writes the same row over and
     * over by design — the cost is one indexed upsert and the benefit is that a kill,
     * a crash or a battery dying all leave a usable resume point.
     *
     * [streamId] should be whatever the caller currently knows the source to be, every
     * time — this replaces the whole row, so a call that omits it wipes out a source
     * remembered by an earlier call for the same video.
     */
    fun save(
        videoId: String?,
        positionMillis: Long,
        durationMillis: Long,
        streamId: String? = null,
    ) {
        val id = videoId?.takeIf { it.isNotBlank() } ?: return
        val current = store ?: return
        if (durationMillis <= 0) return

        if (positionMillis >= durationMillis * FinishedFraction) {
            runCatching { current.delete(id) }
            _positions.value = _positions.value - id
            return
        }

        // Too early to be worth keeping, but not a reason to throw away what is already
        // there: a source switch restarts at zero for a few seconds and the old resume
        // point is exactly what the new source is about to be seeked to.
        if (positionMillis < MinimumMillis) return

        val now = currentTimeMillis()
        runCatching { current.save(id, positionMillis, durationMillis, now, streamId) }
        _positions.value = _positions.value +
            (id to PlaybackPosition(positionMillis, durationMillis, now, streamId))
    }

    fun clear() {
        runCatching { store?.clear() }
        _positions.value = emptyMap()
    }

    private fun refresh() {
        val rows = runCatching { store?.all() }.getOrNull().orEmpty()
        _positions.value = rows.associate { row ->
            row.videoId to PlaybackPosition(
                positionMillis = row.positionMillis,
                durationMillis = row.durationMillis,
                updatedAtMillis = row.updatedAtMillis,
                streamId = row.streamId,
            )
        }
    }

    /** Under half a minute in, nothing has been watched worth coming back to. */
    private const val MinimumMillis = 30_000L

    /** Past this much, the title is finished rather than part way through. */
    private const val FinishedFraction = 0.97f
}
