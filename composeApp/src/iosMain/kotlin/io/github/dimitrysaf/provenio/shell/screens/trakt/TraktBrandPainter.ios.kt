package io.github.dimitrysaf.provenio.shell.screens.trakt

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.trakt_logo_wordmark
import provenio.composeapp.generated.resources.trakt_tv_favicon
import org.jetbrains.compose.resources.painterResource
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktBrandAsset

@Composable
actual fun traktBrandPainter(asset: TraktBrandAsset): Painter =
    when (asset) {
        TraktBrandAsset.Glyph -> painterResource(Res.drawable.trakt_tv_favicon)
        TraktBrandAsset.Wordmark -> painterResource(Res.drawable.trakt_logo_wordmark)
    }
