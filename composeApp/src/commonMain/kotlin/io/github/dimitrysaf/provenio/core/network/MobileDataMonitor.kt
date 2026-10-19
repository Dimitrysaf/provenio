package io.github.dimitrysaf.provenio.core.network

import kotlinx.coroutines.flow.StateFlow

/** Whether the device is online over mobile data rather than Wi-Fi or a wired connection. */
internal expect object MobileDataMonitor {
    /** True while the connection in use is a mobile network. */
    val onMobileData: StateFlow<Boolean>
}
