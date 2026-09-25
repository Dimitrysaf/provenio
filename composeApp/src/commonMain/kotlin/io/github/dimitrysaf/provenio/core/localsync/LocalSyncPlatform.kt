package io.github.dimitrysaf.provenio.core.localsync

// Crypto, sockets and the LAN address for device-to-device sync; only the JVM targets provide them.
internal expect object LocalSyncPlatform {
    val isSupported: Boolean
    fun randomBytes(size: Int): ByteArray
    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray
    fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray
    fun decrypt(key: ByteArray, payload: ByteArray): ByteArray
    fun localIpv4Address(): String?
    suspend fun listen(port: Int): LocalSyncServer
    suspend fun connect(host: String, port: Int, timeoutMs: Int): LocalSyncConnection
    suspend fun sendBeacon(payload: ByteArray, port: Int)
    suspend fun receiveBeacons(port: Int, onBeacon: (payload: ByteArray, host: String) -> Unit)
}

// Android drops Wi-Fi broadcasts unless the app holds a multicast lock while it listens for them.
internal expect object LocalSyncWifiLock {
    fun acquire()
    fun release()
}

/** One side of a sync connection, carrying length-prefixed frames. */
internal interface LocalSyncConnection {
    suspend fun readFrame(): ByteArray
    suspend fun writeFrame(frame: ByteArray)
    fun close()
}

internal interface LocalSyncServer {
    val port: Int
    suspend fun accept(): LocalSyncConnection
    fun close()
}

/** A name the other device shows for this one. */
internal expect fun localSyncDeviceName(): String

/** The QR code for [text] as rows of dark modules, or null where it cannot be drawn. */
internal expect fun localSyncQrMatrix(text: String): List<BooleanArray>?
