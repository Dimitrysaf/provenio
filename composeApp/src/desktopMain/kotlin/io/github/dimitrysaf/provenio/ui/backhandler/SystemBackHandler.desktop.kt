package io.github.dimitrysaf.provenio.ui.backhandler

import androidx.compose.runtime.Composable

// Desktop has no system back gesture or button; navigation relies on the on-screen back
// button, so there is no gesture to preview and nothing to intercept.
@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onProgress: (BackGesture) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) {
}
