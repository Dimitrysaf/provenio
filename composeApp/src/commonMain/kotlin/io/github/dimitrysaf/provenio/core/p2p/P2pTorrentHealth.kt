package io.github.dimitrysaf.provenio.core.p2p

enum class P2pTorrentHealthLevel {
    SUCCESS,
    INFO,
    WARNING,
    ERROR,
}

enum class P2pTorrentHealthIssue(val level: P2pTorrentHealthLevel, internal val holdMs: Long) {
    NO_PEERS(P2pTorrentHealthLevel.ERROR, 20_000L),
    UNAVAILABLE_PIECES(P2pTorrentHealthLevel.ERROR, 15_000L),
    TOO_SLOW(P2pTorrentHealthLevel.WARNING, 8_000L),
    THROTTLED(P2pTorrentHealthLevel.WARNING, 15_000L),
    UNSTABLE(P2pTorrentHealthLevel.WARNING, 5_000L),
    FEW_SEEDERS(P2pTorrentHealthLevel.WARNING, 10_000L),
    CORRUPT_DATA(P2pTorrentHealthLevel.WARNING, 0L),
    TRACKERS_DOWN(P2pTorrentHealthLevel.INFO, 10_000L),
    GETTING_INFO(P2pTorrentHealthLevel.INFO, 0L),
    STABLE(P2pTorrentHealthLevel.SUCCESS, 5_000L),
}

data class P2pTorrentHealthReport(
    val issue: P2pTorrentHealthIssue,
    val seeders: Int = 0,
    val downloadSpeed: Long = 0L,
    val requiredSpeed: Long? = null,
)

/**
 * Turns the torrent details sampled about once a second into the one status worth telling the
 * user about while the engine is using the torrent, or null while it is not.
 *
 * The torrent is in use while the engine fetches its info or pieces of the streamed file. Once
 * enough is buffered ahead of the playhead, playback is paused or the engine is idle, it stops
 * asking for data on purpose, and nothing is reported.
 */
class P2pTorrentHealthMonitor {
    private class Sample(val atMs: Long, val speed: Long, val fetching: Boolean)

    private var infoHash: String? = null
    private val samples = ArrayDeque<Sample>()
    private val heldSince = mutableMapOf<P2pTorrentHealthIssue, Long>()
    private val hashFailures = ArrayDeque<Pair<Long, Long>>()
    private var shown: P2pTorrentHealthReport? = null
    private var pending: P2pTorrentHealthIssue? = null
    private var hasPending = false
    private var pendingSinceMs = 0L

    fun update(details: P2pTorrentDetails, nowMs: Long): P2pTorrentHealthReport? {
        if (details.infoHash != infoHash) reset(details.infoHash)
        val pieces = details.pieces
        val fetching = pieces != null && (0 until pieces.size).any { index ->
            val state = pieces.state(index)
            state == P2pPieceState.DOWNLOADING || state == P2pPieceState.BLOCKING
        }
        samples.addLast(Sample(nowMs, details.downloadSpeed, fetching))
        while (samples.isNotEmpty() && nowMs - samples.first().atMs > HistoryWindowMs) samples.removeFirst()
        hashFailures.addLast(nowMs to details.hashFailedBytes)
        while (hashFailures.isNotEmpty() && nowMs - hashFailures.first().first > CorruptWindowMs) {
            hashFailures.removeFirst()
        }

        val reports = if (details.isComplete()) emptyList() else detect(details, nowMs)
        val active = reports.map { it.issue }.toSet()
        heldSince.keys.retainAll(active)
        active.forEach { heldSince.getOrPut(it) { nowMs } }
        val candidate = reports
            .filter { nowMs - (heldSince[it.issue] ?: nowMs) >= it.issue.holdMs }
            .minByOrNull { it.issue.ordinal }

        if (candidate?.issue == shown?.issue) {
            shown = candidate
            hasPending = false
            return shown
        }
        if (!hasPending || pending != candidate?.issue) {
            pending = candidate?.issue
            hasPending = true
            pendingSinceMs = nowMs
        }
        if (shown == null || nowMs - pendingSinceMs >= SwitchDelayMs) {
            shown = candidate
            hasPending = false
        }
        return shown
    }

    private fun reset(hash: String) {
        infoHash = hash
        samples.clear()
        heldSince.clear()
        hashFailures.clear()
        shown = null
        hasPending = false
    }

    private fun detect(details: P2pTorrentDetails, nowMs: Long): List<P2pTorrentHealthReport> = buildList {
        val seeders = maxOf(details.connectedSeeds, details.swarmSeeds ?: 0)
        val gettingInfo = !details.hasMetadata || details.state == P2pTorrentState.DOWNLOADING_METADATA
        val inUse = gettingInfo || samples.any { it.fetching && nowMs - it.atMs <= InUseWindowMs }
        if (!inUse) return@buildList
        if (gettingInfo) {
            add(P2pTorrentHealthReport(P2pTorrentHealthIssue.GETTING_INFO))
        }
        if (details.connectedPeers == 0) {
            add(P2pTorrentHealthReport(P2pTorrentHealthIssue.NO_PEERS))
        }
        if (!details.hasMetadata) return@buildList

        val pieces = details.pieces
        if (pieces != null && details.connectedPeers > 0) {
            val unavailable = (0 until pieces.size).any { index ->
                pieces.state(index).isMissing() && pieces.availability(index) == 0
            }
            if (unavailable && (details.availability ?: 0f) < 1f) {
                add(P2pTorrentHealthReport(P2pTorrentHealthIssue.UNAVAILABLE_PIECES))
            }
        }
        if (details.connectedPeers > 0 && seeders <= FewSeeders) {
            add(P2pTorrentHealthReport(P2pTorrentHealthIssue.FEW_SEEDERS, seeders = seeders))
        }

        val waiting = pieces != null && (0 until pieces.size).any { pieces.state(it) == P2pPieceState.BLOCKING }
        val fetchingSamples = samples.filter { it.fetching }
        val recentFetching = fetchingSamples.filter { nowMs - it.atMs <= SpeedWindowMs }
        val averageSpeed = recentFetching.takeIf { it.isNotEmpty() }?.map { it.speed }?.average()?.toLong() ?: 0L
        val requiredSpeed = details.requiredSpeed()
        val tooSlowForVideo = requiredSpeed != null && recentFetching.size >= MinSpeedSamples &&
            averageSpeed < requiredSpeed
        if (waiting && (requiredSpeed == null || tooSlowForVideo)) {
            add(
                P2pTorrentHealthReport(
                    P2pTorrentHealthIssue.TOO_SLOW,
                    downloadSpeed = averageSpeed,
                    requiredSpeed = requiredSpeed,
                ),
            )
        }

        val sending = details.peers.filter { it.isInteresting && !it.isChokingUs && !it.isConnecting }
        val stalled = sending.count { it.isSnubbed }
        if (recentFetching.size >= MinSpeedSamples && sending.size >= ThrottlePeers &&
            stalled * 2 >= sending.size && averageSpeed < ThrottleSpeed
        ) {
            add(P2pTorrentHealthReport(P2pTorrentHealthIssue.THROTTLED, downloadSpeed = averageSpeed))
        }

        if (fetchingSamples.size >= MinUnstableSamples) {
            val peak = fetchingSamples.maxOf { it.speed }
            val drops = fetchingSamples.count { it.speed < peak / 10 }
            if (peak >= UnstablePeak && drops * 10 >= fetchingSamples.size * 3) {
                add(P2pTorrentHealthReport(P2pTorrentHealthIssue.UNSTABLE))
            }
        }

        val failedBytes = hashFailures.last().second - hashFailures.first().second
        val pieceLength = details.pieceLength.toLong().coerceAtLeast(1L)
        if (failedBytes >= pieceLength * CorruptPieces) {
            add(P2pTorrentHealthReport(P2pTorrentHealthIssue.CORRUPT_DATA))
        }

        if (details.trackers.isNotEmpty() && details.trackers.all { it.status == P2pTrackerStatus.ERROR }) {
            add(P2pTorrentHealthReport(P2pTorrentHealthIssue.TRACKERS_DOWN))
        }

        val keepsUp = requiredSpeed == null || averageSpeed * 10 >= requiredSpeed * 12
        if (!waiting && recentFetching.size >= MinSpeedSamples && averageSpeed > 0L && keepsUp) {
            add(
                P2pTorrentHealthReport(
                    P2pTorrentHealthIssue.STABLE,
                    downloadSpeed = averageSpeed,
                    requiredSpeed = requiredSpeed,
                ),
            )
        }
    }

    private fun P2pTorrentDetails.isComplete(): Boolean =
        state == P2pTorrentState.SEEDING || state == P2pTorrentState.FINISHED ||
            (fileProgress ?: 0f) >= 0.999f

    private fun P2pTorrentDetails.requiredSpeed(): Long? {
        val size = fileSize ?: return null
        val duration = streamDurationMs?.takeIf { it > 0L } ?: return null
        return size * 1_000L / duration
    }

    private fun P2pPieceState.isMissing(): Boolean =
        this == P2pPieceState.MISSING || this == P2pPieceState.DOWNLOADING || this == P2pPieceState.BLOCKING

    private companion object {
        const val HistoryWindowMs = 30_000L
        const val SpeedWindowMs = 10_000L
        const val InUseWindowMs = 5_000L
        const val CorruptWindowMs = 30_000L
        const val SwitchDelayMs = 3_000L
        const val MinSpeedSamples = 3
        const val MinUnstableSamples = 10
        const val FewSeeders = 2
        const val ThrottlePeers = 4
        const val ThrottleSpeed = 256L * 1024L
        const val UnstablePeak = 512L * 1024L
        const val CorruptPieces = 4L
    }
}
