package io.github.dimitrysaf.provenio.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun dynamicColorScheme(useDarkTheme: Boolean): ColorScheme? = null

actual fun isDynamicColorSupported(): Boolean = false
