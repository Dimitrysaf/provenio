package io.github.dimitrysaf.provenio.core.cast

import java.io.BufferedInputStream
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

internal actual object CastRelay {
    private val lock = Any()
    private var server: ServerSocket? = null
    private var target: String? = null
    private var token: String = ""

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "cast-relay").apply { isDaemon = true }
    }

    private val client = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    actual fun start(targetUrl: String, fileName: String): String? = synchronized(lock) {
        closeLocked()
        val address = localNetworkAddress() ?: return null
        val socket = runCatching { ServerSocket(0, Backlog, address) }.getOrNull() ?: return null
        server = socket
        target = targetUrl
        token = newToken()
        executor.execute { acceptLoop(socket) }
        "http://${address.hostAddress}:${socket.localPort}/$token/${safeFileName(fileName)}"
    }

    actual fun stop() {
        synchronized(lock) { closeLocked() }
    }

    actual fun vpnActive(): Boolean = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { nic ->
            nic.isUp && !nic.isLoopback && (nic.isPointToPoint || nic.name.lowercase().isVpnName())
        }
    }.getOrDefault(false)

    private fun closeLocked() {
        runCatching { server?.close() }
        server = null
        target = null
        token = ""
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val connection = runCatching { socket.accept() }.getOrNull() ?: break
            executor.execute { serve(connection) }
        }
    }

    /** Answers one request from the cast device by fetching the same range from the stream here. */
    private fun serve(connection: Socket) {
        connection.use { socket ->
            runCatching {
                val input = BufferedInputStream(socket.getInputStream())
                val output = socket.getOutputStream()
                val requestLine = input.readHttpLine() ?: return
                val headers = generateSequence { input.readHttpLine()?.takeIf(String::isNotEmpty) }
                    .mapNotNull { line ->
                        val colon = line.indexOf(':')
                        if (colon <= 0) null else line.substring(0, colon).trim().lowercase() to line.substring(colon + 1).trim()
                    }
                    .toMap()
                val parts = requestLine.split(' ')
                val method = parts.getOrNull(0).orEmpty().uppercase()
                val path = parts.getOrNull(1).orEmpty()
                val (expectedToken, targetUrl) = synchronized(lock) { token to target }
                when {
                    targetUrl == null || expectedToken.isEmpty() || !path.startsWith("/$expectedToken/") ->
                        output.writeStatus(404, "Not Found")
                    method == "OPTIONS" -> output.writeStatus(204, "No Content")
                    method != "GET" && method != "HEAD" -> output.writeStatus(405, "Method Not Allowed")
                    targetUrl.startsWith("file:") -> serveFile(method, File(URI(targetUrl)), headers["range"], output)
                    else -> relay(method, targetUrl, headers["range"], output)
                }
            }
        }
    }

    private fun relay(method: String, targetUrl: String, range: String?, output: OutputStream) {
        val request = Request.Builder()
            .url(targetUrl)
            .method(method, null)
            .apply { if (!range.isNullOrBlank()) header("Range", range) }
            .build()
        client.newCall(request).execute().use { response ->
            val head = StringBuilder()
            head.append("HTTP/1.1 ").append(response.code).append(' ')
                .append(response.message.ifBlank { "OK" }).append("\r\n")
            listOf("Content-Type", "Content-Length", "Content-Range", "Last-Modified", "ETag").forEach { name ->
                response.header(name)?.let { head.append(name).append(": ").append(it).append("\r\n") }
            }
            head.append("Accept-Ranges: ").append(response.header("Accept-Ranges") ?: "bytes").append("\r\n")
            head.append(CorsHeaders)
            head.append("Connection: close\r\n\r\n")
            output.write(head.toString().toByteArray(Charsets.ISO_8859_1))
            if (method == "GET") {
                response.body?.byteStream()?.use { body -> body.copyTo(output, CopyBufferBytes) }
            }
            output.flush()
        }
    }

    /** Answers from a downloaded file on this device, the asked-for range of it or all of it. */
    private fun serveFile(method: String, file: File, range: String?, output: OutputStream) {
        if (!file.isFile) {
            output.writeStatus(404, "Not Found")
            return
        }
        val length = file.length()
        val requested = parseRange(range, length)
        if (range != null && requested == null) {
            output.write(
                "HTTP/1.1 416 Range Not Satisfiable\r\n${CorsHeaders}Content-Range: bytes */$length\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    .toByteArray(Charsets.ISO_8859_1),
            )
            output.flush()
            return
        }
        val start = requested?.first ?: 0L
        val end = requested?.last ?: (length - 1)
        val head = StringBuilder()
        if (requested != null) {
            head.append("HTTP/1.1 206 Partial Content\r\n")
            head.append("Content-Range: bytes ").append(start).append('-').append(end).append('/').append(length).append("\r\n")
        } else {
            head.append("HTTP/1.1 200 OK\r\n")
        }
        head.append("Content-Type: ").append(fileContentType(file.name)).append("\r\n")
        head.append("Content-Length: ").append(end - start + 1).append("\r\n")
        head.append("Accept-Ranges: bytes\r\n")
        head.append(CorsHeaders)
        head.append("Connection: close\r\n\r\n")
        output.write(head.toString().toByteArray(Charsets.ISO_8859_1))
        if (method == "GET") {
            RandomAccessFile(file, "r").use { source ->
                source.seek(start)
                val buffer = ByteArray(CopyBufferBytes)
                var remaining = end - start + 1
                while (remaining > 0) {
                    val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }
        output.flush()
    }

    /** The byte range a `Range: bytes=start-end` header asks for, clamped to the file; null when there is none or it is unusable. */
    private fun parseRange(range: String?, length: Long): LongRange? {
        val spec = range?.trim()?.takeIf { it.startsWith("bytes=") }?.removePrefix("bytes=")?.substringBefore(',') ?: return null
        val startText = spec.substringBefore('-').trim()
        val endText = spec.substringAfter('-', "").trim()
        if (length <= 0) return null
        val (start, end) = if (startText.isEmpty()) {
            val suffix = endText.toLongOrNull() ?: return null
            (length - suffix).coerceAtLeast(0) to length - 1
        } else {
            val start = startText.toLongOrNull() ?: return null
            start to (endText.toLongOrNull()?.coerceAtMost(length - 1) ?: (length - 1))
        }
        return if (start in 0..end && start < length) start..end else null
    }

    private fun fileContentType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "ts", "m2ts" -> "video/mp2t"
        "mov" -> "video/quicktime"
        else -> "video/mp4"
    }

    private fun OutputStream.writeStatus(code: Int, message: String) {
        write("HTTP/1.1 $code $message\r\n${CorsHeaders}Content-Length: 0\r\nConnection: close\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
        flush()
    }

    private fun InputStream.readHttpLine(): String? {
        val line = StringBuilder()
        while (true) {
            val byte = read()
            if (byte < 0) return line.takeIf { it.isNotEmpty() }?.toString()
            if (byte == '\n'.code) return line.toString().trimEnd('\r')
            line.append(byte.toChar())
            if (line.length > MaxHeaderLineChars) return null
        }
    }

    /**
     * The Wi-Fi or Ethernet address a cast device can reach this device at: a private IPv4
     * address on a real network interface, never a VPN's.
     */
    private fun localNetworkAddress(): InetAddress? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { nic ->
                nic.isUp && !nic.isLoopback && !nic.isVirtual && !nic.isPointToPoint && !nic.name.lowercase().isVpnName()
            }
            .sortedBy { nic -> if (nic.name.lowercase().isLanName()) 0 else 1 }
            .firstNotNullOfOrNull { nic ->
                nic.inetAddresses.toList().firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
            }
    }.getOrNull()

    private fun String.isVpnName(): Boolean = VpnPrefixes.any(::startsWith)

    private fun String.isLanName(): Boolean = LanPrefixes.any(::startsWith)

    private fun newToken(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun safeFileName(fileName: String): String {
        val name = fileName.substringAfterLast('/').substringAfterLast('\\')
            .map { if (it.isLetterOrDigit() || it == '.' || it == '-' || it == '_') it else '_' }
            .joinToString("")
        return name.takeIf { '.' in it } ?: "stream.mp4"
    }

    private val VpnPrefixes = listOf("tun", "tap", "wg", "ppp", "ipsec", "utun", "nordlynx", "proton")
    private val LanPrefixes = listOf("wlan", "wl", "eth", "en", "ap", "swlan")
    private const val CorsHeaders = "Access-Control-Allow-Origin: *\r\nAccess-Control-Allow-Headers: Range\r\nAccess-Control-Expose-Headers: Content-Length, Content-Range\r\n" +
        "transferMode.dlna.org: Streaming\r\ncontentFeatures.dlna.org: DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000\r\n"
    private const val Backlog = 16
    private const val CopyBufferBytes = 64 * 1024
    private const val MaxHeaderLineChars = 8 * 1024
}
