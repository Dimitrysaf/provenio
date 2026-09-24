package io.github.dimitrysaf.provenio.shell.screens.simkl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.simkl_logo_glyph
import provenio.composeapp.generated.resources.simkl_logo_wordmark
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun simklBrandPainter(asset: SimklBrandAsset): Painter =
    when (asset) {
        SimklBrandAsset.Glyph -> painterResource(Res.drawable.simkl_logo_glyph)
        SimklBrandAsset.Wordmark -> painterResource(Res.drawable.simkl_logo_wordmark)
    }
