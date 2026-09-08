package io.github.dimitrysaf.provenio.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Pulls the whole surface family down to (or towards) black for AMOLED panels.
 *
 * Blackening only `background`/`surface` is not enough: M3 draws the navigation bar,
 * navigation rail and top app bar containers from the `surfaceContainer` roles, so
 * leaving those untouched puts a visible grey seam between the bars and the page body.
 * The container roles are blended toward black rather than set to it so that tonal
 * elevation still separates components instead of collapsing into one flat black.
 */
private fun ColorScheme.toAmoled(): ColorScheme {
    fun deepen(color: Color) = lerp(color, Color.Black, AmoledBlend)
    return copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = deepen(surfaceContainerLow),
        surfaceContainer = deepen(surfaceContainer),
        surfaceContainerHigh = deepen(surfaceContainerHigh),
        surfaceContainerHighest = deepen(surfaceContainerHighest),
        surfaceBright = deepen(surfaceBright),
        surfaceVariant = deepen(surfaceVariant),
    )
}

private const val AmoledBlend = 0.6f

/**
 * Colour is the only slot supplied here; the baseline M3 typography and shape scales are
 * already spec-correct. `MaterialExpressiveTheme` would additionally install the expressive
 * motion, shape and type scales, but it is `internal` in material3 1.12.0-alpha03 — see
 * [MotionTokens] for how motion is handled in the meantime.
 */
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
        baseScheme.toAmoled()
    } else {
        baseScheme
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
