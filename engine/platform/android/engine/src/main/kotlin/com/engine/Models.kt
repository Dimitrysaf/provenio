package com.engine

public enum class EventType(public val nativeValue: Int) {
    TorrentAdded(1),
    TorrentMetadataReady(2),
    TorrentError(3),
    StreamPrepared(4),
    TorrentRemoved(5),
    StreamStopped(6),
    DiskCacheReclaimed(7);

    internal companion object {
        fun fromNative(value: Int): EventType =
            entries.firstOrNull { it.nativeValue == value }
                ?: throw EngineException(-1, "unknown native event type $value")
    }
}

public data class Event(
    val type: EventType,
    val sequence: Long,
    val requestId: Long,
    val droppedEvents: Long,
    val torrentId: String?,
    val message: String?,
    val fileIndex: Int?,
    val fileSize: Long,
    val streamId: String?,
    val streamUrl: String?,
)

public data class TorrentFile(
    val index: Int,
    val offset: Long,
    val size: Long,
    val path: String,
    val pathTruncated: Boolean,
)

public data class Stream(
    val id: String,
    val url: String,
    val torrentId: String,
    val fileIndex: Int,
    val fileSize: Long,
)

public data class StreamStats(
    val fileIndex: Int,
    val fileSize: Long,
    val contiguousReadyBytes: Long,
    val verifiedFileBytes: Long,
    val deliveredBytes: Long,
    val activeDemands: Int = 0,
    val scheduledPieces: Int = 0,
    val blockingPieces: Int = 0,
    val primaryBlockingPiece: Int = -1,
    val secondaryBlockingPiece: Int = -1,
    val lastReadyPiece: Int = -1,
    val primaryDemandStart: Long = 0,
    val primaryDemandEnd: Long = 0,
    val secondaryDemandStart: Long = 0,
    val secondaryDemandEnd: Long = 0,
    val scheduleRevision: Long = 0,
) {
    public val bufferProgress: Float
        get() = ratio(contiguousReadyBytes, fileSize)

    public val fileProgress: Float
        get() = ratio(verifiedFileBytes, fileSize)
}

public data class EngineStats(
    val activeTorrents: Int = 0,
    val activeStreams: Int = 0,
    val activeHttpRequests: Int = 0,
    val connectedPeers: Int = 0,
    val connectedSeeds: Int = 0,
    val knownPeers: Int = 0,
    val connectCandidates: Int = 0,
    val interestedPeers: Int = 0,
    val unchokedPeers: Int = 0,
    val downloadingPeers: Int = 0,
    val snubbedPeers: Int = 0,
    val pendingBlockRequests: Int = 0,
    val targetBlockRequests: Int = 0,
    val timedOutBlockRequests: Int = 0,
    val connectingPeers: Int = 0,
    val handshakingPeers: Int = 0,
    val targetPiecePeers: Int = 0,
    val targetPieceUnchokedPeers: Int = 0,
    val targetPieceDownloadingPeers: Int = 0,
    val offTargetDownloadingPeers: Int = 0,
    val trackerReplyEvents: Int = 0,
    val trackerErrorEvents: Int = 0,
    val dhtReplyEvents: Int = 0,
    val trackerPeersReturned: Long = 0,
    val dhtPeersReturned: Long = 0,
    val peerConnectEvents: Long = 0,
    val peerDisconnectEvents: Long = 0,
    val peerDisconnectTimeouts: Long = 0,
    val peerDisconnectConnectFailures: Long = 0,
    val peerDisconnectRedundant: Long = 0,
    val peerDisconnectTurnover: Long = 0,
    val peerDisconnectOther: Long = 0,
    val torrentFinishedEvents: Long = 0,
    val pendingPieceReads: Int = 0,
    val downloadRateBytesPerSecond: Long = 0,
    val uploadRateBytesPerSecond: Long = 0,
    val totalPayloadDownloadBytes: Long = 0,
    val totalPayloadUploadBytes: Long = 0,
    val memoryCacheCapacityBytes: Long = 0,
    val memoryCacheUsedBytes: Long = 0,
    val memoryCacheHits: Long = 0,
    val memoryCacheMisses: Long = 0,
    val memoryCacheEvictions: Long = 0,
    val memoryCacheEntries: Long = 0,
    val warmTorrents: Int = 0,
    val quiescedTorrents: Int = 0,
    val diskCacheCapacityBytes: Long = 0,
    val diskCacheUsedBytes: Long = 0,
    val diskCacheProtectedBytes: Long = 0,
    val diskCacheEvictions: Long = 0,
    val diskCacheReclaimedBytes: Long = 0,
    val diskCacheOverBudget: Boolean = false,
)

public enum class TorrentState(public val nativeValue: Int) {
    Unknown(0),
    CheckingFiles(1),
    DownloadingMetadata(2),
    Downloading(3),
    Finished(4),
    Seeding(5),
    CheckingResumeData(6);

    internal companion object {
        fun fromNative(value: Int): TorrentState =
            entries.firstOrNull { it.nativeValue == value } ?: Unknown
    }
}

public enum class PeerSource(internal val nativeMask: Int) {
    Tracker(1 shl 0),
    Dht(1 shl 1),
    Pex(1 shl 2),
    Lsd(1 shl 3),
    ResumeData(1 shl 4),
    Incoming(1 shl 5),
}

public enum class PieceState(internal val nativeValue: Byte) {
    Missing(0),
    Have(1),
    Downloading(2),
    Blocking(3),
    /** Have, and this device has sent it to at least one peer. */
    Seeded(4);

    internal companion object {
        fun fromNative(value: Byte): PieceState =
            entries.firstOrNull { it.nativeValue == value } ?: Missing
    }
}

public data class TorrentPeer(
    val address: String,
    val client: String,
    val flags: Int,
    val sources: Set<PeerSource>,
    val progress: Float,
    val downloadRateBytesPerSecond: Long,
    val uploadRateBytesPerSecond: Long,
    val totalDownloadBytes: Long,
    val totalUploadBytes: Long,
    val rttMilliseconds: Int,
    val downloadQueueLength: Int,
    val hashFailures: Int,
    /** The piece this peer is currently sending, or null when it is idle. */
    val downloadingPiece: Int?,
) {
    public val isSeed: Boolean get() = has(FLAG_SEED)
    /** We want pieces this peer has. */
    public val isInteresting: Boolean get() = has(FLAG_INTERESTING)
    /** We are refusing to upload to this peer. */
    public val isChoked: Boolean get() = has(FLAG_CHOKED)
    public val isRemoteInterested: Boolean get() = has(FLAG_REMOTE_INTERESTED)
    /** The peer is refusing to upload to us. */
    public val isRemoteChoked: Boolean get() = has(FLAG_REMOTE_CHOKED)
    public val isSnubbed: Boolean get() = has(FLAG_SNUBBED)
    public val isOptimisticUnchoke: Boolean get() = has(FLAG_OPTIMISTIC_UNCHOKE)
    public val isOutgoing: Boolean get() = has(FLAG_OUTGOING)
    public val isEncrypted: Boolean get() = has(FLAG_ENCRYPTED)
    public val isUtp: Boolean get() = has(FLAG_UTP)
    public val isConnecting: Boolean get() = has(FLAG_CONNECTING)
    public val isHandshaking: Boolean get() = has(FLAG_HANDSHAKE)
    public val isOnParole: Boolean get() = has(FLAG_ON_PAROLE)
    public val isUploadOnly: Boolean get() = has(FLAG_UPLOAD_ONLY)
    public val isEndgame: Boolean get() = has(FLAG_ENDGAME)
    public val isHolepunched: Boolean get() = has(FLAG_HOLEPUNCHED)
    public val isWebSeed: Boolean get() = has(FLAG_WEB_SEED)

    private fun has(flag: Int): Boolean = flags and flag != 0

    public companion object {
        public const val FLAG_SEED: Int = 1 shl 0
        public const val FLAG_INTERESTING: Int = 1 shl 1
        public const val FLAG_CHOKED: Int = 1 shl 2
        public const val FLAG_REMOTE_INTERESTED: Int = 1 shl 3
        public const val FLAG_REMOTE_CHOKED: Int = 1 shl 4
        public const val FLAG_SNUBBED: Int = 1 shl 5
        public const val FLAG_OPTIMISTIC_UNCHOKE: Int = 1 shl 6
        public const val FLAG_OUTGOING: Int = 1 shl 7
        public const val FLAG_ENCRYPTED: Int = 1 shl 8
        public const val FLAG_UTP: Int = 1 shl 9
        public const val FLAG_CONNECTING: Int = 1 shl 10
        public const val FLAG_HANDSHAKE: Int = 1 shl 11
        public const val FLAG_ON_PAROLE: Int = 1 shl 12
        public const val FLAG_UPLOAD_ONLY: Int = 1 shl 13
        public const val FLAG_ENDGAME: Int = 1 shl 14
        public const val FLAG_HOLEPUNCHED: Int = 1 shl 15
        public const val FLAG_WEB_SEED: Int = 1 shl 16
    }
}

public enum class TrackerStatus(public val nativeValue: Int) {
    NotContacted(0),
    Working(1),
    Updating(2),
    Error(3);

    internal companion object {
        fun fromNative(value: Int): TrackerStatus =
            entries.firstOrNull { it.nativeValue == value } ?: NotContacted
    }
}

public data class TorrentTracker(
    val url: String,
    val tier: Int,
    val status: TrackerStatus,
    val message: String?,
    /** Scrape results; null until the tracker has reported them. */
    val seeds: Int?,
    val leechers: Int?,
    val downloaded: Int?,
    val failures: Int,
    val nextAnnounceSeconds: Long?,
)

/**
 * A snapshot of one torrent as a BitTorrent client would describe it. The engine refreshes it
 * about once per second; swarm-wide values the network has not reported yet are null.
 */
public class TorrentDetails(
    public val torrentId: String,
    public val name: String,
    public val state: TorrentState,
    public val hasMetadata: Boolean,
    public val currentTracker: String?,
    public val pieceCount: Int,
    public val pieceLength: Int,
    public val piecesHave: Int,
    public val fileCount: Int,
    public val progress: Float,
    /** Distributed copies: how many complete copies the connected peers hold between them. */
    public val availability: Float?,
    public val connectedPeers: Int,
    public val connectedSeeds: Int,
    public val knownPeers: Int,
    public val knownSeeds: Int,
    public val connectCandidates: Int,
    public val swarmSeeds: Int?,
    public val swarmLeechers: Int?,
    public val totalSize: Long,
    public val totalWanted: Long,
    public val totalWantedDone: Long,
    public val totalDone: Long,
    public val downloadRateBytesPerSecond: Long,
    public val uploadRateBytesPerSecond: Long,
    public val downloadPayloadRateBytesPerSecond: Long,
    public val uploadPayloadRateBytesPerSecond: Long,
    public val sessionPayloadDownloadBytes: Long,
    public val sessionPayloadUploadBytes: Long,
    public val allTimeDownloadBytes: Long,
    public val allTimeUploadBytes: Long,
    public val failedBytes: Long,
    public val redundantBytes: Long,
    public val addedAtEpochSeconds: Long,
    public val activeSeconds: Long,
    public val nextAnnounceSeconds: Long?,
    public val peers: List<TorrentPeer>,
    public val trackers: List<TorrentTracker>,
    private val pieceStates: ByteArray,
    private val pieceAvailability: ByteArray,
) {
    /** Number of entries in the piece map; can be zero before metadata arrives. */
    public val pieceMapSize: Int get() = pieceStates.size

    public fun pieceState(piece: Int): PieceState = PieceState.fromNative(pieceStates[piece])

    /** How many connected peers have [piece], capped at 255. */
    public fun pieceAvailability(piece: Int): Int =
        if (piece < pieceAvailability.size) pieceAvailability[piece].toInt() and 0xff else 0
}

public class EngineException(
    public val status: Int,
    message: String,
) : Exception(message)

private fun ratio(value: Long, total: Long): Float =
    if (total <= 0L) 0f else (value.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
