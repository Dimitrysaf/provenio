package io.github.dimitrysaf.provenio.p2p

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.BindException

/**
 * Runs the loopback server that fronts the swarm.
 *
 * Android has to load a torrent engine in process because it may not execute a downloaded
 * binary, while desktop is free to spawn one. Neither difference reaches this class,
 * because both sides hand the player the same localhost URL.
 */
class JvmP2pEngine : P2pEngine {

    private val server = LocalStreamServer(requestedPort = 0)

    private val _status = MutableStateFlow(P2pStatus())
    override val status: StateFlow<P2pStatus> = _status.asStateFlow()

    override suspend fun start(settings: P2pSettings) {
        try {
            val port = server.start()
            _status.value = P2pStatus(
                state = P2pServiceState.Online,
                listenPort = port,
                baseUrl = "http://127.0.0.1:$port",
                localAddresses = localNetworkAddresses(),
            )
        } catch (conflict: BindException) {
            _status.value = P2pStatus(
                state = P2pServiceState.Offline,
                portInUse = true,
                localAddresses = localNetworkAddresses(),
            )
        } catch (failure: Exception) {
            _status.value = P2pStatus(
                state = P2pServiceState.Offline,
                localAddresses = localNetworkAddresses(),
            )
        }
    }

    override suspend fun stop() {
        server.stop()
        _status.value = P2pStatus()
    }

    override suspend fun clearCache(): Long = 0L

    override suspend fun streamUrl(magnet: String): String? =
        if (_status.value.state == P2pServiceState.Online) {
            server.urlFor(magnet.substringAfter("btih:").substringBefore("&"))
        } else {
            null
        }
}

actual fun createP2pEngine(): P2pEngine = JvmP2pEngine()
