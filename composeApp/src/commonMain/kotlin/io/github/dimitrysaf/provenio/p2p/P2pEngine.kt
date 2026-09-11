package io.github.dimitrysaf.provenio.p2p

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The peer-to-peer engine.
 *
 * Defined before any engine exists so the settings and status UI are written against an
 * interface rather than a particular torrent library — swapping one in later should not
 * touch a single composable.
 */
interface P2pEngine {

    val status: StateFlow<P2pStatus>

    suspend fun start(settings: P2pSettings)

    /**
     * Applies settings to a running engine.
     *
     * Separate from a restart because a restart rebinds the local server, and any URL the
     * player was already given points at the old one. Anything that can be changed without
     * that — rates, connection limits, cache size — belongs here.
     */
    suspend fun apply(settings: P2pSettings)

    suspend fun stop()

    /** Drops cached piece data. Returns the number of bytes reclaimed. */
    suspend fun clearCache(): Long

    /**
     * Registers a torrent and returns the localhost URL to play.
     *
     * The URL is the whole contract between the engine and the player. A player cannot
     * tell one of these apart from any other HTTP source, which is what lets the two
     * platforms launch the engine differently without changing anything above them.
     */
    suspend fun streamUrl(request: TorrentRequest): String?
}

/**
 * Stands in until a real engine is chosen.
 *
 * It reports the addresses the device would actually listen on, so the status screen shows
 * something true rather than placeholder text, and is explicit that no engine is present
 * instead of pretending to run.
 */
class UnavailableP2pEngine : P2pEngine {

    private val _status = MutableStateFlow(P2pStatus())
    override val status: StateFlow<P2pStatus> = _status.asStateFlow()

    override suspend fun start(settings: P2pSettings) {
        _status.value = P2pStatus(
            state = P2pServiceState.NotBuilt,
            localAddresses = localNetworkAddresses(),
            listenPort = settings.listenPort.takeIf { it != 0 },
        )
    }

    override suspend fun apply(settings: P2pSettings) = Unit

    override suspend fun stop() {
        _status.value = P2pStatus()
    }

    override suspend fun clearCache(): Long = 0L

    override suspend fun streamUrl(request: TorrentRequest): String? = null
}

/** The engine for this platform. */
expect fun createP2pEngine(): P2pEngine
