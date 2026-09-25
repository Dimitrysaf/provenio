package io.github.dimitrysaf.provenio.core.localsync

import android.content.Context
import android.net.wifi.WifiManager

internal actual object LocalSyncWifiLock {
    private var appContext: Context? = null
    private var lock: WifiManager.MulticastLock? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun acquire() {
        if (lock?.isHeld == true) return
        val wifiManager = appContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        lock = runCatching {
            wifiManager.createMulticastLock("provenio-local-sync").apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()
    }

    actual fun release() {
        runCatching { lock?.takeIf { it.isHeld }?.release() }
        lock = null
    }
}
