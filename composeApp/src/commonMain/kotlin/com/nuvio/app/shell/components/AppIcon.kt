package com.nuvio.app.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

enum class AppIconResource {
    PlayerPlay,
    PlayerPause,
    PlayerAspectRatio,
    PlayerSubtitles,
    PlayerAudioFilled,
    PlayerSource,
    PlayerEpisodes,
    LibraryAddPlus,
}

@Composable
expect fun appIconPainter(icon: AppIconResource): Painter
