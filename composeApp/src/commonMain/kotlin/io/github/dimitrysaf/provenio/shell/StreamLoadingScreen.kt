package io.github.dimitrysaf.provenio.shell

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.shell.screens.player.OpeningOverlay
import io.github.dimitrysaf.provenio.shell.screens.player.subtitleLoadingStatusMessage
import io.github.dimitrysaf.provenio.core.streams.StreamLaunch
import io.github.dimitrysaf.provenio.core.streams.StreamsUiState
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.debrid_resolving_stream
import provenio.composeapp.generated.resources.player_loading_preparing
import provenio.composeapp.generated.resources.streams_finding_source
import provenio.composeapp.generated.resources.streams_loading_subtitles
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
