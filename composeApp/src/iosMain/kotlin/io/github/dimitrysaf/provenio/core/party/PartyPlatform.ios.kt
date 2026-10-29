package io.github.dimitrysaf.provenio.core.party

// Watch parties are not built for iOS yet, as with Local sync, whose sockets they use.
internal actual class PartyKeyAgreement actual constructor() {
    actual val publicKey: ByteArray = ByteArray(0)

    actual fun sharedSecret(peerPublicKey: ByteArray): ByteArray =
        throw UnsupportedOperationException("Watch parties are not available on iOS")
}

internal actual fun partyGlobalIpv6Addresses(): List<String> = emptyList()
