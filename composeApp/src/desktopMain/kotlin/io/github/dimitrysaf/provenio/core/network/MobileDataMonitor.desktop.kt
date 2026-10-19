package io.github.dimitrysaf.provenio.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal actual object MobileDataMonitor {
    actual val onMobileData: StateFlow<Boolean> = MutableStateFlow(false)
}
