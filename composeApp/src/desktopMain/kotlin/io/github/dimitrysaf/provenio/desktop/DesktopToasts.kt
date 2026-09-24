package io.github.dimitrysaf.provenio.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The message Android would show as a system toast; Main draws it as a snackbar. */
object DesktopToasts {
    private val current = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = current.asStateFlow()

    fun show(message: String) {
        current.value = message
    }

    fun dismiss() {
        current.value = null
    }
}
