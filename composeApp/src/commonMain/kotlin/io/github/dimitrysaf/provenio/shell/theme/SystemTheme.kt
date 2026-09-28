package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.runtime.Composable

@Composable
internal expect fun systemPrefersDarkTheme(): Boolean

@Composable
internal expect fun SystemBarsAppearance(darkTheme: Boolean)
