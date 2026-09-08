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

    suspend fun stop()

    /** Drops cached piece data. Returns the number of bytes reclaimed. */
    suspend fun clearCache(): Long
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
            state = P2pServiceState.Failed,
            localAddresses = localNetworkAddresses(),
            listenPort = settings.listenPort.takeIf { it != 0 },
            detail = "No peer-to-peer engine is bundled in this build yet.",
        )
    }

    override suspend fun stop() {
        _status.value = P2pStatus()
    }

    override suspend fun clearCache(): Long = 0L
}
