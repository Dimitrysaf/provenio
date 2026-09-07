package io.github.dimitrysaf.provenio.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Amoled -> true
    }

    val dynamicScheme = if (useDynamicColor) dynamicColorScheme(useDarkTheme) else null
    val baseScheme = dynamicScheme ?: if (useDarkTheme) darkColorScheme() else lightColorScheme()

    val colorScheme = if (themeMode == ThemeMode.Amoled && useDarkTheme) {
        baseScheme.copy(background = Color.Black, surface = Color.Black)
    } else {
        baseScheme
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
