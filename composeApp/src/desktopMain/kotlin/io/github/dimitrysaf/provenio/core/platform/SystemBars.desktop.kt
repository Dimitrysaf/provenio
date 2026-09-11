package io.github.dimitrysaf.provenio.core.platform

import androidx.compose.runtime.Composable

/** Desktop windows have no system bars to match. */
@Composable
actual fun MatchHostSystemBars() = Unit
