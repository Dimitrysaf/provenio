package io.github.dimitrysaf.provenio.shell.screens.trakt

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import io.github.dimitrysaf.provenio.R
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktBrandAsset

@Composable
actual fun traktBrandPainter(asset: TraktBrandAsset): Painter =
    painterResource(
        id = when (asset) {
            TraktBrandAsset.Glyph -> R.drawable.trakt_tv_favicon
            TraktBrandAsset.Wordmark -> R.drawable.trakt_logo_wordmark
        },
    )
