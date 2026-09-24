package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.dimitrysaf.provenio.core.settings.CustomThemeColors
import io.github.dimitrysaf.provenio.shell.theme.Tokens
import io.github.dimitrysaf.provenio.shell.theme.accentBrush
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.shell.theme.toColorPalette
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.custom_theme_preview
import provenio.composeapp.generated.resources.custom_theme_preview_accent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CustomThemePreview(colors: CustomThemeColors) {
    val tokens = MaterialTheme.provenio
    val palette = remember(colors) { colors.toColorPalette() }
    Column(
        modifier = Modifier.fillMaxWidth().clip(tokens.shapes.card)
            .background(palette.background).padding(tokens.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.s12),
    ) {
        Text(
            text = stringResource(Res.string.custom_theme_preview),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textSecondary,
        )
        Box(
            Modifier.fillMaxWidth().height(Tokens.Space.s32)
                .clip(tokens.shapes.chip).background(palette.accentBrush()),
        )
        Box(
            modifier = Modifier.fillMaxWidth().clip(tokens.shapes.button)
                .background(palette.backgroundCard)
                .border(tokens.borders.medium, palette.accentBrush(), tokens.shapes.button)
                .padding(Tokens.Space.s12),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.custom_theme_preview_accent),
                style = MaterialTheme.typography.titleMedium.copy(brush = palette.accentBrush()),
            )
        }
    }
}
