package io.github.dimitrysaf.provenio.core.cast

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal actual object CastPlatform {
    actual val isSupported: Boolean = true

    actual suspend fun discover(onDevice: (CastDevice) -> Unit) = withContext(Dispatchers.IO) {
        val sockets = openDiscoverySockets()
        if (sockets.isEmpty()) return@withContext
        val query = buildMdnsQuery()
        val group = InetAddress.getByName(MDNS_GROUP)
        try {
            coroutineScope {
                sockets.forEach { socket -> launch { listen(socket, onDevice) } }
                while (currentCoroutineContext().isActive) {
                    sockets.forEach { socket ->
                        runCatching { socket.send(DatagramPacket(query, query.size, group, MDNS_PORT)) }
                    }
                    delay(QUERY_INTERVAL_MS)
                }
            }
        } finally {
            sockets.forEach { runCatching { it.close() } }
        }
    }

    actual suspend fun open(device: CastDevice): CastChannel = withContext(Dispatchers.IO) {
        val context = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(CastCertificateTrust), SecureRandom())
        }
        val socket = context.socketFactory.createSocket() as SSLSocket
        try {
            socket.connect(InetSocketAddress(device.host, device.port), CONNECT_TIMEOUT_MS)
            socket.startHandshake()
        } catch (error: Exception) {
            runCatching { socket.close() }
            throw error
        }
        JvmCastChannel(socket)
    }

    private fun openDiscoverySockets(): List<MulticastSocket> {
        val interfaces = runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }.getOrDefault(emptyList())
        val sockets = interfaces
            .filter { runCatching { it.isUp && !it.isLoopback && it.supportsMulticast() }.getOrDefault(false) }
            .flatMap { nic ->
                nic.inetAddresses.toList().filterIsInstance<Inet4Address>().map { nic to it }
            }
            .mapNotNull { (nic, address) ->
                runCatching {
                    MulticastSocket(InetSocketAddress(address, 0)).apply {
                        networkInterface = nic
                        timeToLive = MDNS_TTL
                        soTimeout = RECEIVE_TIMEOUT_MS
                    }
                }.getOrNull()
            }
        if (sockets.isNotEmpty()) return sockets
        return listOfNotNull(
            runCatching { MulticastSocket().apply { timeToLive = MDNS_TTL; soTimeout = RECEIVE_TIMEOUT_MS } }.getOrNull(),
        )
    }

    private suspend fun listen(socket: MulticastSocket, onDevice: (CastDevice) -> Unit) {
        val buffer = ByteArray(MAX_PACKET_BYTES)
        while (currentCoroutineContext().isActive) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (_: SocketTimeoutException) {
                continue
            } catch (_: Exception) {
                return
            }
            val host = packet.address?.hostAddress ?: continue
            parseCastDevices(buffer.copyOf(packet.length), host).forEach(onDevice)
        }
    }
}

private class JvmCastChannel(private val socket: SSLSocket) : CastChannel {
    private val input = DataInputStream(BufferedInputStream(socket.inputStream))
    private val output = DataOutputStream(BufferedOutputStream(socket.outputStream))
    private val writeLock = Mutex()

    override suspend fun send(source: String, destination: String, namespace: String, payload: String) {
        val message = encodeCastMessage(source, destination, namespace, payload)
        writeLock.withLock {
            withContext(Dispatchers.IO) {
                output.writeInt(message.size)
                output.write(message)
                output.flush()
            }
        }
    }

    override suspend fun receive(): CastFrame = withContext(Dispatchers.IO) {
        val size = input.readInt()
        require(size in 0..MAX_FRAME_BYTES) { "Cast frame of $size bytes" }
        val message = ByteArray(size)
        input.readFully(message)
        decodeCastMessage(message)
    }

    override fun close() {
        runCatching { socket.close() }
    }
}

// Cast receivers present certificates signed by Google's private device CA, which no system
// trust store holds, so the connection is encrypted without being verified, as in every Cast sender.
private object CastCertificateTrust : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

private fun encodeCastMessage(source: String, destination: String, namespace: String, payload: String): ByteArray {
    val out = ByteArrayOutputStream()
    out.writeVarint(1 shl 3)
    out.writeVarint(0)
    out.writeField(2, source.encodeToByteArray())
    out.writeField(3, destination.encodeToByteArray())
    out.writeField(4, namespace.encodeToByteArray())
    out.writeVarint(5 shl 3)
    out.writeVarint(0)
    out.writeField(6, payload.encodeToByteArray())
    return out.toByteArray()
}

private fun decodeCastMessage(message: ByteArray): CastFrame {
    var source = ""
    var destination = ""
    var namespace = ""
    var payload = ""
    var position = 0
    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val byte = message[position++].toInt() and 0xFF
            result = result or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) return result
            shift += 7
        }
    }
    while (position < message.size) {
        val key = readVarint().toInt()
        when (key and 0x07) {
            0 -> readVarint()
            1 -> position += 8
            2 -> {
                val length = readVarint().toInt()
                val value = message.decodeToString(position, position + length)
                when (key ushr 3) {
                    2 -> source = value
                    3 -> destination = value
                    4 -> namespace = value
                    6 -> payload = value
                }
                position += length
            }
            5 -> position += 4
            else -> break
        }
    }
    return CastFrame(source, destination, namespace, payload)
}

private fun ByteArrayOutputStream.writeVarint(value: Int) {
    var remaining = value
    while (remaining and 0x7F.inv() != 0) {
        write((remaining and 0x7F) or 0x80)
        remaining = remaining ushr 7
    }
    write(remaining)
}

private fun ByteArrayOutputStream.writeField(field: Int, value: ByteArray) {
    writeVarint((field shl 3) or 2)
    writeVarint(value.size)
    write(value)
}

private fun buildMdnsQuery(): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(byteArrayOf(0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0))
    CAST_SERVICE.split('.').forEach { label ->
        val bytes = label.encodeToByteArray()
        out.write(bytes.size)
        out.write(bytes)
    }
    out.write(0)
    out.write(0)
    out.write(TYPE_PTR)
    out.write(0x80)
    out.write(0x01)
    return out.toByteArray()
}

private class DnsReader(private val data: ByteArray) {
    var position = 0

    fun u8(): Int = data[position++].toInt() and 0xFF

    fun u16(): Int = (u8() shl 8) or u8()

    fun skip(bytes: Int) {
        position += bytes
    }

    fun name(): String {
        val labels = mutableListOf<String>()
        var cursor = position
        var jumped = false
        var hops = 0
        while (true) {
            val length = data[cursor].toInt() and 0xFF
            when {
                length == 0 -> {
                    cursor++
                    break
                }
                length and 0xC0 == 0xC0 -> {
                    val pointer = ((length and 0x3F) shl 8) or (data[cursor + 1].toInt() and 0xFF)
                    if (!jumped) position = cursor + 2
                    jumped = true
                    cursor = pointer
                    check(++hops <= MAX_NAME_POINTERS)
                }
                else -> {
                    labels += data.decodeToString(cursor + 1, cursor + 1 + length)
                    cursor += 1 + length
                }
            }
        }
        if (!jumped) position = cursor
        return labels.joinToString(".")
    }
}

private fun parseCastDevices(data: ByteArray, sourceHost: String): List<CastDevice> = runCatching {
    val reader = DnsReader(data)
    reader.skip(4)
    val questions = reader.u16()
    val records = reader.u16() + reader.u16() + reader.u16()
    repeat(questions) {
        reader.name()
        reader.skip(4)
    }
    val services = mutableMapOf<String, Pair<Int, String>>()
    val texts = mutableMapOf<String, Map<String, String>>()
    val addresses = mutableMapOf<String, String>()
    val instances = linkedSetOf<String>()
    repeat(records) {
        val name = reader.name().lowercase()
        val type = reader.u16()
        reader.skip(6)
        val length = reader.u16()
        val start = reader.position
        when (type) {
            TYPE_PTR -> if (name == CAST_SERVICE) instances += reader.name().lowercase()
            TYPE_SRV -> {
                reader.skip(4)
                val port = reader.u16()
                services[name] = port to reader.name().lowercase()
            }
            TYPE_TXT -> texts[name] = parseTxt(data, start, length)
            TYPE_A -> if (length == 4) {
                addresses[name] = (0 until 4).joinToString(".") { (data[start + it].toInt() and 0xFF).toString() }
            }
        }
        reader.position = start + length
    }
    (instances + services.keys + texts.keys)
        .filter { it.endsWith(".$CAST_SERVICE") }
        .distinct()
        .map { instance ->
            val text = texts[instance].orEmpty()
            val service = services[instance]
            CastDevice(
                id = text["id"] ?: instance,
                name = text["fn"] ?: instance.removeSuffix(".$CAST_SERVICE"),
                model = text["md"],
                host = service?.second?.let(addresses::get) ?: sourceHost,
                port = service?.first ?: DEFAULT_CAST_PORT,
            )
        }
}.getOrDefault(emptyList())

private fun parseTxt(data: ByteArray, start: Int, length: Int): Map<String, String> {
    val entries = mutableMapOf<String, String>()
    var cursor = start
    val end = start + length
    while (cursor < end) {
        val size = data[cursor].toInt() and 0xFF
        val entry = data.decodeToString(cursor + 1, minOf(end, cursor + 1 + size))
        val separator = entry.indexOf('=')
        if (separator > 0) entries[entry.substring(0, separator)] = entry.substring(separator + 1)
        cursor += 1 + size
    }
    return entries
}

private const val CAST_SERVICE = "_googlecast._tcp.local"
private const val MDNS_GROUP = "224.0.0.251"
private const val MDNS_PORT = 5353
private const val MDNS_TTL = 255
private const val TYPE_A = 1
private const val TYPE_PTR = 12
private const val TYPE_TXT = 16
private const val TYPE_SRV = 33
private const val DEFAULT_CAST_PORT = 8009
private const val QUERY_INTERVAL_MS = 3_000L
private const val RECEIVE_TIMEOUT_MS = 500
private const val CONNECT_TIMEOUT_MS = 5_000
private const val MAX_PACKET_BYTES = 9_000
private const val MAX_FRAME_BYTES = 64 * 1024
private const val MAX_NAME_POINTERS = 32
