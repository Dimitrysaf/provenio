package com.nuvio.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.cloud.CloudLibraryContentType
import com.nuvio.app.core.cloud.CloudLibraryFile
import com.nuvio.app.core.cloud.CloudLibraryItem
import com.nuvio.app.core.cloud.CloudLibraryPlaybackResult
import com.nuvio.app.core.cloud.CloudLibraryPlaybackTargetLookupResult
import com.nuvio.app.core.cloud.CloudLibraryRepository
import com.nuvio.app.core.cloud.playbackVideoId
import com.nuvio.app.core.cloud.providerPosterUrl
import com.nuvio.app.core.downloads.DownloadItem
import com.nuvio.app.core.downloads.DownloadSubtitles
import com.nuvio.app.core.downloads.DownloadsRepository
import com.nuvio.app.core.playback.ExternalPlayerIntentResult
import com.nuvio.app.core.playback.ExternalPlayerPlatform
import com.nuvio.app.core.playback.PlayerLaunch
import com.nuvio.app.core.playback.PlayerLaunchStore
import com.nuvio.app.core.playback.PlayerPlaybackSnapshot
import com.nuvio.app.core.playback.PlayerSettingsRepository
import com.nuvio.app.core.playback.PlayerSettingsUiState
import com.nuvio.app.core.playback.SubtitleLanguageOption
import com.nuvio.app.core.playback.prepareExternalPlayerLaunch
import com.nuvio.app.core.streams.BingeGroupCacheRepository
import com.nuvio.app.core.streams.StreamLaunch
import com.nuvio.app.core.streams.StreamLaunchStore
import com.nuvio.app.core.streams.StreamsRepository
import com.nuvio.app.core.tracking.TrackingScrobbleAction
import com.nuvio.app.core.tracking.TrackingScrobbleCoordinator
import com.nuvio.app.core.tracking.TrackingScrobbleEvent
import com.nuvio.app.core.tracking.buildTrackingMediaReference
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.WatchProgressPlaybackSession
import com.nuvio.app.core.watch.progress.WatchProgressRepository
import com.nuvio.app.core.watch.watching.domain.isShortPlaceholderDuration
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.nav.*
import com.nuvio.app.shell.screens.player.ExternalPlaybackResult
import com.nuvio.app.shell.screens.player.rememberExternalPlayerLauncher
import com.nuvio.app.shell.screens.streams.PlaybackAvailability
import com.nuvio.app.shell.screens.streams.rememberPlaybackAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// The texts playback shows in toasts, resolved once per composition.
internal data class AppPlaybackStrings(
    val playbackUnavailable: String,
    val downloadedProvider: String,
    val externalPlayerNotConfigured: String,
    val externalPlayerFailed: String,
    val failedOpenBrowser: String,
    val cloudPlayFailed: String,
    val cloudPlayDisabled: String,
    val cloudPlayNotConnected: String,
)

// Every way the app starts playback: downloads, cloud files, streams, external players and continue watching.
internal class AppPlayback(
    private val scope: CoroutineScope,
    private val navController: NuvioNavigator,
    private val profileId: Int,
    private val playerSettings: PlayerSettingsUiState,
    private val playbackAvailability: PlaybackAvailability,
    private val uriHandler: UriHandler,
    val strings: AppPlaybackStrings,
    val launchExternalPlayer: (ExternalPlayerIntentResult.Success) -> Boolean,
    val recordExternalLaunch: (PlayerLaunch) -> Unit,
) {
    suspend fun openExternalPlayback(launch: PlayerLaunch): Boolean {
        recordExternalLaunch(launch)

        val bingeGroup = launch.bingeGroup
        if (bingeGroup != null && launch.parentMetaId.isNotBlank()) {
            BingeGroupCacheRepository.save(launch.parentMetaId, bingeGroup)
        }

        val baseRequest = launch.toExternalPlayerPlaybackRequest()
        val shouldForwardSubtitles = playerSettings.externalPlayerForwardSubtitles &&
            !playerSettings.preferredSubtitleLanguage.equals(SubtitleLanguageOption.NONE, ignoreCase = true)
        val shouldSendSkipSegments = playerSettings.externalPlayerSendSkipSegments
        if (shouldForwardSubtitles) {
            StreamsRepository.setOverlayVisible(true, getString(Res.string.streams_loading_subtitles))
        } else if (shouldSendSkipSegments) {
            StreamsRepository.setOverlayVisible(true, getString(Res.string.streams_loading_skip_segments))
        }
        val enrichedRequest = prepareExternalPlayerLaunch(
            request = baseRequest,
            type = launch.contentType ?: launch.parentMetaType,
            videoId = launch.videoId ?: launch.parentMetaId,
            contentId = launch.parentMetaId,
            forwardSubtitles = playerSettings.externalPlayerForwardSubtitles,
            sendSkipSegments = shouldSendSkipSegments,
            preferredLanguage = playerSettings.preferredSubtitleLanguage,
            secondaryLanguage = playerSettings.secondaryPreferredSubtitleLanguage,
            onOverlayMessage = { message -> StreamsRepository.setOverlayVisible(true, message) },
        )
        StreamsRepository.setOverlayVisible(false)
        return when (
            val intentResult = ExternalPlayerPlatform.buildIntent(
                request = enrichedRequest,
                playerId = playerSettings.externalPlayerId,
            )
        ) {
            is ExternalPlayerIntentResult.Success -> {
                val launched = launchExternalPlayer(intentResult)
                if (!launched) {
                    NuvioToastController.show(strings.externalPlayerFailed)
                }
                launched
            }
            ExternalPlayerIntentResult.NotConfigured -> {
                NuvioToastController.show(strings.externalPlayerNotConfigured)
                false
            }
            ExternalPlayerIntentResult.Failed -> {
                NuvioToastController.show(strings.externalPlayerFailed)
                false
            }
        }
    }

    fun openExternalStreamUrl(url: String): Boolean {
        val opened = runCatching {
            uriHandler.openUri(url)
        }.isSuccess
        if (!opened) {
            NuvioToastController.show(strings.failedOpenBrowser)
        }
        return opened
    }

    fun openDownloadedItem(item: DownloadItem) {
        val sourceUrl = DownloadsRepository.playableLocalFileUri(item) ?: return
        val resumeEntry = item.videoId
            .takeIf { it.isNotBlank() }
            ?.let(WatchProgressRepository::progressForVideo)
            ?.takeIf { it.isResumable }

        playLocally(
            PlayerLaunch(
                profileId = profileId,
                title = item.title,
                sourceUrl = sourceUrl,
                sourceHeaders = emptyMap(),
                sourceResponseHeaders = emptyMap(),
                externalSubtitles = DownloadSubtitles.localSubtitles(sourceUrl),
                streamType = null,
                logo = item.logo,
                poster = item.poster,
                background = item.background,
                seasonNumber = item.seasonNumber,
                episodeNumber = item.episodeNumber,
                episodeTitle = item.episodeTitle,
                episodeThumbnail = item.episodeThumbnail,
                streamTitle = item.streamTitle,
                streamSubtitle = item.streamSubtitle,
                providerName = item.providerName,
                providerAddonId = item.providerAddonId,
                contentType = item.contentType,
                videoId = item.videoId,
                parentMetaId = item.parentMetaId,
                parentMetaType = item.parentMetaType,
                initialPositionMs = resumeEntry?.lastPositionMs?.takeIf { it > 0L } ?: 0L,
                initialProgressFraction = resumeEntry?.progressFraction?.takeIf { it > 0f },
            ),
        )
    }

    suspend fun launchCloudLibraryFile(
        item: CloudLibraryItem,
        file: CloudLibraryFile,
        resumePositionMs: Long? = null,
        resumeProgressFraction: Float? = null,
        startFromBeginning: Boolean = false,
    ): Boolean {
        val resolved = CloudLibraryRepository.resolvePlayback(item = item, file = file)
        if (resolved !is CloudLibraryPlaybackResult.Success) return false
        val playbackTitle = resolved.filename
            ?.takeIf { it.isNotBlank() }
            ?: file.name.ifBlank { item.name }
        val playerLaunch = PlayerLaunch(
            profileId = profileId,
            title = playbackTitle,
            sourceUrl = resolved.url,
            streamTitle = playbackTitle,
            streamSubtitle = item.name.takeIf { it != playbackTitle },
            providerName = item.providerName,
            providerAddonId = "cloud:${item.providerId}",
            poster = item.providerPosterUrl(),
            contentType = CloudLibraryContentType,
            videoId = item.playbackVideoId(file),
            parentMetaId = item.stableKey,
            parentMetaType = CloudLibraryContentType,
            initialPositionMs = if (startFromBeginning) 0L else (resumePositionMs ?: 0L),
            initialProgressFraction = if (startFromBeginning) null else resumeProgressFraction,
        )
        if (playerSettings.externalPlayerEnabled) {
            openExternalPlayback(playerLaunch)
        } else {
            openPlayer(playerLaunch)
        }
        return true
    }

    // Plays a downloaded copy when there is one, otherwise opens the sources for the title.
    fun play(
        type: String,
        videoId: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        episodeTitle: String?,
        episodeThumbnail: String?,
        pauseDescription: String?,
        resumePositionMs: Long?,
        resumeProgressFraction: Float?,
        manualSelection: Boolean,
        startFromBeginning: Boolean,
    ) {
        val targetResumePositionMs = if (startFromBeginning) 0L else (resumePositionMs ?: 0L)
        val targetResumeProgressFraction = if (startFromBeginning) null else resumeProgressFraction

        if (!manualSelection) {
            val downloadedItem = DownloadsRepository.findPlayableDownload(
                parentMetaId = parentMetaId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                videoId = videoId,
            )
            val localSourceUrl = downloadedItem?.let(DownloadsRepository::playableLocalFileUri)
            if (!localSourceUrl.isNullOrBlank()) {
                playLocally(
                    PlayerLaunch(
                        profileId = profileId,
                        title = title,
                        sourceUrl = localSourceUrl,
                        sourceHeaders = emptyMap(),
                        sourceResponseHeaders = emptyMap(),
                        externalSubtitles = DownloadSubtitles.localSubtitles(localSourceUrl),
                        logo = logo,
                        poster = poster,
                        background = background,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        episodeThumbnail = episodeThumbnail,
                        streamTitle = downloadedItem.streamTitle.ifBlank { title },
                        streamSubtitle = downloadedItem.streamSubtitle,
                        pauseDescription = pauseDescription,
                        providerName = downloadedItem.providerName.ifBlank { strings.downloadedProvider },
                        providerAddonId = downloadedItem.providerAddonId,
                        contentType = type,
                        videoId = videoId,
                        parentMetaId = parentMetaId,
                        parentMetaType = parentMetaType,
                        initialPositionMs = targetResumePositionMs,
                        initialProgressFraction = targetResumeProgressFraction,
                    ),
                )
                return
            }
        }

        if (!PlaybackAvailability.current().canStream(type, videoId)) {
            NuvioToastController.show(strings.playbackUnavailable)
            return
        }

        val streamLaunchId = StreamLaunchStore.put(
            StreamLaunch(
                profileId = profileId,
                type = type,
                videoId = videoId,
                parentMetaId = parentMetaId,
                parentMetaType = parentMetaType,
                title = title,
                logo = logo,
                poster = poster,
                background = background,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                episodeThumbnail = episodeThumbnail,
                pauseDescription = pauseDescription,
                resumePositionMs = if (startFromBeginning) 0L else resumePositionMs,
                resumeProgressFraction = targetResumeProgressFraction,
                manualSelection = manualSelection,
                startFromBeginning = startFromBeginning,
            ),
        )
        navController.navigate(StreamRoute(launchId = streamLaunchId, title = title))
    }

    val onPlay: ContentPlayAction = contentPlayAction(manualSelection = false)

    val onPlayManually: ContentPlayAction = contentPlayAction(manualSelection = true)

    fun canPlayContinueWatching(item: ContinueWatchingItem): Boolean =
        item.isCloudLibraryContinueWatchingItem() || playbackAvailability.canPlay(
            type = item.parentMetaType,
            videoId = item.videoId,
            parentMetaId = item.parentMetaId,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
        )

    fun canSelectContinueWatchingStreams(item: ContinueWatchingItem): Boolean =
        !item.isCloudLibraryContinueWatchingItem() &&
            playbackAvailability.canStream(item.parentMetaType, item.videoId)

    fun openContinueWatching(
        item: ContinueWatchingItem,
        manualSelection: Boolean = false,
        startFromBeginning: Boolean = false,
    ) {
        if (item.isCloudLibraryContinueWatchingItem()) {
            scope.launch { openCloudContinueWatching(item, startFromBeginning) }
            return
        }
        play(
            type = item.parentMetaType,
            videoId = item.videoId,
            parentMetaId = item.parentMetaId,
            parentMetaType = item.parentMetaType,
            title = item.title,
            logo = item.logo,
            poster = item.poster,
            background = item.background,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
            episodeTitle = item.episodeTitle,
            episodeThumbnail = item.episodeThumbnail,
            pauseDescription = item.pauseDescription,
            resumePositionMs = item.resumePositionMs,
            resumeProgressFraction = item.resumeProgressFraction,
            manualSelection = manualSelection,
            startFromBeginning = startFromBeginning,
        )
    }

    private suspend fun openCloudContinueWatching(item: ContinueWatchingItem, startFromBeginning: Boolean) {
        when (
            val lookup = CloudLibraryRepository.findPlaybackTargetForProgressResult(
                contentId = item.parentMetaId,
                videoId = item.videoId,
            )
        ) {
            is CloudLibraryPlaybackTargetLookupResult.Found -> {
                val launched = launchCloudLibraryFile(
                    item = lookup.target.item,
                    file = lookup.target.file,
                    resumePositionMs = item.resumePositionMs,
                    resumeProgressFraction = item.resumeProgressFraction,
                    startFromBeginning = startFromBeginning,
                )
                if (!launched) {
                    NuvioToastController.show(strings.cloudPlayFailed)
                }
            }

            CloudLibraryPlaybackTargetLookupResult.Disabled -> {
                NuvioToastController.show(strings.cloudPlayDisabled)
            }

            is CloudLibraryPlaybackTargetLookupResult.NotConnected -> {
                val providerName = lookup.providerName?.takeIf { it.isNotBlank() }
                NuvioToastController.show(
                    providerName?.let { name ->
                        getString(Res.string.cloud_library_play_provider_not_connected, name)
                    } ?: strings.cloudPlayNotConnected,
                )
            }

            CloudLibraryPlaybackTargetLookupResult.NotFound -> {
                NuvioToastController.show(strings.cloudPlayFailed)
            }
        }
    }

    // Local files go to the external player when it is preferred, otherwise to the built in one.
    private fun playLocally(playerLaunch: PlayerLaunch) {
        if (playerSettings.externalPlayerEnabled) {
            scope.launch { openExternalPlayback(playerLaunch) }
        } else {
            openPlayer(playerLaunch)
        }
    }

    private fun openPlayer(playerLaunch: PlayerLaunch) {
        val launchId = PlayerLaunchStore.put(playerLaunch)
        navController.navigate(PlayerRoute(launchId = launchId, title = playerLaunch.title))
    }

    private fun contentPlayAction(manualSelection: Boolean): ContentPlayAction =
        { type, videoId, parentMetaId, parentMetaType, title, logo, poster, background, seasonNumber, episodeNumber, episodeTitle, episodeThumbnail, pauseDescription, resumePositionMs ->
            play(
                type = type,
                videoId = videoId,
                parentMetaId = parentMetaId,
                parentMetaType = parentMetaType,
                title = title,
                logo = logo,
                poster = poster,
                background = background,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                episodeTitle = episodeTitle,
                episodeThumbnail = episodeThumbnail,
                pauseDescription = pauseDescription,
                resumePositionMs = resumePositionMs,
                resumeProgressFraction = null,
                manualSelection = manualSelection,
                startFromBeginning = false,
            )
        }
}

@Composable
internal fun rememberAppPlayback(navController: NuvioNavigator, profileId: Int): AppPlayback {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val playbackAvailability = rememberPlaybackAvailability()
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    var lastExternalPlayerLaunch by remember { mutableStateOf<PlayerLaunch?>(null) }
    val launchExternalPlayer = rememberExternalPlayerLauncher { result ->
        val playerLaunch = lastExternalPlayerLaunch
        if (result != null && result.positionMs > 0L) {
            scope.launch { recordExternalPlaybackResult(result, playerLaunch) }
        }
    }
    val strings = AppPlaybackStrings(
        playbackUnavailable = stringResource(Res.string.playback_unavailable_message),
        downloadedProvider = stringResource(Res.string.provider_downloaded),
        externalPlayerNotConfigured = stringResource(Res.string.external_player_not_configured),
        externalPlayerFailed = stringResource(Res.string.external_player_failed),
        failedOpenBrowser = stringResource(Res.string.settings_trakt_failed_open_browser),
        cloudPlayFailed = stringResource(Res.string.cloud_library_play_failed),
        cloudPlayDisabled = stringResource(Res.string.cloud_library_play_disabled),
        cloudPlayNotConnected = stringResource(Res.string.cloud_library_play_not_connected),
    )
    return AppPlayback(
        scope = scope,
        navController = navController,
        profileId = profileId,
        playerSettings = playerSettings,
        playbackAvailability = playbackAvailability,
        uriHandler = uriHandler,
        strings = strings,
        launchExternalPlayer = launchExternalPlayer,
        recordExternalLaunch = { launch -> lastExternalPlayerLaunch = launch },
    )
}

// Saves progress and stops the scrobble for what an external player reports when it returns.
private suspend fun recordExternalPlaybackResult(result: ExternalPlaybackResult, playerLaunch: PlayerLaunch?) {
    val durationMs = result.durationMs
    // Guard: debrid cache-sync placeholders and error clips report a short
    // duration reaching completion. Skip scrobble + progress for those.
    if (durationMs != null && isShortPlaceholderDuration(durationMs)) return
    val progressPercent = if (durationMs != null && durationMs > 0L) {
        (result.positionMs.toFloat() / durationMs.toFloat() * 100f).coerceIn(0f, 100f)
    } else {
        null
    }
    if (progressPercent != null && playerLaunch != null) {
        val trackingMedia = buildTrackingMediaReference(
            contentType = playerLaunch.parentMetaType,
            parentMetaId = playerLaunch.parentMetaId,
            videoId = playerLaunch.videoId,
            title = playerLaunch.title,
            seasonNumber = playerLaunch.seasonNumber,
            episodeNumber = playerLaunch.episodeNumber,
            episodeTitle = playerLaunch.episodeTitle,
        )
        if (trackingMedia.hasResolvableIdentity) {
            runCatching {
                TrackingScrobbleCoordinator.scrobble(
                    profileId = playerLaunch.profileId,
                    action = TrackingScrobbleAction.STOP,
                    event = TrackingScrobbleEvent(
                        media = trackingMedia,
                        progressPercent = progressPercent.toDouble(),
                    ),
                )
            }
        }
    }
    playerLaunch ?: return
    val session = WatchProgressPlaybackSession(
        profileId = playerLaunch.profileId,
        contentType = playerLaunch.contentType ?: playerLaunch.parentMetaType,
        parentMetaId = playerLaunch.parentMetaId,
        parentMetaType = playerLaunch.parentMetaType,
        videoId = playerLaunch.videoId ?: playerLaunch.parentMetaId,
        title = playerLaunch.title,
        logo = playerLaunch.logo,
        poster = playerLaunch.poster,
        background = playerLaunch.background,
        seasonNumber = playerLaunch.seasonNumber,
        episodeNumber = playerLaunch.episodeNumber,
        episodeTitle = playerLaunch.episodeTitle,
        episodeThumbnail = playerLaunch.episodeThumbnail,
        providerName = playerLaunch.providerName,
        providerAddonId = playerLaunch.providerAddonId,
        lastStreamTitle = playerLaunch.streamTitle,
        lastSourceUrl = playerLaunch.sourceUrl,
    )
    val snapshot = PlayerPlaybackSnapshot(
        isLoading = false,
        isPlaying = false,
        isEnded = !result.endedByUser,
        durationMs = durationMs ?: 0L,
        positionMs = result.positionMs,
    )
    WatchProgressRepository.upsertPlaybackProgress(session = session, snapshot = snapshot)
}
