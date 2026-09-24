package com.nuvio.app.shell.screens.trakt

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.nuvio.app.core.tracking.trakt.TraktBrandAsset

@Composable
expect fun traktBrandPainter(asset: TraktBrandAsset): Painter
