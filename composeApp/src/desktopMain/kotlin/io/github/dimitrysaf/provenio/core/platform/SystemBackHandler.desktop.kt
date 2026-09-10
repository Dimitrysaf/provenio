package io.github.dimitrysaf.provenio.core.platform

import androidx.compose.runtime.Composable

// Desktop has no system back gesture or button; the on-screen back button covers it.
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
