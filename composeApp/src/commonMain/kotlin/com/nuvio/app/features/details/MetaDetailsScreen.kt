package com.nuvio.app.features.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.build.TrailerPlaybackMode
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.shell.components.NuvioBackButton
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.components.TrackingListPickerSheet
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.features.details.components.DetailActionButtons
import com.nuvio.app.features.details.components.DetailSecondaryAction
import com.nuvio.app.features.details.components.CommentDetailSheet
import com.nuvio.app.features.details.components.DetailAdditionalInfoSection
import com.nuvio.app.features.details.components.DetailCastSection
import com.nuvio.app.features.details.components.DetailCommentsSection
import com.nuvio.app.features.details.components.DetailFloatingHeader
import com.nuvio.app.features.details.components.DetailHero
import com.nuvio.app.features.details.components.DetailMetaInfo
import com.nuvio.app.features.details.components.DetailPosterGridSection
import com.nuvio.app.features.details.components.DetailPosterRailSection
import com.nuvio.app.features.details.components.DetailProductionSection
import com.nuvio.app.features.details.components.DetailEpisodeListRow
import com.nuvio.app.features.details.components.DetailSectionTitle
import com.nuvio.app.features.details.components.EpisodeListEntry
import com.nuvio.app.features.details.components.buildEpisodeListEntries
import com.nuvio.app.features.details.components.episodeWatchState
import com.nuvio.app.features.details.components.rememberEpisodeSeasonExpansion
import com.nuvio.app.features.details.components.summarizeEpisodeSeasons
import com.nuvio.app.features.details.components.DetailTrailersSection
import com.nuvio.app.features.details.components.EpisodeWatchedActionSheet
import com.nuvio.app.features.details.components.SeasonWatchedActionSheet
import com.nuvio.app.features.settings.ListItemBetweenSpace
import com.nuvio.app.features.home.HomeRepository
import com.nuvio.app.core.home.MetaPreview
import com.nuvio.app.core.library.LibraryRepository
import com.nuvio.app.features.library.PendingTrackingMembershipRemoval
import com.nuvio.app.features.library.TrackingMembershipRemovalConfirmationHost
import com.nuvio.app.features.library.executeTrackingMembershipOperation
import com.nuvio.app.features.library.showTrackingMembershipRewriteFeedback
import com.nuvio.app.core.library.toLibraryItem
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.streams.rememberPlaybackAvailability
import com.nuvio.app.core.streams.StreamAutoPlayPolicy
import com.nuvio.app.core.metadata.tmdb.TmdbSettingsRepository
import com.nuvio.app.core.tracking.trakt.TraktAuthRepository
import com.nuvio.app.core.tracking.trakt.TraktCommentReview
import com.nuvio.app.core.tracking.trakt.TraktCommentsRepository
import com.nuvio.app.core.tracking.trakt.TraktCommentsSettings
import com.nuvio.app.core.tracking.trakt.TraktConnectionMode
import com.nuvio.app.core.tracking.TrackingLibraryTab
import com.nuvio.app.core.tracking.TrackingMembershipApplyResult
import com.nuvio.app.core.tracking.toggleTrackingLibraryMembership
import com.nuvio.app.core.tracking.TrackingSettingsRepository
import com.nuvio.app.core.tracking.TrackingProviderId
import com.nuvio.app.core.watch.watched.WatchedRepository
import com.nuvio.app.core.watch.watched.previousReleasedEpisodesBefore
import com.nuvio.app.core.watch.watched.releasedPlayableEpisodes
import com.nuvio.app.core.watch.watched.releasedEpisodesForSeason
import com.nuvio.app.core.watch.watched.watchedItemKey
import com.nuvio.app.core.watch.progress.CurrentDateProvider
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.core.watch.progress.buildPlaybackVideoId
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesRepository
import com.nuvio.app.core.watch.watching.application.WatchingActions
import com.nuvio.app.core.watch.watching.application.WatchingState
import com.kmpalette.rememberDominantColorState
import com.kmpalette.extensions.painter.rememberPainterDominantColorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.metadata.HeroTrailerAudioState
import com.nuvio.app.core.metadata.MetaCompany
import com.nuvio.app.core.metadata.MetaDetails
import com.nuvio.app.core.metadata.MetaPerson
import com.nuvio.app.core.metadata.MetaScreenBackgroundMode
import com.nuvio.app.core.metadata.MetaScreenSectionItem
import com.nuvio.app.core.metadata.MetaScreenSectionKey
import com.nuvio.app.core.metadata.MetaScreenSettingsRepository
import com.nuvio.app.core.metadata.MetaScreenSettingsUiState
import com.nuvio.app.core.metadata.MetaTrailer
import com.nuvio.app.core.metadata.MetaVideo
import com.nuvio.app.core.metadata.MoreLikeThisSource
import com.nuvio.app.core.metadata.buildDetailHeroSlides
import com.nuvio.app.core.metadata.groupedEpisodesForDisplay
import com.nuvio.app.core.metadata.moreLikeThisFallback
import com.nuvio.app.core.metadata.playLabel
import com.nuvio.app.core.metadata.seriesPrimaryAction
import com.nuvio.app.core.metadata.tabGroupForRendering
import com.nuvio.app.core.metadata.detailSidePaneSection

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
    val playbackAvailability = rememberPlaybackAvailability()
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
    val tmdbSettingsUiState by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
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
    var autoLoadAttempted by remember(type, id) { mutableStateOf(false) }
    var observedOfflineState by remember(type, id) { mutableStateOf(false) }
    var selectedEpisodeForActions by remember(type, id) { mutableStateOf<MetaVideo?>(null) }
    var selectedSeasonForActions by remember(type, id) { mutableStateOf<Int?>(null) }
    val commentsEnabled by remember {
        TraktCommentsSettings.ensureLoaded()
        TraktCommentsSettings.enabled
    }.collectAsStateWithLifecycle()
    var comments by remember(type, id) { mutableStateOf<List<TraktCommentReview>>(emptyList()) }
    var commentsCurrentPage by remember(type, id) { mutableIntStateOf(0) }
    var commentsPageCount by remember(type, id) { mutableIntStateOf(0) }
    var isCommentsLoading by remember(type, id) { mutableStateOf(false) }
    var isCommentsLoadingMore by remember(type, id) { mutableStateOf(false) }
    var commentsError by remember(type, id) { mutableStateOf<String?>(null) }
    var selectedComment by remember(type, id) { mutableStateOf<TraktCommentReview?>(null) }
    val detailsScope = rememberCoroutineScope()
    var showLibraryListPicker by remember(type, id) { mutableStateOf(false) }
    var pickerTabs by remember(type, id) { mutableStateOf<List<TrackingLibraryTab>>(emptyList()) }
    var pickerMembership by remember(type, id) { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var pickerPending by remember(type, id) { mutableStateOf(false) }
    var pickerError by remember(type, id) { mutableStateOf<String?>(null) }
    var pendingTrackingRemoval by remember(type, id) {
        mutableStateOf<PendingTrackingMembershipRemoval?>(null)
    }
    val trackingListsUpdateFailedMessage = stringResource(Res.string.tracking_lists_update_failed)
    var deferredMetaWorkAllowed by remember(type, id) { mutableStateOf(false) }

    LaunchedEffect(
        displayedMeta?.id,
        displayedMeta?.type,
        displayedMeta?.name,
        displayedMeta?.videos,
        watchedUiState.items,
        watchedUiState.isLoaded,
        watchedUiState.hasLoadedRemoteItems,
        fullyWatchedSeriesKeys,
        watchProgressUiState.entries,
        trackingSettingsUiState.watchProgressSource,
    ) {
        val meta = displayedMeta ?: return@LaunchedEffect
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
            "marker state requestedSource=${trackingSettingsUiState.watchProgressSource} " +
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
            comments = emptyList()
            commentsCurrentPage = 0
            commentsPageCount = 0
            commentsError = null
            return@LaunchedEffect
        }
        if (!deferredMetaWorkAllowed) return@LaunchedEffect
        isCommentsLoading = true
        commentsError = null
        try {
            val result = TraktCommentsRepository.getCommentsPage(displayedMeta, page = 1)
            comments = result.items
            commentsCurrentPage = result.currentPage
            commentsPageCount = result.pageCount
        } catch (e: Exception) {
            commentsError = e.message ?: getString(Res.string.details_comments_load_failed)
        }
        isCommentsLoading = false
    }

    LaunchedEffect(type, id, displayedMeta, uiState.isLoading, autoLoadAttempted) {
        if (!autoLoadAttempted && displayedMeta == null && !uiState.isLoading) {
            autoLoadAttempted = true
            MetaDetailsRepository.load(type, id)
        }
    }

    LaunchedEffect(
        type,
        id,
        displayedMeta?.id,
        uiState.isLoading,
        trackingSettingsUiState.moreLikeThisSource,
        traktAuthUiState.mode,
        tmdbSettingsUiState.enabled,
        tmdbSettingsUiState.useMoreLikeThis,
        tmdbSettingsUiState.language,
    ) {
        if (displayedMeta != null && !uiState.isLoading) {
            MetaDetailsRepository.load(type, id)
        }
    }

    LaunchedEffect(networkStatusUiState.condition, displayedMeta, uiState.isLoading, type, id) {
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@LaunchedEffect
                observedOfflineState = false
                if (displayedMeta == null && !uiState.isLoading) {
                    MetaDetailsRepository.load(type, id)
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
            displayedMeta == null && uiState.isLoading -> {
                NuvioLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            displayedMeta == null && uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.details_failed_to_load),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = when (networkStatusUiState.condition) {
                            NetworkCondition.NoInternet -> stringResource(Res.string.details_check_connection)
                            NetworkCondition.ServersUnreachable -> stringResource(Res.string.details_servers_unreachable)
                            else -> uiState.errorMessage.orEmpty()
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            NetworkStatusRepository.requestRefresh(force = true)
                            MetaDetailsRepository.load(type, id)
                        },
                    ) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }

            displayedMeta != null -> {
                val meta = displayedMeta
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
                val openLibraryListPicker = remember(meta) {
                    {
                        val libraryItem = meta.toLibraryItem(savedAtEpochMs = 0L)
                        pickerTabs = LibraryRepository.libraryListTabs(libraryItem)
                        pickerMembership = pickerTabs.associate { it.key to false }
                        pickerPending = true
                        pickerError = null
                        showLibraryListPicker = true
                        detailsScope.launch {
                            runCatching {
                                val snapshot = LibraryRepository.getMembershipSnapshot(libraryItem)
                                val tabs = LibraryRepository.libraryListTabs(libraryItem)
                                pickerTabs = tabs
                                pickerMembership = tabs.associate { tab ->
                                    tab.key to (snapshot[tab.key] == true)
                                }
                            }.onFailure { error ->
                                pickerError = error.message ?: getString(Res.string.trakt_lists_load_failed)
                            }
                            pickerPending = false
                        }
                        Unit
                    }
                }
                val toggleSaved = remember(meta, trackingListsUpdateFailedMessage) {
                    {
                        val item = meta.toLibraryItem(savedAtEpochMs = 0L)
                        detailsScope.launch {
                            val toggleMembership: suspend (Set<TrackingProviderId>) ->
                                TrackingMembershipApplyResult = { confirmedProviders ->
                                LibraryRepository.toggleSaved(
                                    item = item,
                                    confirmedRemovalProviders = confirmedProviders,
                                )
                            }
                            executeTrackingMembershipOperation(
                                operation = { toggleMembership(emptySet()) },
                                onSuccess = { result ->
                                    if (result.requiresRemovalConfirmation) {
                                        pendingTrackingRemoval = PendingTrackingMembershipRemoval(
                                            itemTitle = item.name,
                                            confirmations = result.requiredRemovalConfirmations,
                                            retry = toggleMembership,
                                            onApplied = ::showTrackingMembershipRewriteFeedback,
                                            onFailure = { error ->
                                                NuvioToastController.show(
                                                    error.message ?: trackingListsUpdateFailedMessage,
                                                )
                                            },
                                        )
                                    } else {
                                        showTrackingMembershipRewriteFeedback(result)
                                    }
                                },
                                onFailure = { error ->
                                    NuvioToastController.show(
                                        error.message ?: trackingListsUpdateFailedMessage,
                                    )
                                },
                            )
                        }
                        Unit
                    }
                }
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
                val movieProgress = progressByVideoId[meta.id]
                    ?.takeUnless { it.isCompleted }
                val cwPrefs by ContinueWatchingPreferencesRepository.uiState.collectAsStateWithLifecycle()
                val seriesAction = remember(watchProgressUiState.entries, watchedUiState.items, meta, todayIsoDate, cwPrefs.upNextFromFurthestEpisode, watchedUiState.watchedKeys) {
                    meta.seriesPrimaryAction(
                        entries = watchProgressUiState.entries,
                        watchedItems = watchedUiState.items,
                        todayIsoDate = todayIsoDate,
                        preferFurthestEpisode = cwPrefs.upNextFromFurthestEpisode,
                        watchedKeys = watchedUiState.watchedKeys,
                    )
                }
                val seriesActionVideo = remember(seriesAction, meta.id, meta.videos) {
                    val action = seriesAction ?: return@remember null
                    meta.videos.firstOrNull { video ->
                        if (action.seasonNumber != null && action.episodeNumber != null) {
                            video.season == action.seasonNumber &&
                                video.episode == action.episodeNumber
                        } else {
                            buildPlaybackVideoId(
                                parentMetaId = meta.id,
                                seasonNumber = video.season,
                                episodeNumber = video.episode,
                                fallbackVideoId = video.id,
                            ) == action.videoId || video.id == action.videoId
                        }
                    }
                }
                val seriesPauseDescription = remember(seriesActionVideo) {
                    seriesActionVideo?.overview
                }
                val seriesStreamVideoId = remember(seriesAction, seriesActionVideo) {
                    val action = seriesAction ?: return@remember null
                    seriesActionVideo?.id?.takeIf { it.isNotBlank() } ?: action.videoId
                }
                val hasEpisodes = meta.videos.any { it.season != null || it.episode != null }
                val episodeListGroupedEpisodes = remember(meta.videos, meta.type) {
                    meta.groupedEpisodesForDisplay()
                }
                val episodeSeasonSummary = remember(
                    meta,
                    episodeListGroupedEpisodes,
                    progressByVideoId,
                    watchedUiState.watchedKeys,
                    todayIsoDate,
                ) {
                    summarizeEpisodeSeasons(episodeListGroupedEpisodes, todayIsoDate) { episode ->
                        episodeWatchState(meta, episode, progressByVideoId, watchedUiState.watchedKeys)
                    }
                }
                val episodeSeasonExpansion = rememberEpisodeSeasonExpansion(meta.id)
                val expandedEpisodeSeasons = episodeListGroupedEpisodes.keys.filter { season ->
                    episodeSeasonExpansion.isExpanded(season, episodeSeasonSummary.defaultSeason)
                }.toSet()
                val episodeListEntries = remember(
                    episodeListGroupedEpisodes,
                    expandedEpisodeSeasons,
                    episodeSeasonSummary.completedSeasons,
                ) {
                    buildEpisodeListEntries(
                        groupedEpisodes = episodeListGroupedEpisodes,
                        expandedSeasons = expandedEpisodeSeasons,
                        completedSeasons = episodeSeasonSummary.completedSeasons,
                    )
                }
                val hasProductionSection = remember(meta) {
                    meta.productionCompanies.isNotEmpty() || meta.networks.isNotEmpty()
                }
                val hasAdditionalInfoSection = remember(meta) {
                    meta.status != null ||
                        meta.releaseInfo != null ||
                        meta.runtime != null ||
                        meta.ageRating != null ||
                        meta.country != null ||
                        meta.language != null
                }
                val hasCollectionSection = remember(meta) {
                    meta.collectionName != null && meta.collectionItems.isNotEmpty()
                }
                val moreLikeThisItems = remember(meta, homeSections) {
                    meta.moreLikeThis.ifEmpty { moreLikeThisFallback(meta, homeSections) }
                }
                val hasTrailersSection = remember(meta) {
                    meta.trailers.isNotEmpty()
                }
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
                val onPrimaryPlayClick: () -> Unit = {
                    when {
                        (meta.type == "series" || hasEpisodes) && seriesAction != null -> {
                            onPlay?.invoke(
                                meta.type,
                                seriesStreamVideoId ?: seriesAction.videoId,
                                meta.id,
                                meta.type,
                                meta.name,
                                meta.logo,
                                meta.poster,
                                meta.background,
                                seriesAction.seasonNumber,
                                seriesAction.episodeNumber,
                                seriesAction.episodeTitle,
                                seriesAction.episodeThumbnail,
                                seriesPauseDescription,
                                seriesAction.resumePositionMs,
                            )
                        }

                        else -> {
                            onPlay?.invoke(
                                meta.type,
                                meta.id,
                                meta.id,
                                meta.type,
                                meta.name,
                                meta.logo,
                                meta.poster,
                                meta.background,
                                null,
                                null,
                                null,
                                null,
                                meta.description,
                                movieProgress?.lastPositionMs,
                            )
                        }
                    }
                }
                val manualPlayHandler = onPlayManually
                val showManualPlayOption = manualPlayHandler != null && StreamAutoPlayPolicy.isEffectivelyEnabled(playerSettingsUiState)
                val onPrimaryPlayLongClick: (() -> Unit)? = manualPlayHandler
                    ?.takeIf { showManualPlayOption && playbackAvailability.canStream(meta.type, primaryVideoId) }
                    ?.let { manualPlay ->
                        {
                            when {
                                (meta.type == "series" || hasEpisodes) && seriesAction != null -> {
                                    manualPlay(
                                        meta.type,
                                        seriesStreamVideoId ?: seriesAction.videoId,
                                        meta.id,
                                        meta.type,
                                        meta.name,
                                        meta.logo,
                                        meta.poster,
                                        meta.background,
                                        seriesAction.seasonNumber,
                                        seriesAction.episodeNumber,
                                        seriesAction.episodeTitle,
                                        seriesAction.episodeThumbnail,
                                        seriesPauseDescription,
                                        seriesAction.resumePositionMs,
                                    )
                                }

                                else -> {
                                    manualPlay(
                                        meta.type,
                                        meta.id,
                                        meta.id,
                                        meta.type,
                                        meta.name,
                                        meta.logo,
                                        meta.poster,
                                        meta.background,
                                        null,
                                        null,
                                        null,
                                        null,
                                        meta.description,
                                        movieProgress?.lastPositionMs,
                                    )
                                }
                            }
                        }
                    }
                val onEpisodePlayClick: (MetaVideo) -> Unit = { video ->
                    val season = video.season
                    val episode = video.episode
                    val playbackVideoId = buildPlaybackVideoId(
                        parentMetaId = meta.id,
                        seasonNumber = season,
                        episodeNumber = episode,
                        fallbackVideoId = video.id,
                    )
                    val streamVideoId = video.id.takeIf { it.isNotBlank() } ?: playbackVideoId
                    val savedProgress = watchProgressUiState.progressForVideo(
                        videoId = streamVideoId,
                        parentMetaId = meta.id,
                        seasonNumber = season,
                        episodeNumber = episode,
                    )
                        ?.takeUnless { it.isCompleted }
                    onPlay?.invoke(
                        meta.type,
                        streamVideoId,
                        meta.id,
                        meta.type,
                        meta.name,
                        meta.logo,
                        meta.poster,
                        meta.background,
                        season,
                        episode,
                        video.title,
                        video.thumbnail,
                        video.overview,
                        savedProgress?.lastPositionMs,
                    )
                }
                val onEpisodeManualPlayClick: (MetaVideo) -> Unit = { video ->
                    val season = video.season
                    val episode = video.episode
                    val playbackVideoId = buildPlaybackVideoId(
                        parentMetaId = meta.id,
                        seasonNumber = season,
                        episodeNumber = episode,
                        fallbackVideoId = video.id,
                    )
                    val streamVideoId = video.id.takeIf { it.isNotBlank() } ?: playbackVideoId
                    val savedProgress = watchProgressUiState.progressForVideo(
                        videoId = streamVideoId,
                        parentMetaId = meta.id,
                        seasonNumber = season,
                        episodeNumber = episode,
                    )
                        ?.takeUnless { it.isCompleted }
                    onPlayManually?.invoke(
                        meta.type,
                        streamVideoId,
                        meta.id,
                        meta.type,
                        meta.name,
                        meta.logo,
                        meta.poster,
                        meta.background,
                        season,
                        episode,
                        video.title,
                        video.thumbnail,
                        video.overview,
                        savedProgress?.lastPositionMs,
                    )
                }
                val listState = rememberLazyListState()
                val density = LocalDensity.current
                val safeAreaTopPx = with(density) {
                    WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding()
                        .toPx()
                }
                val heroHeightPx = remember(meta.id) { mutableIntStateOf(0) }
                // Keep pixel-by-pixel list state reads out of this composition. Reading the
                // offset here would recompose every metadata section on every scroll frame.
                val detailScrollOffsetPx = remember(listState, heroHeightPx) {
                    {
                        if (listState.firstVisibleItemIndex == 0) {
                            listState.firstVisibleItemScrollOffset.toFloat()
                        } else {
                            heroHeightPx.intValue.toFloat() + listState.firstVisibleItemScrollOffset
                        }
                    }
                }
                val isHeroCollapsed = remember(listState, heroHeightPx, safeAreaTopPx) {
                    derivedStateOf {
                        val measuredHeroHeightPx = heroHeightPx.intValue
                        val thresholdPx = (measuredHeroHeightPx - safeAreaTopPx).coerceAtLeast(0f)
                        measuredHeroHeightPx > 0 &&
                            (listState.firstVisibleItemIndex > 0 || detailScrollOffsetPx() > thresholdPx)
                    }
                }

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
                                        comments = comments,
                                        isCommentsLoading = isCommentsLoading,
                                        commentsError = commentsError,
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
                                comments = comments,
                                isCommentsLoading = isCommentsLoading,
                                isCommentsLoadingMore = isCommentsLoadingMore,
                                commentsCurrentPage = commentsCurrentPage,
                                commentsPageCount = commentsPageCount,
                                commentsError = commentsError,
                                episodeListEntries = episodeListEntries,
                                todayIsoDate = todayIsoDate,
                                onEpisodeSeasonToggle = { season ->
                                    episodeSeasonExpansion.toggle(season, episodeSeasonSummary.defaultSeason)
                                },
                                onRetryComments = {
                                    detailsScope.launch {
                                        isCommentsLoading = true
                                        commentsError = null
                                        try {
                                            val result = TraktCommentsRepository.getCommentsPage(meta, page = 1, forceRefresh = true)
                                            comments = result.items
                                            commentsCurrentPage = result.currentPage
                                            commentsPageCount = result.pageCount
                                        } catch (e: Exception) {
                                            commentsError = e.message ?: getString(Res.string.details_comments_load_failed)
                                        }
                                        isCommentsLoading = false
                                    }
                                },
                                onLoadMoreComments = {
                                    detailsScope.launch {
                                        isCommentsLoadingMore = true
                                        try {
                                            val nextPage = commentsCurrentPage + 1
                                            val result = TraktCommentsRepository.getCommentsPage(meta, page = nextPage)
                                            val existingIds = comments.map { it.id }.toSet()
                                            val newComments = result.items.filter { it.id !in existingIds }
                                            comments = comments + newComments
                                            commentsCurrentPage = result.currentPage
                                            commentsPageCount = result.pageCount
                                        } catch (_: Exception) { }
                                        isCommentsLoadingMore = false
                                    }
                                },
                                onCommentClick = { review -> selectedComment = review },
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
                    val dominantImageBitmapColorState = rememberDominantColorState(
                        defaultColor = colorScheme.background,
                        defaultOnColor = colorScheme.onBackground,
                    )
                    val dominantPainterColorState = rememberPainterDominantColorState(
                        defaultColor = colorScheme.background,
                        defaultOnColor = colorScheme.onBackground,
                    )
                    LaunchedEffect(dominantColorEnabled, dominantBackdropImageBitmap, dominantBackdropPainter) {
                        val imageBitmap = dominantBackdropImageBitmap
                        val painter = dominantBackdropPainter
                        if (dominantColorEnabled) {
                            when {
                                imageBitmap != null -> runCatching {
                                    dominantImageBitmapColorState.updateFrom(imageBitmap)
                                }
                                painter != null -> runCatching {
                                    dominantPainterColorState.updateFrom(painter)
                                }
                            }
                        }
                    }
                    val extractedDominantColor = if (dominantBackdropImageBitmap != null) {
                        dominantImageBitmapColorState.color
                    } else {
                        dominantPainterColorState.color
                    }
                    val dominantBackdropTargetColor = if (dominantColorEnabled) {
                        dominantBackdropBlendColor(extractedDominantColor, colorScheme.background)
                    } else {
                        colorScheme.background
                    }
                    val dominantBackdropColor by animateColorAsState(
                        targetValue = dominantBackdropTargetColor,
                        animationSpec = tween(
                            durationMillis = 320,
                            easing = LinearOutSlowInEasing,
                        ),
                        label = "detail_dominant_backdrop_color",
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().detailsContentReveal(metaScreenSettingsUiState.posterTransitionEnabled)) {
                            when (backgroundMode) {
                                MetaScreenBackgroundMode.Normal -> Unit
                                MetaScreenBackgroundMode.Cinematic -> if (deferredMetaWorkAllowed && backdropUrl != null) {
                                    AsyncImage(
                                        model = backdropUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(30.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(colorScheme.background.copy(alpha = 0.92f)),
                                    )
                                }
                                MetaScreenBackgroundMode.DominantColor -> if (deferredMetaWorkAllowed) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(dominantBackdropColor),
                                    )
                                }
                            }
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
                                        Spacer(modifier = Modifier.height(nuvioSafeBottomPadding(32.dp)))
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
                                            Spacer(modifier = Modifier.height(nuvioSafeBottomPadding(32.dp)))
                                        }
                                    }
                                }
                            }

                            if (backgroundMode.usesBackdropBackground && deferredMetaWorkAllowed && heroHeightPx.intValue > 0) {
                                val blendColor = dominantBackdropColor.takeIf { dominantColorEnabled }
                                    ?: colorScheme.background
                                Box(
                                    modifier = Modifier
                                        .zIndex(0.5f)
                                        .fillMaxWidth(primaryPaneWeight)
                                        .height(132.dp)
                                        .graphicsLayer {
                                            translationY = heroHeightPx.intValue.toFloat() - detailScrollOffsetPx()
                                        }
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    blendColor.copy(alpha = 0.98f),
                                                    blendColor.copy(alpha = 0.84f),
                                                    blendColor.copy(alpha = 0.52f),
                                                    Color.Transparent,
                                                ),
                                            ),
                                        ),
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
                            val isSelectedEpisodeWatched = remember(meta, selectedEpisode, watchedUiState.watchedKeys, progressByVideoId) {
                                isEpisodeWatchedForActions(
                                    meta = meta,
                                    episode = selectedEpisode,
                                    watchedKeys = watchedUiState.watchedKeys,
                                    progressByVideoId = progressByVideoId,
                                )
                            }
                            val previousEpisodes = remember(meta, selectedEpisode, todayIsoDate) {
                                meta.previousReleasedEpisodesBefore(
                                    target = selectedEpisode,
                                    todayIsoDate = todayIsoDate,
                                )
                            }
                            val seasonEpisodes = remember(meta, selectedEpisode, todayIsoDate) {
                                meta.releasedEpisodesForSeason(
                                    seasonNumber = selectedEpisode.season,
                                    todayIsoDate = todayIsoDate,
                                )
                            }
                            val arePreviousEpisodesWatched = remember(previousEpisodes, watchedUiState.watchedKeys, progressByVideoId) {
                                areEpisodesWatchedForActions(
                                    meta = meta,
                                    episodes = previousEpisodes,
                                    watchedKeys = watchedUiState.watchedKeys,
                                    progressByVideoId = progressByVideoId,
                                )
                            }
                            val isSeasonWatched = remember(seasonEpisodes, watchedUiState.watchedKeys, progressByVideoId) {
                                areEpisodesWatchedForActions(
                                    meta = meta,
                                    episodes = seasonEpisodes,
                                    watchedKeys = watchedUiState.watchedKeys,
                                    progressByVideoId = progressByVideoId,
                                )
                            }
                            EpisodeWatchedActionSheet(
                                episode = selectedEpisode,
                                seasonLabel = selectedEpisode.season?.let {
                                    stringResource(Res.string.episodes_season, it)
                                } ?: stringResource(Res.string.episodes_specials),
                                isEpisodeWatched = isSelectedEpisodeWatched,
                                canMarkPreviousEpisodes = previousEpisodes.isNotEmpty(),
                                arePreviousEpisodesWatched = arePreviousEpisodesWatched,
                                isSeasonWatched = isSeasonWatched,
                                thumbnailUrl = selectedEpisode.thumbnail ?: meta.background ?: meta.poster,
                                blurThumbnail = metaScreenSettingsUiState.blurUnwatchedEpisodes && !isSelectedEpisodeWatched,
                                onDismiss = { selectedEpisodeForActions = null },
                                onToggleWatched = {
                                    WatchingActions.toggleEpisodeWatched(
                                        meta = meta,
                                        episode = selectedEpisode,
                                        isCurrentlyWatched = isSelectedEpisodeWatched,
                                    )
                                },
                                onTogglePreviousWatched = {
                                    WatchingActions.togglePreviousEpisodesWatched(
                                        meta = meta,
                                        episodes = previousEpisodes,
                                        areCurrentlyWatched = arePreviousEpisodesWatched,
                                    )
                                },
                                onToggleSeasonWatched = {
                                    WatchingActions.toggleSeasonWatched(
                                        meta = meta,
                                        episodes = seasonEpisodes,
                                        areCurrentlyWatched = isSeasonWatched,
                                    )
                                },
                                showPlayManually = showManualPlayOption && playbackAvailability.canStream(meta.type, selectedEpisode.id),
                                onPlayManually = {
                                    onEpisodeManualPlayClick(selectedEpisode)
                                },
                            )
                        }

                        selectedSeasonForActions?.let { selectedSeason ->
                            val seasonLabel = selectedSeasonLabel(selectedSeason)
                            val seasonEpisodes = remember(meta, selectedSeason, todayIsoDate) {
                                meta.releasedEpisodesForSeason(
                                    seasonNumber = selectedSeason,
                                    todayIsoDate = todayIsoDate,
                                )
                            }
                            val previousSeasonEpisodes = remember(meta, selectedSeason, todayIsoDate) {
                                val normalizedSelectedSeason = selectedSeason.coerceAtLeast(0)
                                meta.releasedPlayableEpisodes(todayIsoDate)
                                    .filter { episode ->
                                        val season = episode.season?.coerceAtLeast(0) ?: 0
                                        season > 0 && season < normalizedSelectedSeason
                                    }
                            }
                            val isSeasonWatched = remember(seasonEpisodes, watchedUiState.watchedKeys, progressByVideoId) {
                                areEpisodesWatchedForActions(
                                    meta = meta,
                                    episodes = seasonEpisodes,
                                    watchedKeys = watchedUiState.watchedKeys,
                                    progressByVideoId = progressByVideoId,
                                )
                            }
                            val canMarkPreviousSeasons = remember(previousSeasonEpisodes, watchedUiState.watchedKeys, progressByVideoId) {
                                previousSeasonEpisodes.any { episode ->
                                    !isEpisodeWatchedForActions(
                                        meta = meta,
                                        episode = episode,
                                        watchedKeys = watchedUiState.watchedKeys,
                                        progressByVideoId = progressByVideoId,
                                    )
                                }
                            }
                            SeasonWatchedActionSheet(
                                seasonLabel = seasonLabel,
                                posterUrl = meta.poster ?: meta.background,
                                showTitle = meta.name,
                                isSeasonWatched = isSeasonWatched,
                                canMarkPreviousSeasons = canMarkPreviousSeasons,
                                onDismiss = { selectedSeasonForActions = null },
                                onToggleSeasonWatched = {
                                    WatchingActions.toggleSeasonWatched(
                                        meta = meta,
                                        episodes = seasonEpisodes,
                                        areCurrentlyWatched = isSeasonWatched,
                                    )
                                },
                                onMarkPreviousSeasonsWatched = {
                                    WatchingActions.togglePreviousEpisodesWatched(
                                        meta = meta,
                                        episodes = previousSeasonEpisodes,
                                        areCurrentlyWatched = false,
                                    )
                                },
                            )
                        }

                        TrackingListPickerSheet(
                            visible = showLibraryListPicker,
                            title = meta.name,
                            tabs = pickerTabs,
                            membership = pickerMembership,
                            isPending = pickerPending,
                            errorMessage = pickerError,
                            onToggle = { listKey ->
                                val previousMembership = pickerMembership
                                pickerMembership = toggleTrackingLibraryMembership(
                                    tabs = pickerTabs,
                                    membership = pickerMembership,
                                    key = listKey,
                                )
                                detailsScope.launch {
                                    pickerPending = true
                                    pickerError = null
                                    val item = meta.toLibraryItem(savedAtEpochMs = 0L)
                                    val desiredMembership = pickerMembership.toMap()
                                    val applyMembership: suspend (Set<TrackingProviderId>) ->
                                        TrackingMembershipApplyResult = { confirmedProviders ->
                                        LibraryRepository.applyMembershipChanges(
                                            item = item,
                                            desiredMembership = desiredMembership,
                                            confirmedRemovalProviders = confirmedProviders,
                                        )
                                    }
                                    val revertMembership: suspend (Throwable) -> Unit = { error ->
                                        pickerMembership = previousMembership
                                        pickerError = error.message ?: trackingListsUpdateFailedMessage
                                    }
                                    executeTrackingMembershipOperation(
                                        operation = { applyMembership(emptySet()) },
                                        onSuccess = { result ->
                                            if (result.requiresRemovalConfirmation) {
                                                pendingTrackingRemoval = PendingTrackingMembershipRemoval(
                                                    itemTitle = item.name,
                                                    confirmations = result.requiredRemovalConfirmations,
                                                    retry = applyMembership,
                                                    onApplied = { applied ->
                                                        showTrackingMembershipRewriteFeedback(applied)
                                                    },
                                                    onFailure = revertMembership,
                                                    onCancelled = { pickerMembership = previousMembership },
                                                )
                                            } else {
                                                showTrackingMembershipRewriteFeedback(result)
                                            }
                                        },
                                        onFailure = revertMembership,
                                    )
                                    pickerPending = false
                                }
                            },
                            onDismiss = {
                                showLibraryListPicker = false
                                pickerError = null
                            },
                        )

                        TrackingMembershipRemovalConfirmationHost(
                            pending = pendingTrackingRemoval,
                            onPendingChange = { pendingTrackingRemoval = it },
                        )

                        selectedComment?.let { comment ->
                            val commentIndex = comments.indexOfFirst { it.id == comment.id }.coerceAtLeast(0)
                            CommentDetailSheet(
                                comment = comment,
                                currentIndex = commentIndex,
                                totalCount = comments.size,
                                canGoBack = commentIndex > 0,
                                canGoForward = commentIndex < comments.size - 1,
                                onPrevious = {
                                    if (commentIndex > 0) {
                                        selectedComment = comments[commentIndex - 1]
                                    }
                                },
                                onNext = {
                                    val nextIndex = commentIndex + 1
                                    if (nextIndex < comments.size) {
                                        selectedComment = comments[nextIndex]
                                    }
                                    if (nextIndex >= comments.size - 3 && commentsCurrentPage < commentsPageCount) {
                                        detailsScope.launch {
                                            isCommentsLoadingMore = true
                                            try {
                                                val nextPage = commentsCurrentPage + 1
                                                val result = TraktCommentsRepository.getCommentsPage(meta, page = nextPage)
                                                val existingIds = comments.map { it.id }.toSet()
                                                val newComments = result.items.filter { it.id !in existingIds }
                                                comments = comments + newComments
                                                commentsCurrentPage = result.currentPage
                                                commentsPageCount = result.pageCount
                                            } catch (_: Exception) { }
                                            isCommentsLoadingMore = false
                                        }
                                    }
                                },
                                onDismiss = { selectedComment = null },
                            )
                        }
                    }
                }
            }
        }

        if (displayedMeta == null) {
            NuvioBackButton(
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

@Composable
private fun selectedSeasonLabel(season: Int): String =
    if (season == 0) {
        stringResource(Res.string.episodes_specials)
    } else {
        stringResource(Res.string.episodes_season, season)
    }

private fun isEpisodeWatchedForActions(
    meta: MetaDetails,
    episode: MetaVideo,
    watchedKeys: Set<String>,
    progressByVideoId: Map<String, WatchProgressEntry>,
): Boolean {
    val episodeVideoId = buildPlaybackVideoId(
        parentMetaId = meta.id,
        seasonNumber = episode.season,
        episodeNumber = episode.episode,
        fallbackVideoId = episode.id,
    )
    return progressByVideoId[episodeVideoId]?.isEffectivelyCompleted == true ||
        WatchingState.isEpisodeWatched(
            watchedKeys = watchedKeys,
            metaType = meta.type,
            metaId = meta.id,
            episode = episode,
        )
}

private fun areEpisodesWatchedForActions(
    meta: MetaDetails,
    episodes: Collection<MetaVideo>,
    watchedKeys: Set<String>,
    progressByVideoId: Map<String, WatchProgressEntry>,
): Boolean = episodes.isNotEmpty() && episodes.all { episode ->
    isEpisodeWatchedForActions(
        meta = meta,
        episode = episode,
        watchedKeys = watchedKeys,
        progressByVideoId = progressByVideoId,
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

private fun LazyListScope.configuredMetaSectionItems(
    settings: MetaScreenSettingsUiState,
    meta: MetaDetails,
    isTablet: Boolean,
    contentHorizontalPadding: Dp,
    contentMaxWidth: Dp,
    playButtonLabel: String,
    isPrimaryPlayEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    onPrimaryPlayClick: () -> Unit,
    onPrimaryPlayLongClick: (() -> Unit)?,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    showManualPlayOption: Boolean,
    hasProductionSection: Boolean,
    hasTrailersSection: Boolean,
    hasEpisodes: Boolean,
    hasAdditionalInfoSection: Boolean,
    hasCollectionSection: Boolean,
    moreLikeThisItems: List<MetaPreview>,
    shouldShowComments: Boolean,
    comments: List<TraktCommentReview>,
    isCommentsLoading: Boolean,
    isCommentsLoadingMore: Boolean,
    commentsCurrentPage: Int,
    commentsPageCount: Int,
    commentsError: String?,
    episodeListEntries: List<EpisodeListEntry>,
    todayIsoDate: String,
    onEpisodeSeasonToggle: (Int) -> Unit,
    onRetryComments: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    onTrailerClick: (MetaTrailer) -> Unit,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    blurUnwatchedEpisodes: Boolean,
    onEpisodeClick: (MetaVideo) -> Unit,
    onEpisodeLongPress: (MetaVideo) -> Unit,
    onSeasonLongPress: (Int) -> Unit,
    onOpenMeta: ((MetaPreview) -> Unit)?,
    onCastClick: ((MetaPerson, String?) -> Unit)?,
    onCompanyClick: ((MetaCompany, String) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val enabledItems = settings.items.filter { it.enabled }
    fun sectionHasContent(key: MetaScreenSectionKey): Boolean =
        metaSectionHasContent(
            key = key,
            meta = meta,
            hasProductionSection = hasProductionSection,
            hasTrailersSection = hasTrailersSection,
            hasEpisodes = hasEpisodes,
            hasAdditionalInfoSection = hasAdditionalInfoSection,
            hasCollectionSection = hasCollectionSection,
            moreLikeThisItems = moreLikeThisItems,
            shouldShowComments = shouldShowComments,
            comments = comments,
            isCommentsLoading = isCommentsLoading,
            commentsError = commentsError,
        )

    fun addSectionItem(
        key: String,
        sectionItems: List<MetaScreenSectionItem>,
        forceTabLayout: Boolean = settings.tabLayout,
    ) {
        item(key = key) {
            DetailSectionContainer(
                horizontalPadding = contentHorizontalPadding,
                contentMaxWidth = contentMaxWidth,
            ) {
                ConfiguredMetaSections(
                    settings = settings.copy(
                        items = sectionItems,
                        tabLayout = forceTabLayout,
                    ),
                    meta = meta,
                    isTablet = isTablet,
                    horizontalScrollPadding = contentHorizontalPadding,
                    playButtonLabel = playButtonLabel,
                    isPrimaryPlayEnabled = isPrimaryPlayEnabled,
                    isSaved = isSaved,
                    isWatched = isWatched,
                    onPrimaryPlayClick = onPrimaryPlayClick,
                    onPrimaryPlayLongClick = onPrimaryPlayLongClick,
                    onSaveClick = onSaveClick,
                    onSaveLongClick = onSaveLongClick,
                    onWatchedClick = onWatchedClick,
                    showManualPlayOption = showManualPlayOption,
                    hasProductionSection = hasProductionSection,
                    hasTrailersSection = hasTrailersSection,
                    hasEpisodes = hasEpisodes,
                    hasAdditionalInfoSection = hasAdditionalInfoSection,
                    hasCollectionSection = hasCollectionSection,
                    moreLikeThisItems = moreLikeThisItems,
                    shouldShowComments = shouldShowComments,
                    comments = comments,
                    isCommentsLoading = isCommentsLoading,
                    isCommentsLoadingMore = isCommentsLoadingMore,
                    commentsCurrentPage = commentsCurrentPage,
                    commentsPageCount = commentsPageCount,
                    commentsError = commentsError,
                    onRetryComments = onRetryComments,
                    onLoadMoreComments = onLoadMoreComments,
                    onCommentClick = onCommentClick,
                    onTrailerClick = onTrailerClick,
                    watchedKeys = watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onOpenMeta = onOpenMeta,
                    onCastClick = onCastClick,
                    onCompanyClick = onCompanyClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }
    }

    fun addLazyEpisodeListItems(key: String) {
        if (episodeListEntries.isEmpty()) return

        item(
            key = "$key-header",
            contentType = "detail-episode-header",
        ) {
            DetailSectionContainer(
                horizontalPadding = contentHorizontalPadding,
                contentMaxWidth = contentMaxWidth,
                bottomPadding = 14.dp,
            ) {
                DetailSectionTitle(title = stringResource(Res.string.details_episodes))
            }
        }
        itemsIndexed(
            items = episodeListEntries,
            key = { _, entry -> "$key-${entry.key}" },
            contentType = { _, entry ->
                if (entry is EpisodeListEntry.Season) "detail-episode-season" else "detail-episode"
            },
        ) { index, entry ->
            DetailSectionContainer(
                horizontalPadding = contentHorizontalPadding,
                contentMaxWidth = contentMaxWidth,
                bottomPadding = if (index == episodeListEntries.lastIndex) 20.dp else ListItemBetweenSpace,
                modifier = Modifier.animateItem(),
            ) {
                DetailEpisodeListRow(
                    entry = entry,
                    index = index,
                    count = episodeListEntries.size,
                    meta = meta,
                    todayIsoDate = todayIsoDate,
                    progressByVideoId = progressByVideoId,
                    watchedKeys = watchedKeys,
                    blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                    onSeasonClick = onEpisodeSeasonToggle,
                    onSeasonLongPress = onSeasonLongPress,
                    onEpisodeClick = onEpisodeClick,
                    onEpisodeLongPress = onEpisodeLongPress,
                )
            }
        }
    }

    fun addStandaloneSection(
        section: MetaScreenSectionItem,
        key: String,
        forceTabLayout: Boolean = false,
    ) {
        if (section.key == MetaScreenSectionKey.EPISODES) {
            addLazyEpisodeListItems(key)
        } else {
            addSectionItem(
                key = key,
                sectionItems = listOf(section),
                forceTabLayout = forceTabLayout,
            )
        }
    }

    if (!settings.tabLayout) {
        enabledItems
            .filter { sectionHasContent(it.key) }
            .forEach { section ->
                addStandaloneSection(
                    section = section,
                    key = "detail-section-${section.key.name}",
                )
            }
        return
    }

    val processedGroups = mutableSetOf<Int>()
    enabledItems.forEach { section ->
        val groupId = section.tabGroupForRendering()
        if (groupId == null) {
            if (sectionHasContent(section.key)) {
                addStandaloneSection(
                    section = section,
                    key = "detail-section-${section.key.name}",
                    forceTabLayout = true,
                )
            }
        } else if (groupId !in processedGroups) {
            processedGroups.add(groupId)
            val groupMembers = enabledItems.filter { item ->
                item.tabGroupForRendering() == groupId && sectionHasContent(item.key)
            }
            if (groupMembers.isNotEmpty()) {
                if (groupMembers.size == 1) {
                    addStandaloneSection(
                        section = groupMembers.single(),
                        key = "detail-section-group-$groupId",
                    )
                } else {
                    addSectionItem(
                        key = "detail-section-group-$groupId",
                        sectionItems = groupMembers,
                        forceTabLayout = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionContainer(
    horizontalPadding: Dp,
    contentMaxWidth: Dp,
    bottomPadding: Dp = 20.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (contentMaxWidth == Dp.Unspecified) {
                        Modifier
                    } else {
                        Modifier.widthIn(max = contentMaxWidth)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private fun metaSectionHasContent(
    key: MetaScreenSectionKey,
    meta: MetaDetails,
    hasProductionSection: Boolean,
    hasTrailersSection: Boolean,
    hasEpisodes: Boolean,
    hasAdditionalInfoSection: Boolean,
    hasCollectionSection: Boolean,
    moreLikeThisItems: List<MetaPreview>,
    shouldShowComments: Boolean,
    comments: List<TraktCommentReview>,
    isCommentsLoading: Boolean,
    commentsError: String?,
): Boolean =
    when (key) {
        MetaScreenSectionKey.ACTIONS -> true
        MetaScreenSectionKey.OVERVIEW -> true
        MetaScreenSectionKey.PRODUCTION -> hasProductionSection
        MetaScreenSectionKey.CAST -> meta.cast.isNotEmpty()
        MetaScreenSectionKey.COMMENTS -> shouldShowComments && (isCommentsLoading || comments.isNotEmpty() || !commentsError.isNullOrBlank())
        MetaScreenSectionKey.TRAILERS -> hasTrailersSection
        MetaScreenSectionKey.EPISODES -> hasEpisodes
        MetaScreenSectionKey.DETAILS -> hasAdditionalInfoSection
        MetaScreenSectionKey.COLLECTION -> !hasEpisodes && hasCollectionSection
        MetaScreenSectionKey.MORE_LIKE_THIS -> moreLikeThisItems.isNotEmpty()
    }

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun ConfiguredMetaSections(
    settings: MetaScreenSettingsUiState,
    meta: MetaDetails,
    isTablet: Boolean,
    horizontalScrollPadding: Dp,
    playButtonLabel: String,
    isPrimaryPlayEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    onPrimaryPlayClick: () -> Unit,
    onPrimaryPlayLongClick: (() -> Unit)?,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    showManualPlayOption: Boolean,
    hasProductionSection: Boolean,
    hasTrailersSection: Boolean,
    hasEpisodes: Boolean,
    hasAdditionalInfoSection: Boolean,
    hasCollectionSection: Boolean,
    moreLikeThisItems: List<MetaPreview>,
    shouldShowComments: Boolean,
    comments: List<TraktCommentReview>,
    isCommentsLoading: Boolean,
    isCommentsLoadingMore: Boolean,
    commentsCurrentPage: Int,
    commentsPageCount: Int,
    commentsError: String?,
    onRetryComments: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    onTrailerClick: (MetaTrailer) -> Unit,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onOpenMeta: ((MetaPreview) -> Unit)?,
    onCastClick: ((MetaPerson, String?) -> Unit)?,
    onCompanyClick: ((MetaCompany, String) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val enabledItems = settings.items.filter { it.enabled }

    // Helper to check if a section actually has content to show
    val sectionHasContent: (MetaScreenSectionKey) -> Boolean = { key ->
        when (key) {
            MetaScreenSectionKey.ACTIONS -> true
            MetaScreenSectionKey.OVERVIEW -> true
            MetaScreenSectionKey.PRODUCTION -> hasProductionSection
            MetaScreenSectionKey.CAST -> meta.cast.isNotEmpty()
            MetaScreenSectionKey.COMMENTS -> shouldShowComments && (isCommentsLoading || comments.isNotEmpty() || !commentsError.isNullOrBlank())
            MetaScreenSectionKey.TRAILERS -> hasTrailersSection
            MetaScreenSectionKey.EPISODES -> hasEpisodes
            MetaScreenSectionKey.DETAILS -> hasAdditionalInfoSection
            MetaScreenSectionKey.COLLECTION -> !hasEpisodes && hasCollectionSection
            MetaScreenSectionKey.MORE_LIKE_THIS -> moreLikeThisItems.isNotEmpty()
        }
    }

    @Composable
    fun RenderSection(key: MetaScreenSectionKey, showHeader: Boolean = true) {
        when (key) {
            MetaScreenSectionKey.ACTIONS -> {
                DetailActionButtons(
                    playLabel = if (isPrimaryPlayEnabled) playButtonLabel else stringResource(Res.string.playback_unavailable),
                    playEnabled = isPrimaryPlayEnabled,
                    secondaryActions = buildList {
                        add(DetailSecondaryAction(
                            label = if (isWatched) {
                                stringResource(Res.string.hero_mark_unwatched)
                            } else {
                                stringResource(Res.string.hero_mark_watched)
                            },
                            icon = if (isWatched) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.CheckCircleOutline
                            },
                            isActive = isWatched,
                            onClick = onWatchedClick,
                        ))
                        add(DetailSecondaryAction(
                            label = if (isSaved) {
                                stringResource(Res.string.hero_remove_from_library)
                            } else {
                                stringResource(Res.string.hero_add_to_library)
                            },
                            icon = if (isSaved) {
                                Icons.Default.Check
                            } else {
                                Icons.Default.Add
                            },
                            isActive = isSaved,
                            onClick = onSaveClick,
                        ))
                        onSaveLongClick?.let { openListPicker ->
                            add(DetailSecondaryAction(
                                label = stringResource(Res.string.details_save_to_lists),
                                icon = Icons.Rounded.CollectionsBookmark,
                                onClick = openListPicker,
                            ))
                        }
                    },
                    isTablet = isTablet,
                    onPlayClick = onPrimaryPlayClick,
                    onPlayLongClick = if (showManualPlayOption) onPrimaryPlayLongClick else null,
                )
            }
            MetaScreenSectionKey.OVERVIEW -> {
                DetailMetaInfo(
                    meta = meta,
                    horizontalScrollPadding = horizontalScrollPadding,
                )
            }
            MetaScreenSectionKey.PRODUCTION -> {
                if (hasProductionSection) {
                    DetailProductionSection(meta = meta, showHeader = showHeader, onCompanyClick = onCompanyClick)
                }
            }
            MetaScreenSectionKey.CAST -> {
                DetailCastSection(
                    cast = meta.cast,
                    showHeader = showHeader,
                    horizontalScrollPadding = horizontalScrollPadding,
                    onCastClick = onCastClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
            MetaScreenSectionKey.COMMENTS -> {
                if (shouldShowComments && (isCommentsLoading || comments.isNotEmpty() || !commentsError.isNullOrBlank())) {
                    DetailCommentsSection(
                        comments = comments,
                        isLoading = isCommentsLoading,
                        isLoadingMore = isCommentsLoadingMore,
                        canLoadMore = commentsCurrentPage < commentsPageCount,
                        error = commentsError,
                        onRetry = onRetryComments,
                        onLoadMore = onLoadMoreComments,
                        onCommentClick = onCommentClick,
                        showHeader = showHeader,
                        horizontalScrollPadding = horizontalScrollPadding,
                    )
                }
            }
            MetaScreenSectionKey.TRAILERS -> {
                if (hasTrailersSection) {
                    DetailTrailersSection(
                        trailers = meta.trailers,
                        onTrailerClick = onTrailerClick,
                        showHeader = showHeader,
                        horizontalScrollPadding = horizontalScrollPadding,
                    )
                }
            }
            // Episodes are lazy list items of their own, see addLazyEpisodeListItems.
            MetaScreenSectionKey.EPISODES -> Unit
            MetaScreenSectionKey.DETAILS -> {
                if (hasAdditionalInfoSection) {
                    DetailAdditionalInfoSection(meta = meta, showHeader = showHeader)
                }
            }
            MetaScreenSectionKey.COLLECTION -> {
                if (!hasEpisodes && hasCollectionSection) {
                    DetailPosterRailSection(
                        title = meta.collectionName.orEmpty(),
                        items = meta.collectionItems,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        showHeader = showHeader,
                        horizontalScrollPadding = horizontalScrollPadding,
                        onPosterClick = onOpenMeta,
                    )
                }
            }
            MetaScreenSectionKey.MORE_LIKE_THIS -> {
                if (moreLikeThisItems.isNotEmpty()) {
                    val sourceLabel = when (meta.moreLikeThisSource) {
                        MoreLikeThisSource.TMDB -> stringResource(Res.string.detail_more_like_this_powered_by_tmdb)
                        MoreLikeThisSource.TRAKT -> stringResource(Res.string.detail_more_like_this_powered_by_trakt)
                        null -> null
                    }
                    DetailPosterGridSection(
                        title = stringResource(Res.string.details_more_like_this),
                        items = moreLikeThisItems,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        showHeader = showHeader,
                        sourceLabel = sourceLabel,
                        onPosterClick = onOpenMeta,
                    )
                }
            }
        }
    }

    if (!settings.tabLayout) {
        // Standard mode: render sections individually in order
        enabledItems.forEach { section -> RenderSection(section.key) }
    } else {
        // Tab layout mode: group sections by tabGroup, render grouped ones as tabs
        val processedGroups = mutableSetOf<Int>()

        enabledItems.forEach { section ->
            val groupId = section.tabGroup
            if (groupId == null) {
                // Standalone section
                RenderSection(section.key)
            } else if (groupId !in processedGroups) {
                // First encounter of this group — render the whole tabbed group
                processedGroups.add(groupId)
                val groupMembers = enabledItems
                    .filter { it.tabGroup == groupId && sectionHasContent(it.key) }
                if (groupMembers.isEmpty()) return@forEach
                if (groupMembers.size == 1) {
                    // Only one member with content — render standalone
                    RenderSection(groupMembers.first().key)
                } else {
                    TabbedSectionGroup(
                        tabs = groupMembers.map { it.key to it.title },
                    ) { activeKey ->
                        RenderSection(activeKey, showHeader = false)
                    }
                }
            }
            // else: already processed as part of group, skip
        }
    }
}

@Composable
private fun TabbedSectionGroup(
    tabs: List<Pair<MetaScreenSectionKey, String>>,
    content: @Composable (MetaScreenSectionKey) -> Unit,
) {
    if (tabs.isEmpty()) return

    var selectedIndex by remember { mutableIntStateOf(0) }
    val clampedIndex = selectedIndex.coerceIn(0, tabs.lastIndex)
    if (clampedIndex != selectedIndex) selectedIndex = clampedIndex

    val headerColor = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Tab row using the same style as DetailSectionTitle
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val titleSize = if (maxWidth >= 720.dp) 22.sp else 20.sp
            val headerStyle = MaterialTheme.typography.titleLarge.copy(
                fontSize = titleSize,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, (_, title) ->
                    if (index > 0) {
                        Text(
                            text = "|",
                            style = headerStyle,
                            color = headerColor.copy(alpha = 0.45f),
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                    }

                    Text(
                        text = title,
                        style = headerStyle,
                        color = if (index == selectedIndex) {
                            headerColor
                        } else {
                            headerColor.copy(alpha = 0.55f)
                        },
                        maxLines = 1,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { selectedIndex = index },
                    )
                }
            }
        }

        // Content with crossfade
        Crossfade(
            targetState = tabs[selectedIndex].first,
            animationSpec = tween(durationMillis = 200),
            label = "tabbedSectionCrossfade",
        ) { activeKey ->
            content(activeKey)
        }
    }
}

private fun detailTabletContentMaxWidth(maxWidth: Dp, isTablet: Boolean): Dp =
    if (!isTablet) {
        maxWidth
    } else {
        (maxWidth * 0.6f).coerceIn(520.dp, 680.dp)
    }

private fun dominantBackdropBlendColor(dominantColor: Color, backgroundColor: Color): Color =
    backgroundColor.blendTowards(dominantColor, fraction = 0.42f)

private fun Color.blendTowards(target: Color, fraction: Float): Color {
    val clamped = fraction.coerceIn(0f, 1f)
    return Color(
        red = red + (target.red - red) * clamped,
        green = green + (target.green - green) * clamped,
        blue = blue + (target.blue - blue) * clamped,
        alpha = alpha + (target.alpha - alpha) * clamped,
    )
}
