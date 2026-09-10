package io.github.dimitrysaf.provenio.core.platform

import androidx.compose.runtime.Composable

/**
 * Opens a URL in the platform's browser.
 *
 * Addon configuration pages are web pages served by the addon itself, so configuring one
 * means leaving the app rather than rendering their form here.
 */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit
