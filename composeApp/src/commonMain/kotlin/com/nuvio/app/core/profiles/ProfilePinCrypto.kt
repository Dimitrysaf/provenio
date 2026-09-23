package com.nuvio.app.core.profiles

internal expect object ProfilePinCrypto {
    fun sha256Hex(value: String): String
}