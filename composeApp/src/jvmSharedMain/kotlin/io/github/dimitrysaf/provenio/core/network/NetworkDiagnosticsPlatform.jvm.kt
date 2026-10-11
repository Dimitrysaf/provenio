package io.github.dimitrysaf.provenio.core.network

import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink

internal actual object NetworkDiagnosticsPlatform {
    actual val isSupported: Boolean = true

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    actual fun connections(): List<NetworkConnectionInfo> =
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .filterNot { it.name.isContainerInterface() }
            .mapNotNull { networkInterface ->
                val addresses = networkInterface.inetAddresses.toList()
                val ipv4 = addresses.filterIsInstance<Inet4Address>().mapNotNull { it.hostAddress }
                val ipv6 = addresses.filterIsInstance<Inet6Address>()
                    .filterNot { it.isLinkLocalAddress || it.isLoopbackAddress }
                    .mapNotNull { it.hostAddress?.substringBefore('%') }
                if (ipv4.isEmpty() && ipv6.isEmpty()) return@mapNotNull null
                NetworkConnectionInfo(
                    name = networkInterface.displayName?.takeIf(String::isNotBlank) ?: networkInterface.name,
                    kind = networkKind(networkInterface.name, networkInterface.displayName.orEmpty()),
                    ipv4 = ipv4,
                    ipv6 = ipv6,
                )
            }
            .sortedBy { it.kind.ordinal }

    actual suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    actual suspend fun timeRequest(url: String): Long = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        client.newCall(speedTestRequest(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.bytes()
        }
        (System.nanoTime() - start) / 1_000_000
    }

    actual suspend fun download(
        url: String,
        maxMillis: Long,
        onProgress: (Long, Long) -> Unit,
    ): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val call = client.newCall(speedTestRequest(url).build())
        val start = System.nanoTime()
        var bytes = 0L
        var lastReport = 0L
        call.execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val stream = response.body?.byteStream() ?: throw IOException("Empty body")
            val buffer = ByteArray(ChunkBytes)
            while (true) {
                coroutineContext.ensureActive()
                val read = stream.read(buffer)
                if (read < 0) break
                bytes += read
                val elapsed = elapsedMillis(start)
                if (elapsed - lastReport >= ReportEveryMillis) {
                    lastReport = elapsed
                    onProgress(bytes, elapsed)
                }
                if (elapsed >= maxMillis) {
                    call.cancel()
                    break
                }
            }
        }
        bytes to elapsedMillis(start)
    }

    actual suspend fun upload(
        url: String,
        totalBytes: Long,
        maxMillis: Long,
        onProgress: (Long, Long) -> Unit,
    ): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val context = coroutineContext
        val start = System.nanoTime()
        var sent = 0L
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()

            override fun contentLength() = totalBytes

            override fun writeTo(sink: BufferedSink) {
                val chunk = ByteArray(ChunkBytes) { (it * 31).toByte() }
                var lastReport = 0L
                while (sent < totalBytes) {
                    context.ensureActive()
                    val size = minOf(chunk.size.toLong(), totalBytes - sent).toInt()
                    sink.write(chunk, 0, size)
                    sent += size
                    val elapsed = elapsedMillis(start)
                    if (elapsed - lastReport >= ReportEveryMillis) {
                        lastReport = elapsed
                        onProgress(sent, elapsed)
                    }
                    // Stopping short of the length announced fails the request; what was sent by
                    // then is the measurement.
                    if (elapsed >= maxMillis) throw IOException(UploadTimeUp)
                }
            }
        }
        try {
            client.newCall(speedTestRequest(url).post(body).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            }
        } catch (error: IOException) {
            if (error.message != UploadTimeUp || sent == 0L) throw error
        }
        sent to elapsedMillis(start)
    }

    /**
     * A request to Cloudflare's speed test servers as its own speed test page makes it, which
     * they expect, and with compression off so the bytes counted are the bytes that crossed the
     * network.
     */
    private fun speedTestRequest(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("Referer", "https://speed.cloudflare.com/")
        .header("Origin", "https://speed.cloudflare.com")
        .header("Accept-Encoding", "identity")
        .header("Cache-Control", "no-store")

    private fun elapsedMillis(start: Long): Long = (System.nanoTime() - start) / 1_000_000

    private fun networkKind(name: String, displayName: String): NetworkKind {
        val id = name.lowercase()
        val label = displayName.lowercase()
        return when {
            id.startsWith("tun") || id.startsWith("tap") || id.startsWith("wg") || id.startsWith("utun") ||
                id.startsWith("ipsec") || "vpn" in label || "wireguard" in label || "tap-" in label -> NetworkKind.Vpn
            id.startsWith("wlan") || id.startsWith("wl") || id.startsWith("wifi") || id.startsWith("ath") ||
                "wi-fi" in label || "wireless" in label || "wlan" in label -> NetworkKind.WiFi
            id.startsWith("rmnet") || id.startsWith("ccmni") || id.startsWith("wwan") || id.startsWith("pdp") ||
                id.startsWith("radio") || "mobile" in label || "cellular" in label -> NetworkKind.Cellular
            id.startsWith("eth") || id.startsWith("en") || id.startsWith("em") || "ethernet" in label -> NetworkKind.Ethernet
            else -> NetworkKind.Other
        }
    }

    private fun String.isContainerInterface(): Boolean {
        val id = lowercase()
        return ContainerPrefixes.any(id::startsWith)
    }

    private val ContainerPrefixes = listOf("docker", "veth", "br-", "virbr", "vmnet", "vboxnet", "lxc", "cni", "flannel")

    private const val ChunkBytes = 64 * 1024
    private const val ReportEveryMillis = 200L
    private const val UploadTimeUp = "speed test time is up"
}
