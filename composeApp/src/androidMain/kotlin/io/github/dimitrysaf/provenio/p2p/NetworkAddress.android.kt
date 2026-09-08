package io.github.dimitrysaf.provenio.p2p

import java.net.NetworkInterface

actual fun localNetworkAddresses(): List<String> = try {
    NetworkInterface.getNetworkInterfaces()
        .toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { networkInterface -> networkInterface.inetAddresses.toList() }
        .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
        .mapNotNull { it.hostAddress }
        // IPv6 addresses arrive with a scope suffix that means nothing to the user.
        .map { it.substringBefore("%") }
        .distinct()
} catch (failure: Exception) {
    // Enumerating interfaces can be denied or simply unavailable; an empty list is a
    // truthful answer here, and this must never take the settings screen down.
    emptyList()
}
