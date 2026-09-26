package io.github.dimitrysaf.provenio.core.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Whether the first real screen has drawn, which is what the system splash waits for before it lets go.
object AppStartupState {
    private val _firstScreenReady = MutableStateFlow(false)
    val firstScreenReady: StateFlow<Boolean> = _firstScreenReady.asStateFlow()

    fun markFirstScreenReady() {
        _firstScreenReady.value = true
    }
}
