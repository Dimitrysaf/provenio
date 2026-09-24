package com.nuvio.app.core.playback

import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_loading_buffering
import nuvio.composeapp.generated.resources.player_loading_building
import nuvio.composeapp.generated.resources.player_loading_starting
import org.jetbrains.compose.resources.StringResource

internal fun playerLoadingStatusResource(
    showStatus: Boolean,
    controllerReady: Boolean,
    buffering: Boolean,
): StringResource? = when {
    !showStatus -> null
    !controllerReady -> Res.string.player_loading_building
    buffering -> Res.string.player_loading_buffering
    else -> Res.string.player_loading_starting
}
