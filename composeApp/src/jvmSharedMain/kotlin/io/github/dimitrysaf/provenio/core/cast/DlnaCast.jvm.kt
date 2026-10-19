package io.github.dimitrysaf.provenio.core.cast

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Casting to DLNA renderers, the standard nearly every smart TV speaks. Renderers are found with
 * SSDP and driven through their UPnP AVTransport service, behind the same channel the Cast
 * session talks to: the Cast messages it sends are carried out as AVTransport actions, and the
 * renderer's state comes back as the Cast messages the session expects.
 */
internal object DlnaCast {
    private val controlUrls = ConcurrentHashMap<String, String>()

    fun handles(device: CastDevice): Boolean = device.id.startsWith(DLNA_ID_PREFIX)

    /** Searches for renderers until cancelled, reporting each once its description has been read. */
    suspend fun discover(onDevice: (CastDevice) -> Unit) = withContext(Dispatchers.IO) {
        val sockets = openSearchSockets()
        if (sockets.isEmpty()) return@withContext
        val seen = ConcurrentHashMap.newKeySet<String>()
        val search = SSDP_SEARCH.encodeToByteArray()
        val group = InetAddress.getByName(SSDP_GROUP)
        try {
            coroutineScope {
                sockets.forEach { socket ->
                    launch {
                        val buffer = ByteArray(MAX_PACKET_BYTES)
                        while (currentCoroutineContext().isActive) {
                            val packet = DatagramPacket(buffer, buffer.size)
                            try {
                                socket.receive(packet)
                            } catch (_: SocketTimeoutException) {
                                continue
                            } catch (_: Exception) {
                                return@launch
                            }
                            val location = headerValue(buffer.decodeToString(0, packet.length), "LOCATION")
                                ?: continue
                            if (seen.add(location)) {
                                launch { describe(location)?.let(onDevice) }
                            }
                        }
                    }
                }
                while (currentCoroutineContext().isActive) {
                    sockets.forEach { socket ->
                        runCatching { socket.send(DatagramPacket(search, search.size, group, SSDP_PORT)) }
                    }
                    delay(SEARCH_INTERVAL_MS)
                }
            }
        } finally {
            sockets.forEach { runCatching { it.close() } }
        }
    }

    fun open(device: CastDevice): CastChannel {
        val controlUrl = controlUrls[device.id] ?: error("No AVTransport control URL for ${device.name}")
        return DlnaChannel(controlUrl)
    }

    private fun openSearchSockets(): List<DatagramSocket> {
        val addresses = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }
            .getOrDefault(emptyList())
            .filter { runCatching { it.isUp && !it.isLoopback && it.supportsMulticast() }.getOrDefault(false) }
            .flatMap { nic -> nic.inetAddresses.toList().filterIsInstance<Inet4Address>() }
        return addresses.mapNotNull { address ->
            runCatching { DatagramSocket(InetSocketAddress(address, 0)).apply { soTimeout = RECEIVE_TIMEOUT_MS } }.getOrNull()
        }
    }

    /** Reads a renderer's description; null for a device without an AVTransport service. */
    private fun describe(location: String): CastDevice? = runCatching {
        val xml = httpGet(location)
        val service = Regex("<service>(.*?)</service>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .map { it.groupValues[1] }
            .firstOrNull { it.contains(AV_TRANSPORT) }
            ?: return null
        val control = xmlValue(service, "controlURL") ?: return null
        val base = xmlValue(xml, "URLBase")?.takeIf { it.isNotBlank() } ?: location
        val controlUrl = URL(URL(base), control).toString()
        val udn = xmlValue(xml, "UDN") ?: location
        val id = DLNA_ID_PREFIX + udn
        controlUrls[id] = controlUrl
        val url = URI(location)
        CastDevice(
            id = id,
            name = xmlValue(xml, "friendlyName")?.let(::unescapeXml) ?: url.host,
            model = listOfNotNull(xmlValue(xml, "modelName")?.let(::unescapeXml), "DLNA").joinToString(" · "),
            host = url.host,
            port = url.port.takeIf { it > 0 } ?: 80,
        )
    }.getOrNull()
}

/** A DLNA renderer behind the Cast channel the session uses, one AVTransport action per command. */
private class DlnaChannel(private val controlUrl: String) : CastChannel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inbox = Channel<CastFrame>(Channel.UNLIMITED)
    private val actions = Mutex()
    private var mediaSessionId = 0
    private var playing = false
    private var positionMs = 0L
    private var durationMs = 0L

    override suspend fun send(source: String, destination: String, namespace: String, payload: String) {
        val json = runCatching { Json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return
        when (json.text("type")) {
            "LAUNCH" -> reply(
                NS_RECEIVER,
                buildJsonObject {
                    put("type", "RECEIVER_STATUS")
                    put(
                        "status",
                        buildJsonObject {
                            put(
                                "applications",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("appId", DEFAULT_MEDIA_RECEIVER)
                                            put("transportId", TRANSPORT_ID)
                                            put("sessionId", TRANSPORT_ID)
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            )
            "LOAD" -> scope.launch { load(json) }
            "PLAY" -> scope.launch { action("Play", "<Speed>1</Speed>") }
            "PAUSE" -> scope.launch { action("Pause") }
            "SEEK" -> scope.launch {
                val seconds = json["currentTime"]?.jsonPrimitive?.doubleOrNull ?: return@launch
                action("Seek", "<Unit>REL_TIME</Unit><Target>${formatTime((seconds * 1000).toLong())}</Target>")
            }
            "STOP" -> scope.launch { action("Stop") }
            "GET_STATUS" -> if (namespace == NS_MEDIA) scope.launch { reportStatus() }
        }
    }

    override suspend fun receive(): CastFrame = inbox.receive()

    override fun close() {
        scope.cancel()
        inbox.close()
    }

    private suspend fun load(json: JsonObject) {
        val media = json["media"]?.jsonObject
        val url = media?.text("contentUrl") ?: media?.text("contentId") ?: return
        val mimeType = media?.text("contentType") ?: "video/mp4"
        val startMs = ((json["currentTime"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000).toLong()
        try {
            runCatching { action("Stop") }
            action(
                "SetAVTransportURI",
                "<CurrentURI>${escapeXml(url)}</CurrentURI><CurrentURIMetaData>${escapeXml(didlLite(url, mimeType))}</CurrentURIMetaData>",
            )
            action("Play", "<Speed>1</Speed>")
            mediaSessionId += 1
            playing = false
            positionMs = 0L
            durationMs = 0L
            mediaStatus("BUFFERING")
            if (startMs > 0L) seekOncePlaying(startMs)
        } catch (_: Exception) {
            reply(NS_MEDIA, buildJsonObject { put("type", "LOAD_FAILED") })
        }
    }

    private suspend fun seekOncePlaying(startMs: Long) {
        repeat(SEEK_WAIT_ATTEMPTS) {
            delay(SEEK_WAIT_STEP_MS)
            val state = runCatching { transportState() }.getOrNull()
            if (state == "PLAYING" || state == "PAUSED_PLAYBACK") {
                runCatching { action("Seek", "<Unit>REL_TIME</Unit><Target>${formatTime(startMs)}</Target>") }
                return
            }
        }
    }

    private suspend fun reportStatus() {
        if (mediaSessionId == 0) return
        val state = runCatching { transportState() }.getOrNull() ?: return
        runCatching {
            val position = action("GetPositionInfo")
            parseTime(xmlValue(position, "RelTime"))?.let { positionMs = it }
            parseTime(xmlValue(position, "TrackDuration"))?.takeIf { it > 0L }?.let { durationMs = it }
        }
        when (state) {
            "PLAYING" -> {
                playing = true
                mediaStatus("PLAYING")
            }
            "PAUSED_PLAYBACK", "PAUSED_RECORDING" -> {
                playing = true
                mediaStatus("PAUSED")
            }
            "STOPPED", "NO_MEDIA_PRESENT" -> if (playing) {
                val finished = durationMs > 0L && positionMs >= durationMs - FINISHED_MARGIN_MS
                mediaStatus("IDLE", idleReason = if (finished) "FINISHED" else "CANCELLED")
            } else {
                mediaStatus("BUFFERING")
            }
            else -> mediaStatus("BUFFERING")
        }
    }

    private suspend fun transportState(): String? = xmlValue(action("GetTransportInfo"), "CurrentTransportState")

    private suspend fun mediaStatus(playerState: String, idleReason: String? = null) = reply(
        NS_MEDIA,
        buildJsonObject {
            put("type", "MEDIA_STATUS")
            put(
                "status",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("mediaSessionId", mediaSessionId)
                            put("playerState", playerState)
                            idleReason?.let { put("idleReason", it) }
                            put("currentTime", positionMs / 1000.0)
                            put("media", buildJsonObject { if (durationMs > 0L) put("duration", durationMs / 1000.0) })
                        },
                    )
                },
            )
        },
    )

    private suspend fun reply(namespace: String, body: JsonObject) {
        inbox.send(CastFrame(TRANSPORT_ID, "sender-0", namespace, body.toString()))
    }

    /** Runs one AVTransport action on instance 0 and returns the response body; throws when the renderer refuses it. */
    private suspend fun action(name: String, arguments: String = ""): String = actions.withLock {
        withContext(Dispatchers.IO) {
            val body = """<?xml version="1.0" encoding="utf-8"?><s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><s:Body><u:$name xmlns:u="$AV_TRANSPORT"><InstanceID>0</InstanceID>$arguments</u:$name></s:Body></s:Envelope>"""
            val connection = URL(controlUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = HTTP_TIMEOUT_MS
                connection.readTimeout = HTTP_TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                connection.setRequestProperty("SOAPACTION", "\"$AV_TRANSPORT#$name\"")
                connection.outputStream.use { it.write(body.encodeToByteArray()) }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.use { it.readBytes().decodeToString() }
                    .orEmpty()
                check(code in 200..299) { "$name failed with HTTP $code" }
                text
            } finally {
                connection.disconnect()
            }
        }
    }
}

private fun didlLite(url: String, mimeType: String): String =
    """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"><item id="0" parentID="-1" restricted="1"><dc:title>Provenio</dc:title><upnp:class>object.item.videoItem</upnp:class><res protocolInfo="http-get:*:$mimeType:*">${escapeXml(url)}</res></item></DIDL-Lite>"""

private fun httpGet(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = HTTP_TIMEOUT_MS
        connection.readTimeout = HTTP_TIMEOUT_MS
        connection.inputStream.use { it.readBytes().decodeToString() }
    } finally {
        connection.disconnect()
    }
}

private fun headerValue(response: String, name: String): String? =
    response.lineSequence()
        .firstOrNull { it.startsWith("$name:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

private fun xmlValue(xml: String, tag: String): String? =
    Regex("<(?:[A-Za-z0-9_]+:)?$tag(?:\\s[^>]*)?>(.*?)</(?:[A-Za-z0-9_]+:)?$tag>", RegexOption.DOT_MATCHES_ALL)
        .find(xml)
        ?.groupValues
        ?.get(1)
        ?.trim()

private fun escapeXml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

private fun unescapeXml(text: String): String = text
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&amp;", "&")

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60)
}

private fun parseTime(value: String?): Long? {
    val parts = value?.trim()?.substringBefore('.')?.split(':')?.takeIf { it.size == 3 } ?: return null
    val (hours, minutes, seconds) = parts.map { it.toLongOrNull() ?: return null }
    return ((hours * 3600) + (minutes * 60) + seconds) * 1000
}

private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private const val DLNA_ID_PREFIX = "dlna:"
private const val AV_TRANSPORT = "urn:schemas-upnp-org:service:AVTransport:1"
private const val SSDP_GROUP = "239.255.255.250"
private const val SSDP_PORT = 1900
private const val SSDP_SEARCH =
    "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $AV_TRANSPORT\r\n\r\n"
private const val TRANSPORT_ID = "dlna-0"
private const val DEFAULT_MEDIA_RECEIVER = "CC1AD845"
private const val NS_RECEIVER = "urn:x-cast:com.google.cast.receiver"
private const val NS_MEDIA = "urn:x-cast:com.google.cast.media"
private const val SEARCH_INTERVAL_MS = 5_000L
private const val RECEIVE_TIMEOUT_MS = 1_000
private const val MAX_PACKET_BYTES = 4_096
private const val HTTP_TIMEOUT_MS = 5_000
private const val SEEK_WAIT_ATTEMPTS = 20
private const val SEEK_WAIT_STEP_MS = 500L
private const val FINISHED_MARGIN_MS = 15_000L
