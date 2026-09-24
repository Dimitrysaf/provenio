package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedUiState
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressUiState
import io.github.dimitrysaf.provenio.core.tracking.WatchProgressSource
import io.github.dimitrysaf.provenio.core.tracking.trakt.MoreLikeThisSourcePreference
import io.github.dimitrysaf.provenio.core.library.LibraryUiState
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import io.github.dimitrysaf.provenio.shell.screens.library.rememberLibraryListPickerState
import io.github.dimitrysaf.provenio.shell.screens.library.LibraryListPickerHost
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.build.TrailerPlaybackMode
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.shell.components.BackButton
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailFloatingHeader
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailHero
import io.github.dimitrysaf.provenio.core.home.HomeRepository
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.shell.screens.library.PendingTrackingMembershipRemoval
import io.github.dimitrysaf.provenio.shell.screens.library.TrackingMembershipRemovalConfirmationHost
import io.github.dimitrysaf.provenio.core.library.toLibraryItem
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.shell.screens.streams.rememberPlaybackAvailability
import io.github.dimitrysaf.provenio.core.streams.StreamAutoPlayPolicy
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsSettings
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktConnectionMode
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import io.github.dimitrysaf.provenio.core.watch.watched.watchedItemKey
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.progress.buildPlaybackVideoId
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingActions
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.metadata.HeroTrailerAudioState
import io.github.dimitrysaf.provenio.core.metadata.MetaCompany
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaPerson
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenBackgroundMode
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsUiState
import io.github.dimitrysaf.provenio.core.metadata.MetaTrailer
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.metadata.buildDetailHeroSlides
import io.github.dimitrysaf.provenio.core.metadata.detailSidePaneSection
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository

private val watchedMarkerDiagnosticLog = Logger.withTag("WatchedMarkerDiag")

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun MetaDetailsScreen(
    type: String,
    id: String,
    onBack: () -> Unit,
    onPlay: ((type: String, videoId: String, parentMetaId: String, parentMetaType: String, title: String, logo: String?, poster: String?, background: String?, seasonNumber: Int?, episodeNumber: Int?, episodeTitle: String?, episodeThumbnail: String?, pauseDescription: String?, resumePositionMs: Long?) -> Unit)? = null,
    onPlayManually: ((type: String, videoId: String, parentMetaId: String, parentMetaType: String, title: String, logo: String?, poster: String?, background: String?, seasonNumber: Int?, episodeNumber: Int?, episodeTitle: String?, episodeThumbnail: String?, pauseDescription: String?, resumePositionMs: Long?) -> Unit)? = null,
    onOpenMeta: ((MetaPreview) -> Unit)? = null,
    onCastClick: ((MetaPerson, String?) -> Unit)? = null,
    onCompanyClick: ((MetaCompany, String) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by MetaDetailsRepository.uiState.collectAsStateWithLifecycle()
    val homeSections = HomeRepository.uiState.collectAsStateWithLifecycle().value.sections
    val displayedMeta = uiState.meta?.takeIf { it.type == type && it.id == id }
        ?: MetaDetailsRepository.peek(type, id)
    val metaScreenSettingsUiState by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val traktAuthUiState by remember {
        TraktAuthRepository.ensureLoaded()
        TraktAuthRepository.uiState
    }.collectAsStateWithLifecycle()
    val trackingSettingsUiState by remember {
        TrackingSettingsRepository.ensureLoaded()
        TrackingSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val libraryUiState by remember {
        LibraryRepository.ensureLoaded()
        LibraryRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val watchProgressUiState by remember {
        WatchProgressRepository.ensureLoaded()
        WatchProgressRepository.uiState
    }.collectAsStateWithLifecycle()
    val progressByVideoId = remember(watchProgressUiState.entries, id) {
        watchProgressUiState.byVideoIdForContent(id)
    }
    val playerSettingsUiState by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val commentsEnabled by remember {
        TraktCommentsSettings.ensureLoaded()
        TraktCommentsSettings.enabled
    }.collectAsStateWithLifecycle()
    val comments = remember(type, id) { DetailCommentsState() }
    var deferredMetaWorkAllowed by remember(type, id) { mutableStateOf(false) }

    WatchedMarkerDiagnosticsEffect(
        meta = displayedMeta,
        watchedUiState = watchedUiState,
        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
        watchProgressUiState = watchProgressUiState,
        progressByVideoId = progressByVideoId,
        watchProgressSource = trackingSettingsUiState.watchProgressSource,
    )

    val shouldShowComments = commentsEnabled &&
        traktAuthUiState.mode == TraktConnectionMode.CONNECTED &&
        displayedMeta != null &&
        displayedMeta.type.lowercase().let { it == "movie" || it == "series" || it == "show" || it == "tv" }

    LaunchedEffect(displayedMeta?.id) {
        deferredMetaWorkAllowed = false
        if (displayedMeta != null) {
            delay(250)
            deferredMetaWorkAllowed = true
        }
    }

    LaunchedEffect(displayedMeta?.id, shouldShowComments, deferredMetaWorkAllowed) {
        if (displayedMeta == null || !shouldShowComments) {
            comments.clear()
            return@LaunchedEffect
        }
        if (!deferredMetaWorkAllowed) return@LaunchedEffect
        comments.loadFirstPage(displayedMeta)
    }

    MetaDetailsLoadEffects(
        type = type,
        id = id,
        displayedMeta = displayedMeta,
        isLoading = uiState.isLoading,
        networkCondition = networkStatusUiState.condition,
        moreLikeThisSource = trackingSettingsUiState.moreLikeThisSource,
        traktMode = traktAuthUiState.mode,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            displayedMeta == null && uiState.isLoading -> {
                LoadingSpinner(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            displayedMeta == null && uiState.errorMessage != null -> {
                DetailsLoadError(
                    condition = networkStatusUiState.condition,
                    errorMessage = uiState.errorMessage.orEmpty(),
                    onRetry = {
                        NetworkStatusRepository.requestRefresh(force = true)
                        MetaDetailsRepository.load(type, id)
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            displayedMeta != null -> {
                MetaDetailsContent(
                    meta = displayedMeta,
                    watchedUiState = watchedUiState,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    watchProgressUiState = watchProgressUiState,
                    progressByVideoId = progressByVideoId,
                    libraryUiState = libraryUiState,
                    metaScreenSettingsUiState = metaScreenSettingsUiState,
                    homeSections = homeSections,
                    playerSettingsUiState = playerSettingsUiState,
                    comments = comments,
                    shouldShowComments = shouldShowComments,
                    deferredMetaWorkAllowed = deferredMetaWorkAllowed,
                    onBack = onBack,
                    onPlay = onPlay,
                    onPlayManually = onPlayManually,
                    onOpenMeta = onOpenMeta,
                    onCastClick = onCastClick,
                    onCompanyClick = onCompanyClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }

        if (displayedMeta == null) {
            BackButton(
                onClick = onBack,
                modifier = Modifier.padding(
                    start = 12.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                ),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// Logs how watched markers line up with this title, to diagnose markers that fail to show.
@Composable
private fun WatchedMarkerDiagnosticsEffect(
    meta: MetaDetails?,
    watchedUiState: WatchedUiState,
    fullyWatchedSeriesKeys: Set<String>,
    watchProgressUiState: WatchProgressUiState,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchProgressSource: WatchProgressSource,
) {
    LaunchedEffect(
        meta?.id,
        meta?.type,
        meta?.name,
        meta?.videos,
        watchedUiState.items,
        watchedUiState.isLoaded,
        watchedUiState.hasLoadedRemoteItems,
        fullyWatchedSeriesKeys,
        watchProgressUiState.entries,
        watchProgressSource,
    ) {
        meta ?: return@LaunchedEffect
        val posterKey = watchedItemKey(meta.type, meta.id)
        val expectedEpisodeKeys = meta.videos.map { episode ->
            watchedItemKey(meta.type, meta.id, episode.season, episode.episode)
        }
        val matchedEpisodeKeys = expectedEpisodeKeys.filter(watchedUiState.watchedKeys::contains)
        val completedProgressMatches = meta.videos.count { episode ->
            val videoId = buildPlaybackVideoId(
                parentMetaId = meta.id,
                seasonNumber = episode.season,
                episodeNumber = episode.episode,
                fallbackVideoId = episode.id,
            )
            progressByVideoId[videoId]?.isEffectivelyCompleted == true
        }
        val directItemKeys = watchedUiState.items
            .asSequence()
            .filter { item -> item.id == meta.id }
            .take(10)
            .joinToString(separator = ",") { item ->
                watchedItemKey(item.type, item.id, item.season, item.episode)
            }
        val titleCandidateKeys = watchedUiState.items
            .asSequence()
            .filter { item -> item.name.equals(meta.name, ignoreCase = true) }
            .take(10)
            .joinToString(separator = ",") { item ->
                watchedItemKey(item.type, item.id, item.season, item.episode)
            }
        watchedMarkerDiagnosticLog.i {
            "marker state requestedSource=$watchProgressSource " +
                "content=${meta.type}:${meta.id} repositoryLoaded=${watchedUiState.isLoaded} " +
                "remoteLoaded=${watchedUiState.hasLoadedRemoteItems} repositoryItems=${watchedUiState.items.size} " +
                "posterKey=$posterKey posterInWatched=${posterKey in watchedUiState.watchedKeys} " +
                "posterInFullyWatched=${posterKey in fullyWatchedSeriesKeys} videos=${meta.videos.size} " +
                "episodeMarkerMatches=${matchedEpisodeKeys.size} completedProgressMatches=$completedProgressMatches " +
                "directItemKeys=[$directItemKeys] titleCandidateKeys=[$titleCandidateKeys] " +
                "expectedEpisodeKeys=[${expectedEpisodeKeys.take(10).joinToString(",")}] " +
                "repositoryKeySample=[${watchedUiState.watchedKeys.take(10).joinToString(",")}]"
        }
    }
}

// Loads the title on first show, reloads it when a source setting changes, and retries after going back online.
@Composable
private fun MetaDetailsLoadEffects(
    type: String,
    id: String,
    displayedMeta: MetaDetails?,
    isLoading: Boolean,
    networkCondition: NetworkCondition,
    moreLikeThisSource: MoreLikeThisSourcePreference,
    traktMode: TraktConnectionMode,
) {
    var autoLoadAttempted by remember(type, id) { mutableStateOf(false) }
    var observedOfflineState by remember(type, id) { mutableStateOf(false) }
    val tmdbSettingsUiState by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    LaunchedEffect(type, id, displayedMeta, isLoading, autoLoadAttempted) {
        if (!autoLoadAttempted && displayedMeta == null && !isLoading) {
            autoLoadAttempted = true
            MetaDetailsRepository.load(type, id)
        }
    }

    LaunchedEffect(
        type,
        id,
        displayedMeta?.id,
        isLoading,
        moreLikeThisSource,
        traktMode,
        tmdbSettingsUiState.enabled,
        tmdbSettingsUiState.useMoreLikeThis,
        tmdbSettingsUiState.language,
    ) {
        if (displayedMeta != null && !isLoading) {
            MetaDetailsRepository.load(type, id)
        }
    }

    LaunchedEffect(networkCondition, displayedMeta, isLoading, type, id) {
        when (networkCondition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@LaunchedEffect
                observedOfflineState = false
                if (displayedMeta == null && !isLoading) {
                    MetaDetailsRepository.load(type, id)
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }
}

@Composable
private fun DetailsLoadError(
    condition: NetworkCondition,
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.details_failed_to_load),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = when (condition) {
                NetworkCondition.NoInternet -> stringResource(Res.string.details_check_connection)
                NetworkCondition.ServersUnreachable -> stringResource(Res.string.details_servers_unreachable)
                else -> errorMessage
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.action_retry))
        }
    }
}

// Everything shown once the title has loaded: the hero, the sections, and their sheets.
@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun MetaDetailsContent(
    meta: MetaDetails,
    watchedUiState: WatchedUiState,
    fullyWatchedSeriesKeys: Set<String>,
    watchProgressUiState: WatchProgressUiState,
    progressByVideoId: Map<String, WatchProgressEntry>,
    libraryUiState: LibraryUiState,
    metaScreenSettingsUiState: MetaScreenSettingsUiState,
    homeSections: List<HomeCatalogSection>,
    playerSettingsUiState: PlayerSettingsUiState,
    comments: DetailCommentsState,
    shouldShowComments: Boolean,
    deferredMetaWorkAllowed: Boolean,
    onBack: () -> Unit,
    onPlay: DetailPlayHandler?,
    onPlayManually: DetailPlayHandler?,
    onOpenMeta: ((MetaPreview) -> Unit)?,
    onCastClick: ((MetaPerson, String?) -> Unit)?,
    onCompanyClick: ((MetaCompany, String) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val playbackAvailability = rememberPlaybackAvailability()
    val detailsScope = rememberCoroutineScope()
    val libraryListPicker = rememberLibraryListPickerState()
    var pendingTrackingRemoval by remember(meta.id) {
        mutableStateOf<PendingTrackingMembershipRemoval?>(null)
    }
    var selectedEpisodeForActions by remember(meta.id) { mutableStateOf<MetaVideo?>(null) }
    var selectedSeasonForActions by remember(meta.id) { mutableStateOf<Int?>(null) }
    val metaPreview = remember(meta) { meta.toMetaPreview() }
    val todayIsoDate = CurrentDateProvider.todayIsoDate()
    val isSaved = remember(
        libraryUiState.items,
        libraryUiState.sections,
        libraryUiState.sourceMode,
        meta.id,
        meta.type,
    ) {
        LibraryRepository.isSaved(meta.id, meta.type)
    }
    val isWatched = remember(watchedUiState.watchedKeys, fullyWatchedSeriesKeys, metaPreview) {
        WatchingState.isPosterWatched(
            watchedKeys = watchedUiState.watchedKeys,
            item = metaPreview,
            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
        )
    }
    val openLibraryListPicker: () -> Unit = {
        libraryListPicker.open(meta.toLibraryItem(savedAtEpochMs = 0L), meta.name)
    }
    val toggleSaved = rememberToggleSaved(meta, detailsScope) { pendingTrackingRemoval = it }
    val toggleWatched = remember(metaPreview) {
        {
            detailsScope.launch {
                WatchingActions.togglePosterWatched(metaPreview)
            }
            Unit
        }
    }
    LaunchedEffect(meta.id, meta.type, watchProgressUiState.hasLoadedRemoteProgress) {
        if (meta.type.lowercase() in setOf("series", "show", "tv", "tvshow")) {
            WatchProgressRepository.refreshEpisodeProgress(meta.id)
        }
    }
    LaunchedEffect(
        meta.id,
        meta.type,
        todayIsoDate,
        watchedUiState.isLoaded,
        watchProgressUiState.hasLoadedRemoteProgress,
        watchedUiState.watchedKeys,
        watchProgressUiState.entries,
    ) {
        if (watchedUiState.isLoaded && watchProgressUiState.hasLoadedRemoteProgress) {
            WatchingActions.reconcileSeriesWatchedState(
                meta = meta,
                todayIsoDate = todayIsoDate,
            )
        }
    }
    val primaryPlay = rememberDetailPrimaryPlay(meta, watchProgressUiState, watchedUiState, progressByVideoId, todayIsoDate)
    val seriesAction = primaryPlay.seriesAction
    val seriesStreamVideoId = primaryPlay.seriesStreamVideoId
    val seriesPauseDescription = primaryPlay.seriesPauseDescription
    val movieProgress = primaryPlay.movieProgress
    val hasEpisodes = meta.videos.any { it.season != null || it.episode != null }
    val episodeList = rememberDetailEpisodeList(meta, progressByVideoId, watchedUiState.watchedKeys, todayIsoDate)
    val sectionContent = rememberDetailSectionContent(meta, homeSections)
    val hasProductionSection = sectionContent.hasProduction
    val hasAdditionalInfoSection = sectionContent.hasAdditionalInfo
    val hasCollectionSection = sectionContent.hasCollection
    val hasTrailersSection = sectionContent.hasTrailers
    val moreLikeThisItems = sectionContent.moreLikeThisItems
    val inAppTrailerPlaybackEnabled = AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
    var isLeavingDetails by remember(meta.id) { mutableStateOf(false) }
    val heroTrailerPlaybackEnabled = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        inAppTrailerPlaybackEnabled &&
        metaScreenSettingsUiState.heroTrailerPlayback
    val heroSlides = remember(meta, heroTrailerPlaybackEnabled) {
        buildDetailHeroSlides(meta, includeTrailers = heroTrailerPlaybackEnabled)
    }
    val heroTrailerMuted by HeroTrailerAudioState.muted.collectAsStateWithLifecycle()
    val onBackFromDetails: () -> Unit = {
        isLeavingDetails = true
        onBack()
    }
    // Trailers will open in the main player; until then a tap has nowhere to go.
    val playTrailer: (MetaTrailer) -> Unit = {}
    val primaryVideoId = seriesStreamVideoId ?: seriesAction?.videoId ?: meta.id
    val isPrimaryPlayEnabled = playbackAvailability.canPlay(
        type = meta.type,
        videoId = primaryVideoId,
        parentMetaId = meta.id,
        seasonNumber = seriesAction?.seasonNumber,
        episodeNumber = seriesAction?.episodeNumber,
    )
    val playText = stringResource(Res.string.action_play)
    val resumeText = stringResource(Res.string.action_resume)
    val playButtonLabel = remember(movieProgress, seriesAction, meta.type, hasEpisodes, playText, resumeText) {
        when {
            (meta.type == "series" || hasEpisodes) && seriesAction != null ->
                seriesAction.label
            meta.type != "series" && !hasEpisodes && movieProgress != null ->
                resumeText
            else -> playText
        }
    }
    val playPrimaryWith: (DetailPlayHandler) -> Unit = { handler ->
        meta.playPrimary(
            handler = handler,
            seriesAction = seriesAction,
            seriesStreamVideoId = seriesStreamVideoId,
            seriesPauseDescription = seriesPauseDescription,
            movieResumePositionMs = movieProgress?.lastPositionMs,
            hasEpisodes = hasEpisodes,
        )
    }
    val onPrimaryPlayClick: () -> Unit = { onPlay?.let(playPrimaryWith) }
    val showManualPlayOption = onPlayManually != null && StreamAutoPlayPolicy.isEffectivelyEnabled(playerSettingsUiState)
    val onPrimaryPlayLongClick: (() -> Unit)? = onPlayManually
        ?.takeIf { showManualPlayOption && playbackAvailability.canStream(meta.type, primaryVideoId) }
        ?.let { manualPlay -> { playPrimaryWith(manualPlay) } }
    val onEpisodePlayClick: (MetaVideo) -> Unit = { video ->
        onPlay?.let { meta.playEpisode(it, video, watchProgressUiState) }
    }
    val onEpisodeManualPlayClick: (MetaVideo) -> Unit = { video ->
        onPlayManually?.let { meta.playEpisode(it, video, watchProgressUiState) }
    }
    val loadMoreComments: () -> Unit = {
        detailsScope.launch { comments.loadNextPage(meta) }
    }
    val scroll = rememberDetailScrollState(meta.id)
    val listState = scroll.listState
    val heroHeightPx = scroll.heroHeightPx
    val detailScrollOffsetPx = scroll.scrollOffsetPx
    val isHeroCollapsed = scroll.isHeroCollapsed

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val colorScheme = MaterialTheme.colorScheme
        val isTablet = maxWidth >= 720.dp
        val viewportHeight = maxHeight
        val contentHorizontalPadding = if (isTablet) 32.dp else 18.dp
        val contentMaxWidth = detailTabletContentMaxWidth(maxWidth, isTablet)
        val sidePaneSection = if (maxWidth >= DetailTwoPaneMinWidth) {
            detailSidePaneSection(
                metaScreenSettingsUiState.items
                    .filter { item ->
                        item.enabled && metaSectionHasContent(
                            key = item.key,
                            meta = meta,
                            hasProductionSection = hasProductionSection,
                            hasTrailersSection = hasTrailersSection,
                            hasEpisodes = hasEpisodes,
                            hasAdditionalInfoSection = hasAdditionalInfoSection,
                            hasCollectionSection = hasCollectionSection,
                            moreLikeThisItems = moreLikeThisItems,
                            shouldShowComments = shouldShowComments,
                            comments = comments.items,
                            isCommentsLoading = comments.isLoading,
                            commentsError = comments.error,
                        )
                    }
                    .map { it.key },
            )
        } else {
            null
        }
        val primaryPaneWeight = if (sidePaneSection != null) DetailPrimaryPaneWeight else 1f
        val sectionHorizontalPadding = if (sidePaneSection != null) 24.dp else contentHorizontalPadding
        val primaryPaneSettings = remember(metaScreenSettingsUiState, sidePaneSection) {
            metaScreenSettingsUiState.copy(
                items = metaScreenSettingsUiState.items.filterNot { it.key == sidePaneSection },
            )
        }
        val sidePaneSettings = remember(metaScreenSettingsUiState, sidePaneSection) {
            sidePaneSection?.let { key ->
                metaScreenSettingsUiState.copy(
                    items = metaScreenSettingsUiState.items
                        .filter { it.key == key }
                        .map { it.copy(tabGroup = null) },
                    tabLayout = false,
                )
            }
        }
        val detailSectionItems: LazyListScope.(MetaScreenSettingsUiState, Dp) -> Unit =
            { sectionSettings, sectionMaxWidth ->
                configuredMetaSectionItems(
                    settings = sectionSettings,
                    meta = meta,
                    isTablet = isTablet,
                    contentHorizontalPadding = sectionHorizontalPadding,
                    contentMaxWidth = sectionMaxWidth,
                    playButtonLabel = playButtonLabel,
                    isPrimaryPlayEnabled = isPrimaryPlayEnabled,
                    isSaved = isSaved,
                    isWatched = isWatched,
                    onPrimaryPlayClick = onPrimaryPlayClick,
                    onPrimaryPlayLongClick = onPrimaryPlayLongClick,
                    onSaveClick = toggleSaved,
                    onSaveLongClick = openLibraryListPicker,
                    onWatchedClick = toggleWatched,
                    showManualPlayOption = showManualPlayOption,
                    hasProductionSection = hasProductionSection,
                    hasTrailersSection = hasTrailersSection,
                    hasEpisodes = hasEpisodes,
                    hasAdditionalInfoSection = hasAdditionalInfoSection,
                    hasCollectionSection = hasCollectionSection,
                    moreLikeThisItems = moreLikeThisItems,
                    shouldShowComments = shouldShowComments,
                    comments = comments.items,
                    isCommentsLoading = comments.isLoading,
                    isCommentsLoadingMore = comments.isLoadingMore,
                    commentsCurrentPage = comments.currentPage,
                    commentsPageCount = comments.pageCount,
                    commentsError = comments.error,
                    episodeListEntries = episodeList.entries,
                    todayIsoDate = todayIsoDate,
                    onEpisodeSeasonToggle = episodeList.onSeasonToggle,
                    onRetryComments = {
                        detailsScope.launch { comments.loadFirstPage(meta, forceRefresh = true) }
                    },
                    onLoadMoreComments = loadMoreComments,
                    onCommentClick = { review -> comments.selected = review },
                    onTrailerClick = playTrailer,
                    progressByVideoId = progressByVideoId,
                    watchedKeys = watchedUiState.watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    blurUnwatchedEpisodes = metaScreenSettingsUiState.blurUnwatchedEpisodes,
                    onEpisodeClick = onEpisodePlayClick,
                    onEpisodeLongPress = { video -> selectedEpisodeForActions = video },
                    onSeasonLongPress = { season -> selectedSeasonForActions = season },
                    onOpenMeta = onOpenMeta,
                    onCastClick = onCastClick,
                    onCompanyClick = onCompanyClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        val backdropUrl = meta.background ?: meta.poster
        val backgroundMode = metaScreenSettingsUiState.backgroundMode
        val dominantColorEnabled = backgroundMode == MetaScreenBackgroundMode.DominantColor &&
            deferredMetaWorkAllowed &&
            !backdropUrl.isNullOrBlank()
        var dominantBackdropPainter by remember(meta.id, backdropUrl) {
            mutableStateOf<Painter?>(null)
        }
        var dominantBackdropImageBitmap by remember(meta.id, backdropUrl) {
            mutableStateOf<ImageBitmap?>(null)
        }
        val dominantBackdropColor = rememberDominantBackdropColor(
            enabled = dominantColorEnabled,
            imageBitmap = dominantBackdropImageBitmap,
            painter = dominantBackdropPainter,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().detailsContentReveal(metaScreenSettingsUiState.posterTransitionEnabled)) {
                DetailBackdrop(
                    mode = backgroundMode,
                    backdropUrl = backdropUrl,
                    visible = deferredMetaWorkAllowed,
                    dominantColor = dominantBackdropColor,
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(primaryPaneWeight)
                            .fillMaxHeight(),
                    ) {
                        item(key = "detail-hero") {
                            DetailHero(
                                meta = meta,
                                slides = heroSlides,
                                viewportHeight = viewportHeight,
                                onHeightChanged = { heroHeightPx.intValue = it },
                                trailerResolutionEnabled = heroTrailerPlaybackEnabled &&
                                    deferredMetaWorkAllowed &&
                                    !isLeavingDetails,
                                trailerPlayWhenReady = {
                                    !isLeavingDetails && !isHeroCollapsed.value
                                },
                                trailerMuted = heroTrailerMuted,
                                onTrailerMuteToggle = {
                                    HeroTrailerAudioState.toggleMuted()
                                },
                                onBackdropLoaded = { painter, imageBitmap ->
                                    dominantBackdropPainter = painter
                                    dominantBackdropImageBitmap = imageBitmap
                                },
                            )
                        }

                        detailSectionItems(
                            primaryPaneSettings,
                            if (isTablet && sidePaneSection == null) contentMaxWidth else Dp.Unspecified,
                        )

                        item(key = "detail-bottom-spacer") {
                            Spacer(modifier = Modifier.height(safeBottomPadding(32.dp)))
                        }
                    }

                    if (sidePaneSettings != null) {
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier
                                .weight(1f - primaryPaneWeight)
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                                    TopAppBarDefaults.TopAppBarExpandedHeight,
                            ),
                        ) {
                            detailSectionItems(sidePaneSettings, Dp.Unspecified)

                            item(key = "detail-side-bottom-spacer") {
                                Spacer(modifier = Modifier.height(safeBottomPadding(32.dp)))
                            }
                        }
                    }
                }

                if (backgroundMode.usesBackdropBackground && deferredMetaWorkAllowed && heroHeightPx.intValue > 0) {
                    DetailHeroFade(
                        color = dominantBackdropColor.takeIf { dominantColorEnabled } ?: colorScheme.background,
                        widthFraction = primaryPaneWeight,
                        heroHeightPx = heroHeightPx,
                        scrollOffsetPx = detailScrollOffsetPx,
                    )
                }
            }

            DetailHeaderOverlay(
                meta = meta,
                isHeroCollapsed = isHeroCollapsed,
                backgroundColor = dominantBackdropColor.takeIf { dominantColorEnabled },
                onBack = onBackFromDetails,
            )

            selectedEpisodeForActions?.let { selectedEpisode ->
                EpisodeActionsSheet(
                    meta = meta,
                    episode = selectedEpisode,
                    watchedKeys = watchedUiState.watchedKeys,
                    progressByVideoId = progressByVideoId,
                    todayIsoDate = todayIsoDate,
                    blurUnwatchedEpisodes = metaScreenSettingsUiState.blurUnwatchedEpisodes,
                    showPlayManually = showManualPlayOption && playbackAvailability.canStream(meta.type, selectedEpisode.id),
                    onDismiss = { selectedEpisodeForActions = null },
                    onPlayManually = { onEpisodeManualPlayClick(selectedEpisode) },
                )
            }

            selectedSeasonForActions?.let { selectedSeason ->
                SeasonActionsSheet(
                    meta = meta,
                    season = selectedSeason,
                    watchedKeys = watchedUiState.watchedKeys,
                    progressByVideoId = progressByVideoId,
                    todayIsoDate = todayIsoDate,
                    onDismiss = { selectedSeasonForActions = null },
                )
            }

            LibraryListPickerHost(
                state = libraryListPicker,
                onRemovalNeedsConfirmation = { pendingTrackingRemoval = it },
            )

            TrackingMembershipRemovalConfirmationHost(
                pending = pendingTrackingRemoval,
                onPendingChange = { pendingTrackingRemoval = it },
            )

            CommentDetailHost(state = comments, onLoadMore = loadMoreComments)
        }
    }
}

@Composable
private fun DetailHeaderOverlay(
    meta: MetaDetails,
    isHeroCollapsed: State<Boolean>,
    backgroundColor: Color?,
    onBack: () -> Unit,
) {
    val headerTarget = if (isHeroCollapsed.value) 1f else 0f
    val headerProgress by animateFloatAsState(
        targetValue = headerTarget,
        animationSpec = tween(
            durationMillis = if (headerTarget > 0f) 150 else 100,
            easing = LinearOutSlowInEasing,
        ),
        label = "detail_floating_header_progress",
    )

    DetailFloatingHeader(
        meta = meta,
        progress = headerProgress,
        backgroundColor = backgroundColor,
        onBack = onBack,
        modifier = Modifier.zIndex(2f),
    )
}

private fun MetaDetails.toMetaPreview(): MetaPreview =
    MetaPreview(
        id = id,
        type = type,
        name = name,
        poster = poster,
        banner = background,
        logo = logo,
        description = description,
        releaseInfo = releaseInfo,
        imdbRating = imdbRating,
        genres = genres,
    )

private fun detailTabletContentMaxWidth(maxWidth: Dp, isTablet: Boolean): Dp =
    if (!isTablet) {
        maxWidth
    } else {
        (maxWidth * 0.6f).coerceIn(520.dp, 680.dp)
    }
