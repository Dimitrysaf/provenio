package io.github.dimitrysaf.provenio.ui.chrome

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Current visibility of app chrome (top bar / nav). Read by pages that host their own top bar. */
val LocalBarsVisible = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(true) }

/** Lets any screen (e.g. a full-screen video player) hide or restore the top bar and nav together. */
val LocalSetBarsVisible = staticCompositionLocalOf<(Boolean) -> Unit> { {} }
