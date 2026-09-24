package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Whether this device/OS can derive Material You colors from the wallpaper (Android 12+ only). */
expect fun isDynamicColorAvailable(): Boolean

/** The wallpaper-derived Material You [ColorScheme], or null where dynamic color isn't available. */
@Composable
expect fun rememberDynamicColorScheme(): ColorScheme?

/** Maps a wallpaper-derived [ColorScheme] onto the app's own [ThemeColorPalette] shape. */
internal fun ColorScheme.toDynamicThemeColorPalette(amoled: Boolean): ThemeColorPalette = ThemeColorPalette(
    secondary = primary,
    secondaryVariant = secondary,
    accentGradient = listOf(tertiary, primary, secondary),
    nativeAccentHex = primary.toHexString(),
    onSecondary = onPrimary,
    onSecondaryVariant = onSecondary,
    focusRing = primary,
    focusBackground = primaryContainer,
    background = if (amoled) Color.Black else background,
    backgroundElevated = surfaceContainerLow,
    backgroundCard = surfaceContainerHigh,
)

private fun Int.toHex2(): String = toString(16).padStart(2, '0').uppercase()

private fun Color.toHexString(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return "#${r.toHex2()}${g.toHex2()}${b.toHex2()}"
}
