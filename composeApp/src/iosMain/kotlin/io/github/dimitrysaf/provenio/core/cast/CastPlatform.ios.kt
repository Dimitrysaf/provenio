package io.github.dimitrysaf.provenio.core.cast

internal actual object CastPlatform {
    actual val isSupported: Boolean = false

    actual suspend fun discover(onDevice: (CastDevice) -> Unit) = Unit

    actual suspend fun open(device: CastDevice): CastChannel =
        throw UnsupportedOperationException("Casting is not available on iOS")
}

internal actual object CastNetwork {
    actual fun hasLocalNetwork(): Boolean = false
}
