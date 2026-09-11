package io.github.dimitrysaf.provenio.p2p

import io.github.dimitrysaf.provenio.core.platform.cacheDirectoryPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.BindException

/**
 * libtorrent behind the loopback server that fronts it.
 *
 * Android has to load a torrent engine in process because it may not execute a downloaded
 * binary, while desktop is free to spawn one. Neither difference reaches this class,
 * because both sides hand the player the same localhost URL.
 */
class JvmP2pEngine : P2pEngine {

    private val torrents = TorrentSession(File(cacheDirectoryPath()))
    private var server: LocalStreamServer? = null

    private val _status = MutableStateFlow(P2pStatus())
    override val status: StateFlow<P2pStatus> = _status.asStateFlow()

    override suspend fun start(settings: P2pSettings) {
        try {
            torrents.start(settings)
            // Deliberately not settings.listenPort: that one is the swarm's, and
            // libtorrent has just bound it. This is the loopback port the player reads
            // from, and asking the OS for a free one is what keeps the two from
            // colliding. Settings changes apply in place rather than rebinding, so the
            // URL handed to a player stays valid.
            val stream = LocalStreamServer(EphemeralPort, torrents)
            val port = stream.start()
            server = stream

            _status.value = P2pStatus(
                state = P2pServiceState.Online,
                listenPort = port,
                baseUrl = "http://127.0.0.1:$port",
                localAddresses = localNetworkAddresses(),
                cacheUsedBytes = torrents.cacheBytes(),
            )
        } catch (conflict: BindException) {
            p2pLog("port ${settings.listenPort} already in use")
            stopQuietly()
            _status.value = P2pStatus(
                state = P2pServiceState.Offline,
                portInUse = true,
                localAddresses = localNetworkAddresses(),
            )
        } catch (failure: Exception) {
            p2pLog("engine failed to start: $failure")
            stopQuietly()
            _status.value = P2pStatus(
                state = P2pServiceState.Offline,
                localAddresses = localNetworkAddresses(),
                detail = failure.message,
            )
        }
    }

    override suspend fun apply(settings: P2pSettings) {
        torrents.apply(settings)
        _status.value = _status.value.copy(cacheUsedBytes = torrents.cacheBytes())
    }

    override suspend fun stop() {
        stopQuietly()
        _status.value = P2pStatus()
    }

    override suspend fun clearCache(): Long {
        val freed = torrents.clearCache()
        _status.value = _status.value.copy(cacheUsedBytes = torrents.cacheBytes())
        return freed
    }

    /**
     * Joins the swarm, waits for metadata, and only then answers with a URL.
     *
     * The waiting has to happen somewhere, and it cannot be in the request handler: a
     * player expects response headers in a couple of seconds and finding a swarm takes
     * far longer than that, so doing it there times the player out before a byte moves.
     * Here the caller is the sources sheet, which already shows a spinner and can afford
     * to wait — and a torrent nobody is seeding fails as "no source" instead of as a
     * mysterious playback error.
     */
    override suspend fun streamUrl(request: TorrentRequest): String? {
        val stream = server
        if (stream == null || !torrents.isRunning) {
            p2pLog("cannot stream: engine is not running")
            return null
        }
        val key = torrents.register(request)
        if (torrents.open(key) == null) {
            p2pLog("no stream for $key — giving up")
            return null
        }
        val url = stream.urlFor(key)
        p2pLog("ready: $url")
        return url
    }

    private companion object {
        /** 0 asks the operating system for any free port. */
        const val EphemeralPort = 0
    }

    private fun stopQuietly() {
        runCatching { server?.stop() }
        server = null
        runCatching { torrents.stop() }
    }
}

actual fun createP2pEngine(): P2pEngine = JvmP2pEngine()
