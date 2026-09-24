package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import io.github.dimitrysaf.provenio.shell.components.SkeletonAnimationProvider
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.roboto_bold
import provenio.composeapp.generated.resources.roboto_medium
import provenio.composeapp.generated.resources.roboto_regular
import org.jetbrains.compose.resources.Font
import io.github.dimitrysaf.provenio.core.settings.AppTheme
import io.github.dimitrysaf.provenio.core.settings.CustomThemeColors

val LocalAppTheme = staticCompositionLocalOf { AppTheme.WHITE }

val LocalThemePalette = staticCompositionLocalOf { ThemeColors.White }

val MaterialTheme.themePalette: ThemeColorPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalThemePalette.current

val MaterialTheme.appTheme: AppTheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTheme.current

// Roboto is the Material You type family. Roboto ships Regular/Medium/Bold as static
// instances and has no SemiBold, so the theme's SemiBold styles resolve to Medium — the
// weight M3's own type scale uses for emphasis — rather than being synthetically bolded.
internal val RobotoFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.roboto_bold, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.roboto_medium, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.roboto_medium, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.roboto_regular, FontWeight.Normal, FontStyle.Normal),
    )

private val ThemeTypography: Typography
    @Composable
    get() = Typography(
        displayLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.pageDisplay,
            lineHeight = Tokens.LineHeight.pageDisplay,
            fontWeight = FontWeight.Bold,
            letterSpacing = Tokens.LetterSpacing.pageDisplay,
        ),
        headlineLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.headline,
            lineHeight = Tokens.LineHeight.headline,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = Tokens.LetterSpacing.headline,
        ),
        titleLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.titleSm,
            lineHeight = Tokens.LineHeight.materialTitleLarge,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.bodyLg,
            lineHeight = Tokens.LineHeight.bodyMd,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.bodyApp,
            lineHeight = Tokens.LineHeight.bodyApp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.bodyMd,
            lineHeight = Tokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.bodyMd,
            lineHeight = Tokens.LineHeight.bodySm,
            fontWeight = FontWeight.SemiBold,
        ),
        labelMedium = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.labelSm,
            lineHeight = Tokens.LineHeight.labelXs,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = Tokens.LetterSpacing.label,
        ),
    )

private val TypeTokens: TypeScale
    @Composable
    get() = TypeScale(
        labelXs = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.labelXs,
            lineHeight = Tokens.LineHeight.labelXs,
            fontWeight = FontWeight.SemiBold,
        ),
        labelSm = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.labelSm,
            lineHeight = Tokens.LineHeight.labelSm,
            fontWeight = FontWeight.SemiBold,
        ),
        bodySm = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.bodySm,
            lineHeight = Tokens.LineHeight.bodySm,
            fontWeight = FontWeight.Normal,
        ),
        bodyMd = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.bodyMd,
            lineHeight = Tokens.LineHeight.bodyMd,
            fontWeight = FontWeight.Normal,
        ),
        bodyLg = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.bodyLg,
            lineHeight = Tokens.LineHeight.bodyLg,
            fontWeight = FontWeight.Normal,
        ),
        titleSm = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.titleSm,
            lineHeight = Tokens.LineHeight.titleSm,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMd = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.titleMd,
            lineHeight = Tokens.LineHeight.titleMd,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLg = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.titleLg,
            lineHeight = Tokens.LineHeight.titleLg,
            fontWeight = FontWeight.SemiBold,
        ),
        displaySm = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.displaySm,
            lineHeight = Tokens.LineHeight.displaySm,
            fontWeight = FontWeight.Bold,
        ),
        displayMd = TextStyle(
            fontFamily = RobotoFontFamily,
            fontSize = Tokens.Type.displayMd,
            lineHeight = Tokens.LineHeight.displayMd,
            fontWeight = FontWeight.Bold,
        ),
    )

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.WHITE,
    amoled: Boolean = false,
    customThemeColors: CustomThemeColors = CustomThemeColors.Default,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dynamicColorScheme = if (useDynamicColor) rememberDynamicColorScheme() else null
    val palette = remember(appTheme, customThemeColors, dynamicColorScheme, amoled) {
        dynamicColorScheme?.toDynamicThemeColorPalette(amoled)
            ?: ThemeColors.getColorPalette(appTheme, customThemeColors)
    }
    // Material You's own scheme wherever the platform supplies one (Android 12+). Below that,
    // and on iOS, there is no wallpaper to derive a palette from, so fall back to Material's
    // baseline scheme rather than to a brand palette — a brand palette is not Material You.
    val colorScheme = remember(dynamicColorScheme, darkTheme, amoled) {
        val scheme = dynamicColorScheme
            ?: if (darkTheme) darkColorScheme() else lightColorScheme()
        if (amoled) scheme.copy(background = Color.Black, surface = Color.Black) else scheme
    }
    val tokens = defaultThemeTokens(palette, amoled = amoled, colorScheme = colorScheme)

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1f,
        ),
        LocalThemeTokens provides tokens,
        LocalTypeScale provides TypeTokens,
        LocalAppTheme provides appTheme,
        LocalThemePalette provides palette,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ThemeTypography,
        ) {
            SkeletonAnimationProvider(content = content)
        }
    }
}
