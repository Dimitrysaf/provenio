package io.github.dimitrysaf.provenio.core.party

import io.github.dimitrysaf.provenio.core.localsync.isContainerInterface
import java.net.Inet6Address
import java.net.NetworkInterface
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

private const val MAX_IPV6_ADDRESSES = 2

internal actual class PartyKeyAgreement actual constructor() {
    private val keyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()

    actual val publicKey: ByteArray = keyPair.public.encoded

    actual fun sharedSecret(peerPublicKey: ByteArray): ByteArray {
        val peer = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(peerPublicKey))
        return KeyAgreement.getInstance("ECDH").run {
            init(keyPair.private)
            doPhase(peer, true)
            generateSecret()
        }
    }
}

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
