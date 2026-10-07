package io.github.dimitrysaf.provenio.core.cast

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class CastMedia(
    val url: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val startPositionMs: Long,
)

data class CastPlayback(
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

enum class CastFailure { Unreachable, Unsupported, Disconnected }

sealed interface CastState {
    data object Idle : CastState
    data class Connecting(val device: CastDevice) : CastState
    data class Active(val device: CastDevice, val playback: CastPlayback) : CastState
    data class Failed(val device: CastDevice, val reason: CastFailure) : CastState
}

@OptIn(ExperimentalCoroutinesApi::class)
object CastController {
    private val log = Logger.withTag("Cast")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val _devices = MutableStateFlow<List<CastDevice>>(emptyList())
    private val _state = MutableStateFlow<CastState>(CastState.Idle)
    private var discoveryJob: Job? = null
    private var sessionJob: Job? = null
    private var session: CastSession? = null

    val isSupported: Boolean get() = CastPlatform.isSupported

    fun hasLocalNetwork(): Boolean = isSupported && runCatching { CastNetwork.hasLocalNetwork() }.getOrDefault(false)
    val devices: StateFlow<List<CastDevice>> = _devices.asStateFlow()
    val state: StateFlow<CastState> = _state.asStateFlow()

    fun startDiscovery() {
        if (!isSupported || discoveryJob?.isActive == true) return
        discoveryJob = scope.launch {
            try {
                CastPlatform.discover { device ->
                    _devices.update { current ->
                        (current.filterNot { it.id == device.id } + device).sortedBy { it.name.lowercase() }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                log.w(error) { "Cast discovery stopped" }
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    fun cast(device: CastDevice, media: CastMedia) {
        stopDiscovery()
        scope.launch {
            sessionJob?.cancel()
            session?.close()
            _state.value = CastState.Connecting(device)
            sessionJob = scope.launch { runSession(device, media) }
        }
    }

    fun play() = scope.launch { session?.play() }

    fun pause() = scope.launch { session?.pause() }

    fun seekTo(positionMs: Long) = scope.launch { session?.seekTo(positionMs) }

    /** Ends playback on the receiver and closes the connection. */
    fun stop() = scope.launch {
        val current = session
        sessionJob?.cancel()
        runCatching { current?.stopReceiverApp() }
        current?.close()
        session = null
        _state.value = CastState.Idle
    }

    /** Leaves whatever is playing on the receiver and only closes the connection. */
    fun disconnect() = scope.launch {
        sessionJob?.cancel()
        session?.close()
        session = null
        _state.value = CastState.Idle
    }

    fun dismissFailure() {
        _state.update { if (it is CastState.Failed) CastState.Idle else it }
    }

    private suspend fun runSession(device: CastDevice, media: CastMedia) {
        val channel = try {
            CastPlatform.open(device)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            log.w(error) { "Could not reach ${device.name} at ${device.host}:${device.port}" }
            _state.value = CastState.Failed(device, CastFailure.Unreachable)
            return
        }
        val current = CastSession(channel, media)
        session = current
        try {
            current.run { playback -> _state.value = CastState.Active(device, playback) }
            _state.value = CastState.Idle
        } catch (error: CancellationException) {
            throw error
        } catch (_: CastLoadException) {
            _state.value = CastState.Failed(device, CastFailure.Unsupported)
        } catch (error: Exception) {
            log.w(error) { "Cast session with ${device.name} ended" }
            _state.value = CastState.Failed(device, CastFailure.Disconnected)
        } finally {
            current.close()
            if (session === current) session = null
        }
    }
}

private class CastLoadException : Exception()

private class CastSession(
    private val channel: CastChannel,
    private val media: CastMedia,
) {
    private var requestId = 0
    private var transportId: String? = null
    private var appSessionId: String? = null
    private var mediaSessionId: Int? = null
    private var mediaStarted = false
    private var durationMs = 0L

    suspend fun run(onPlayback: (CastPlayback) -> Unit) = coroutineScope {
        send(NS_CONNECTION, RECEIVER_ID) { put("type", "CONNECT") }
        val heartbeat = launch {
            while (isActive) {
                send(NS_HEARTBEAT, RECEIVER_ID) { put("type", "PING") }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
        val poller = launch {
            while (isActive) {
                delay(STATUS_INTERVAL_MS)
                val transport = transportId ?: continue
                val mediaSession = mediaSessionId ?: continue
                request(NS_MEDIA, transport) {
                    put("type", "GET_STATUS")
                    put("mediaSessionId", mediaSession)
                }
            }
        }
        val watchdog = launch {
            delay(START_TIMEOUT_MS)
            if (!mediaStarted) throw CastLoadException()
        }
        request(NS_RECEIVER, RECEIVER_ID) {
            put("type", "LAUNCH")
            put("appId", DEFAULT_MEDIA_RECEIVER)
        }
        try {
            while (handle(channel.receive(), onPlayback)) Unit
        } finally {
            heartbeat.cancel()
            poller.cancel()
            watchdog.cancel()
        }
    }

    suspend fun play() = mediaCommand("PLAY")

    suspend fun pause() = mediaCommand("PAUSE")

    suspend fun seekTo(positionMs: Long) = mediaCommand("SEEK") {
        put("currentTime", positionMs / 1000.0)
    }

    suspend fun stopReceiverApp() {
        val sessionId = appSessionId ?: return
        request(NS_RECEIVER, RECEIVER_ID) {
            put("type", "STOP")
            put("sessionId", sessionId)
        }
    }

    fun close() = channel.close()

    private suspend fun handle(frame: CastFrame, onPlayback: (CastPlayback) -> Unit): Boolean {
        val json = runCatching { Json.parseToJsonElement(frame.payload).jsonObject }.getOrNull() ?: return true
        val type = json.string("type")
        when (frame.namespace) {
            NS_HEARTBEAT -> if (type == "PING") send(NS_HEARTBEAT, frame.source) { put("type", "PONG") }
            NS_CONNECTION -> if (type == "CLOSE" && frame.source == transportId) return false
            NS_RECEIVER -> when (type) {
                "RECEIVER_STATUS" -> return onReceiverStatus(json)
                "LAUNCH_ERROR" -> throw CastLoadException()
            }
            NS_MEDIA -> when (type) {
                "MEDIA_STATUS" -> return onMediaStatus(json, onPlayback)
                "LOAD_FAILED", "LOAD_CANCELLED", "INVALID_REQUEST" -> throw CastLoadException()
            }
        }
        return true
    }

    private suspend fun onReceiverStatus(json: JsonObject): Boolean {
        val app = json["status"]?.jsonObject?.get("applications")?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it.string("appId") == DEFAULT_MEDIA_RECEIVER }
        if (app == null) return transportId == null
        if (transportId != null) return true
        val transport = app.string("transportId") ?: return true
        transportId = transport
        appSessionId = app.string("sessionId")
        send(NS_CONNECTION, transport) { put("type", "CONNECT") }
        request(NS_MEDIA, transport) {
            put("type", "LOAD")
            appSessionId?.let { put("sessionId", it) }
            put("autoplay", true)
            put("currentTime", media.startPositionMs / 1000.0)
            put(
                "media",
                buildJsonObject {
                    put("contentId", media.url)
                    put("contentUrl", media.url)
                    put("streamType", "BUFFERED")
                    put("contentType", castContentType(media.url))
                    put(
                        "metadata",
                        buildJsonObject {
                            put("metadataType", 0)
                            put("title", media.title)
                            media.subtitle?.let { put("subtitle", it) }
                            media.imageUrl?.let { image ->
                                put("images", buildJsonArray { add(buildJsonObject { put("url", image) }) })
                            }
                        },
                    )
                },
            )
        }
        return true
    }

    private fun onMediaStatus(json: JsonObject, onPlayback: (CastPlayback) -> Unit): Boolean {
        val status = json["status"]?.jsonArray?.firstOrNull()?.jsonObject ?: return true
        status["mediaSessionId"]?.jsonPrimitive?.intOrNull?.let { mediaSessionId = it }
        status["media"]?.jsonObject?.get("duration")?.jsonPrimitive?.doubleOrNull?.let { seconds ->
            if (seconds > 0) durationMs = (seconds * 1000).toLong()
        }
        val playerState = status.string("playerState")
        if (playerState == "IDLE") {
            when (status.string("idleReason")) {
                "ERROR" -> throw CastLoadException()
                "FINISHED", "CANCELLED", "INTERRUPTED" -> if (mediaStarted) return false
            }
            return true
        }
        mediaStarted = true
        val positionMs = ((status["currentTime"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000).toLong()
        onPlayback(
            CastPlayback(
                isPlaying = playerState == "PLAYING",
                isBuffering = playerState == "BUFFERING",
                positionMs = positionMs,
                durationMs = durationMs,
            ),
        )
        return true
    }

    private suspend fun mediaCommand(type: String, extra: JsonObjectBuilder.() -> Unit = {}) {
        val transport = transportId ?: return
        val mediaSession = mediaSessionId ?: return
        request(NS_MEDIA, transport) {
            put("type", type)
            put("mediaSessionId", mediaSession)
            extra()
        }
    }

    private suspend fun request(namespace: String, destination: String, body: JsonObjectBuilder.() -> Unit) {
        requestId += 1
        val id = requestId
        send(namespace, destination) {
            body()
            put("requestId", id)
        }
    }

    private suspend fun send(namespace: String, destination: String, body: JsonObjectBuilder.() -> Unit) {
        channel.send(SENDER_ID, destination, namespace, buildJsonObject(body).toString())
    }
}

private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun castContentType(url: String): String {
    val path = url.substringBefore('?').substringBefore('#').lowercase()
    return when {
        path.endsWith(".m3u8") -> "application/x-mpegurl"
        path.endsWith(".mpd") -> "application/dash+xml"
        path.endsWith(".webm") -> "video/webm"
        path.endsWith(".mkv") -> "video/x-matroska"
        else -> "video/mp4"
    }
}

private const val SENDER_ID = "sender-0"
private const val RECEIVER_ID = "receiver-0"
private const val DEFAULT_MEDIA_RECEIVER = "CC1AD845"
private const val NS_CONNECTION = "urn:x-cast:com.google.cast.tp.connection"
private const val NS_HEARTBEAT = "urn:x-cast:com.google.cast.tp.heartbeat"
private const val NS_RECEIVER = "urn:x-cast:com.google.cast.receiver"
private const val NS_MEDIA = "urn:x-cast:com.google.cast.media"
private const val HEARTBEAT_INTERVAL_MS = 5_000L
private const val STATUS_INTERVAL_MS = 1_000L
private const val START_TIMEOUT_MS = 30_000L
