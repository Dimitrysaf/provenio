package com.nuvio.app.shell.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NuvioTokens {
    object Space {
        val hairline = 0.5.dp
        val s1 = 1.dp
        val s2 = 2.dp
        val s4 = 4.dp
        val s5 = 5.dp
        val s6 = 6.dp
        val s8 = 8.dp
        val s10 = 10.dp
        val s12 = 12.dp
        val s14 = 14.dp
        val s16 = 16.dp
        val s18 = 18.dp
        val s20 = 20.dp
        val s24 = 24.dp
        val s32 = 32.dp
        val s36 = 36.dp
        val s40 = 40.dp
        val s48 = 48.dp
        val s64 = 64.dp
        val s80 = 80.dp
    }

    object Radius {
        val sm = Space.s6
        val lg = Space.s12
        val xl = Space.s16
        val xxl = Space.s24
        val full = 999.dp

        val card = xxl
        val compactCard = lg
        val sheet = xxl
        val dialog = xxl
        val button = xl
        val chip = full
        val poster = lg
        val avatar = full
        val playerPanel = xxl
    }

    object Border {
        val hairline = Space.hairline
        val thin = Space.s1
        val medium = Space.s2
    }

    object Opacity {
        const val disabled = 0.38f
        const val secondary = 0.70f
        const val muted = 0.60f
        const val selected = 0.15f
        const val pressed = 0.12f
        const val subtle = 0.06f
        const val medium = 0.52f
        const val strong = 0.75f
        const val visible = 1f
    }

    object Motion {
        const val fastMillis = 150
        const val normalMillis = 220
    }

    object Icon {
        val sm = Space.s16
        val md = Space.s20
        val xl = Space.s32
    }

    object Type {
        val labelXs = 11.sp
        val labelSm = 12.sp
        val bodySm = 13.sp
        val bodyMd = 14.sp
        val bodyApp = 15.sp
        val bodyLg = 16.sp
        val titleSm = 18.sp
        val headline = 26.sp
        val titleMd = 22.sp
        val titleLg = 28.sp
        val displaySm = 32.sp
        val pageDisplay = 38.sp
        val displayMd = 48.sp
    }

    object LineHeight {
        val labelXs = 14.sp
        val labelSm = 15.sp
        val bodySm = 18.sp
        val bodyMd = 20.sp
        val bodyApp = 22.sp
        val bodyLg = 22.sp
        val titleSm = 22.sp
        val materialTitleLarge = 24.sp
        val headline = 30.sp
        val titleMd = 26.sp
        val titleLg = 32.sp
        val displaySm = 36.sp
        val pageDisplay = 42.sp
        val displayMd = 52.sp
    }

    object LetterSpacing {
        val none = 0.sp
        val pageDisplay = (-1.2).sp
        val headline = (-0.8).sp
        val label = 0.8.sp
    }

    object Z {
        const val dialog = 10f
    }
}

@Immutable
data class NuvioColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceCard: Color,
    val surfaceDialog: Color,
    val surfacePopover: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val focusRing: Color,
    val focusBackground: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderFocus: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val overlaySelected: Color,
    val skeleton: Color,
)

@Immutable
data class NuvioShapeTokens(
    val card: Shape,
    val compactCard: Shape,
    val sheet: Shape,
    val dialog: Shape,
    val button: Shape,
    val chip: Shape,
    val poster: Shape,
    val avatar: Shape,
    val playerPanel: Shape,
)

@Immutable
data class NuvioSpacingTokens(
    val listGap: Dp,
    val railGap: Dp,
    val cardPadding: Dp,
    val controlGap: Dp,
    val dialogPadding: Dp,
)

@Immutable
data class NuvioBorderTokens(
    val hairline: Dp,
    val thin: Dp,
    val medium: Dp,
)

@Immutable
data class NuvioOpacityTokens(
    val disabled: Float,
    val secondary: Float,
    val muted: Float,
    val selected: Float,
    val pressed: Float,
    val subtle: Float,
    val medium: Float,
    val strong: Float,
    val visible: Float,
)

@Immutable
data class NuvioIconTokens(
    val sm: Dp,
    val md: Dp,
    val xl: Dp,
)

@Immutable
data class NuvioComponentTokens(
    val avatarSize: Dp,
)

@Immutable
data class NuvioThemeTokens(
    val colors: NuvioColorTokens,
    val spacing: NuvioSpacingTokens,
    val shapes: NuvioShapeTokens,
    val borders: NuvioBorderTokens,
    val opacity: NuvioOpacityTokens,
    val icons: NuvioIconTokens,
    val components: NuvioComponentTokens,
)

internal val LocalNuvioThemeTokens = staticCompositionLocalOf {
    defaultNuvioThemeTokens(ThemeColors.White, amoled = false, colorScheme = null)
}

val MaterialTheme.nuvio: NuvioThemeTokens
    @Composable
    @Stable
    get() = LocalNuvioThemeTokens.current

internal fun defaultNuvioThemeTokens(
    palette: ThemeColorPalette,
    amoled: Boolean,
    colorScheme: ColorScheme?,
): NuvioThemeTokens {
    val background = if (amoled) Color.Black else palette.background
    val textPrimary = Color(0xFFF5F7F8)
    val textSecondary = Color(0xFFB8BEC5)
    val textMuted = Color(0xFF969CA3)
    val surface = palette.backgroundElevated
    val surfaceCard = palette.backgroundCard
    val accent = palette.secondary
    val borderSubtle = Color(0xFF252A2A).copy(alpha = 0.55f)
    val borderDefault = Color(0xFF252A2A)

    return NuvioThemeTokens(
        colors = NuvioColorTokens(
            background = background,
            surface = surface,
            surfaceElevated = surface,
            surfaceCard = surfaceCard,
            surfaceDialog = surface,
            surfacePopover = surfaceCard,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textMuted = textMuted,
            accent = accent,
            onAccent = palette.onSecondary,
            focusRing = palette.focusRing,
            focusBackground = palette.focusBackground,
            borderSubtle = borderSubtle,
            borderDefault = borderDefault,
            borderFocus = palette.focusRing,
            success = Color(0xFF66BB6A),
            warning = Color(0xFFFFC857),
            danger = colorScheme?.error ?: Color(0xFFE36A8A),
            overlaySelected = Color.White.copy(alpha = NuvioTokens.Opacity.selected),
            skeleton = Color.White.copy(alpha = 0.06f),
        ),
        spacing = NuvioSpacingTokens(
            listGap = NuvioTokens.Space.s12,
            railGap = NuvioTokens.Space.s14,
            cardPadding = NuvioTokens.Space.s18,
            controlGap = NuvioTokens.Space.s8,
            dialogPadding = NuvioTokens.Space.s20,
        ),
        shapes = NuvioShapeTokens(
            card = RoundedCornerShape(NuvioTokens.Radius.card),
            compactCard = RoundedCornerShape(NuvioTokens.Radius.compactCard),
            sheet = RoundedCornerShape(NuvioTokens.Radius.sheet),
            dialog = RoundedCornerShape(NuvioTokens.Radius.dialog),
            button = RoundedCornerShape(NuvioTokens.Radius.button),
            chip = RoundedCornerShape(NuvioTokens.Radius.chip),
            poster = RoundedCornerShape(NuvioTokens.Radius.poster),
            avatar = RoundedCornerShape(NuvioTokens.Radius.avatar),
            playerPanel = RoundedCornerShape(NuvioTokens.Radius.playerPanel),
        ),
        borders = NuvioBorderTokens(
            hairline = NuvioTokens.Border.hairline,
            thin = NuvioTokens.Border.thin,
            medium = NuvioTokens.Border.medium,
        ),
        opacity = NuvioOpacityTokens(
            disabled = NuvioTokens.Opacity.disabled,
            secondary = NuvioTokens.Opacity.secondary,
            muted = NuvioTokens.Opacity.muted,
            selected = NuvioTokens.Opacity.selected,
            pressed = NuvioTokens.Opacity.pressed,
            subtle = NuvioTokens.Opacity.subtle,
            medium = NuvioTokens.Opacity.medium,
            strong = NuvioTokens.Opacity.strong,
            visible = NuvioTokens.Opacity.visible,
        ),
        icons = NuvioIconTokens(
            sm = NuvioTokens.Icon.sm,
            md = NuvioTokens.Icon.md,
            xl = NuvioTokens.Icon.xl,
        ),
        components = NuvioComponentTokens(
            avatarSize = NuvioTokens.Space.s48,
        ),
    )
}
