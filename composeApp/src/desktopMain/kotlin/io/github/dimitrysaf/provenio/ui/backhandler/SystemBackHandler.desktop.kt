package io.github.dimitrysaf.provenio.ui.backhandler

import androidx.compose.runtime.Composable

// Desktop has no system back gesture/button; navigation there relies on the on-screen back button.
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
