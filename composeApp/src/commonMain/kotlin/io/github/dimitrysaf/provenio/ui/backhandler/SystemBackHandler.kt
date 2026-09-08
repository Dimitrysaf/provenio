package io.github.dimitrysaf.provenio.ui.backhandler

import androidx.compose.runtime.Composable

/** Intercepts the platform's system back navigation (Android back gesture/button). */
@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
