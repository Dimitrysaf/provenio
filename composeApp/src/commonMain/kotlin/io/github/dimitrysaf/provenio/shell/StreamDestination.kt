package io.github.dimitrysaf.provenio.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlayableResult
import io.github.dimitrysaf.provenio.core.debrid.DirectDebridPlaybackResolver
import io.github.dimitrysaf.provenio.core.debrid.toastMessage
import io.github.dimitrysaf.provenio.shell.screens.p2p.P2pConsentDialog
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunch
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunchStore
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamItem
import io.github.dimitrysaf.provenio.core.streams.StreamLaunchStore
import io.github.dimitrysaf.provenio.core.streams.StreamLinkCacheRepository
import io.github.dimitrysaf.provenio.core.streams.StreamsRepository
import io.github.dimitrysaf.provenio.shell.screens.streams.ActiveStreamStore
import io.github.dimitrysaf.provenio.shell.screens.streams.StreamsSheet
import io.github.dimitrysaf.provenio.core.streams.shouldShowAutoPlayLoading
import io.github.dimitrysaf.provenio.core.streams.shouldUseLandscapeAutoPlayLoading
import io.github.dimitrysaf.provenio.core.streams.StreamsUiState
import io.github.dimitrysaf.provenio.shell.nav.*
import kotlinx.coroutines.launch

private data class PendingP2pStreamOpen(
    val stream: StreamItem,
    val resumePositionMs: Long?,
    val resumeProgressFraction: Float?,
    val isAutoPlay: Boolean,
)

@Composable
internal fun StreamDestination(
    route: StreamRoute,
    navController: Navigator,
    p2pEnabled: Boolean,
    openExternalPlayback: suspend (PlayerLaunch) -> Boolean,
    openExternalStreamUrl: (String) -> Boolean,
    onLandscapeLoadingChanged: (Boolean) -> Unit,
) {
    val onBack = rememberGuardedPopBackStack(navController, route)
    val launch = remember(route.launchId) {
        StreamLaunchStore.get(route.launchId)
    }
    if (launch == null) {
        LaunchedEffect(route.launchId) {
            onBack()
        }
        return
    }
    val streamRouteScope = rememberCoroutineScope()
    var autoPlayNavigationStarted by remember(route.launchId) { mutableStateOf(false) }
    var resolvingDebridStream by rememberSaveable(route.launchId) { mutableStateOf(false) }
    var pendingP2pStreamOpen by remember { mutableStateOf<PendingP2pStreamOpen?>(null) }
    val episodeVideoId = rememberEpisodeVideoId(launch)
    val effectiveVideoId = episodeVideoId.value
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    // Opens the player, replacing this sheet when the stream was picked for the user.
    fun openPlayer(playerLaunch: PlayerLaunch, replaceStreamRoute: Boolean) {
        val launchId = PlayerLaunchStore.put(playerLaunch)
        navController.navigate(PlayerRoute(launchId = launchId, title = playerLaunch.title)) {
            if (replaceStreamRoute) {
                popUpTo<StreamRoute> { inclusive = true }
            }
        }
    }

    fun openP2pStream(
        stream: StreamItem,
        resolvedResumePositionMs: Long?,
        resolvedResumeProgressFraction: Float?,
        replaceStreamRoute: Boolean,
    ) {
        val infoHash = stream.p2pInfoHash ?: return
        if (playerSettings.streamReuseLastLinkEnabled) {
            launch.cacheP2pLink(effectiveVideoId, stream, infoHash)
        }
        val playerLaunch = launch.p2pPlayerLaunch(
            videoId = effectiveVideoId,
            stream = stream,
            infoHash = infoHash,
            resumePositionMs = resolvedResumePositionMs,
            resumeProgressFraction = resolvedResumeProgressFraction,
        )
        autoPlayNavigationStarted = replaceStreamRoute
        StreamsRepository.cancelLoading()
        openPlayer(playerLaunch, replaceStreamRoute)
    }

    // Torrents need P2P turned on; when it is off, ask first and open once the user agrees.
    fun requestOrOpenP2pStream(
        stream: StreamItem,
        resolvedResumePositionMs: Long?,
        resolvedResumeProgressFraction: Float?,
        isAutoPlay: Boolean,
    ) {
        if (stream.p2pInfoHash == null || !P2pSettingsRepository.isVisible) {
            if (isAutoPlay) StreamsRepository.skipAutoPlayStream(stream)
            return
        }
        if (!p2pEnabled) {
            pendingP2pStreamOpen = PendingP2pStreamOpen(
                stream = stream,
                resumePositionMs = resolvedResumePositionMs,
                resumeProgressFraction = resolvedResumeProgressFraction,
                isAutoPlay = isAutoPlay,
            )
            return
        }
        openP2pStream(
            stream = stream,
            resolvedResumePositionMs = resolvedResumePositionMs,
            resolvedResumeProgressFraction = resolvedResumeProgressFraction,
            replaceStreamRoute = isAutoPlay,
        )
    }

    var reuseHandled by rememberSaveable(launch.videoId, effectiveVideoId) { mutableStateOf(false) }
    var reuseNavigated by remember { mutableStateOf(false) }
    LaunchedEffect(effectiveVideoId, episodeVideoId.isResolved, playerSettings.streamReuseLastLinkEnabled, launch.manualSelection) {
        if (!episodeVideoId.isResolved) return@LaunchedEffect
        if (reuseHandled) return@LaunchedEffect
        reuseHandled = true
        if (launch.manualSelection) return@LaunchedEffect
        if (!playerSettings.streamReuseLastLinkEnabled) return@LaunchedEffect
        val maxAgeMs = playerSettings.streamReuseLastLinkCacheHours * 60L * 60L * 1000L
        val cached = StreamLinkCacheRepository.getValid(launch.linkCacheKey(effectiveVideoId), maxAgeMs)
            ?: return@LaunchedEffect
        if (cached.url.isBlank() && !cached.infoHash.isNullOrBlank()) {
            requestOrOpenP2pStream(
                stream = cached.toP2pStream(),
                resolvedResumePositionMs = launch.resumePositionMs,
                resolvedResumeProgressFraction = launch.resumeProgressFraction,
                isAutoPlay = true,
            )
            reuseNavigated = true
            return@LaunchedEffect
        }
        val playerLaunch = launch.cachedPlayerLaunch(effectiveVideoId, cached)
        if (playerSettings.externalPlayerEnabled) {
            openExternalPlayback(playerLaunch)
            StreamsRepository.cancelLoading()
            StreamsRepository.setOverlayVisible(false)
            reuseNavigated = true
            return@LaunchedEffect
        }
        StreamsRepository.clear()
        reuseNavigated = true
        autoPlayNavigationStarted = true
        openPlayer(playerLaunch, replaceStreamRoute = true)
    }

    val streamsUiState by StreamsRepository.uiState.collectAsStateWithLifecycle()
    val expectedStreamsRequestToken = StreamsRepository.requestToken(
        type = launch.type,
        videoId = effectiveVideoId,
        season = launch.seasonNumber,
        episode = launch.episodeNumber,
        manualSelection = launch.manualSelection,
    )
    val showLoadingScreen = autoPlayNavigationStarted || resolvingDebridStream || streamsUiState.shouldShowAutoPlayLoading(
        expectedRequestToken = expectedStreamsRequestToken,
        settings = playerSettings,
        manualSelection = launch.manualSelection,
    )
    val useLandscapeLoading = autoPlayNavigationStarted || streamsUiState.shouldUseLandscapeAutoPlayLoading(
        expectedRequestToken = expectedStreamsRequestToken,
        manualSelection = launch.manualSelection,
    )
    SideEffect { onLandscapeLoadingChanged(useLandscapeLoading) }
    var autoPlayHandled by rememberSaveable(launch.videoId, effectiveVideoId) { mutableStateOf(false) }
    LaunchedEffect(
        streamsUiState.autoPlayStream,
        streamsUiState.requestToken,
        expectedStreamsRequestToken,
        reuseHandled,
        launch.manualSelection,
    ) {
        if (!reuseHandled) return@LaunchedEffect
        if (launch.manualSelection) return@LaunchedEffect
        if (reuseNavigated) return@LaunchedEffect
        if (autoPlayHandled) return@LaunchedEffect
        if (streamsUiState.requestToken != expectedStreamsRequestToken) return@LaunchedEffect
        val selectedStream = streamsUiState.autoPlayStream ?: return@LaunchedEffect
        val stream = launch.resolveAutoPlayStream(selectedStream, effectiveVideoId) ?: return@LaunchedEffect
        val sourceUrl = stream.playableDirectUrl
        if (sourceUrl == null && stream.needsLocalDebridResolve && stream.p2pInfoHash != null) {
            autoPlayHandled = true
            requestOrOpenP2pStream(
                stream = stream,
                resolvedResumePositionMs = launch.resumePositionMs,
                resolvedResumeProgressFraction = launch.resumeProgressFraction,
                isAutoPlay = true,
            )
            StreamsRepository.consumeAutoPlay()
            return@LaunchedEffect
        }
        if (sourceUrl == null) {
            StreamsRepository.skipAutoPlayStream(selectedStream)
            return@LaunchedEffect
        }
        autoPlayHandled = true
        if (playerSettings.streamReuseLastLinkEnabled) {
            launch.cacheDirectLink(effectiveVideoId, stream, sourceUrl)
        }
        val playerLaunch = launch.directPlayerLaunch(
            videoId = effectiveVideoId,
            stream = stream,
            sourceUrl = sourceUrl,
            resumePositionMs = launch.resumePositionMs,
            resumeProgressFraction = launch.resumeProgressFraction,
        )
        if (playerSettings.externalPlayerEnabled) {
            openExternalPlayback(playerLaunch)
            StreamsRepository.consumeAutoPlay()
            StreamsRepository.cancelLoading()
            return@LaunchedEffect
        }
        StreamsRepository.consumeAutoPlay()
        StreamsRepository.cancelLoading()
        autoPlayNavigationStarted = true
        openPlayer(playerLaunch, replaceStreamRoute = true)
    }

    fun openSelectedStream(
        stream: StreamItem,
        resolvedResumePositionMs: Long?,
        resolvedResumeProgressFraction: Float?,
        forceExternal: Boolean,
        forceInternal: Boolean,
        recordAsActive: Boolean = true,
    ) {
        // Recorded before a debrid link is resolved: what the list shows is this stream, not the
        // URL it turns into, so that is the identity the list can recognise later.
        if (recordAsActive) ActiveStreamStore.set(videoId = effectiveVideoId, stream = stream)
        if (DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
            if (resolvingDebridStream) return
            streamRouteScope.launch {
                resolvingDebridStream = true
                val resolved = DirectDebridPlaybackResolver.resolveToPlayableStream(
                    stream = stream,
                    season = launch.seasonNumber,
                    episode = launch.episodeNumber,
                )
                resolvingDebridStream = false
                when (resolved) {
                    is DirectDebridPlayableResult.Success -> openSelectedStream(
                        stream = resolved.stream,
                        resolvedResumePositionMs = resolvedResumePositionMs,
                        resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                        forceExternal = forceExternal,
                        forceInternal = forceInternal,
                        recordAsActive = false,
                    )
                    else -> {
                        resolved.toastMessage()?.let { ToastController.show(it) }
                        if (resolved == DirectDebridPlayableResult.Stale) {
                            launch.reloadStreams(effectiveVideoId)
                        }
                    }
                }
            }
            return
        }
        if (stream.needsLocalDebridResolve && stream.p2pInfoHash != null) {
            requestOrOpenP2pStream(
                stream = stream,
                resolvedResumePositionMs = resolvedResumePositionMs,
                resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                isAutoPlay = false,
            )
            return
        }
        if (stream.shouldOpenExternally) {
            val opened = stream.externalOpenUrl?.let { url -> openExternalStreamUrl(url) } == true
            if (opened) {
                StreamsRepository.cancelLoading()
            }
            return
        }
        val sourceUrl = stream.playableDirectUrl ?: return
        if (playerSettings.streamReuseLastLinkEnabled) {
            launch.cacheDirectLink(effectiveVideoId, stream, sourceUrl)
        }
        val playerLaunch = launch.directPlayerLaunch(
            videoId = effectiveVideoId,
            stream = stream,
            sourceUrl = sourceUrl,
            resumePositionMs = resolvedResumePositionMs,
            resumeProgressFraction = resolvedResumeProgressFraction,
        )

        if (!forceInternal && (forceExternal || playerSettings.externalPlayerEnabled)) {
            streamRouteScope.launch {
                openExternalPlayback(playerLaunch)
                StreamsRepository.cancelLoading()
            }
            return
        }

        StreamsRepository.cancelLoading()
        openPlayer(playerLaunch, replaceStreamRoute = false)
    }

    LaunchedEffect(reuseNavigated) {
        if (reuseNavigated) {
            StreamsRepository.setOverlayVisible(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamsSheet(
            showLoadingScreen = showLoadingScreen,
            // Opening the sheet does not wait on the episode's own id being looked up: the sheet
            // comes up straight away and holds its list until there is something to ask for.
            preparing = !episodeVideoId.isResolved,
            type = launch.type,
            videoId = effectiveVideoId,
            parentMetaId = launch.parentMetaId ?: effectiveVideoId,
            parentMetaType = launch.parentMetaType ?: launch.type,
            title = launch.title,
            logo = launch.logo,
            poster = launch.poster,
            background = launch.background,
            seasonNumber = launch.seasonNumber,
            episodeNumber = launch.episodeNumber,
            episodeTitle = launch.episodeTitle,
            episodeThumbnail = launch.episodeThumbnail,
            resumePositionMs = launch.resumePositionMs,
            resumeProgressFraction = launch.resumeProgressFraction,
            manualSelection = launch.manualSelection,
            startFromBeginning = launch.startFromBeginning,
            onStreamSelected = { stream, resolvedResumePositionMs, resolvedResumeProgressFraction ->
                openSelectedStream(
                    stream = stream,
                    resolvedResumePositionMs = resolvedResumePositionMs,
                    resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                    forceExternal = false,
                    forceInternal = false,
                )
            },
            onStreamActionOpen = { stream, openExternally, resolvedResumePositionMs, resolvedResumeProgressFraction ->
                openSelectedStream(
                    stream = stream,
                    resolvedResumePositionMs = resolvedResumePositionMs,
                    resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                    forceExternal = openExternally,
                    forceInternal = !openExternally,
                )
            },
            onBack = onBack,
        )
        pendingP2pStreamOpen?.let { pending ->
            P2pConsentDialog(
                onEnableP2p = {
                    P2pSettingsRepository.setP2pEnabled(true)
                    pendingP2pStreamOpen = null
                    openP2pStream(
                        stream = pending.stream,
                        resolvedResumePositionMs = pending.resumePositionMs,
                        resolvedResumeProgressFraction = pending.resumeProgressFraction,
                        replaceStreamRoute = pending.isAutoPlay,
                    )
                },
                onDismiss = {
                    if (pending.isAutoPlay) {
                        StreamsRepository.skipAutoPlayStream(pending.stream)
                        StreamsRepository.consumeAutoPlay()
                    }
                    pendingP2pStreamOpen = null
                },
            )
        }
        if (showLoadingScreen) {
            StreamLoadingScreen(
                launch = launch,
                state = streamsUiState.takeIf { it.requestToken == expectedStreamsRequestToken } ?: StreamsUiState(),
                showStatus = playerSettings.showPlayerLoadingStatus,
                resolvingDebridStream = resolvingDebridStream,
                onBack = onBack,
            )
        }
    }
}
