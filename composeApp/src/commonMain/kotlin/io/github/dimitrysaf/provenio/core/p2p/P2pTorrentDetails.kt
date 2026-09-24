package io.github.dimitrysaf.provenio.core.p2p

enum class P2pTorrentState {
    UNKNOWN,
    CHECKING_FILES,
    DOWNLOADING_METADATA,
    DOWNLOADING,
    FINISHED,
    SEEDING,
    CHECKING_RESUME_DATA,
}

enum class P2pPeerSource {
    TRACKER,
    DHT,
    PEX,
    LSD,
    RESUME_DATA,
    INCOMING,
}

enum class P2pTrackerStatus {
    NOT_CONTACTED,
    WORKING,
    UPDATING,
    ERROR,
}

enum class P2pPieceState {
    MISSING,
    HAVE,
    DOWNLOADING,
    /** Missing, and the player is waiting on it. */
    BLOCKING,
    /** Cached here and sent to at least one peer. */
    SEEDED,
}

/** One second of transfer, payload only. */
data class P2pSpeedSample(
    val downloadBytesPerSecond: Long,
    val uploadBytesPerSecond: Long,
)

data class P2pPeerDetails(
    val address: String,
    val client: String,
    val progress: Float,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val totalDownloaded: Long,
    val totalUploaded: Long,
    val rttMs: Int,
    val sources: Set<P2pPeerSource>,
    val isSeed: Boolean,
    val isEncrypted: Boolean,
    val isUtp: Boolean,
    val isSnubbed: Boolean,
    /** The peer is refusing to send to us. */
    val isChokingUs: Boolean,
    /** The peer has pieces we still want. */
    val isInteresting: Boolean,
    val isOutgoing: Boolean,
    val isWebSeed: Boolean,
    val isConnecting: Boolean,
)

data class P2pTrackerDetails(
    val url: String,
    val tier: Int,
    val status: P2pTrackerStatus,
    val message: String?,
    val seeds: Int?,
    val leechers: Int?,
    val downloaded: Int?,
    val nextAnnounceSeconds: Long?,
)

/**
 * The pieces that hold the file being played or downloaded, in file order. Piece maps of other
 * files in the torrent are left out, since nothing here asks for them.
 */
class P2pPieceMap(
    private val states: ByteArray,
    private val availability: ByteArray,
    /** Index of the first piece in the torrent. */
    val firstPiece: Int,
    /** How much of the file is ready to play from its start, 0..1. */
    val readyFraction: Float,
) {
    val size: Int get() = states.size

    fun state(index: Int): P2pPieceState = when (states[index].toInt()) {
        1 -> P2pPieceState.HAVE
        2 -> P2pPieceState.DOWNLOADING
        3 -> P2pPieceState.BLOCKING
        4 -> P2pPieceState.SEEDED
        else -> P2pPieceState.MISSING
    }

    /** How many connected peers have the piece. */
    fun availability(index: Int): Int =
        if (index < availability.size) availability[index].toInt() and 0xff else 0

    fun count(state: P2pPieceState): Int = (0 until size).count { state(it) == state }
}

/** Everything the engine reports about the torrent it is serving, as a BitTorrent client shows it. */
data class P2pTorrentDetails(
    val infoHash: String,
    val name: String,
    val state: P2pTorrentState,
    val hasMetadata: Boolean,
    val fileName: String?,
    val fileSize: Long?,
    /** Verified share of the streamed file. It only grows, unlike [progress]. */
    val fileProgress: Float?,
    val pieceCount: Int,
    val pieceLength: Int,
    val piecesHave: Int,
    val fileCount: Int,
    /**
     * libtorrent's progress over the pieces currently wanted. While streaming, the wanted set
     * follows the playhead, so this can fall as well as rise.
     */
    val progress: Float,
    /** Complete copies the connected peers hold between them; null until measured. */
    val availability: Float?,
    val connectedPeers: Int,
    val connectedSeeds: Int,
    val knownPeers: Int,
    val knownSeeds: Int,
    val swarmSeeds: Int?,
    val swarmLeechers: Int?,
    val totalSize: Long,
    val totalWanted: Long,
    val totalWantedDone: Long,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val downloadSpeedWithOverhead: Long,
    val uploadSpeedWithOverhead: Long,
    val sessionDownloaded: Long,
    val sessionUploaded: Long,
    val allTimeDownloaded: Long,
    val allTimeUploaded: Long,
    val hashFailedBytes: Long,
    val redundantBytes: Long,
    val addedAtEpochSeconds: Long,
    val activeSeconds: Long,
    val nextAnnounceSeconds: Long?,
    val currentTracker: String?,
    val peers: List<P2pPeerDetails>,
    val trackers: List<P2pTrackerDetails>,
    val pieces: P2pPieceMap?,
    val speedHistory: List<P2pSpeedSample>,
) {
    val shareRatio: Float?
        get() = if (allTimeDownloaded > 0L) allTimeUploaded.toFloat() / allTimeDownloaded else null

    /** Time to finish what is wanted at the current speed; null when stalled or done. */
    val etaSeconds: Long?
        get() {
            val remaining = totalWanted - totalWantedDone
            return if (remaining > 0L && downloadSpeed > 0L) remaining / downloadSpeed else null
        }
}

/** How many one-second speed samples the details keep. */
internal const val P2P_SPEED_HISTORY_SIZE = 120
