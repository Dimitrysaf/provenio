package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_player_airs_prefix
import provenio.composeapp.generated.resources.compose_player_downloaded
import provenio.composeapp.generated.resources.compose_player_resize_fill
import provenio.composeapp.generated.resources.compose_player_resize_fit
import provenio.composeapp.generated.resources.compose_player_resize_zoom
import provenio.composeapp.generated.resources.generic_unknown
import provenio.composeapp.generated.resources.parental_alcohol
import provenio.composeapp.generated.resources.parental_frightening
import provenio.composeapp.generated.resources.parental_nudity
import provenio.composeapp.generated.resources.parental_profanity
import provenio.composeapp.generated.resources.parental_severity_mild
import provenio.composeapp.generated.resources.parental_severity_moderate
import provenio.composeapp.generated.resources.parental_severity_severe
import provenio.composeapp.generated.resources.parental_violence
import provenio.composeapp.generated.resources.compose_player_tba
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.playback.ParentalGuideLabels
import io.github.dimitrysaf.provenio.core.playback.PlayerStreamsRepository
import io.github.dimitrysaf.provenio.core.playback.mergeStreamAndAddonSubtitles
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.playback.SubtitleRepository

@Composable
internal fun PlayerScreenContent(args: PlayerScreenArgs) {
    val playerSettingsUiState by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val p2pSettingsUiState by remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val p2pStreamingState by P2pStreamingEngine.state.collectAsStateWithLifecycle()
    val metaScreenSettingsUiState by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchProgressUiState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    val sourceStreamsState by PlayerStreamsRepository.sourceState.collectAsStateWithLifecycle()
    val episodeStreamsRepoState by PlayerStreamsRepository.episodeStreamsState.collectAsStateWithLifecycle()
    val metaUiState by MetaDetailsRepository.uiState.collectAsStateWithLifecycle()
    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val addonSubtitles by SubtitleRepository.addonSubtitles.collectAsStateWithLifecycle()
    val isLoadingAddonSubtitles by SubtitleRepository.isLoading.collectAsStateWithLifecycle()

    val runtime = remember { PlayerScreenRuntime(args) }
    runtime.args = args

    BoxWithConstraints(
        modifier = args.modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val density = LocalDensity.current
        val horizontalSafePadding = playerHorizontalSafePadding()
        val metrics = remember(maxWidth) { PlayerLayoutMetrics.fromWidth(maxWidth) }

        runtime.scope = rememberCoroutineScope()
        runtime.hapticFeedback = LocalHapticFeedback.current
        runtime.gestureController = rememberPlayerGestureController()
        runtime.playerSettingsUiState = playerSettingsUiState
        runtime.p2pSettingsUiState = p2pSettingsUiState
        runtime.p2pStreamingState = p2pStreamingState
        runtime.metaScreenSettingsUiState = metaScreenSettingsUiState
        runtime.watchedUiState = watchedUiState
        runtime.watchProgressUiState = watchProgressUiState
        runtime.sourceStreamsState = sourceStreamsState
        runtime.episodeStreamsRepoState = episodeStreamsRepoState
        runtime.metaUiState = metaUiState
        runtime.addonsUiState = addonsUiState
        runtime.addonSubtitles = mergeStreamAndAddonSubtitles(addonSubtitles, runtime.externalSubtitles)
        runtime.isLoadingAddonSubtitles = isLoadingAddonSubtitles
        runtime.horizontalSafePadding = horizontalSafePadding
        runtime.metrics = metrics
        runtime.sliderEdgePadding = horizontalSafePadding + metrics.horizontalPadding
        runtime.overlayBottomPadding = sliderOverlayBottomPadding(metrics)
        runtime.sideGestureSystemEdgeExclusionPx = with(density) {
            PlayerSideGestureSystemEdgeExclusion.toPx()
        }
        runtime.resizeModeFitLabel = stringResource(Res.string.compose_player_resize_fit)
        runtime.resizeModeFillLabel = stringResource(Res.string.compose_player_resize_fill)
        runtime.resizeModeZoomLabel = stringResource(Res.string.compose_player_resize_zoom)
        runtime.downloadedLabel = stringResource(Res.string.compose_player_downloaded)
        runtime.airsPrefix = stringResource(Res.string.compose_player_airs_prefix)
        runtime.tbaLabel = stringResource(Res.string.compose_player_tba)
        runtime.genericUnknownLabel = stringResource(Res.string.generic_unknown)
        runtime.parentalGuideLabels = ParentalGuideLabels(
            nudity = stringResource(Res.string.parental_nudity),
            violence = stringResource(Res.string.parental_violence),
            profanity = stringResource(Res.string.parental_profanity),
            alcohol = stringResource(Res.string.parental_alcohol),
            frightening = stringResource(Res.string.parental_frightening),
            severe = stringResource(Res.string.parental_severity_severe),
            moderate = stringResource(Res.string.parental_severity_moderate),
            mild = stringResource(Res.string.parental_severity_mild),
        )
        if (runtime.playerMetaVideos.isEmpty()) {
            runtime.playerMetaVideos = MetaDetailsRepository.peek(
                args.parentMetaType,
                args.parentMetaId,
            )?.videos ?: emptyList()
        }
        if (runtime.lastSyncedSettingsResizeMode != playerSettingsUiState.resizeMode) {
            runtime.resizeMode = playerSettingsUiState.resizeMode
            runtime.lastSyncedSettingsResizeMode = playerSettingsUiState.resizeMode
        }
        runtime.resetIdentityStateIfNeeded()

        val keepScreenAwake = runtime.errorMessage == null &&
            (runtime.playbackSnapshot.isPlaying ||
                (runtime.shouldPlay && runtime.playbackSnapshot.isLoading))
        EnterImmersivePlayerMode(keepScreenAwake = keepScreenAwake)
        // Loading counts as playing so the player can float while a stream starts.
        ManagePlayerPictureInPicture(
            isPlaying = keepScreenAwake,
            videoSize = IntSize(
                runtime.playbackSnapshot.videoWidth,
                runtime.playbackSnapshot.videoHeight,
            ),
        )
        runtime.BindPlayerRuntimeEffects()
        runtime.RenderPlayerRuntimeUi()
    }
}
