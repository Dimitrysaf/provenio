package io.github.dimitrysaf.provenio.core.profiles

internal expect object ProfilePinCrypto {
    fun sha256Hex(value: String): String
}