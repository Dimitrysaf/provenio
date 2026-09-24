package com.nuvio.app.shell.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.NuvioStatusModal
import com.nuvio.app.shell.screens.player.skip.NextEpisodeCard
import com.nuvio.app.core.playback.skip.NextEpisodeInfo
import com.nuvio.app.shell.screens.player.skip.SkipIntroButton
import com.nuvio.app.core.playback.skip.SkipInterval
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_player_go_back
import nuvio.composeapp.generated.resources.compose_player_playback_error
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BoxScope.PlayerPlaybackOverlays(
    playerControlsLocked: Boolean,
    lockedOverlayVisible: Boolean,
    metrics: PlayerLayoutMetrics,
    onUnlock: () -> Unit,
    showOpeningOverlay: Boolean,
    backdropArtwork: String?,
    logo: String?,
    title: String,
    onBackWithProgress: () -> Unit,
    openingStatusLines: List<String>,
    initialLoadCompleted: Boolean,
    activeSkipInterval: SkipInterval?,
    skipIntervalDismissed: Boolean,
    controlsVisible: Boolean,
    onSkipInterval: (SkipInterval) -> Unit,
    onDismissSkipInterval: () -> Unit,
    isSeries: Boolean,
    nextEpisodeInfo: NextEpisodeInfo?,
    showNextEpisodeCard: Boolean,
    nextEpisodeLoading: Boolean,
    blurUnwatchedEpisodes: Boolean,
    onPlayNextEpisode: () -> Unit,
    onDismissNextEpisode: () -> Unit,
    errorMessage: String?,
    onDismissError: () -> Unit,
) {
    AnimatedVisibility(
        visible = playerControlsLocked && lockedOverlayVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LockedPlayerOverlay(
            metrics = metrics,
            onUnlock = onUnlock,
            modifier = Modifier.fillMaxSize(),
        )
    }

    AnimatedVisibility(
        visible = showOpeningOverlay,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        OpeningOverlay(
            artwork = backdropArtwork,
            logo = logo,
            title = title,
            onBack = onBackWithProgress,
            metrics = metrics,
            modifier = Modifier.fillMaxSize(),
            statusLines = openingStatusLines,
        )
    }

    if (!playerControlsLocked) {
        // Sits just above the progress bar, whether or not the controls are showing.
        SkipIntroButton(
            interval = if (!initialLoadCompleted) null else activeSkipInterval,
            dismissed = skipIntervalDismissed,
            controlsVisible = controlsVisible,
            onSkip = {
                activeSkipInterval?.let(onSkipInterval)
            },
            onDismiss = onDismissSkipInterval,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .playerFrameInsets(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                .padding(end = metrics.horizontalPadding)
                .padding(bottom = metrics.sliderBottomOffset + playerBottomControlsHeight(metrics) + 8.dp),
        )
    }

    if (isSeries && !playerControlsLocked) {
        NextEpisodeCard(
            nextEpisode = nextEpisodeInfo,
            visible = showNextEpisodeCard,
            isLoading = nextEpisodeLoading,
            blurred = blurUnwatchedEpisodes && nextEpisodeInfo?.isWatched == false,
            onPlayNext = onPlayNextEpisode,
            onDismiss = onDismissNextEpisode,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .playerFrameInsets(WindowInsetsSides.Horizontal)
                .padding(end = metrics.horizontalPadding),
        )
    }

    NuvioStatusModal(
        title = stringResource(Res.string.compose_player_playback_error),
        message = errorMessage.orEmpty(),
        isVisible = errorMessage != null,
        confirmText = stringResource(Res.string.compose_player_go_back),
        onConfirm = onDismissError,
        onDismiss = onDismissError,
    )
}
