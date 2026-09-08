package io.github.dimitrysaf.provenio.ui.backhandler

import androidx.compose.runtime.Composable

/**
 * A back gesture in flight. [progress] runs 0f..1f as the user drags; [fromStart] is the
 * screen edge the drag began from, which decides which way the leaving page slides.
 */
data class BackGesture(val progress: Float, val fromStart: Boolean)

/**
 * Intercepts system back navigation.
 *
 * On platforms with a predictive back gesture the handler reports [onProgress] continuously
 * while the user drags — so the UI can preview where it is going — and then exactly one of
 * [onCancel] (released short) or [onBack] (committed).
 */
@Composable
expect fun SystemBackHandler(
    enabled: Boolean = true,
    onProgress: (BackGesture) -> Unit = {},
    onCancel: () -> Unit = {},
    onBack: () -> Unit,
)
