package com.nuvio.app.shell.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

internal enum class IntegrationLogo {
    Tmdb,
    Trakt,
    Simkl,
    MdbList,
    IntroDb,
}

@Composable
internal expect fun integrationLogoPainter(logo: IntegrationLogo): Painter
