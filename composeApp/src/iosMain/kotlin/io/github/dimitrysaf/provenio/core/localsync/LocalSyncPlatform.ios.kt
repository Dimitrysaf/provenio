package io.github.dimitrysaf.provenio.core.localsync

import platform.UIKit.UIDevice

// Local sync is not built for iOS yet; the settings entry stays hidden there.
internal actual object LocalSyncPlatform {
    actual val isSupported: Boolean = false

    actual fun randomBytes(size: Int): ByteArray = unsupported()

    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = unsupported()

    actual fun encrypt(key: ByteArray, plaintext: ByteArray): ByteArray = unsupported()

    actual fun decrypt(key: ByteArray, payload: ByteArray): ByteArray = unsupported()

    actual fun localIpv4Address(): String? = null

    actual suspend fun listen(port: Int): LocalSyncServer = unsupported()

    actual suspend fun connect(host: String, port: Int, timeoutMs: Int): LocalSyncConnection = unsupported()

    actual suspend fun sendBeacon(payload: ByteArray, port: Int): Unit = unsupported()

    actual suspend fun receiveBeacons(port: Int, onBeacon: (payload: ByteArray, host: String) -> Unit): Unit = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("Local sync is not available on iOS")
}

internal actual fun localSyncDeviceName(): String = UIDevice.currentDevice.name

internal actual fun localSyncQrMatrix(text: String): List<BooleanArray>? = null

internal actual object LocalSyncWifiLock {
    actual fun acquire() = Unit

    actual fun release() = Unit
}
