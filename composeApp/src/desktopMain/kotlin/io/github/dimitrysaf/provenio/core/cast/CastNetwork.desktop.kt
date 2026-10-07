package io.github.dimitrysaf.provenio.core.cast

import java.net.Inet4Address
import java.net.NetworkInterface

internal actual object CastNetwork {
    actual fun hasLocalNetwork(): Boolean = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { nic ->
            nic.isUp && !nic.isLoopback && !nic.isVirtual && nic.supportsMulticast() &&
                nic.inetAddresses.toList().any { it is Inet4Address && (it.isSiteLocalAddress || it.isLinkLocalAddress) }
        }
    }.getOrDefault(false)
}
