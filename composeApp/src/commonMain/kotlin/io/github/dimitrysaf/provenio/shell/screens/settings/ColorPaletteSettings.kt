package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.settings.AppIconOption
import io.github.dimitrysaf.provenio.core.settings.ColorPalette
import io.github.dimitrysaf.provenio.core.settings.labelResource
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.theme.rememberDynamicColorScheme
import io.github.dimitrysaf.provenio.shell.theme.rememberIconColorScheme
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_appearance_color_palette
import provenio.composeapp.generated.resources.settings_appearance_color_palette_wallpaper
import provenio.composeapp.generated.resources.settings_appearance_color_palette_wallpaper_description

internal fun ColorPalette.effective(dynamicColorAvailable: Boolean): ColorPalette =
    if (this == ColorPalette.Dynamic && !dynamicColorAvailable) ColorPalette.Icon(AppIconOption.DEFAULT) else this

@Composable
internal fun ColorPalette.label(): String = when (this) {
    ColorPalette.Dynamic -> stringResource(Res.string.settings_appearance_color_palette_wallpaper)
    is ColorPalette.Icon -> stringResource(icon.labelResource)
}

@Composable
internal fun ColorPaletteSwatch(
    palette: ColorPalette,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scheme = when (palette) {
        ColorPalette.Dynamic -> rememberDynamicColorScheme(darkTheme)
        is ColorPalette.Icon -> rememberIconColorScheme(palette.icon, darkTheme)
    } ?: MaterialTheme.colorScheme
    ColorSchemeSwatch(scheme = scheme, modifier = modifier.size(size))
}

@Composable
private fun ColorSchemeSwatch(scheme: ColorScheme, modifier: Modifier) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
    ) {
        drawRect(color = scheme.primary, size = Size(size.width, size.height / 2f))
        drawRect(
            color = scheme.secondary,
            topLeft = Offset(0f, size.height / 2f),
            size = Size(size.width / 2f, size.height / 2f),
        )
        drawRect(
            color = scheme.tertiary,
            topLeft = Offset(size.width / 2f, size.height / 2f),
            size = Size(size.width / 2f, size.height / 2f),
        )
    }
}

@Composable
internal fun ColorPaletteBottomSheet(
    selected: ColorPalette,
    dynamicColorAvailable: Boolean,
    onSelected: (ColorPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    val palettes = buildList {
        if (dynamicColorAvailable) add(ColorPalette.Dynamic)
        AppIconOption.entries.forEach { add(ColorPalette.Icon(it)) }
    }
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_appearance_color_palette),
        options = palettes.map { palette ->
            SingleChoiceOption(
                value = palette,
                label = palette.label(),
                supportingText = if (palette == ColorPalette.Dynamic) {
                    stringResource(Res.string.settings_appearance_color_palette_wallpaper_description)
                } else {
                    null
                },
                leadingContent = { ColorPaletteSwatch(palette = palette) },
            )
        },
        isSelected = { it == selected },
        onSelected = onSelected,
        onDismiss = onDismiss,
    )
}
