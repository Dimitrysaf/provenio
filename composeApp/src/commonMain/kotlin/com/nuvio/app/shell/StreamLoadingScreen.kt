package com.nuvio.app.shell

import androidx.compose.runtime.Composable
import com.nuvio.app.shell.screens.player.OpeningOverlay
import com.nuvio.app.shell.screens.player.subtitleLoadingStatusMessage
import com.nuvio.app.core.streams.StreamLaunch
import com.nuvio.app.core.streams.StreamsUiState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.debrid_resolving_stream
import nuvio.composeapp.generated.resources.player_loading_preparing
import nuvio.composeapp.generated.resources.streams_finding_source
import nuvio.composeapp.generated.resources.streams_loading_subtitles
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StreamLoadingScreen(
    launch: StreamLaunch,
    state: StreamsUiState,
    showStatus: Boolean,
    resolvingDebridStream: Boolean,
    onBack: () -> Unit,
) {
    val message = when {
        !showStatus -> null
        resolvingDebridStream -> stringResource(Res.string.debrid_resolving_stream)
        state.overlayMessage == stringResource(Res.string.streams_loading_subtitles) -> subtitleLoadingStatusMessage()
        state.overlayMessage != null -> state.overlayMessage
        state.autoPlayStream != null -> stringResource(Res.string.player_loading_preparing)
        else -> stringResource(Res.string.streams_finding_source)
    }
    OpeningOverlay(
        artwork = launch.background ?: launch.poster,
        logo = launch.logo,
        title = launch.title,
        onBack = onBack,
        statusLines = listOfNotNull(message),
    )
}
