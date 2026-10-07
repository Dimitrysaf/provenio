package io.github.dimitrysaf.provenio.core.cast

/** A Cast receiver found on the local network. */
data class CastDevice(
    val id: String,
    val name: String,
    val model: String?,
    val host: String,
    val port: Int,
)

/** One message of the Cast v2 protocol, carried as a UTF-8 JSON payload. */
internal data class CastFrame(
    val source: String,
    val destination: String,
    val namespace: String,
    val payload: String,
)

/** An open, encrypted connection to a Cast receiver. */
internal interface CastChannel {
    suspend fun send(source: String, destination: String, namespace: String, payload: String)
    suspend fun receive(): CastFrame
    fun close()
}

// Discovery and the encrypted socket need the JVM; other targets report casting as unsupported.
internal expect object CastPlatform {
    val isSupported: Boolean
    suspend fun discover(onDevice: (CastDevice) -> Unit)
    suspend fun open(device: CastDevice): CastChannel
}

// Casting needs a local network a receiver could be on: Wi-Fi or Ethernet, never mobile data alone.
internal expect object CastNetwork {
    fun hasLocalNetwork(): Boolean
}
