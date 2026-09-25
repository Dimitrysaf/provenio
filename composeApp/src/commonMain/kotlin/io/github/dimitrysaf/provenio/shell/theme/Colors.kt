package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

data class ThemeColorPalette(
    val secondary: Color,
    val secondaryVariant: Color,
    val accentGradient: List<Color> = listOf(secondary),
    val nativeAccentHex: String,
    val onSecondary: Color = Color.White,
    val onSecondaryVariant: Color = Color.White,
    val focusRing: Color,
    val focusBackground: Color,
    val background: Color = Color(0xFF0D0D0D),
    val backgroundElevated: Color = Color(0xFF1A1A1A),
    val backgroundCard: Color = Color(0xFF242424),
)

// The palette used wherever the platform has no Material You wallpaper colours to offer.
object ThemeColors {

    val White = ThemeColorPalette(
        secondary = Color(0xFFF5F5F5),
        secondaryVariant = Color(0xFFE0E0E0),
        nativeAccentHex = "#F5F5F5",
        onSecondary = Color(0xFF111111),
        onSecondaryVariant = Color(0xFF111111),
        focusRing = Color(0xFFFFFFFF),
        focusBackground = Color(0xFF303030),
        background = Color(0xFF0D0D0D),
        backgroundElevated = Color(0xFF1A1A1A),
        backgroundCard = Color(0xFF222222),
    )
}

fun ThemeColorPalette.accentBrush(): Brush =
    if (accentGradient.size >= 2) Brush.linearGradient(accentGradient)
    else SolidColor(accentGradient.firstOrNull() ?: secondary)
