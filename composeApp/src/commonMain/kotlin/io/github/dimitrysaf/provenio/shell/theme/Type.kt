package io.github.dimitrysaf.provenio.shell.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

data class TypeScale(
    val labelXs: TextStyle,
    val labelSm: TextStyle,
    val bodySm: TextStyle,
    val bodyMd: TextStyle,
    val bodyLg: TextStyle,
    val titleSm: TextStyle,
    val titleMd: TextStyle,
    val titleLg: TextStyle,
    val displaySm: TextStyle,
    val displayMd: TextStyle,
)

internal val LocalTypeScale = staticCompositionLocalOf {
    TypeScale(
        labelXs = TextStyle(fontSize = Tokens.Type.labelXs, lineHeight = Tokens.LineHeight.labelXs, fontWeight = FontWeight.Medium),
        labelSm = TextStyle(fontSize = Tokens.Type.labelSm, lineHeight = Tokens.LineHeight.labelSm, fontWeight = FontWeight.Medium),
        bodySm = TextStyle(fontSize = Tokens.Type.bodySm, lineHeight = Tokens.LineHeight.bodySm, fontWeight = FontWeight.Normal),
        bodyMd = TextStyle(fontSize = Tokens.Type.bodyMd, lineHeight = Tokens.LineHeight.bodyMd, fontWeight = FontWeight.Normal),
        bodyLg = TextStyle(fontSize = Tokens.Type.bodyLg, lineHeight = Tokens.LineHeight.bodyLg, fontWeight = FontWeight.Medium),
        titleSm = TextStyle(fontSize = Tokens.Type.titleSm, lineHeight = Tokens.LineHeight.titleSm, fontWeight = FontWeight.Bold),
        titleMd = TextStyle(fontSize = Tokens.Type.titleMd, lineHeight = Tokens.LineHeight.titleMd, fontWeight = FontWeight.Bold),
        titleLg = TextStyle(fontSize = Tokens.Type.titleLg, lineHeight = Tokens.LineHeight.titleLg, fontWeight = FontWeight.Bold),
        displaySm = TextStyle(fontSize = Tokens.Type.displaySm, lineHeight = Tokens.LineHeight.displaySm, fontWeight = FontWeight.ExtraBold),
        displayMd = TextStyle(fontSize = Tokens.Type.displayMd, lineHeight = Tokens.LineHeight.displayMd, fontWeight = FontWeight.ExtraBold),
    )
}

val MaterialTheme.typeScale: TypeScale
    @Composable
    get() = LocalTypeScale.current
