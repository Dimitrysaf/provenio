package io.github.dimitrysaf.provenio.p2p

import io.github.dimitrysaf.provenio.core.platform.cacheDirectoryPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sampler: Job? = null

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
            startSampling()
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
     * Registers the torrent and answers with the URL to play.
     *
     * No network work happens here. The player opens straight away and shows its own
     * buffering while the swarm is found, which is the whole reason the engine hands back
     * a URL rather than bytes.
     */
    override suspend fun streamUrl(request: TorrentRequest): String? {
        val stream = server
        if (stream == null || !torrents.isRunning) {
            p2pLog("cannot stream: engine is not running")
            return null
        }
        val url = stream.urlFor(torrents.register(request))
        p2pLog("ready: $url")
        return url
    }

    /**
     * Keeps the status readings live while the engine is up.
     *
     * Peers, seeds and rates are the only honest answer to "is this working". They are read
     * on a timer rather than pushed, because libtorrent has no notion of a status change —
     * these numbers move continuously and a snapshot a second apart is what a person can
     * actually read.
     */
    private fun startSampling() {
        sampler?.cancel()
        sampler = scope.launch {
            var sinceCacheRead = 0
            while (isActive) {
                delay(SampleIntervalMillis)
                if (!torrents.isRunning) continue
                val sample = torrents.sample()
                // Walking the cache directory touches every file, so it is read once every
                // ten samples rather than every one.
                val cacheBytes = if (sinceCacheRead++ % CacheEverySamples == 0) {
                    torrents.cacheBytes()
                } else {
                    _status.value.cacheUsedBytes
                }
                _status.value = _status.value.copy(
                    peers = sample.peers,
                    seeds = sample.seeds,
                    downloadBytesPerSecond = sample.downloadBytesPerSecond,
                    uploadBytesPerSecond = sample.uploadBytesPerSecond,
                    sessionDownloadedBytes = sample.downloadedBytes,
                    sessionUploadedBytes = sample.uploadedBytes,
                    activeTorrents = sample.activeTorrents,
                    cacheUsedBytes = cacheBytes,
                )
            }
        }
    }

    private companion object {
        /** 0 asks the operating system for any free port. */
        const val EphemeralPort = 0
        const val SampleIntervalMillis = 1_000L
        const val CacheEverySamples = 10
    }

    private fun stopQuietly() {
        sampler?.cancel()
        sampler = null
        runCatching { server?.stop() }
        server = null
        runCatching { torrents.stop() }
    }
}

actual fun createP2pEngine(): P2pEngine = JvmP2pEngine()
