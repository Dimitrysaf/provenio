package io.github.dimitrysaf.provenio.shell.screens.simkl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import io.github.dimitrysaf.provenio.R

@Composable
actual fun simklBrandPainter(asset: SimklBrandAsset): Painter =
    painterResource(
        id = when (asset) {
            SimklBrandAsset.Glyph -> R.drawable.simkl_logo_glyph
            SimklBrandAsset.Wordmark -> R.drawable.simkl_logo_wordmark
        },
    )
