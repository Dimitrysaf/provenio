package io.github.dimitrysaf.provenio.core.localsync

internal actual object LocalSyncWifiLock {
    actual fun acquire() = Unit

    actual fun release() = Unit
}
