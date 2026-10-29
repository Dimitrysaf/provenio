package io.github.dimitrysaf.provenio.core.party

/**
 * One side of an elliptic-curve Diffie–Hellman exchange on P-256, for a guest who joins with
 * only the code: the two sides agree on a key without sharing a secret first.
 */
internal expect class PartyKeyAgreement() {
    val publicKey: ByteArray

    fun sharedSecret(peerPublicKey: ByteArray): ByteArray
}

/** This device's IPv6 addresses that other networks can reach, if the router lets them through. */
internal expect fun partyGlobalIpv6Addresses(): List<String>
