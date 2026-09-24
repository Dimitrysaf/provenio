package io.github.dimitrysaf.provenio.shell.screens.trakt

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktBrandAsset

@Composable
expect fun traktBrandPainter(asset: TraktBrandAsset): Painter
