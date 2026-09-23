package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor

fun ThemeColorPalette.accentBrush(): Brush =
    if (accentGradient.size >= 2) Brush.linearGradient(accentGradient)
    else SolidColor(accentGradient.firstOrNull() ?: secondary)
