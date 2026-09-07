package io.github.dimitrysaf.provenio.ui.chrome

import androidx.compose.runtime.staticCompositionLocalOf

/** Lets any screen (e.g. a full-screen video player) hide or restore the top bar and nav together. */
val LocalSetBarsVisible = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
