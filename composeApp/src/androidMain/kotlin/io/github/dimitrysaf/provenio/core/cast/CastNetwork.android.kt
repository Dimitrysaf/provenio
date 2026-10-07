package io.github.dimitrysaf.provenio.core.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

internal actual object CastNetwork {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    @Suppress("DEPRECATION")
    actual fun hasLocalNetwork(): Boolean {
        val manager = appContext?.getSystemService(ConnectivityManager::class.java) ?: return false
        return runCatching {
            manager.allNetworks.any { network ->
                val capabilities = manager.getNetworkCapabilities(network) ?: return@any false
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        }.getOrDefault(false)
    }
}
