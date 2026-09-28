package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
internal actual fun SystemBarsAppearance(darkTheme: Boolean) = Unit
