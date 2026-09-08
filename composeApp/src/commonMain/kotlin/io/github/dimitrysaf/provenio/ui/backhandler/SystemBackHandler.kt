package io.github.dimitrysaf.provenio.ui.backhandler

import androidx.compose.runtime.Composable

/**
 * Intercepts system back.
 *
 * Navigation Compose handles back between destinations; this is for back *within* one
 * destination — a two-pane screen where back should close the detail pane rather than
 * leave the screen entirely.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)
