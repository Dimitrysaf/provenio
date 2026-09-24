package com.nuvio.app.shell.screens.simkl

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

enum class SimklBrandAsset {
    Glyph,
    Wordmark,
}

@Composable
expect fun simklBrandPainter(asset: SimklBrandAsset): Painter
