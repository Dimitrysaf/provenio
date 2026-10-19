package io.github.dimitrysaf.provenio.core.p2p

import io.github.dimitrysaf.provenio.core.addons.httpRequestRaw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Streams torrents through a streaming server on another machine that speaks Stremio's streaming
 * server API, so the torrent runs there and this device only plays the video it serves.
 */
internal object ExternalTorrentServer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<P2pStreamingState>(P2pStreamingState.Idle)
    val state: StateFlow<P2pStreamingState> = _state.asStateFlow()

    private var statsJob: Job? = null
    private var active: ActiveTorrent? = null

    /**
     * The server's base address from what was typed: an IP, a host name or a URL. Adds http://
     * without a scheme and Stremio's port 11470 without a port. Null for anything else.
     */
    fun normalizeAddress(input: String): String? {
        var text = input.trim().trimEnd('/')
        if (text.isEmpty() || text.any(Char::isWhitespace)) return null
        if ("://" !in text) text = "http://$text"
        val scheme = text.substringBefore("://").lowercase()
        if (scheme != "http" && scheme != "https") return null
        val rest = text.substringAfter("://")
        val authority = rest.substringBefore('/')
        if (authority.isEmpty()) return null
        val path = rest.substring(authority.length)
        val bracketed = authority.startsWith("[")
        val bareIpv6 = !bracketed && authority.count { it == ':' } > 1
        val host = if (bareIpv6) "[$authority]" else authority
        val hasPort = when {
            bareIpv6 -> false
            bracketed -> authority.substringAfter("]").startsWith(":")
            else -> ':' in authority
        }
        return "$scheme://$host${if (hasPort) "" else ":$DefaultPort"}$path"
    }

    /**
     * Asks the server at [baseUrl] for its settings, to learn whether it answers. Returns the
     * server's version when it gives one, an empty string when it answers without it, and throws
     * when it does not answer in time or answers with an error.
     */
    suspend fun probe(baseUrl: String): String {
        val response = withTimeout(ProbeTimeoutMillis) {
            httpRequestRaw("GET", "$baseUrl/settings", emptyMap(), "")
        }
        if (response.status !in 200..299) throw P2pStreamingException("HTTP ${response.status}")
        return runCatching {
            Json.parseToJsonElement(response.body).jsonObject["values"]
                ?.jsonObject?.get("serverVersion")?.jsonPrimitive?.content
        }.getOrNull().orEmpty()
    }

    /** Hands the torrent to the server at [baseUrl] and returns the address it serves the file at. */
    suspend fun startStream(baseUrl: String, request: P2pStreamRequest): String {
        stopStream()
        val infoHash = canonicalP2pInfoHash(request.infoHash)
        _state.value = P2pStreamingState.Connecting(phase = "add_magnet")
        val body = buildJsonObject {
            putJsonObject("torrent") { put("infoHash", infoHash) }
            putJsonObject("peerSearch") {
                putJsonArray("sources") {
                    request.trackers.filter(String::isNotBlank).distinct().forEach { add("tracker:$it") }
                    add("dht:$infoHash")
                }
                put("min", PeerSearchMin)
                put("max", PeerSearchMax)
            }
            if (request.fileIdx == null) putJsonObject("guessFileIdx") {}
        }
        val response = try {
            httpRequestRaw(
                method = "POST",
                url = "$baseUrl/$infoHash/create",
                headers = mapOf("Content-Type" to "application/json"),
                body = body.toString(),
            )
        } catch (error: CancellationException) {
            _state.value = P2pStreamingState.Idle
            throw error
        } catch (error: Exception) {
            _state.value = P2pStreamingState.Error(error.message ?: "Server unreachable")
            throw P2pStreamingException("Could not reach the torrent server at $baseUrl")
        }
        if (response.status !in 200..299) {
            _state.value = P2pStreamingState.Error("HTTP ${response.status}")
            throw P2pStreamingException("The torrent server answered ${response.status}")
        }
        val fileIdx = request.fileIdx ?: guessFileIdx(response.body, request.filename)
            ?: run {
                _state.value = P2pStreamingState.Error("No playable file")
                throw P2pStreamingException("The torrent server found no file to play")
            }
        val streamUrl = "$baseUrl/$infoHash/$fileIdx"
        active = ActiveTorrent(baseUrl, infoHash, fileIdx, streamUrl)
        _state.value = P2pStreamingState.Streaming(
            localUrl = streamUrl,
            downloadSpeed = 0L,
            uploadSpeed = 0L,
            peers = 0,
            seeds = 0,
            bufferProgress = 0f,
            totalProgress = 0f,
        )
        startStats()
        return streamUrl
    }

    /** Stops reading stats and tells the server to drop the torrent. */
    fun stopStream() {
        statsJob?.cancel()
        statsJob = null
        val torrent = active ?: run {
            _state.value = P2pStreamingState.Idle
            return
        }
        active = null
        _state.value = P2pStreamingState.Idle
        scope.launch {
            runCatching {
                httpRequestRaw("GET", "${torrent.baseUrl}/${torrent.infoHash}/remove", emptyMap(), "")
            }
        }
    }

    private fun startStats() {
        val torrent = active ?: return
        statsJob = scope.launch {
            while (isActive && active == torrent) {
                runCatching {
                    val response = httpRequestRaw(
                        "GET",
                        "${torrent.baseUrl}/${torrent.infoHash}/${torrent.fileIdx}/stats.json",
                        emptyMap(),
                        "",
                    )
                    if (response.status in 200..299 && active == torrent) {
                        val stats = Json.parseToJsonElement(response.body).jsonObject
                        val progress = stats.double("streamProgress")?.toFloat()?.coerceIn(0f, 1f) ?: 0f
                        _state.value = P2pStreamingState.Streaming(
                            localUrl = torrent.streamUrl,
                            downloadSpeed = stats.double("downloadSpeed")?.toLong() ?: 0L,
                            uploadSpeed = stats.double("uploadSpeed")?.toLong() ?: 0L,
                            peers = stats.int("peers") ?: 0,
                            seeds = stats.int("unchoked") ?: 0,
                            bufferProgress = progress,
                            totalProgress = progress,
                            downloadedBytes = stats.long("downloaded") ?: 0L,
                        )
                    }
                }
                delay(StatsIntervalMillis)
            }
        }
    }

    /**
     * The file to play when the stream did not name one: the server's own guess, then the file
     * whose name matches [filename], then the largest video.
     */
    private fun guessFileIdx(createBody: String, filename: String?): Int? {
        val json = runCatching { Json.parseToJsonElement(createBody).jsonObject }.getOrNull() ?: return null
        json.int("guessedFileIdx")?.takeIf { it >= 0 }?.let { return it }
        val files = (json["files"] as? JsonArray)?.mapIndexedNotNull { index, element ->
            val file = element as? JsonObject ?: return@mapIndexedNotNull null
            val name = file["name"]?.jsonPrimitive?.content.orEmpty()
            val length = file.long("length") ?: 0L
            Triple(index, name, length)
        }.orEmpty()
        filename?.takeIf(String::isNotBlank)?.let { wanted ->
            files.firstOrNull { (_, name, _) -> name.equals(wanted, ignoreCase = true) }?.let { return it.first }
        }
        return files
            .filter { (_, name, _) -> VideoExtensions.any { name.lowercase().endsWith(it) } }
            .maxByOrNull { it.third }
            ?.first
    }

    private fun JsonObject.double(key: String): Double? = runCatching { this[key]?.jsonPrimitive?.doubleOrNull }.getOrNull()
    private fun JsonObject.int(key: String): Int? = runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull()
    private fun JsonObject.long(key: String): Long? = runCatching { this[key]?.jsonPrimitive?.longOrNull }.getOrNull()

    private data class ActiveTorrent(
        val baseUrl: String,
        val infoHash: String,
        val fileIdx: Int,
        val streamUrl: String,
    )

    private const val DefaultPort = 11470
    private const val PeerSearchMin = 40
    private const val PeerSearchMax = 150
    private const val StatsIntervalMillis = 1_000L
    private const val ProbeTimeoutMillis = 5_000L
    private val VideoExtensions = listOf(".mkv", ".mp4", ".avi", ".mov", ".webm", ".m4v", ".ts", ".wmv", ".flv")
}

/** How far into the torrent's video the player is, 0..1. */
data class P2pPlaybackPosition(val infoHash: String, val fraction: Float)

/**
 * Where playback's torrent streams go: the external server when one is set in P2P settings,
 * otherwise Provenio's own engine. Playback asks here, never the engine directly.
 */
object P2pStreams {
    private val usingExternal = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _playbackPosition = MutableStateFlow<P2pPlaybackPosition?>(null)
    private var playbackPositionOwner: Any? = null

    /** Where the player is in the torrent it plays, for the torrent details to mark. */
    val playbackPosition: StateFlow<P2pPlaybackPosition?> = _playbackPosition.asStateFlow()

    fun setPlaybackPosition(owner: Any, position: P2pPlaybackPosition?) {
        playbackPositionOwner = owner
        _playbackPosition.value = position
    }

    /** Forgets [owner]'s position, unless another player has reported one since. */
    fun clearPlaybackPosition(owner: Any) {
        if (playbackPositionOwner !== owner) return
        playbackPositionOwner = null
        _playbackPosition.value = null
    }

    /** The state of whichever one is streaming now. */
    val state: StateFlow<P2pStreamingState> = combine(
        P2pStreamingEngine.state,
        ExternalTorrentServer.state,
        usingExternal,
    ) { engine, external, useExternal -> if (useExternal) external else engine }
        .stateIn(scope, SharingStarted.Eagerly, P2pStreamingState.Idle)

    private val externalServer: String?
        get() {
            P2pSettingsRepository.ensureLoaded()
            return P2pSettingsRepository.uiState.value.externalServer
        }

    val usesExternalServer: Boolean
        get() = externalServer != null

    suspend fun startStream(request: P2pStreamRequest): String {
        val server = externalServer
        return if (server != null) {
            P2pStreamingEngine.stopStream()
            usingExternal.value = true
            ExternalTorrentServer.startStream(server, request)
        } else {
            ExternalTorrentServer.stopStream()
            usingExternal.value = false
            P2pStreamingEngine.startStream(request)
        }
    }

    fun stopStream() {
        if (usingExternal.value) ExternalTorrentServer.stopStream() else P2pStreamingEngine.stopStream()
    }

    fun setStreamDuration(durationMs: Long) {
        if (!usingExternal.value) P2pStreamingEngine.setStreamDuration(durationMs)
    }

    /** Readies the engine for what is about to play; an external server needs nothing. */
    fun warmUp(requests: List<P2pStreamRequest>) {
        if (usesExternalServer) return
        P2pStreamingEngine.warmUp()
        P2pStreamingEngine.prefetchTorrents(requests)
    }
}
