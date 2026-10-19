package io.github.dimitrysaf.provenio.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal actual object MobileDataMonitor {
    private val state = MutableStateFlow(false)
    private var registered = false

    actual val onMobileData: StateFlow<Boolean> = state.asStateFlow()

    /** Starts following the default network; the first call does it, later ones do nothing. */
    fun initialize(context: Context) {
        if (registered) return
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        registered = runCatching {
            manager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        state.value = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    }

                    override fun onLost(network: Network) {
                        state.value = false
                    }
                },
            )
        }.isSuccess
    }
}
