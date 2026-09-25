package io.github.dimitrysaf.provenio.core.localsync

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private const val IV_SIZE = 12
private const val TAG_BITS = 128
private const val MAX_FRAME_BYTES = 64 * 1024 * 1024
private const val READ_TIMEOUT_MS = 60_000
private const val BEACON_POLL_MS = 1_000
private const val BEACON_BUFFER_BYTES = 2_048

internal actual object LocalSyncPlatform {
    private val random = SecureRandom()

    actual val isSupported: Boolean = true

    actual fun randomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    actual fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray {
        val iv = randomBytes(IV_SIZE)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return iv + cipher.doFinal(plaintext)
    }

    actual fun decrypt(key: ByteArray, payload: ByteArray): ByteArray {
        require(payload.size > IV_SIZE) { "Frame is too short" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BITS, payload, 0, IV_SIZE),
        )
        return cipher.doFinal(payload, IV_SIZE, payload.size - IV_SIZE)
    }

    actual fun localIpv4Address(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.name.isContainerInterface() }
            .sortedBy { it.name.interfacePreference() }
            .flatMap { networkInterface -> networkInterface.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()

    actual suspend fun listen(port: Int): LocalSyncServer = withContext(Dispatchers.IO) {
        // The usual port keeps saved addresses valid; any free port will do if it is taken.
        val server = runCatching { bindServer(port) }.getOrElse { bindServer(0) }
        JvmSyncServer(server)
    }

    actual suspend fun connect(host: String, port: Int, timeoutMs: Int): LocalSyncConnection =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.soTimeout = READ_TIMEOUT_MS
                JvmSyncConnection(socket)
            } catch (error: Throwable) {
                runCatching { socket.close() }
                throw error
            }
        }

    actual suspend fun sendBeacon(payload: ByteArray, port: Int) {
        withContext(Dispatchers.IO) {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val address = InetAddress.getByName("255.255.255.255")
                socket.send(DatagramPacket(payload, payload.size, address, port))
            }
        }
    }

    actual suspend fun receiveBeacons(port: Int, onBeacon: (payload: ByteArray, host: String) -> Unit) {
        withContext(Dispatchers.IO) {
            val socket = DatagramSocket(null)
            try {
                socket.reuseAddress = true
                socket.broadcast = true
                // A short timeout lets the loop notice cancellation between packets.
                socket.soTimeout = BEACON_POLL_MS
                socket.bind(InetSocketAddress(port))
                val buffer = ByteArray(BEACON_BUFFER_BYTES)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val host = packet.address?.hostAddress ?: continue
                    onBeacon(packet.data.copyOf(packet.length), host)
                }
            } finally {
                socket.close()
            }
        }
    }

    private fun bindServer(port: Int): ServerSocket {
        val server = ServerSocket()
        try {
            server.reuseAddress = true
            server.bind(InetSocketAddress(port))
            return server
        } catch (error: Throwable) {
            runCatching { server.close() }
            throw error
        }
    }
}

private fun String.isContainerInterface(): Boolean =
    startsWith("docker") || startsWith("br-") || startsWith("veth") || startsWith("virbr") || startsWith("tun")

// Wi-Fi and Ethernet first, so a phone's mobile data link is never offered.
private fun String.interfacePreference(): Int = when {
    startsWith("wlan") || startsWith("wl") -> 0
    startsWith("en") || startsWith("eth") -> 1
    else -> 2
}

private class JvmSyncConnection(private val socket: Socket) : LocalSyncConnection {
    private val input = DataInputStream(socket.getInputStream().buffered())
    private val output = DataOutputStream(socket.getOutputStream().buffered())

    override suspend fun readFrame(): ByteArray = withContext(Dispatchers.IO) {
        val size = input.readInt()
        require(size in 0..MAX_FRAME_BYTES) { "Frame is too large" }
        val frame = ByteArray(size)
        input.readFully(frame)
        frame
    }

    override suspend fun writeFrame(frame: ByteArray) {
        withContext(Dispatchers.IO) {
            output.writeInt(frame.size)
            output.write(frame)
            output.flush()
        }
    }

    override fun close() {
        runCatching { socket.close() }
    }
}

private class JvmSyncServer(private val server: ServerSocket) : LocalSyncServer {
    override val port: Int get() = server.localPort

    override suspend fun accept(): LocalSyncConnection = withContext(Dispatchers.IO) {
        val socket = server.accept()
        socket.soTimeout = READ_TIMEOUT_MS
        JvmSyncConnection(socket)
    }

    override fun close() {
        runCatching { server.close() }
    }
}

internal actual fun localSyncQrMatrix(text: String): List<BooleanArray>? = runCatching {
    val hints = mapOf(EncodeHintType.MARGIN to 0)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
    List(matrix.height) { y -> BooleanArray(matrix.width) { x -> matrix.get(x, y) } }
}.getOrNull()
