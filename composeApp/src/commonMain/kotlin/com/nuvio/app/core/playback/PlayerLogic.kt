package com.nuvio.app.core.playback

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_resize_fill
import nuvio.composeapp.generated.resources.compose_player_resize_fit
import nuvio.composeapp.generated.resources.compose_player_resize_zoom
import org.jetbrains.compose.resources.StringResource

internal val PlayerResizeMode.labelRes: StringResource
    get() = when (this) {
        PlayerResizeMode.Fit -> Res.string.compose_player_resize_fit
        PlayerResizeMode.Fill -> Res.string.compose_player_resize_fill
        PlayerResizeMode.Zoom -> Res.string.compose_player_resize_zoom
    }

internal fun PlayerResizeMode.next(): PlayerResizeMode =
    when (this) {
        PlayerResizeMode.Fit -> PlayerResizeMode.Fill
        PlayerResizeMode.Fill -> PlayerResizeMode.Zoom
        PlayerResizeMode.Zoom -> PlayerResizeMode.Fit
    }
