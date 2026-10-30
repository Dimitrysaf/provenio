package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import io.github.dimitrysaf.provenio.core.playback.skip.PlayerNextEpisodeRules

import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.cast.CastController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.WatchPartyLineBar
import io.github.dimitrysaf.provenio.shell.WatchPartyLineHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingState
import io.github.dimitrysaf.provenio.core.p2p.formatP2pSpeed
import io.github.dimitrysaf.provenio.core.build.isIos
import io.github.dimitrysaf.provenio.shell.screens.p2p.TorrentDetailsSheet
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.party.PartyControlAction
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerPlatform
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerPlaybackRequest
import io.github.dimitrysaf.provenio.core.playback.PlayerStreamsRepository
import io.github.dimitrysaf.provenio.core.playback.SubtitleInput
import io.github.dimitrysaf.provenio.core.playback.selectionKey
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository

@Composable
internal fun PlayerScreenRuntime.RenderPlayerRuntimeUi() {
    val runtime = this
    val displayedPositionMs = scrubbingPositionMs ?: playbackSnapshot.positionMs
    val isP2pPlaybackActive = activeTorrentInfoHash != null
    val p2pConnecting = p2pStreamingState as? P2pStreamingState.Connecting
    val p2pStats = p2pStreamingState as? P2pStreamingState.Streaming
    val showTorrentStats = isP2pPlaybackActive && !p2pSettingsUiState.hideTorrentStats
    val torrentStatusLines = when {
        !showTorrentStats -> emptyList()
        p2pStats != null -> listOf(
            org.jetbrains.compose.resources.stringResource(Res.string.player_torrent_seeds, p2pStats.seeds),
            org.jetbrains.compose.resources.stringResource(Res.string.player_torrent_peers, p2pStats.peers),
            formatP2pSpeed(p2pStats.downloadSpeed),
            org.jetbrains.compose.resources.stringResource(
                Res.string.player_torrent_downloaded,
                "${(p2pStats.totalProgress * 100f).toInt().coerceIn(0, 100)}%",
            ),
        )
        p2pConnecting != null -> listOf(
            org.jetbrains.compose.resources.stringResource(Res.string.player_torrent_seeds, p2pConnecting.seeds),
            org.jetbrains.compose.resources.stringResource(Res.string.player_torrent_peers, p2pConnecting.peers),
            formatP2pSpeed(p2pConnecting.downloadSpeed),
        )
        else -> emptyList()
    }
    // The loading phase leads the top right status while the stream starts.
    val openingPhase = when {
        !playerSettingsUiState.showPlayerLoadingStatus -> null
        isP2pPlaybackActive && p2pConnecting != null -> p2pConnectingPhaseLabel(p2pConnecting.phase)
        isP2pPlaybackActive && p2pStats == null -> org.jetbrains.compose.resources.stringResource(
            Res.string.player_torrent_starting_engine,
        )
        else -> playerLoadingStatusMessage(
            showStatus = true,
            controllerReady = playerController != null,
            buffering = playbackSnapshot.isLoading,
        )
    }
    val openingStatusLines = listOfNotNull(openingPhase) + torrentStatusLines
    val gestureCallbacks = rememberSurfaceGestureCallbacks()
    val playbackGesturesEnabled = initialLoadCompleted && errorMessage == null
    // A stall mid-playback only counts once it lasts, so brief hiccups show nothing.
    val stalled = initialLoadCompleted && errorMessage == null && playbackSnapshot.isLoading
    var stallShown by remember { mutableStateOf(false) }
    LaunchedEffect(stalled) {
        if (stalled) {
            delay(PlayerStallRevealDelayMs)
            stallShown = true
        } else {
            stallShown = false
        }
    }
    // The share of the buffer the player waits for before it plays: 2.5s to start, 5s to resume.
    val bufferTargetMs = if (initialLoadCompleted) 5_000f else 2_500f
    val bufferFraction = (playbackSnapshot.bufferedPositionMs - playbackSnapshot.positionMs).coerceAtLeast(0L) / bufferTargetMs
    // The room's line takes the top, and the video moves down by its height.
    val partyLine = partyPlayerLine()
    val partyLineInset by animateDpAsState(if (partyLine != null) WatchPartyLineHeight else 0.dp)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = partyLineInset)
            .onSizeChanged { layoutSize = it }
            .then(playerKeyboardAndMouse())
            .playerSurfaceTapGestures(
                layoutSize = layoutSize,
                playbackGesturesEnabled = playbackGesturesEnabled,
                playerControlsLockedState = gestureCallbacks.playerControlsLocked,
                onSurfaceTap = gestureCallbacks.onSurfaceTap,
                onSurfaceDoubleTap = gestureCallbacks.onSurfaceDoubleTap,
                activateHoldToSpeedState = gestureCallbacks.activateHoldToSpeed,
                deactivateHoldToSpeedState = gestureCallbacks.deactivateHoldToSpeed,
                revealLockedOverlayState = gestureCallbacks.revealLockedOverlay,
            )
            .playerSurfaceDragGestures(
                gestureController = gestureController,
                volumeBoost = volumeBoost,
                layoutSize = layoutSize,
                playbackGesturesEnabled = playbackGesturesEnabled,
                sideGestureSystemEdgeExclusionPx = sideGestureSystemEdgeExclusionPx,
                playerControlsLockedState = gestureCallbacks.playerControlsLocked,
                touchGesturesEnabledState = gestureCallbacks.touchGesturesEnabled,
                isHoldToSpeedGestureActiveState = gestureCallbacks.isHoldToSpeedGestureActive,
                showBrightnessFeedbackState = gestureCallbacks.showBrightnessFeedback,
                showVolumeFeedbackState = gestureCallbacks.showVolumeFeedback,
                revealLockedOverlayState = gestureCallbacks.revealLockedOverlay,
            ),
    ) {
        val surfaceSource = rememberPlayerSurfaceSource(if (isP2pPlaybackActive) p2pResolvedSourceUrl else activeSourceUrl)
        val initialPositionRequestKey = currentInitialPositionRequestKey()
        if (surfaceSource != null) {
            PlatformPlayerSurface(
                sourceUrl = surfaceSource.url,
                sourceAudioUrl = surfaceSource.audioUrl,
                sourceHeaders = activeSourceHeaders,
                sourceResponseHeaders = activeSourceResponseHeaders,
                externalSubtitles = externalSubtitles,
                streamType = activeStreamType,
                useYoutubeChunkedPlayback = surfaceSource.isYouTube,
                modifier = Modifier.fillMaxSize(),
                playWhenReady = shouldPlay && !castingToDevice,
                initialPositionMs = activeInitialPositionMs.takeIf { it > 0L },
                initialPositionRequestKey = initialPositionRequestKey,
                resizeMode = resizeMode,
                onInitialPositionHandled = { key, handled ->
                    if (key == currentInitialPositionRequestKey()) {
                        initialSeekApplied = handled
                    }
                },
                onControllerReady = { controller ->
                    localPlayerController = controller
                    playerController = if (castingToDevice) CastPlayerController(controller) else controller
                    playerControllerSourceUrl = activeSourceUrl
                },
                onSnapshot = { snapshot ->
                    if (castingToDevice) return@PlatformPlayerSurface
                    playbackSnapshot = snapshot
                    refreshAudioTracksIfChanged()
                    if (!snapshot.isLoading) initialLoadCompleted = true
                    if (snapshot.isEnded) {
                        shouldPlay = false
                        controlsVisible = !playerControlsLocked
                    }
                },
                onError = { message ->
                    if (message != null && tryRefreshCredentialedSourceAfterError(message)) {
                        return@PlatformPlayerSurface
                    }
                    errorMessage = message
                    if (message != null) {
                        controlsVisible = !playerControlsLocked
                        removeFailedStreamFromCache()
                    }
                },
            )
            RenderAppDrawnSubtitles()
        }
        RenderCastBackdrop()

        // The artwork sits under the controls, which stay usable while the stream loads.
        AnimatedVisibility(
            visible = playerSettingsUiState.showLoadingOverlay && isStillLoading(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            OpeningOverlay(
                artworkCandidates = listOf(background, poster),
                logo = logo,
                title = title,
                onBack = {},
                metrics = metrics,
                modifier = Modifier.fillMaxSize(),
                backdropOnly = true,
            )
        }
        AnimatedVisibility(
            visible = playerSettingsUiState.showLoadingOverlay && stallShown,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
        }
        // The logo is its own layer, over the artwork or the stalled video and under the controls.
        AnimatedVisibility(
            visible = playerSettingsUiState.showLoadingOverlay && (isStillLoading() || stallShown),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerBufferingLogo(
                logo = logo,
                title = title,
                fraction = bufferFraction,
                modifier = Modifier.fillMaxSize(),
            )
        }
        RenderPlayerControls(
            displayedPositionMs = displayedPositionMs,
            statusLines = if (isStillLoading()) openingStatusLines else torrentStatusLines,
        )
        PlayerGestureFeedbackOverlay(
            visible = (liveGestureFeedback ?: gestureFeedback) != null,
            feedback = renderedGestureFeedback,
        )
        RenderPlaybackOverlays(runtime = runtime)
        PlayerWatchPartyChat(runtime = runtime)
        RenderPlayerModals(displayedPositionMs = displayedPositionMs)
        PlayerSubtitleAudioSettings(
            visible = showSubtitleAudioSettings,
            onDismiss = { showSubtitleAudioSettings = false },
        )
        if (showTorrentDetailsSheet) {
            TorrentDetailsSheet(
                infoHash = activeTorrentInfoHash,
                onDismiss = { showTorrentDetailsSheet = false },
            )
        }
        if (showInfoSheet) {
            val infoMeta = MetaDetailsRepository.peek(parentMetaType, parentMetaId)
                ?: metaUiState.meta?.takeIf { it.id == parentMetaId }
            PlayerInfoSheet(
                title = title,
                meta = infoMeta,
                episode = if (isSeries) {
                    (playerMetaVideos.takeIf { it.isNotEmpty() } ?: infoMeta?.videos.orEmpty())
                        .firstOrNull { it.id == activeVideoId }
                } else {
                    null
                },
                poster = poster,
                background = background,
                onDismiss = {
                    showInfoSheet = false
                    controlsVisible = true
                },
            )
        }
        RenderCastLayer()
    }
    WatchPartyLineBar(
        text = partyLine?.text,
        action = partyLine?.action,
        onClick = { partyLine?.onClick?.invoke() },
        modifier = Modifier.align(Alignment.TopCenter),
    )
    }
}

// Until the first frame is ready the player shows its artwork and logo, and only the controls that need no stream work.
internal fun PlayerScreenRuntime.isStillLoading(): Boolean = !initialLoadCompleted && errorMessage == null

@Composable
private fun p2pConnectingPhaseLabel(phase: String): String = when (phase) {
    "add_magnet" -> org.jetbrains.compose.resources.stringResource(
        provenio.composeapp.generated.resources.Res.string.player_torrent_fetching_metadata,
    )
    "prepare_stream", "attach_route" -> org.jetbrains.compose.resources.stringResource(
        provenio.composeapp.generated.resources.Res.string.player_torrent_preparing_stream,
    )
    else -> org.jetbrains.compose.resources.stringResource(
        provenio.composeapp.generated.resources.Res.string.player_torrent_starting_engine,
    )
}

private fun PlayerScreenRuntime.currentInitialPositionRequestKey(): String? {
    val positionMs = activeInitialPositionMs.takeIf { it > 0L } ?: return null
    return "$activePlaybackIdentity:${activeVideoId.orEmpty()}:$positionMs"
}

@Composable
private fun PlayerScreenRuntime.RenderPlayerControls(displayedPositionMs: Long, statusLines: List<String>) {
    val isInPip = rememberIsInPictureInPicture()
    val loading = isStillLoading()
    val externalPlayerId = playerSettingsUiState.externalPlayerId
    val externalPlayerConfigured = remember(externalPlayerId) {
        !externalPlayerId.isNullOrBlank() &&
            ExternalPlayerPlatform.availablePlayers().any { it.id == externalPlayerId }
    }
    AnimatedVisibility(
        visible = controlsVisible && !playerControlsLocked && !isInPip,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val primaryControlFocus = remember { FocusRequester() }
        LaunchedEffect(primaryControlFocusPending) {
            if (!primaryControlFocusPending) return@LaunchedEffect
            // The controls take a few frames to arrive as they fade in.
            repeat(PrimaryControlFocusFrames) {
                withFrameNanos {}
                if (runCatching { primaryControlFocus.requestFocus(FocusDirection.Enter) }.getOrDefault(false)) {
                    primaryControlFocusPending = false
                    return@LaunchedEffect
                }
            }
            primaryControlFocusPending = false
        }
        CompositionLocalProvider(LocalPlayerPrimaryControlFocus provides primaryControlFocus) {
            PlayerControlsShell(
                title = title,
                seasonNumber = activeSeasonNumber,
                episodeNumber = activeEpisodeNumber,
                episodeTitle = activeEpisodeTitle,
                playbackSnapshot = playbackSnapshot,
                displayedPositionMs = displayedPositionMs,
                chapters = chapters,
                metrics = metrics,
                resizeMode = resizeMode,
                showPlaybackControls = controlsVisible && !showPartyChat,
                controlsReady = !loading && playerController != null,
                playbackRequested = shouldPlay,
                seekEnabled = !partySeekLocked,
                pauseEnabled = !partyPauseLocked,
                speedEnabled = !partySpeedLocked,
                statusLines = statusLines,
                onStatusClick = if (activeTorrentInfoHash != null) {
                    { showTorrentDetailsSheet = true }
                } else {
                    null
                },
                onTitleClick = {
                    showInfoSheet = true
                    controlsVisible = false
                },
                onLockToggle = {
                    if (playerControlsLocked) unlockPlayerControls() else lockPlayerControls()
                },
                onBack = {
                    flushWatchProgress()
                    args.onBack()
                },
                onTogglePlayback = { togglePlayback() },
                onSeekBack = { seekBy(-seekStepMs(PlayerSeekDirection.Backward)) },
                onSeekForward = { seekBy(seekStepMs(PlayerSeekDirection.Forward)) },
                seekBackwardSeconds = playerSettingsUiState.seekBackwardSeconds,
                seekForwardSeconds = playerSettingsUiState.seekForwardSeconds,
                onResizeModeClick = { showResizeSheet = true },
                onSpeedClick = { showSpeedSheet = true },
                onSubtitleClick = {
                    refreshTracks()
                    showSubtitleModal = true
                },
                onAudioClick = {
                    refreshTracks()
                    showAudioModal = true
                },
                showCastButton = CastController.isSupported,
                casting = castingToDevice,
                onCastClick = {
                    if (castingToDevice) {
                        stopCasting()
                    } else {
                        showCastSheet = true
                    }
                    controlsVisible = true
                },
                onVideoSettingsClick = if (isIos) {
                    {
                        showVideoSettingsModal = true
                        controlsVisible = true
                    }
                } else {
                    null
                },
                // A guest in a watch party plays what the host plays, so it neither picks
                // another source or episode nor skips ahead.
                onSourcesClick = if (activeVideoId != null && !partyGuest) { { openSourcesPanel() } } else null,
                onEpisodesClick = if (isSeries && playerMetaVideos.isNotEmpty() && !partyGuest) { { openEpisodesPanel() } } else null,
                onChatClick = partyRoom?.takeIf { it.settings.chatEnabled }?.let {
                    {
                        showPartyChat = true
                        controlsVisible = false
                    }
                },
                showControlLabels = playerSettingsUiState.showPlayerControlLabels,
                onNextEpisode = nextEpisodeInfo?.takeIf { it.hasAired && !partyGuest }?.let {
                    {
                        nextEpisodeAutoPlayJob?.cancel()
                        playNextEpisode()
                    }
                },
                onOpenInExternalPlayer = args.onOpenInExternalPlayer?.takeIf { externalPlayerConfigured }?.let { openExternal ->
                    {
                        val loadedSubtitles = addonSubtitles
                            .takeIf { it.isNotEmpty() }
                            ?.map { sub ->
                                SubtitleInput(
                                    url = sub.url,
                                    name = buildString {
                                        if (!sub.addonName.isNullOrBlank()) append("[${sub.addonName}] ")
                                        append(sub.display)
                                    },
                                    lang = sub.language,
                                )
                            }
                        PlayerStreamsRepository.pauseSearchForPlayback()
                        openExternal(
                            ExternalPlayerPlaybackRequest(
                                sourceUrl = externalPlayerSourceUrl(),
                                title = title,
                                streamTitle = activeStreamTitle,
                                sourceHeaders = activeSourceHeaders,
                                resumePositionMs = playbackSnapshot.positionMs,
                                subtitles = loadedSubtitles,
                                season = activeSeasonNumber,
                                episode = activeEpisodeNumber,
                                episodeTitle = activeEpisodeTitle,
                            ),
                        )
                    }
                },
                onMoveWindow = args.onMoveWindow?.let { { moveToOtherWindow() } },
                inSeparateWindow = args.inSeparateWindow,
                onSubmitIntroClick = if (
                    isSeries &&
                    playerSettingsUiState.introSubmitEnabled &&
                    playerSettingsUiState.introDbApiKey.isNotBlank()
                ) {
                    { showSubmitIntroModal = true }
                } else {
                    null
                },
                onScrubChange = { positionMs ->
                    isScrubbingTimeline = true
                    scrubbingPositionMs = positionMs
                },
                onScrubFinished = { positionMs ->
                    isScrubbingTimeline = false
                    scrubbingPositionMs = null
                    if (partySeekSuggests) {
                        askPartyHost(PartyControlAction.SEEK, positionMs)
                    } else {
                        playerController?.seekTo(positionMs)
                        scheduleProgressSyncAfterSeek()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BoxScope.RenderPlaybackOverlays(
    runtime: PlayerScreenRuntime,
) {
    runtime.run {
        PlayerPlaybackOverlays(
            playerControlsLocked = playerControlsLocked,
            lockedOverlayVisible = lockedOverlayVisible,
            metrics = metrics,
            onUnlock = { unlockPlayerControls() },
            initialLoadCompleted = initialLoadCompleted,
            activeSkipInterval = activeSkipInterval,
            skipIntervalDismissed = skipIntervalDismissed,
            controlsVisible = controlsVisible,
            onSkipInterval = { interval ->
                val rawMs = (interval.endTime * 1000.0).toLong()
                val durationMs = playbackSnapshot.durationMs
                val seekMs = if (durationMs > 0L) rawMs.coerceAtMost(durationMs - 1) else rawMs
                playerController?.seekTo(seekMs)
                scheduleProgressSyncAfterSeek()
                skipIntervalDismissed = true
            },
            onDismissSkipInterval = { skipIntervalDismissed = true },
            isSeries = isSeries,
            nextEpisodeInfo = nextEpisodeInfo,
            showNextEpisodeCard = showNextEpisodeCard,
            inPlayNextWindow = PlayerNextEpisodeRules.isInPlayNextWindow(
                positionMs = playbackSnapshot.positionMs,
                durationMs = playbackSnapshot.durationMs,
                thresholdMode = playerSettingsUiState.nextEpisodeThresholdMode,
                thresholdPercent = playerSettingsUiState.nextEpisodeThresholdPercent,
                thresholdMinutesBeforeEnd = playerSettingsUiState.nextEpisodeThresholdMinutesBeforeEnd,
            ),
            nextEpisodeLoading = nextEpisodeAutoPlaySearching,
            nextEpisodeCountdown = nextEpisodeAutoPlayCountdown,
            blurUnwatchedEpisodes = metaScreenSettingsUiState.blurUnwatchedEpisodes,
            onPlayNextEpisode = {
                nextEpisodeAutoPlayJob?.cancel()
                if (PartyRepository.isGuest) {
                    // The host picks what plays; a guest's next episode goes to them as a suggestion.
                    suggestNextEpisodeToHost()
                } else {
                    markCurrentEpisodeWatched()
                    playNextEpisode()
                }
            },
            onDismissNextEpisode = {
                nextEpisodeAutoPlayJob?.cancel()
                nextEpisodeCardDismissed = true
                showNextEpisodeCard = false
                nextEpisodeAutoPlaySearching = false
                nextEpisodeAutoPlaySourceName = null
                nextEpisodeAutoPlayCountdown = null
            },
            errorMessage = errorMessage,
            onDismissError = {
                flushWatchProgress()
                args.onBack()
            },
            playNextLabel = if (partyGuest) stringResource(Res.string.watch_party_suggest) else null,
        )
    }
}

@Composable
private fun PlayerScreenRuntime.RenderPlayerModals(displayedPositionMs: Long) {
    PlayerScreenModalHosts(
        pendingP2pSwitch = pendingP2pSwitch,
        onPendingP2pSwitchChanged = { pendingP2pSwitch = it },
        onP2pEpisodeStreamSelected = { stream, episode, isAutoPlay ->
            switchToP2pEpisodeStream(stream, episode, isAutoPlay)
        },
        onP2pSourceStreamSelected = { stream -> switchToP2pSourceStream(stream) },
        onNextEpisodeAutoPlaySearchingChanged = { nextEpisodeAutoPlaySearching = it },
        onNextEpisodeAutoPlayCountdownChanged = { nextEpisodeAutoPlayCountdown = it },
        onNextEpisodeAutoPlaySourceNameChanged = { nextEpisodeAutoPlaySourceName = it },
        showAudioModal = showAudioModal,
        audioTracks = audioTracks,
        selectedAudioIndex = selectedAudioIndex,
        onAudioTrackSelected = { index ->
            selectedAudioIndex = index
            persistAudioPreference(audioTracks.firstOrNull { it.index == index })
            playerController?.selectAudioTrack(index)
            scope.launch {
                kotlinx.coroutines.delay(200)
                showAudioModal = false
            }
        },
        onAudioModalDismissed = { showAudioModal = false },
        onAudioMoreSettings = {
            showAudioModal = false
            showSubtitleAudioSettings = true
        },
        volumeBoostEnabled = volumeBoost.enabled,
        onVolumeBoostChanged = volumeBoost::switchOn,
        sleepTimer = sleepTimer,
        onSleepTimerClick = {
            showAudioModal = false
            showSleepTimerSheet = true
        },
        showSleepTimerSheet = showSleepTimerSheet,
        onSleepTimerSheetDismissed = {
            showSleepTimerSheet = false
            showAudioModal = true
        },
        showSubtitleModal = showSubtitleModal,
        onSubtitleMoreSettings = {
            showSubtitleModal = false
            showSubtitleAudioSettings = true
        },
        subtitleTracks = subtitleTracks,
        selectedSubtitleIndex = selectedSubtitleIndex,
        addonSubtitles = visibleAddonSubtitles,
        selectedAddonSubtitleId = selectedAddonSubtitleId,
        isLoadingAddonSubtitles = isLoadingAddonSubtitles,
        subtitleStyle = subtitleStyle,
        subtitleDelayMs = subtitleDelayMs,
        selectedAddonSubtitle = selectedAddonSubtitle,
        onBuiltInSubtitleTrackSelected = { index -> selectBuiltInSubtitle(index) },
        onAddonSubtitleSelected = { addon -> selectAddonSubtitle(addon) },
        onFetchAddonSubtitles = { fetchAddonSubtitlesForActiveItem() },
        onSubtitleDelayChanged = { delayMs -> setSubtitleDelay(delayMs) },
        onSubtitleModalDismissed = { showSubtitleModal = false },
        showSpeedSheet = showSpeedSheet,
        currentSpeed = playbackSnapshot.playbackSpeed,
        onSpeedSelected = { speed -> setPlaybackSpeedTo(speed) },
        onSpeedSheetDismissed = { showSpeedSheet = false },
        showResizeSheet = showResizeSheet,
        resizeMode = resizeMode,
        onResizeModeSelected = { mode -> setResizeModeTo(mode) },
        onResizeSheetDismissed = { showResizeSheet = false },
        showVideoSettingsModal = showVideoSettingsModal,
        playerSettings = playerSettingsUiState,
        onVideoSettingsChanged = {
            playerController?.configureIosVideoOutput(PlayerSettingsRepository.uiState.value)
        },
        onVideoSettingsModalDismissed = { showVideoSettingsModal = false },
        showSourcesPanel = showSourcesPanel,
        sourceStreamsState = sourceStreamsState,
        contentTitle = title,
        activeEpisodeTitle = activeEpisodeTitle,
        activeSourceUrl = activeSourceUrl,
        activeStreamTitle = activeStreamTitle,
        activeTorrentInfoHash = activeTorrentInfoHash,
        activeTorrentFileIdx = activeTorrentFileIdx,
        onSourceStreamSelected = { stream -> switchToSource(stream) },
        sourceStreamActions = { stream, onDismiss ->
            PlayerStreamActionsSheet(target = PlayerStreamActionsTarget(stream, episode = null), onDismiss = onDismiss)
        },
        onReloadSources = {
            val vid = activeVideoId
            if (vid != null) {
                PlayerStreamsRepository.loadSources(
                    type = contentType ?: parentMetaType,
                    videoId = vid,
                    season = activeSeasonNumber,
                    episode = activeEpisodeNumber,
                    forceRefresh = true,
                    parentMetaId = parentMetaId,
                )
            }
        },
        onSourcesPanelDismissed = {
            showSourcesPanel = false
            PlayerStreamsRepository.stopSourcesLoading()
            controlsVisible = true
        },
        isSeries = isSeries,
        showEpisodesPanel = showEpisodesPanel,
        allEpisodes = playerMetaVideos,
        parentMetaType = parentMetaType,
        parentMetaId = parentMetaId,
        poster = poster,
        background = background,
        activeSeasonNumber = activeSeasonNumber,
        activeEpisodeNumber = activeEpisodeNumber,
        watchProgressByVideoId = watchProgressUiState.byVideoIdForContent(parentMetaId),
        watchedKeys = watchedUiState.watchedKeys,
        blurUnwatchedEpisodes = metaScreenSettingsUiState.blurUnwatchedEpisodes,
        episodeStreamsPanelState = episodeStreamsPanelState,
        episodeStreamsRepoState = episodeStreamsRepoState,
        onEpisodeSelectedForDownload = { episode ->
            selectDownloadedEpisodeForPlayback(
                parentMetaId = parentMetaId,
                episode = episode,
                onDownloadedEpisodeSelected = { item, video -> switchToDownloadedEpisode(item, video) },
            )
        },
        onEpisodeStreamsRequested = { episode ->
            PlayerStreamsRepository.loadEpisodeStreams(
                type = contentType ?: parentMetaType,
                videoId = episode.id,
                season = episode.season,
                episode = episode.episode,
                parentMetaId = parentMetaId,
            )
            episodeStreamsPanelState = EpisodeStreamsPanelState(showStreams = true, selectedEpisode = episode)
        },
        onEpisodeStreamSelected = { stream, episode -> switchToEpisodeStream(stream, episode) },
        episodeStreamActions = { stream, episode, onDismiss ->
            PlayerStreamActionsSheet(target = PlayerStreamActionsTarget(stream, episode), onDismiss = onDismiss)
        },
        onBackToEpisodes = {
            episodeStreamsPanelState = EpisodeStreamsPanelState()
            PlayerStreamsRepository.clearEpisodeStreams()
        },
        onReloadEpisodeStreams = {
            val episode = episodeStreamsPanelState.selectedEpisode
            if (episode != null) {
                PlayerStreamsRepository.loadEpisodeStreams(
                    type = contentType ?: parentMetaType,
                    videoId = episode.id,
                    season = episode.season,
                    episode = episode.episode,
                    forceRefresh = true,
                    parentMetaId = parentMetaId,
                )
            }
        },
        onEpisodesPanelDismissed = {
            showEpisodesPanel = false
            episodeStreamsPanelState = EpisodeStreamsPanelState()
            PlayerStreamsRepository.clearEpisodeStreams()
            controlsVisible = true
        },
        showSubmitIntroModal = showSubmitIntroModal,
        activeVideoId = activeVideoId,
        metaUiState = metaUiState,
        displayedPositionMs = displayedPositionMs,
        submitIntroSegmentType = submitIntroSegmentType,
        onSubmitIntroSegmentTypeChanged = { submitIntroSegmentType = it },
        submitIntroStartTimeStr = submitIntroStartTimeStr,
        onSubmitIntroStartTimeChanged = { submitIntroStartTimeStr = it },
        submitIntroEndTimeStr = submitIntroEndTimeStr,
        onSubmitIntroEndTimeChanged = { submitIntroEndTimeStr = it },
        onSubmitIntroDismissed = { showSubmitIntroModal = false },
        onSubmitIntroSuccess = {
            submitIntroStartTimeStr = "00:00"
            submitIntroEndTimeStr = "00:00"
            submitIntroSegmentType = "intro"
            showSubmitIntroModal = false
        },
    )
}

private const val PlayerStallRevealDelayMs = 500L

private const val PrimaryControlFocusFrames = 20
