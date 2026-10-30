package io.github.dimitrysaf.provenio.core.party

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncPlatform
import io.github.dimitrysaf.provenio.core.localsync.isContainerInterface
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

private const val MAX_IPV6_ADDRESSES = 2
private const val LEASE_SECONDS = 3_600
private const val MAPPING_DESCRIPTION = "Provenio watch party"

private const val SSDP_ADDRESS = "239.255.255.250"
private const val SSDP_PORT = 1_900
private const val SSDP_WAIT_MS = 2_500L
private const val SSDP_SETTLE_MS = 400L
private const val HTTP_CONNECT_TIMEOUT_MS = 3_000
private const val PROBE_CONNECT_TIMEOUT_MS = 800
private const val HTTP_READ_TIMEOUT_MS = 5_000
private const val HTTP_MAX_BODY_BYTES = 512 * 1024

private const val NAT_PMP_PORT = 5_351
private const val NAT_PMP_FIRST_WAIT_MS = 250
private const val NAT_PMP_TRIES = 3
private const val NAT_PMP_CANDIDATES = 4

private const val UPNP_CONFLICT = 718
private const val PORT_ATTEMPTS = 5

private val log = Logger.withTag("WatchParty")

// Where common routers keep their UPnP description, asked over plain HTTP when no router answers the search.
private val KnownDescriptions = listOf(
    1900 to "/gatedesc.xml",
    1900 to "/rootDesc.xml",
    1900 to "/igd.xml",
    1900 to "/description.xml",
    5000 to "/rootDesc.xml",
    52869 to "/picsdesc.xml",
    49000 to "/igddesc.xml",
    49000 to "/igd2desc.xml",
    49152 to "/rootDesc.xml",
    49152 to "/IGDdevicedesc_brlan0.xml",
    2869 to "/IGatewayDeviceDescDoc",
    5431 to "/igdevicedesc.xml",
    1780 to "/InternetGatewayDevice.xml",
    80 to "/rootDesc.xml",
)

private val SearchTargets = listOf(
    "urn:schemas-upnp-org:device:InternetGatewayDevice:2",
    "urn:schemas-upnp-org:device:InternetGatewayDevice:1",
    "urn:schemas-upnp-org:service:WANIPConnection:1",
    "urn:schemas-upnp-org:service:WANPPPConnection:1",
)

internal actual object PartyPortMapper {
    actual suspend fun map(internalPort: Int): PartyPortMapping? = withContext(Dispatchers.IO) {
        val gateways = runCatching { discoverGateways() }
            .onFailure { log.w(it) { "UPnP search failed" } }
            .getOrDefault(emptyList())
        log.i { "UPnP routers found: ${gateways.map { it.first }}" }
        // A firewall or a switch can drop the search and its answers; the router's own address still answers HTTP.
        val described = gateways.ifEmpty {
            runCatching { probeDescriptions() }.getOrDefault(emptyList()).also { found ->
                log.i { "UPnP descriptions found directly: ${found.map { it.first }}" }
            }
        }
        runCatching { mapWithUpnp(described, internalPort) }.onFailure { log.w(it) { "UPnP mapping failed" } }.getOrNull()
            ?: runCatching { mapWithNatPmp(gateways.map { it.second }, internalPort) }
                .onFailure { log.w(it) { "NAT-PMP mapping failed" } }
                .getOrNull()
    }

    actual suspend fun renew(mapping: PartyPortMapping): PartyPortMapping? = withContext(Dispatchers.IO) {
        runCatching {
            when (mapping.method) {
                PartyPortMappingMethod.UPNP -> {
                    val service = UpnpService(mapping.controlUrl ?: return@runCatching null, mapping.serviceType ?: return@runCatching null)
                    val lease = addUpnpMapping(service, mapping.externalPort, mapping.internalPort, mapping.internalClient).lease
                        ?: return@runCatching null
                    mapping.copy(leaseSeconds = lease, externalAddress = upnpExternalAddress(service) ?: mapping.externalAddress)
                }
                PartyPortMappingMethod.NAT_PMP -> {
                    val gateway = InetAddress.getByName(mapping.gateway ?: return@runCatching null)
                    natPmpMap(gateway, mapping.internalPort, mapping.externalPort, LEASE_SECONDS)
                        ?.let { (externalPort, lease) -> mapping.copy(externalPort = externalPort, leaseSeconds = lease) }
                }
            }
        }.getOrNull()
    }

    actual suspend fun unmap(mapping: PartyPortMapping) {
        withContext(Dispatchers.IO) {
            runCatching {
                when (mapping.method) {
                    PartyPortMappingMethod.UPNP -> {
                        val service = UpnpService(mapping.controlUrl ?: return@runCatching, mapping.serviceType ?: return@runCatching)
                        soap(
                            service,
                            "DeletePortMapping",
                            listOf(
                                "NewRemoteHost" to "",
                                "NewExternalPort" to mapping.externalPort.toString(),
                                "NewProtocol" to "TCP",
                            ),
                        )
                    }
                    PartyPortMappingMethod.NAT_PMP -> {
                        val gateway = InetAddress.getByName(mapping.gateway ?: return@runCatching)
                        natPmpMap(gateway, mapping.internalPort, 0, 0)
                    }
                }
                Unit
            }
        }
    }
}

// ---- UPnP: find the router, read what it offers, and ask it to forward the port ----

private class UpnpService(val controlUrl: String, val serviceType: String)

// Each router that answers, as its description's address and its own address, searched from every local address.
private fun discoverGateways(): List<Pair<String, String>> {
    val sources = LocalSyncPlatform.localIpv4Addresses().mapNotNull { address ->
        runCatching { DatagramSocket(InetSocketAddress(InetAddress.getByName(address), 0)) }.getOrNull()
    }.ifEmpty { listOf(DatagramSocket()) }
    // Asked directly too, the router's answer is a reply a firewall lets in; the whole network's answers it may drop.
    val routers = (listOfNotNull(defaultGateway()) + guessedGateways()).distinct()
        .mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }
    try {
        val group = InetAddress.getByName(SSDP_ADDRESS)
        sources.forEach { socket ->
            socket.soTimeout = 100
            SearchTargets.forEach { target ->
                val search = "M-SEARCH * HTTP/1.1\r\nHOST: $SSDP_ADDRESS:$SSDP_PORT\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $target\r\n\r\n"
                    .toByteArray(Charsets.US_ASCII)
                runCatching { socket.send(DatagramPacket(search, search.size, group, SSDP_PORT)) }
                routers.forEach { router ->
                    val direct = "M-SEARCH * HTTP/1.1\r\nHOST: ${router.hostAddress}:$SSDP_PORT\r\nMAN: \"ssdp:discover\"\r\nST: $target\r\n\r\n"
                        .toByteArray(Charsets.US_ASCII)
                    runCatching { socket.send(DatagramPacket(direct, direct.size, router, SSDP_PORT)) }
                }
            }
        }
        val found = LinkedHashMap<String, String>()
        val buffer = ByteArray(2_048)
        var deadline = System.currentTimeMillis() + SSDP_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            sources.forEach { socket ->
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    return@forEach
                }
                val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val location = response.lineSequence()
                    .firstOrNull { it.startsWith("location:", ignoreCase = true) }
                    ?.substringAfter(':')
                    ?.trim()
                    ?: return@forEach
                val responder = packet.address?.hostAddress ?: return@forEach
                if (found.isEmpty()) deadline = minOf(deadline, System.currentTimeMillis() + SSDP_SETTLE_MS)
                found.putIfAbsent(location, responder)
            }
        }
        return found.toList()
    } finally {
        sources.forEach { it.close() }
    }
}

// Asks the router directly at the addresses common routers use, each port at once; a closed port is skipped quickly.
private suspend fun probeDescriptions(): List<Pair<String, String>> = coroutineScope {
    val routers = listOfNotNull(defaultGateway()).ifEmpty { guessedGateways() }.distinct()
    routers.flatMap { router ->
        KnownDescriptions.groupBy({ it.first }, { it.second }).map { (port, paths) ->
            async(Dispatchers.IO) {
                for (path in paths) {
                    val url = "http://$router:$port$path"
                    val result = runCatching { http(url, "GET", connectTimeoutMs = PROBE_CONNECT_TIMEOUT_MS) }.getOrNull()
                        ?: return@async null
                    val body = result.body
                    if (result.status == 200 && ("WANIPConnection" in body || "WANPPPConnection" in body)) return@async url to router
                }
                null
            }
        }
    }.awaitAll().filterNotNull()
}

private fun mapWithUpnp(gateways: List<Pair<String, String>>, internalPort: Int): PartyPortMapping? {
    gateways.forEach { (location, _) ->
        val description = runCatching { http(location, "GET") }.getOrNull() ?: return@forEach
        if (description.status != 200) return@forEach
        val internalClient = description.localAddress ?: return@forEach
        for (service in upnpServices(description.body, location)) {
            for (attempt in 0 until PORT_ATTEMPTS) {
                // The same port outside first; another one if something else already has it.
                val externalPort = if (attempt == 0) internalPort else Random.nextInt(20_000, 60_000)
                val result = addUpnpMapping(service, externalPort, internalPort, internalClient)
                log.i { "UPnP ${service.serviceType} port $externalPort: ${result.lease?.let { "lease $it" } ?: "error ${result.errorCode}"}" }
                result.lease?.let { lease ->
                    return PartyPortMapping(
                        method = PartyPortMappingMethod.UPNP,
                        externalAddress = upnpExternalAddress(service),
                        externalPort = externalPort,
                        internalPort = internalPort,
                        internalClient = internalClient,
                        leaseSeconds = lease,
                        controlUrl = service.controlUrl,
                        serviceType = service.serviceType,
                    )
                }
                if (result.errorCode != UPNP_CONFLICT) break
            }
        }
    }
    return null
}

private class UpnpAddResult(val lease: Int?, val errorCode: Int?)

// The lease the router took, or the error it refused with; a refused lease is asked again without one.
private fun addUpnpMapping(service: UpnpService, externalPort: Int, internalPort: Int, internalClient: String): UpnpAddResult {
    var errorCode: Int? = null
    for (lease in listOf(LEASE_SECONDS, 0)) {
        val result = soap(
            service,
            "AddPortMapping",
            listOf(
                "NewRemoteHost" to "",
                "NewExternalPort" to externalPort.toString(),
                "NewProtocol" to "TCP",
                "NewInternalPort" to internalPort.toString(),
                "NewInternalClient" to internalClient,
                "NewEnabled" to "1",
                "NewPortMappingDescription" to MAPPING_DESCRIPTION,
                "NewLeaseDuration" to lease.toString(),
            ),
        )
        if (result.status == 200) return UpnpAddResult(lease = lease, errorCode = null)
        errorCode = Regex("<errorCode>\\s*(\\d+)\\s*</errorCode>").find(result.body)?.groupValues?.get(1)?.toIntOrNull()
            ?: result.status
        // Many routers refuse a lease with some other code than 725; only a taken port needs another one.
        if (errorCode == UPNP_CONFLICT) break
    }
    return UpnpAddResult(lease = null, errorCode = errorCode)
}

private fun upnpExternalAddress(service: UpnpService): String? = runCatching {
    val result = soap(service, "GetExternalIPAddress", emptyList())
    if (result.status != 200) return@runCatching null
    Regex("<NewExternalIPAddress>\\s*([^<\\s]+)\\s*</NewExternalIPAddress>").find(result.body)?.groupValues?.get(1)
}.getOrNull()

// The WAN connection services in a router's description, with their control addresses made absolute.
private fun upnpServices(description: String, location: String): List<UpnpService> {
    val base = tagValue(description, "URLBase")?.takeIf { it.isNotBlank() } ?: location
    return Regex("<service>(.*?)</service>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        .findAll(description)
        .mapNotNull { match ->
            val block = match.groupValues[1]
            val type = tagValue(block, "serviceType") ?: return@mapNotNull null
            if (!type.contains("WANIPConnection") && !type.contains("WANPPPConnection")) return@mapNotNull null
            val control = tagValue(block, "controlURL") ?: return@mapNotNull null
            UpnpService(URL(URL(base), control).toString(), type)
        }
        .toList()
}

private fun tagValue(xml: String, tag: String): String? =
    Regex("<$tag>\\s*(.*?)\\s*</$tag>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        .find(xml)?.groupValues?.get(1)

private fun soap(service: UpnpService, action: String, arguments: List<Pair<String, String>>): HttpResult {
    val body = buildString {
        append("<?xml version=\"1.0\"?>")
        append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" ")
        append("s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body>")
        append("<u:$action xmlns:u=\"${service.serviceType}\">")
        arguments.forEach { (name, value) -> append("<$name>${value.xmlEscaped()}</$name>") }
        append("</u:$action></s:Body></s:Envelope>")
    }
    return http(
        url = service.controlUrl,
        method = "POST",
        body = body,
        headers = mapOf(
            "Content-Type" to "text/xml; charset=\"utf-8\"",
            "SOAPAction" to "\"${service.serviceType}#$action\"",
        ),
    )
}

private fun String.xmlEscaped(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private class HttpResult(val status: Int, val body: String, val localAddress: String?)

// Plain HTTP over a socket: routers speak nothing else, and it keeps Android's cleartext rules out of it.
private fun http(
    url: String,
    method: String,
    body: String? = null,
    headers: Map<String, String> = emptyMap(),
    connectTimeoutMs: Int = HTTP_CONNECT_TIMEOUT_MS,
): HttpResult {
    val target = URL(url)
    val port = if (target.port > 0) target.port else 80
    Socket().use { socket ->
        socket.connect(InetSocketAddress(target.host, port), connectTimeoutMs)
        socket.soTimeout = HTTP_READ_TIMEOUT_MS
        val payload = body?.toByteArray(Charsets.UTF_8)
        val request = buildString {
            append("$method ${target.file.ifEmpty { "/" }} HTTP/1.1\r\n")
            append("Host: ${target.host}:$port\r\n")
            append("Connection: close\r\n")
            append("User-Agent: Provenio UPnP/1.1\r\n")
            headers.forEach { (name, value) -> append("$name: $value\r\n") }
            if (payload != null) append("Content-Length: ${payload.size}\r\n")
            append("\r\n")
        }
        val output = socket.getOutputStream()
        output.write(request.toByteArray(Charsets.ISO_8859_1))
        payload?.let(output::write)
        output.flush()

        val input = BufferedInputStream(socket.getInputStream())
        val statusLine = input.readLine() ?: return HttpResult(0, "", null)
        val status = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: 0
        val responseHeaders = mutableMapOf<String, String>()
        while (true) {
            val line = input.readLine() ?: break
            if (line.isEmpty()) break
            val separator = line.indexOf(':').takeIf { it > 0 } ?: continue
            responseHeaders[line.substring(0, separator).trim().lowercase()] = line.substring(separator + 1).trim()
        }
        val bytes = when {
            responseHeaders["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true -> input.readChunked()
            responseHeaders["content-length"]?.toIntOrNull() != null -> {
                val length = responseHeaders.getValue("content-length").toInt().coerceIn(0, HTTP_MAX_BODY_BYTES)
                ByteArray(length).also { DataInputStream(input).readFully(it) }
            }
            else -> input.readUpTo(HTTP_MAX_BODY_BYTES)
        }
        return HttpResult(status, String(bytes, Charsets.UTF_8), socket.localAddress?.hostAddress)
    }
}

private fun InputStream.readLine(): String? {
    val line = ByteArrayOutputStream()
    while (true) {
        val byte = read()
        if (byte == -1) return if (line.size() == 0) null else line.toString(Charsets.ISO_8859_1.name())
        if (byte == '\n'.code) return line.toString(Charsets.ISO_8859_1.name()).trimEnd('\r')
        line.write(byte)
    }
}

private fun InputStream.readChunked(): ByteArray {
    val body = ByteArrayOutputStream()
    val data = DataInputStream(this)
    while (body.size() < HTTP_MAX_BODY_BYTES) {
        val size = readLine()?.substringBefore(';')?.trim()?.toIntOrNull(16) ?: break
        if (size == 0) break
        val chunk = ByteArray(size)
        data.readFully(chunk)
        body.write(chunk)
        readLine()
    }
    return body.toByteArray()
}

private fun InputStream.readUpTo(limit: Int): ByteArray {
    val body = ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    while (body.size() < limit) {
        val read = try {
            read(buffer)
        } catch (_: SocketTimeoutException) {
            break
        }
        if (read == -1) break
        body.write(buffer, 0, read)
    }
    return body.toByteArray()
}

// ---- NAT-PMP: the simpler protocol some routers answer instead ----

private fun mapWithNatPmp(responders: List<String>, internalPort: Int): PartyPortMapping? {
    val candidates = (responders + listOfNotNull(defaultGateway()) + guessedGateways()).distinct().take(NAT_PMP_CANDIDATES)
    candidates.forEach { candidate ->
        val gateway = runCatching { InetAddress.getByName(candidate) }.getOrNull() as? Inet4Address ?: return@forEach
        val external = natPmpExternalAddress(gateway)
        log.i { "NAT-PMP $candidate: ${external ?: "no answer"}" }
        external ?: return@forEach
        val (externalPort, lease) = natPmpMap(gateway, internalPort, internalPort, LEASE_SECONDS) ?: return@forEach
        return PartyPortMapping(
            method = PartyPortMappingMethod.NAT_PMP,
            externalAddress = external,
            externalPort = externalPort,
            internalPort = internalPort,
            internalClient = LocalSyncPlatform.localIpv4Address().orEmpty(),
            leaseSeconds = lease,
            gateway = candidate,
        )
    }
    return null
}

private fun natPmpExternalAddress(gateway: InetAddress): String? {
    val response = natPmpRequest(gateway, byteArrayOf(0, 0), expectedOpcode = 128) ?: return null
    if (response.size < 12 || response.u16(2) != 0) return null
    return "${response.u8(8)}.${response.u8(9)}.${response.u8(10)}.${response.u8(11)}"
}

// Returns the external port the router gave and its lease, or null if it refused.
private fun natPmpMap(gateway: InetAddress, internalPort: Int, externalPort: Int, lifetimeSeconds: Int): Pair<Int, Int>? {
    val request = ByteArray(12)
    request[1] = 2 // TCP
    request.putU16(4, internalPort)
    request.putU16(6, externalPort)
    request.putU32(8, lifetimeSeconds)
    val response = natPmpRequest(gateway, request, expectedOpcode = 130) ?: return null
    if (response.size < 16 || response.u16(2) != 0) return null
    return response.u16(10) to response.u32(12)
}

private fun natPmpRequest(gateway: InetAddress, request: ByteArray, expectedOpcode: Int): ByteArray? =
    DatagramSocket().use { socket ->
        var wait = NAT_PMP_FIRST_WAIT_MS
        repeat(NAT_PMP_TRIES) {
            socket.soTimeout = wait
            socket.send(DatagramPacket(request, request.size, gateway, NAT_PMP_PORT))
            try {
                val buffer = ByteArray(16)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                if (packet.length >= 4 && buffer[0].toInt() == 0 && buffer.u8(1) == expectedOpcode) {
                    return@use buffer.copyOf(packet.length)
                }
            } catch (_: SocketTimeoutException) {
                wait *= 2
            }
        }
        null
    }

private fun ByteArray.u8(index: Int): Int = this[index].toInt() and 0xff

private fun ByteArray.u16(index: Int): Int = (u8(index) shl 8) or u8(index + 1)

private fun ByteArray.u32(index: Int): Int = (u16(index) shl 16) or u16(index + 2)

private fun ByteArray.putU16(index: Int, value: Int) {
    this[index] = (value shr 8).toByte()
    this[index + 1] = value.toByte()
}

private fun ByteArray.putU32(index: Int, value: Int) {
    putU16(index, value shr 16)
    putU16(index + 2, value)
}

// The default route's gateway, where the system shows it (Linux, and Android before 10).
private fun defaultGateway(): String? = runCatching {
    File("/proc/net/route").readLines()
        .drop(1)
        .map { it.trim().split(Regex("\\s+")) }
        .firstOrNull { fields -> fields.size > 2 && fields[1] == "00000000" && fields[2] != "00000000" }
        ?.get(2)
        ?.let { hex ->
            val value = hex.toLong(16)
            // Little-endian, as the kernel writes it.
            "${value and 0xff}.${(value shr 8) and 0xff}.${(value shr 16) and 0xff}.${(value shr 24) and 0xff}"
        }
}.getOrNull()

// Where a home router usually sits: the first or the last address of this device's network.
private fun guessedGateways(): List<String> =
    LocalSyncPlatform.localIpv4Addresses().flatMap { address ->
        val network = address.substringBeforeLast('.')
        listOf("$network.1", "$network.254")
    }

// ---- IPv6 ----

internal actual fun partyGlobalIpv6Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual && !it.name.isContainerInterface() }
        .flatMap { networkInterface -> networkInterface.inetAddresses.toList() }
        .filterIsInstance<Inet6Address>()
        .filter { it.isGlobalUnicast() }
        .mapNotNull { it.hostAddress?.substringBefore('%') }
        .distinct()
        .take(MAX_IPV6_ADDRESSES)
}.getOrDefault(emptyList())

// Leaves out link-local, the private fc00::/7 range and the like, which only work nearby.
private fun Inet6Address.isGlobalUnicast(): Boolean {
    if (isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress || isAnyLocalAddress || isMulticastAddress) return false
    val first = address[0].toInt() and 0xff
    return first and 0xe0 == 0x20
}
