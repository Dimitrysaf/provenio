package com.nuvio.app.shell.screens.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.shell.components.LocalNuvioBottomNavigationOverlayPadding
import com.nuvio.app.shell.components.ScreenActivityEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.NuvioScreen
import com.nuvio.app.shell.components.NuvioNetworkOfflineCard
import com.nuvio.app.shell.components.nuvioSafeBottomPadding
import com.nuvio.app.shell.components.rememberPosterCardStyleUiState
import com.nuvio.app.shell.components.withDuplicateSafeLazyKeys
import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.addons.firstEnabledManifestError
import com.nuvio.app.core.cloud.CloudLibraryContentType
import com.nuvio.app.core.cloud.CloudLibraryRepository
import com.nuvio.app.core.cloud.CloudLibraryUiState
import com.nuvio.app.core.cloud.findPlaybackTargetForProgress
import com.nuvio.app.core.metadata.MetaDetails
import com.nuvio.app.core.metadata.MetaDetailsRepository
import com.nuvio.app.core.metadata.MetaVideo
import com.nuvio.app.core.metadata.SeriesPrimaryAction
import com.nuvio.app.core.metadata.seriesPrimaryAction
import com.nuvio.app.shell.screens.home.components.HomeCatalogRowSection
import com.nuvio.app.shell.screens.home.components.HomeContinueWatchingSection
import com.nuvio.app.shell.screens.home.components.HomeEmptyStateCard
import com.nuvio.app.shell.screens.home.components.HomeHeroReservedSpace
import com.nuvio.app.shell.screens.home.components.HomeHeroSection
import com.nuvio.app.shell.screens.home.components.HomeSkeletonHero
import com.nuvio.app.shell.screens.home.components.HomeSkeletonRow
import com.nuvio.app.shell.screens.home.components.HomeContinueWatchingSectionBottomPadding
import com.nuvio.app.shell.screens.home.components.ContinueWatchingLayout
import com.nuvio.app.core.tracking.TrackingSettingsRepository
import com.nuvio.app.core.tracking.WatchProgressSource
import com.nuvio.app.core.watch.watched.WatchedItem
import com.nuvio.app.core.watch.watched.WatchedRepository
import com.nuvio.app.core.watch.watched.WatchedUiState
import com.nuvio.app.core.watch.watched.episodePlaybackId
import com.nuvio.app.core.watch.watched.resolveWatchedBadgesBulk
import com.nuvio.app.core.watch.watched.watchedItemKey
import com.nuvio.app.core.watch.progress.CachedInProgressItem
import com.nuvio.app.core.watch.progress.CachedNextUpItem
import com.nuvio.app.core.watch.progress.ContinueWatchingEnrichmentCache
import com.nuvio.app.core.watch.progress.CurrentDateProvider
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesRepository
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesUiState
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.ContinueWatchingSortMode
import com.nuvio.app.core.watch.progress.isMalformedNextUpSeedContentId
import com.nuvio.app.core.watch.progress.isSeriesTypeForContinueWatching
import com.nuvio.app.core.watch.progress.nextUpDismissKey
import com.nuvio.app.core.watch.progress.parseReleaseDateToEpochMs
import com.nuvio.app.core.watch.progress.resolvedProgressKey
import com.nuvio.app.core.watch.progress.shouldTreatAsInProgressForContinueWatching
import com.nuvio.app.core.watch.progress.shouldUseAsCompletedSeedForContinueWatching
import com.nuvio.app.core.watch.progress.WatchProgressClock
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.core.watch.progress.WatchProgressRepository
import com.nuvio.app.core.watch.progress.WatchProgressSourceCoordinator
import com.nuvio.app.core.watch.progress.buildContinueWatchingEpisodeSubtitle
import com.nuvio.app.core.watch.progress.continueWatchingEntries
import com.nuvio.app.core.watch.progress.toContinueWatchingItem
import com.nuvio.app.core.watch.progress.toUpNextContinueWatchingItem
import com.nuvio.app.shell.components.DisintegrationRequest
import com.nuvio.app.core.watch.watching.application.WatchingState
import com.nuvio.app.core.watch.watching.domain.WatchingContentRef
import com.nuvio.app.core.watch.watching.domain.isReleasedBy
import com.nuvio.app.core.collection.CollectionRepository
import com.nuvio.app.core.profiles.ProfileRepository
import com.nuvio.app.shell.screens.home.components.HomeCollectionRowSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import com.nuvio.app.shell.screens.home.components.continueWatchingHeroViewportReserveHeight
import com.nuvio.app.shell.screens.home.components.homeSectionHorizontalPaddingForWidth
import com.nuvio.app.shell.screens.home.components.rememberContinueWatchingLayout
import kotlinx.coroutines.CancellationException
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.home.HomeCatalogSection
import com.nuvio.app.core.home.MetaPreview
import com.nuvio.app.core.home.canOpenCatalog
import com.nuvio.app.core.home.shouldShowHomeHeroSlot
import com.nuvio.app.core.home.shouldShowInitialHomeLoading
import com.nuvio.app.core.home.CachedNextUpRelease
import com.nuvio.app.core.home.CompletedSeriesCandidate
import com.nuvio.app.core.home.HomeContinueWatchingMaxRecentProgressItems
import com.nuvio.app.core.home.HomeNextUpCandidateMetadataDecision
import com.nuvio.app.core.home.HomeNextUpCandidateMetadataOutcome
import com.nuvio.app.core.home.buildHomeNextUpSeedCandidates
import com.nuvio.app.core.home.cachedNextUpHasAired
import com.nuvio.app.core.home.filterEntriesForContinueWatchingWindow
import com.nuvio.app.core.home.filterNextUpItemsByCurrentSeeds
import com.nuvio.app.core.home.hasHomeNextUpSeedChangedFromCache
import com.nuvio.app.core.home.hasUsableHomeNextUpMetadata
import com.nuvio.app.core.home.isHomeNextUpSeedSourceLoaded
import com.nuvio.app.core.home.planHomeNextUpResolutionCandidates
import com.nuvio.app.core.home.splitUpcomingItems
import com.nuvio.app.core.home.HomeCatalogSettingsItem
import com.nuvio.app.core.home.HomeCatalogSettingsRepository
import com.nuvio.app.core.home.HomeRepository
import com.nuvio.app.core.home.buildHomeContinueWatchingItems
import com.nuvio.app.core.home.buildHomeInProgressCacheSnapshot
import com.nuvio.app.core.home.classifyHomeNextUpCandidateMetadata
import com.nuvio.app.core.home.mergeHomeNextUpItemsWithCache
import com.nuvio.app.core.home.nonBlankOrNull
import com.nuvio.app.core.home.toContinueWatchingItem

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    animateCollectionGifs: Boolean = true,
    scrollToTopRequests: Flow<Unit> = emptyFlow(),
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
    onContinueWatchingLongPress: ((ContinueWatchingItem) -> Unit)? = null,
    continueWatchingDisintegrationRequest: DisintegrationRequest<String>? = null,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    onFirstCatalogRendered: (() -> Unit)? = null,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
        CollectionRepository.initialize()
        ContinueWatchingPreferencesRepository.ensureLoaded()
        WatchedRepository.ensureLoaded()
        WatchProgressRepository.ensureLoaded()
        val authState = AuthRepository.state.value
        if (authState !is AuthState.Authenticated || authState.isAnonymous) {
            WatchProgressSourceCoordinator.ensureStarted()
        }
    }

    val addonsUiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val homeUiState by HomeRepository.uiState.collectAsStateWithLifecycle()
    val homeSettingsUiState by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val homeListState = rememberLazyListState()
    val continueWatchingListState = rememberLazyListState()
    val upcomingListState = rememberLazyListState()
    ScreenActivityEffect(homeListState, continueWatchingListState, upcomingListState) { active ->
        if (!active) {
            homeListState.stopScroll(MutatePriority.PreventUserInput)
            continueWatchingListState.stopScroll(MutatePriority.PreventUserInput)
            upcomingListState.stopScroll(MutatePriority.PreventUserInput)
        }
    }
    val collections by CollectionRepository.collections.collectAsStateWithLifecycle()
    val continueWatchingPreferences by ContinueWatchingPreferencesRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by WatchedRepository.uiState.collectAsStateWithLifecycle()
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val watchProgressUiState by WatchProgressRepository.uiState.collectAsStateWithLifecycle()
    val effectiveWatchProgressSource = watchProgressUiState.source
    val cloudLibraryUiState by CloudLibraryRepository.uiState.collectAsStateWithLifecycle()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val trackingSettingsUiState by remember {
        TrackingSettingsRepository.ensureLoaded()
        TrackingSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    var observedOfflineState by remember { mutableStateOf(false) }

    ScreenActivityEffect(scrollToTopRequests) { active ->
        if (!active) return@ScreenActivityEffect
        scrollToTopRequests.collect {
            homeListState.animateScrollToItem(0)
        }
    }

    ScreenActivityEffect(networkStatusUiState.condition) { active ->
        if (!active) return@ScreenActivityEffect
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (observedOfflineState) {
                    observedOfflineState = false
                    HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    val progressProviderOwnsCompletedHistory = remember(effectiveWatchProgressSource) {
        WatchProgressRepository.activeProviderOwnsCompletedHistoryProjection()
    }
    val continueWatchingCutoffEpochMs = remember(
        effectiveWatchProgressSource,
        trackingSettingsUiState.continueWatchingDaysCap,
    ) {
        WatchProgressRepository.activeProviderContinueWatchingCutoffEpochMs(
            daysCap = trackingSettingsUiState.continueWatchingDaysCap,
            nowEpochMs = WatchProgressClock.nowEpochMs(),
        )
    }

    val nextUpWatchedItems = remember(watchedUiState.items, progressProviderOwnsCompletedHistory) {
        if (progressProviderOwnsCompletedHistory) emptyList() else watchedUiState.items
    }

    val effectiveWatchProgressEntries = remember(
        watchProgressUiState.entries,
        watchProgressUiState.hiddenContentIds,
        continueWatchingCutoffEpochMs,
    ) {
        val visibleProviderEntries = watchProgressUiState.entries.filterNot { entry ->
            entry.parentMetaId in watchProgressUiState.hiddenContentIds ||
                WatchProgressRepository.isDroppedShow(entry.parentMetaId)
        }
        filterEntriesForContinueWatchingWindow(
            entries = visibleProviderEntries,
            cutoffEpochMs = continueWatchingCutoffEpochMs,
        )
    }

    val allNextUpSeedCandidates = remember(
        watchProgressUiState.entries,
        watchProgressUiState.hiddenContentIds,
        nextUpWatchedItems,
        progressProviderOwnsCompletedHistory,
        continueWatchingPreferences.upNextFromFurthestEpisode,
    ) {
        buildHomeNextUpSeedCandidates(
            progressEntries = watchProgressUiState.entries,
            watchedItems = nextUpWatchedItems,
            providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
            preferFurthestEpisode = continueWatchingPreferences.upNextFromFurthestEpisode,
            nowEpochMs = WatchProgressClock.nowEpochMs(),
            shouldUseProgressSeed = WatchProgressRepository::shouldUseAsNextUpSeed,
            isContentHidden = { contentId ->
                contentId in watchProgressUiState.hiddenContentIds ||
                    WatchProgressRepository.isDroppedShow(contentId)
            },
        )
    }

    val recentNextUpSeedCandidates = remember(
        allNextUpSeedCandidates,
        continueWatchingCutoffEpochMs,
    ) {
        filterHomeNextUpCandidatesForContinueWatchingWindow(
            candidates = allNextUpSeedCandidates,
            cutoffEpochMs = continueWatchingCutoffEpochMs,
        )
    }

    val activeNextUpSeedContentIds = remember(allNextUpSeedCandidates) {
        allNextUpSeedCandidates.mapTo(mutableSetOf()) { candidate -> candidate.content.id }
    }

    val currentNextUpSeedByContentId = remember(allNextUpSeedCandidates) {
        allNextUpSeedCandidates.associate { candidate ->
            candidate.content.id to (candidate.seasonNumber to candidate.episodeNumber)
        }.toMap()
    }

    val visibleContinueWatchingEntries = remember(effectiveWatchProgressEntries) {
        effectiveWatchProgressEntries.continueWatchingEntries(limit = HomeContinueWatchingMaxRecentProgressItems)
    }

    val watchProgressSeedKey = remember(watchProgressUiState.entries) {
        watchProgressUiState.entries.map { entry ->
            Triple(entry.parentMetaId, entry.seasonNumber, entry.episodeNumber)
        }
    }

    ScreenActivityEffect(visibleContinueWatchingEntries) { active ->
        if (!active) return@ScreenActivityEffect
        if (visibleContinueWatchingEntries.any(WatchProgressEntry::isCloudLibraryProgressEntry)) {
            CloudLibraryRepository.ensureLoaded()
        }
    }

    val latestCompletedAtBySeries = remember(allNextUpSeedCandidates) {
        allNextUpSeedCandidates
            .groupBy { candidate -> candidate.content.id }
            .mapValues { (_, candidates) -> candidates.maxOfOrNull { candidate -> candidate.markedAtEpochMs } ?: Long.MIN_VALUE }
    }

    val nextUpSuppressedSeriesIds = remember(visibleContinueWatchingEntries, latestCompletedAtBySeries) {
        visibleContinueWatchingEntries
            .asSequence()
            .filter { entry -> entry.parentMetaType.isSeriesTypeForContinueWatching() }
            .filter { entry ->
                shouldTreatAsActiveInProgressForNextUpSuppression(
                    progress = entry,
                    latestCompletedAt = latestCompletedAtBySeries[entry.parentMetaId],
                )
            }
            .map { entry -> entry.parentMetaId }
            .filter(String::isNotBlank)
            .toSet()
    }

    val completedSeriesCandidates = remember(recentNextUpSeedCandidates, nextUpSuppressedSeriesIds) {
        recentNextUpSeedCandidates.filter { candidate ->
            candidate.content.id !in nextUpSuppressedSeriesIds
        }
    }
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val activeProfileId = profileState.activeProfile?.profileIndex ?: 1
    val cwCacheGeneration by ContinueWatchingEnrichmentCache.generation.collectAsStateWithLifecycle()
    var hasUserScrolledContinueWatching by remember(activeProfileId) { mutableStateOf(false) }
    var hasUserScrolledUpcoming by remember(activeProfileId) { mutableStateOf(false) }

    LaunchedEffect(activeProfileId, continueWatchingListState) {
        snapshotFlow { continueWatchingListState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling) hasUserScrolledContinueWatching = true
        }
    }

    LaunchedEffect(activeProfileId, upcomingListState) {
        snapshotFlow { upcomingListState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling) hasUserScrolledUpcoming = true
        }
    }

    var nextUpItemsBySeries by remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf<Map<String, Pair<Long, ContinueWatchingItem>>>(emptyMap())
    }
    var processedNextUpContentIds by remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf<Set<String>>(emptySet())
    }

    LaunchedEffect(activeProfileId, effectiveWatchProgressSource, cwCacheGeneration) {
        nextUpItemsBySeries = emptyMap()
        processedNextUpContentIds = emptySet()
    }

    var cachedSnapshots by remember(activeProfileId, effectiveWatchProgressSource, cwCacheGeneration) {
        mutableStateOf(
            ContinueWatchingEnrichmentCache.getSnapshots(
                profileId = activeProfileId,
                source = effectiveWatchProgressSource,
            ),
        )
    }
    var cachedProjectionGeneration by remember(activeProfileId, effectiveWatchProgressSource, cwCacheGeneration) {
        mutableStateOf(0)
    }
    ScreenActivityEffect(activeProfileId, effectiveWatchProgressSource, cwCacheGeneration) { active ->
        if (!active) return@ScreenActivityEffect
        cachedSnapshots = ContinueWatchingEnrichmentCache.getSnapshots(
            profileId = activeProfileId,
            source = effectiveWatchProgressSource,
        )
        cachedProjectionGeneration += 1
    }
    val cachedNextUpReleases = remember(cachedSnapshots.first) {
        cachedSnapshots.first.map(::CachedNextUpRelease)
    }
    val shouldValidateMissingNextUpSeeds = remember(
        watchProgressUiState.hasLoadedRemoteProgress,
        watchedUiState.isLoaded,
        watchedUiState.hasLoadedRemoteItems,
        progressProviderOwnsCompletedHistory,
    ) {
        isHomeNextUpSeedSourceLoaded(
            providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
            hasLoadedRemoteProgress = watchProgressUiState.hasLoadedRemoteProgress,
            hasLoadedWatchedItems = watchedUiState.isLoaded,
            hasLoadedRemoteWatchedItems = watchedUiState.hasLoadedRemoteItems,
        )
    }
    val cachedNextUpItems = remember(
        cachedNextUpReleases,
        cachedProjectionGeneration,
        continueWatchingPreferences.dismissedNextUpKeys,
        activeNextUpSeedContentIds,
        currentNextUpSeedByContentId,
        progressProviderOwnsCompletedHistory,
        watchProgressUiState.hasLoadedRemoteProgress,
        shouldValidateMissingNextUpSeeds,
        processedNextUpContentIds,
        nextUpItemsBySeries,
        continueWatchingPreferences.showUnairedNextUp,
        watchedUiState.isLoaded,
        watchProgressUiState.hiddenContentIds,
    ) {
        val nowEpochMs = WatchProgressClock.nowEpochMs()
        cachedNextUpReleases.mapNotNull { cachedRelease ->
            val cached = cachedRelease.item
            if (
                shouldValidateMissingNextUpSeeds &&
                cached.contentId !in activeNextUpSeedContentIds
            ) {
                return@mapNotNull null
            }
            val currentSeed = currentNextUpSeedByContentId[cached.contentId]
            if (currentSeed != null) {
                val (currentSeason, currentEpisode) = currentSeed
                if (
                    hasHomeNextUpSeedChangedFromCache(
                        currentSeason = currentSeason,
                        currentEpisode = currentEpisode,
                        cachedSeason = cached.seedSeason,
                        cachedEpisode = cached.seedEpisode,
                    )
                ) {
                    return@mapNotNull null
                }
            }
            if (
                progressProviderOwnsCompletedHistory &&
                watchProgressUiState.hasLoadedRemoteProgress &&
                cached.contentId in processedNextUpContentIds &&
                cached.contentId !in nextUpItemsBySeries.keys
            ) {
                return@mapNotNull null
            }
            if (nextUpDismissKey(cached.contentId, cached.seedSeason, cached.seedEpisode) in continueWatchingPreferences.dismissedNextUpKeys) {
                return@mapNotNull null
            }
            val releaseEpochMs = cachedRelease.epochMs()
            if (
                !cachedNextUpHasAired(cached, nowEpochMs, releaseEpochMs) &&
                !continueWatchingPreferences.showUnairedNextUp
            ) {
                return@mapNotNull null
            }
            if (
                cached.contentId in watchProgressUiState.hiddenContentIds ||
                WatchProgressRepository.isDroppedShow(cached.contentId)
            ) {
                return@mapNotNull null
            }
            val item = cached.toContinueWatchingItem(releaseEpochMs, nowEpochMs)
            val sortTimestamp = if (item.isReleaseAlert) {
                releaseEpochMs ?: cached.lastWatched
            } else {
                cached.lastWatched
            }
            cached.contentId to (sortTimestamp to item)
        }.toMap()
    }
    val cachedInProgressItems = remember(
        cachedSnapshots.second,
        effectiveWatchProgressSource,
        watchProgressUiState.hiddenContentIds,
    ) {
        cachedSnapshots.second.mapNotNull { cached ->
            if (
                cached.contentId in watchProgressUiState.hiddenContentIds ||
                WatchProgressRepository.isDroppedShow(cached.contentId)
            ) {
                return@mapNotNull null
            }
            cached.resolvedProgressKey() to cached.toContinueWatchingItem()
        }.toMap()
    }

    val effectivNextUpItems = remember(
        nextUpItemsBySeries,
        cachedNextUpItems,
        continueWatchingPreferences.dismissedNextUpKeys,
        activeNextUpSeedContentIds,
        currentNextUpSeedByContentId,
        shouldValidateMissingNextUpSeeds,
        processedNextUpContentIds,
    ) {
        val liveNextUpItems = filterNextUpItemsByCurrentSeeds(
            nextUpItemsBySeries = nextUpItemsBySeries,
            activeSeedContentIds = activeNextUpSeedContentIds,
            currentSeedByContentId = currentNextUpSeedByContentId,
            shouldDropItemsWithoutActiveSeed = shouldValidateMissingNextUpSeeds,
        ).filterValues { (_, item) ->
            nextUpDismissKey(
                item.parentMetaId,
                item.nextUpSeedSeasonNumber,
                item.nextUpSeedEpisodeNumber,
            ) !in continueWatchingPreferences.dismissedNextUpKeys
        }
        mergeHomeNextUpItemsWithCache(
            resolvedItems = liveNextUpItems,
            cachedItems = cachedNextUpItems,
            conclusivelyProcessedContentIds = processedNextUpContentIds,
        )
    }

    val allContinueWatchingItems = remember(
        visibleContinueWatchingEntries,
        cachedInProgressItems,
        effectivNextUpItems,
        nextUpSuppressedSeriesIds,
        continueWatchingPreferences.sortMode,
        cloudLibraryUiState,
    ) {
        buildHomeContinueWatchingItems(
            visibleEntries = visibleContinueWatchingEntries,
            cachedInProgressByProgressKey = cachedInProgressItems,
            nextUpItemsBySeries = effectivNextUpItems,
            nextUpSuppressedSeriesIds = nextUpSuppressedSeriesIds,
            sortMode = continueWatchingPreferences.sortMode,
            todayIsoDate = CurrentDateProvider.todayIsoDate(),
            cloudLibraryUiState = cloudLibraryUiState,
        )
    }
    val (continueWatchingItems, upcomingItems) = remember(
        allContinueWatchingItems,
        continueWatchingPreferences.sortMode,
    ) {
        splitUpcomingItems(
            items = allContinueWatchingItems,
            mode = continueWatchingPreferences.sortMode,
        )
    }
    val hasContinueWatchingRows = continueWatchingItems.isNotEmpty() || upcomingItems.isNotEmpty()

    ScreenActivityEffect(activeProfileId, continueWatchingItems.isNotEmpty(), hasUserScrolledContinueWatching) { active ->
        if (!active) return@ScreenActivityEffect
        if (!hasUserScrolledContinueWatching && continueWatchingItems.isNotEmpty()) {
            snapshotFlow {
                continueWatchingListState.firstVisibleItemIndex to
                    continueWatchingListState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                if (
                    !hasUserScrolledContinueWatching &&
                    !continueWatchingListState.isScrollInProgress &&
                    (index != 0 || offset != 0)
                ) {
                    continueWatchingListState.scrollToItem(0)
                }
            }
        }
    }
    ScreenActivityEffect(activeProfileId, upcomingItems.isNotEmpty(), hasUserScrolledUpcoming) { active ->
        if (!active) return@ScreenActivityEffect
        if (!hasUserScrolledUpcoming && upcomingItems.isNotEmpty()) {
            snapshotFlow {
                upcomingListState.firstVisibleItemIndex to
                    upcomingListState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                if (
                    !hasUserScrolledUpcoming &&
                    !upcomingListState.isScrollInProgress &&
                    (index != 0 || offset != 0)
                ) {
                    upcomingListState.scrollToItem(0)
                }
            }
        }
    }
    val enabledAddons = remember(addonsUiState.addons) {
        addonsUiState.addons.enabledAddons()
    }
    val availableManifests = remember(enabledAddons) {
        enabledAddons.mapNotNull { addon -> addon.manifest }
    }

    val metaProviderKey = remember(availableManifests) {
        availableManifests
            .filter { manifest -> manifest.resources.any { resource -> resource.name == "meta" } }
            .map { manifest -> manifest.transportUrl }
            .sorted()
    }
    val metaProviderReadinessKey = remember(enabledAddons) {
        enabledAddons
            .sortedBy { addon -> addon.manifestUrl }
            .joinToString(separator = "|") { addon ->
                "${addon.manifestUrl}:${addon.manifest != null}:${addon.isRefreshing}:${addon.errorMessage.orEmpty()}"
            }
    }
    var nextUpResolutionRetryAttempt by remember(
        activeProfileId,
        effectiveWatchProgressSource,
        completedSeriesCandidates,
        metaProviderKey,
        metaProviderReadinessKey,
        networkStatusUiState.condition,
        continueWatchingPreferences.showUnairedNextUp,
        continueWatchingPreferences.upNextFromFurthestEpisode,
        continueWatchingPreferences.dismissedNextUpKeys,
        cwCacheGeneration,
    ) {
        mutableStateOf(0)
    }

    ScreenActivityEffect(activeProfileId, collections) { active ->
        if (!active) return@ScreenActivityEffect
        HomeCatalogSettingsRepository.syncCollections(collections)
    }

    val preparedProjectionGeneration = cachedProjectionGeneration
    ScreenActivityEffect(
        preparedProjectionGeneration,
        completedSeriesCandidates,
        metaProviderKey,
        metaProviderReadinessKey,
        networkStatusUiState.condition,
        nextUpResolutionRetryAttempt,
        continueWatchingPreferences.showUnairedNextUp,
        continueWatchingPreferences.upNextFromFurthestEpisode,
        continueWatchingPreferences.dismissedNextUpKeys,
        watchProgressSeedKey,
        visibleContinueWatchingEntries,
        nextUpWatchedItems,
        watchedUiState.isLoaded,
        watchedUiState.hasLoadedRemoteItems,
        watchProgressUiState.hasLoadedRemoteProgress,
        activeProfileId,
        effectiveWatchProgressSource,
        cwCacheGeneration,
    ) { active ->
        if (!active || preparedProjectionGeneration != cachedProjectionGeneration) return@ScreenActivityEffect
        if (
            !isHomeNextUpSeedSourceLoaded(
                providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
                hasLoadedRemoteProgress = watchProgressUiState.hasLoadedRemoteProgress,
                hasLoadedWatchedItems = watchedUiState.isLoaded,
                hasLoadedRemoteWatchedItems = watchedUiState.hasLoadedRemoteItems,
            )
        ) {
            return@ScreenActivityEffect
        }

        if (completedSeriesCandidates.isEmpty()) {
            nextUpItemsBySeries = emptyMap()
            processedNextUpContentIds = emptySet()
            saveContinueWatchingSnapshots(
                profileId = activeProfileId,
                source = effectiveWatchProgressSource,
                cacheGeneration = cwCacheGeneration,
                nextUpItemsBySeries = emptyMap(),
                visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                todayIsoDate = CurrentDateProvider.todayIsoDate(),
                seedLastWatchedMap = emptyMap(),
            )
            return@ScreenActivityEffect
        }

        withContext(Dispatchers.Default) {
            val cachedResolvedNextUpItems = completedSeriesCandidates.mapNotNull { candidate ->
                val cached = cachedNextUpItems[candidate.content.id] ?: return@mapNotNull null
                val item = cached.second
                if (
                    item.nextUpSeedSeasonNumber != candidate.seasonNumber ||
                    item.nextUpSeedEpisodeNumber != candidate.episodeNumber
                ) {
                    return@mapNotNull null
                }
                if (!hasUsableHomeNextUpMetadata(item)) {
                    return@mapNotNull null
                }
                candidate.content.id to cached
            }.toMap()
            val candidatesToResolve = completedSeriesCandidates.filter { candidate ->
                candidate.content.id !in cachedResolvedNextUpItems
            }
            val resolutionPlan = planHomeNextUpResolutionCandidates(candidatesToResolve)
            val resolutionCandidates = resolutionPlan.initialCandidates
            val deferredResolutionCandidates = resolutionPlan.deferredCandidates
            val seedLastWatchedMap = completedSeriesCandidates.associate { it.content.id to it.markedAtEpochMs }
            if (candidatesToResolve.isEmpty()) {
                val cachedResults = mergeHomeNextUpItemsWithCache(
                    resolvedItems = cachedResolvedNextUpItems,
                    cachedItems = cachedNextUpItems,
                    conclusivelyProcessedContentIds = cachedResolvedNextUpItems.keys,
                )
                withContext(Dispatchers.Main) {
                    nextUpItemsBySeries = cachedResults
                    processedNextUpContentIds = cachedResolvedNextUpItems.keys
                }
                saveContinueWatchingSnapshots(
                    profileId = activeProfileId,
                    source = effectiveWatchProgressSource,
                    cacheGeneration = cwCacheGeneration,
                    nextUpItemsBySeries = cachedResults,
                    visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                    todayIsoDate = CurrentDateProvider.todayIsoDate(),
                    seedLastWatchedMap = seedLastWatchedMap,
                )
                return@withContext
            }

            val todayIsoDate = CurrentDateProvider.todayIsoDate()
            val semaphore = Semaphore(NEXT_UP_RESOLUTION_CONCURRENCY)
            val freshResults = mutableMapOf<String, Pair<Long, ContinueWatchingItem>>()
            val processedFreshContentIds = mutableSetOf<String>()

            suspend fun resolveCandidatesStreaming(
                candidates: List<CompletedSeriesCandidate>,
            ) {
                if (candidates.isEmpty()) return

                val results = Channel<HomeNextUpCandidateResolution>(Channel.UNLIMITED)
                candidates.forEach { completedEntry ->
                    launch {
                        val attempt = try {
                            semaphore.withPermit {
                                resolveHomeNextUpCandidate(
                                    completedEntry = completedEntry,
                                    watchProgressEntries = watchProgressUiState.entries,
                                    watchedItems = nextUpWatchedItems,
                                    cachedFallbackItem = cachedNextUpItems[completedEntry.content.id]?.second,
                                    todayIsoDate = todayIsoDate,
                                    preferFurthestEpisode = continueWatchingPreferences.upNextFromFurthestEpisode,
                                    showUnairedNextUp = continueWatchingPreferences.showUnairedNextUp,
                                    dismissedNextUpKeys = continueWatchingPreferences.dismissedNextUpKeys,
                                    providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
                                )
                            }
                        } catch (error: Throwable) {
                            if (error is CancellationException) throw error
                            HomeNextUpResolutionAttempt.transientFailure()
                        }
                        results.send(
                            HomeNextUpCandidateResolution(
                                candidate = completedEntry,
                                attempt = attempt,
                            ),
                        )
                    }
                }

                repeat(candidates.size) {
                    val resolution = results.receive()
                    if (resolution.attempt.isConclusive) {
                        processedFreshContentIds += resolution.candidate.content.id
                    }

                    var changed = false
                    resolution.attempt.resolved?.let { (contentId, item) ->
                        if (cachedResolvedNextUpItems.size + freshResults.size < HomeContinueWatchingMaxRecentProgressItems) {
                            val previous = freshResults.put(contentId, item)
                            changed = previous != item
                        }
                    }

                    if (changed || resolution.attempt.isConclusive) {
                        val resolvedResults = cachedResolvedNextUpItems + freshResults
                        val conclusiveContentIds = cachedResolvedNextUpItems.keys + processedFreshContentIds
                        val progressiveResults = mergeHomeNextUpItemsWithCache(
                            resolvedItems = resolvedResults,
                            cachedItems = cachedNextUpItems,
                            conclusivelyProcessedContentIds = conclusiveContentIds,
                        )
                        withContext(Dispatchers.Main) {
                            nextUpItemsBySeries = progressiveResults
                            processedNextUpContentIds = conclusiveContentIds
                        }
                        saveContinueWatchingSnapshots(
                            profileId = activeProfileId,
                            source = effectiveWatchProgressSource,
                            cacheGeneration = cwCacheGeneration,
                            nextUpItemsBySeries = progressiveResults,
                            visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                            todayIsoDate = todayIsoDate,
                            seedLastWatchedMap = seedLastWatchedMap,
                        )
                    }
                    yield()
                }
                results.close()
            }

            resolveCandidatesStreaming(candidates = resolutionCandidates)

            val resolvedResults = cachedResolvedNextUpItems + freshResults
            val conclusiveContentIds = cachedResolvedNextUpItems.keys + processedFreshContentIds
            val results = mergeHomeNextUpItemsWithCache(
                resolvedItems = resolvedResults,
                cachedItems = cachedNextUpItems,
                conclusivelyProcessedContentIds = conclusiveContentIds,
            )
            withContext(Dispatchers.Main) {
                nextUpItemsBySeries = results
                processedNextUpContentIds = conclusiveContentIds
            }

            saveContinueWatchingSnapshots(
                profileId = activeProfileId,
                source = effectiveWatchProgressSource,
                cacheGeneration = cwCacheGeneration,
                nextUpItemsBySeries = results,
                visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                todayIsoDate = todayIsoDate,
                seedLastWatchedMap = seedLastWatchedMap,
            )

            if (deferredResolutionCandidates.isNotEmpty()) {
                resolveCandidatesStreaming(
                    candidates = deferredResolutionCandidates,
                )

                val deferredResolvedResults = cachedResolvedNextUpItems + freshResults
                val deferredConclusiveContentIds = cachedResolvedNextUpItems.keys + processedFreshContentIds
                val deferredResults = mergeHomeNextUpItemsWithCache(
                    resolvedItems = deferredResolvedResults,
                    cachedItems = cachedNextUpItems,
                    conclusivelyProcessedContentIds = deferredConclusiveContentIds,
                )
                withContext(Dispatchers.Main) {
                    nextUpItemsBySeries = deferredResults
                    processedNextUpContentIds = deferredConclusiveContentIds
                }
                saveContinueWatchingSnapshots(
                    profileId = activeProfileId,
                    source = effectiveWatchProgressSource,
                    cacheGeneration = cwCacheGeneration,
                    nextUpItemsBySeries = deferredResults,
                    visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                    todayIsoDate = todayIsoDate,
                    seedLastWatchedMap = seedLastWatchedMap,
                )
            }

            val transientContentIds = candidatesToResolve
                .asSequence()
                .map { candidate -> candidate.content.id }
                .filterNot { contentId -> contentId in processedFreshContentIds }
                .toList()
            if (
                transientContentIds.isNotEmpty() &&
                nextUpResolutionRetryAttempt < MAX_NEXT_UP_RESOLUTION_RETRIES &&
                networkStatusUiState.condition == NetworkCondition.Online
            ) {
                val retryDelayMs = NEXT_UP_RESOLUTION_RETRY_BASE_DELAY_MS *
                    (1L shl nextUpResolutionRetryAttempt)
                delay(retryDelayMs)
                withContext(Dispatchers.Main) {
                    nextUpResolutionRetryAttempt += 1
                }
            }
        }
    }

    val hasActiveAddons = enabledAddons.any { it.manifest != null }
    val addonManifestsLoading = enabledAddons.any { it.isRefreshing }
    val addonManifestErrorMessage = enabledAddons.firstEnabledManifestError()
    val isResolvingHeroSources = addonManifestsLoading || homeUiState.isLoading
    var firstCatalogReported by remember { mutableStateOf(false) }

    LaunchedEffect(homeUiState.sections.firstOrNull()?.key, onFirstCatalogRendered) {
        if (firstCatalogReported || homeUiState.sections.isEmpty()) return@LaunchedEffect
        firstCatalogReported = true
        onFirstCatalogRendered?.invoke()
    }

    val visibleCollections = remember(collections) {
        collections.filter { it.folders.isNotEmpty() }
    }
    val collectionsMap = remember(visibleCollections) {
        visibleCollections.associateBy { "collection_${it.id}" }
    }
    val sectionsMap = remember(homeUiState.sections) {
        homeUiState.sections.associateBy(HomeCatalogSection::key)
    }
    val enabledHomeItems = remember(homeSettingsUiState.items) {
        homeSettingsUiState.items.filter { it.enabled }
    }
    val keyedEnabledHomeItems = remember(enabledHomeItems) {
        enabledHomeItems.withDuplicateSafeLazyKeys(HomeCatalogSettingsItem::key)
    }
    val resolvedBadgeInputs = remember(activeProfileId, effectiveWatchProgressSource) {
        mutableStateOf<Triple<WatchedUiState, List<WatchProgressEntry>, String>?>(null)
    }
    ScreenActivityEffect(
        activeProfileId,
        effectiveWatchProgressSource,
        watchedUiState,
        watchProgressUiState.entries,
    ) { active ->
        if (!active) return@ScreenActivityEffect
        val inputs = Triple(watchedUiState, watchProgressUiState.entries, CurrentDateProvider.todayIsoDate())
        if (resolvedBadgeInputs.value == inputs) return@ScreenActivityEffect
        if (
            resolveWatchedBadgesBulk(
                watchedItems = watchedUiState.items,
                progressEntries = watchProgressUiState.entries,
                todayIsoDate = inputs.third,
            )
        ) {
            resolvedBadgeInputs.value = inputs
        }
    }
    val hasRenderableCollectionRows = remember(enabledHomeItems, collectionsMap) {
        enabledHomeItems.any { item ->
            item.isCollection && collectionsMap[item.key] != null
        }
    }
    val hasRenderableHomeRows = homeUiState.sections.isNotEmpty() || hasRenderableCollectionRows
    val showHeroSlot = shouldShowHomeHeroSlot(
        heroEnabled = homeSettingsUiState.heroEnabled,
        hasHeroItems = homeUiState.heroItems.isNotEmpty(),
        isResolvingHeroSources = isResolvingHeroSources,
        hasRenderableHomeRows = hasRenderableHomeRows,
    )
    MaintainHomeScrollPosition(
        listState = homeListState,
        profileId = activeProfileId,
        showHeroSlot = showHeroSlot,
    )
    val showHeroSkeleton = showHeroSlot &&
        homeUiState.heroItems.isEmpty() &&
        isResolvingHeroSources
    val isInitialHomeContentLoading = shouldShowInitialHomeLoading(
        hasRenderableHomeRows = hasRenderableHomeRows,
        addonManifestsLoading = addonManifestsLoading,
        homeCatalogLoading = homeUiState.isLoading,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val homeSectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value)
        val posterCardStyle = rememberPosterCardStyleUiState()
        val continueWatchingLayout = rememberContinueWatchingLayout(maxWidth.value, posterCardStyle)
        val nativeBottomNavigationOverlayHeight =
            if (LocalNuvioBottomNavigationOverlayPadding.current > 0.dp) {
                nuvioSafeBottomPadding()
            } else {
                0.dp
            }
        val mobileHeroBelowSectionHeightHint = remember(
            maxWidth.value,
            continueWatchingPreferences.isVisible,
            continueWatchingPreferences.style,
            hasContinueWatchingRows,
            continueWatchingLayout,
            posterCardStyle.widthDp,
            nativeBottomNavigationOverlayHeight,
        ) {
            if (
                maxWidth.value < 600f &&
                continueWatchingPreferences.isVisible &&
                hasContinueWatchingRows
            ) {
                continueWatchingHeroViewportReserveHeight(
                    style = continueWatchingPreferences.style,
                    layout = continueWatchingLayout,
                    basePosterWidthDp = posterCardStyle.widthDp,
                ) + nativeBottomNavigationOverlayHeight
            } else {
                null
            }
        }

        // No title: Home's hero is its heading, so it gets no app bar.
        NuvioScreen(
            modifier = Modifier.fillMaxSize(),
            horizontalPadding = 0.dp,
            topPadding = if (showHeroSlot) 0.dp else null,
            listState = homeListState,
        ) {
            if (showHeroSlot) {
                item(key = "home_hero", contentType = "hero") {
                    Crossfade(
                        targetState = showHeroSkeleton,
                        animationSpec = tween(320),
                        label = "HomeHeroLoading",
                    ) { isLoading ->
                        when {
                            isLoading -> HomeSkeletonHero(
                                modifier = Modifier,
                                viewportHeight = maxHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                            )

                            homeUiState.heroItems.isNotEmpty() -> HomeHeroSection(
                                items = homeUiState.heroItems,
                                modifier = Modifier,
                                viewportHeight = maxHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                                onItemClick = onPosterClick,
                            )

                            else -> HomeHeroReservedSpace(
                                modifier = Modifier,
                                viewportHeight = maxHeight,
                                mobileBelowSectionHeightHint = mobileHeroBelowSectionHeightHint,
                            )
                        }
                    }
                }
            }

            when {
                isInitialHomeContentLoading -> {
                    homeContinueWatchingSections(
                        preferences = continueWatchingPreferences,
                        continueWatchingItems = continueWatchingItems,
                        upcomingItems = upcomingItems,
                        dataSourceKey = effectiveWatchProgressSource,
                        sectionPadding = homeSectionPadding,
                        layout = continueWatchingLayout,
                        continueWatchingListState = continueWatchingListState,
                        upcomingListState = upcomingListState,
                        onItemClick = onContinueWatchingClick,
                        onItemLongPress = onContinueWatchingLongPress,
                        disintegrationRequest = continueWatchingDisintegrationRequest,
                    )
                    items(
                        count = 3,
                        key = { "home_skeleton_$it" },
                        contentType = { "skeleton" },
                    ) {
                        HomeSkeletonRow(
                            horizontalPadding = homeSectionPadding,
                        )
                    }
                }

                !hasActiveAddons && !hasRenderableCollectionRows -> {
                    homeContinueWatchingSections(
                        preferences = continueWatchingPreferences,
                        continueWatchingItems = continueWatchingItems,
                        upcomingItems = upcomingItems,
                        dataSourceKey = effectiveWatchProgressSource,
                        sectionPadding = homeSectionPadding,
                        layout = continueWatchingLayout,
                        continueWatchingListState = continueWatchingListState,
                        upcomingListState = upcomingListState,
                        onItemClick = onContinueWatchingClick,
                        onItemLongPress = onContinueWatchingLongPress,
                        disintegrationRequest = continueWatchingDisintegrationRequest,
                    )
                    item(key = "home_empty", contentType = "empty") {
                        when {
                            networkStatusUiState.isOfflineLike && addonManifestErrorMessage != null -> {
                                NuvioNetworkOfflineCard(
                                    condition = networkStatusUiState.condition,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onRetry = {
                                        NetworkStatusRepository.requestRefresh(force = true)
                                        AddonRepository.refreshAll()
                                    },
                                )
                            }

                            addonManifestErrorMessage != null -> {
                                HomeEmptyStateCard(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = stringResource(Res.string.home_load_failed_title),
                                    message = addonManifestErrorMessage,
                                    actionLabel = stringResource(Res.string.action_retry),
                                    onActionClick = {
                                        NetworkStatusRepository.requestRefresh(force = true)
                                        AddonRepository.refreshAll()
                                    },
                                )
                            }

                            else -> {
                                EmptyState(
                                    icon = Icons.Rounded.Extension,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    title = stringResource(Res.string.compose_search_empty_no_active_addons_title),
                                    message = stringResource(Res.string.home_empty_no_active_addons_message),
                                )
                            }
                        }
                    }
                }

                homeUiState.sections.isEmpty() && homeUiState.heroItems.isEmpty() &&
                    (!continueWatchingPreferences.isVisible || !hasContinueWatchingRows) &&
                    !hasRenderableCollectionRows -> {
                    item(key = "home_empty", contentType = "empty") {
                        val loadFailed = !homeUiState.errorMessage.isNullOrBlank()
                        if (networkStatusUiState.isOfflineLike && loadFailed) {
                            NuvioNetworkOfflineCard(
                                condition = networkStatusUiState.condition,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onRetry = {
                                    NetworkStatusRepository.requestRefresh(force = true)
                                    HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                                },
                            )
                        } else {
                            HomeEmptyStateCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = stringResource(
                                    if (loadFailed) {
                                        Res.string.home_load_failed_title
                                    } else {
                                        Res.string.home_empty_no_rows_title
                                    },
                                ),
                                message = homeUiState.errorMessage
                                    ?: stringResource(Res.string.home_empty_no_rows_message),
                                actionLabel = if (loadFailed) stringResource(Res.string.action_retry) else null,
                                onActionClick = if (loadFailed) {
                                    {
                                        NetworkStatusRepository.requestRefresh(force = true)
                                        HomeRepository.refresh(addonsUiState.addons.enabledAddons(), force = true)
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }

                else -> {
                    homeContinueWatchingSections(
                        preferences = continueWatchingPreferences,
                        continueWatchingItems = continueWatchingItems,
                        upcomingItems = upcomingItems,
                        dataSourceKey = effectiveWatchProgressSource,
                        sectionPadding = homeSectionPadding,
                        layout = continueWatchingLayout,
                        continueWatchingListState = continueWatchingListState,
                        upcomingListState = upcomingListState,
                        onItemClick = onContinueWatchingClick,
                        onItemLongPress = onContinueWatchingLongPress,
                        disintegrationRequest = continueWatchingDisintegrationRequest,
                    )

                    keyedEnabledHomeItems.forEach { keyedSettingsItem ->
                        val settingsItem = keyedSettingsItem.value
                        if (settingsItem.isCollection) {
                            val collection = collectionsMap[settingsItem.key]
                            if (collection != null) {
                                item(key = keyedSettingsItem.lazyKey, contentType = "collection") {
                                    HomeCollectionRowSection(
                                        collection = collection,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        sectionPadding = homeSectionPadding,
                                        animateGifs = animateCollectionGifs,
                                        onFolderClick = onFolderClick,
                                    )
                                }
                            }
                        } else {
                            val section = sectionsMap[settingsItem.key]
                            if (section != null && section.items.isNotEmpty()) {
                                item(key = keyedSettingsItem.lazyKey, contentType = "catalog") {
                                    HomeCatalogRowSection(
                                        section = section,
                                        entries = section.items.take(HOME_CATALOG_PREVIEW_LIMIT),
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        sectionPadding = homeSectionPadding,
                                        onViewAllClick = if (section.canOpenCatalog(HOME_CATALOG_PREVIEW_LIMIT)) {
                                            onCatalogClick?.let { { it(section) } }
                                        } else {
                                            null
                                        },
                                        watchedKeys = watchedUiState.watchedKeys,
                                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                        onPosterClick = onPosterClick,
                                        onPosterLongClick = onPosterLongClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.homeContinueWatchingSections(
    preferences: ContinueWatchingPreferencesUiState,
    continueWatchingItems: List<ContinueWatchingItem>,
    upcomingItems: List<ContinueWatchingItem>,
    dataSourceKey: WatchProgressSource,
    sectionPadding: Dp,
    layout: ContinueWatchingLayout,
    continueWatchingListState: LazyListState,
    upcomingListState: LazyListState,
    onItemClick: ((ContinueWatchingItem) -> Unit)?,
    onItemLongPress: ((ContinueWatchingItem) -> Unit)?,
    disintegrationRequest: DisintegrationRequest<String>?,
) {
    if (!preferences.isVisible) return

    if (continueWatchingItems.isNotEmpty()) {
        item(key = HOME_CONTINUE_WATCHING_SECTION_KEY, contentType = "continue_watching") {
            HomeContinueWatchingSection(
                items = continueWatchingItems,
                dataSourceKey = dataSourceKey,
                style = preferences.style,
                useEpisodeThumbnails = preferences.useEpisodeThumbnails,
                blurNextUp = preferences.blurNextUp,
                modifier = Modifier.padding(bottom = HomeContinueWatchingSectionBottomPadding),
                sectionPadding = sectionPadding,
                layout = layout,
                listState = continueWatchingListState,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                disintegrationRequest = disintegrationRequest,
            )
        }
    }

    if (upcomingItems.isNotEmpty()) {
        item(key = HOME_UPCOMING_SECTION_KEY, contentType = "continue_watching") {
            HomeContinueWatchingSection(
                items = upcomingItems,
                dataSourceKey = dataSourceKey,
                style = preferences.style,
                useEpisodeThumbnails = preferences.useEpisodeThumbnails,
                blurNextUp = preferences.blurNextUp,
                modifier = Modifier.padding(bottom = HomeContinueWatchingSectionBottomPadding),
                title = stringResource(Res.string.upcoming_section_title),
                sectionPadding = sectionPadding,
                layout = layout,
                listState = upcomingListState,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                disintegrationRequest = disintegrationRequest,
            )
        }
    }
}

private const val HOME_CATALOG_PREVIEW_LIMIT = 18

private const val HOME_CONTINUE_WATCHING_SECTION_KEY = "home_continue_watching"

private const val HOME_UPCOMING_SECTION_KEY = "home_upcoming"

private const val NEXT_UP_RESOLUTION_CONCURRENCY = 4

private const val MAX_NEXT_UP_RESOLUTION_RETRIES = 3

private const val NEXT_UP_RESOLUTION_RETRY_BASE_DELAY_MS = 1_500L

private fun String.isHomeSeriesLikeType(): Boolean =
    trim().lowercase() in setOf("series", "show", "tv", "tvshow")

internal fun filterHomeNextUpCandidatesForContinueWatchingWindow(
    candidates: List<CompletedSeriesCandidate>,
    cutoffEpochMs: Long?,
): List<CompletedSeriesCandidate> = cutoffEpochMs
    ?.let { cutoff -> candidates.filter { candidate -> candidate.markedAtEpochMs >= cutoff } }
    ?: candidates

private suspend fun resolveHomeNextUpCandidate(
    completedEntry: CompletedSeriesCandidate,
    watchProgressEntries: List<WatchProgressEntry>,
    watchedItems: List<WatchedItem>,
    cachedFallbackItem: ContinueWatchingItem?,
    todayIsoDate: String,
    preferFurthestEpisode: Boolean,
    showUnairedNextUp: Boolean,
    dismissedNextUpKeys: Set<String>,
    providerOwnsCompletedHistory: Boolean,
): HomeNextUpResolutionAttempt {
    val contentId = completedEntry.content.id
    val meta = try {
        MetaDetailsRepository.fetch(
            type = completedEntry.content.type,
            id = contentId,
        )
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        null
    }
    if (meta == null) {
        return HomeNextUpResolutionAttempt.transientFailure()
    }

    val resolvedProgressEntries = WatchProgressRepository.prepareNextUpProgressEntries(
        entries = watchProgressEntries,
        contentId = contentId,
    )
    val resolvedWatchedItems = watchedItems
    val resolvedWatchedKeys = resolvedWatchedItems.mapTo(linkedSetOf()) { item ->
        watchedItemKey(item.type, item.id, item.season, item.episode)
    }

    if (!providerOwnsCompletedHistory) {
        WatchedRepository.reconcileFullyWatchedSeriesState(
            meta = meta,
            todayIsoDate = todayIsoDate,
            isEpisodeWatched = { episode ->
                watchedItemKey(meta.type, meta.id, episode.season, episode.episode) in resolvedWatchedKeys
            },
            isEpisodeCompleted = { episode ->
                val playbackId = meta.episodePlaybackId(episode)
                resolvedProgressEntries.any { entry ->
                    entry.videoId == playbackId && entry.isEffectivelyCompleted
                }
            },
        )
    }

    val action = meta.seriesPrimaryAction(
        content = completedEntry.content,
        entries = resolvedProgressEntries,
        watchedItems = resolvedWatchedItems,
        todayIsoDate = todayIsoDate,
        preferFurthestEpisode = preferFurthestEpisode,
        showUnairedNextUp = showUnairedNextUp,
    )
    if (action == null) {
        return HomeNextUpResolutionAttempt.conclusiveNone()
    }
    if (action.resumePositionMs != null) {
        return HomeNextUpResolutionAttempt.conclusiveNone()
    }

    val nextEpisode = meta.videoForSeriesAction(action)
    if (nextEpisode == null) {
        return HomeNextUpResolutionAttempt.conclusiveNone()
    }
    val metadataDecision = classifyHomeNextUpCandidateMetadata(
        freshItem = completedEntry.toContinueWatchingSeed(meta)
            .toUpNextContinueWatchingItem(nextEpisode),
        cachedFallbackItem = cachedFallbackItem,
        dismissedNextUpKeys = dismissedNextUpKeys,
    )
    val item = metadataDecision.item
    if (metadataDecision.outcome == HomeNextUpCandidateMetadataOutcome.Dismissed) {
        return HomeNextUpResolutionAttempt.conclusiveNone()
    }
    if (metadataDecision.outcome == HomeNextUpCandidateMetadataOutcome.Transient) {
        return HomeNextUpResolutionAttempt.transientFailure()
    }

    val sortTimestamp = if (item.isReleaseAlert) {
        com.nuvio.app.core.watch.progress.parseReleaseDateToEpochMs(item.released) ?: completedEntry.markedAtEpochMs
    } else {
        completedEntry.markedAtEpochMs
    }
    return HomeNextUpResolutionAttempt.success(
        contentId to (sortTimestamp to item),
    )
}

private fun MetaDetails.videoForSeriesAction(action: SeriesPrimaryAction): MetaVideo? {
    if (action.seasonNumber != null && action.episodeNumber != null) {
        videos.firstOrNull { video ->
            video.season == action.seasonNumber &&
                video.episode == action.episodeNumber
        }?.let { return it }
    }
    return videos.firstOrNull { video ->
        com.nuvio.app.core.watch.progress.buildPlaybackVideoId(
            parentMetaId = id,
            seasonNumber = video.season,
            episodeNumber = video.episode,
            fallbackVideoId = video.id,
        ) == action.videoId || video.id == action.videoId
    }
}

private fun shouldTreatAsActiveInProgressForNextUpSuppression(
    progress: WatchProgressEntry,
    latestCompletedAt: Long?,
): Boolean {
    if (!progress.shouldTreatAsInProgressForContinueWatching()) return false
    if (latestCompletedAt == null || latestCompletedAt == Long.MIN_VALUE) return true
    return progress.lastUpdatedEpochMs >= latestCompletedAt
}

private data class HomeNextUpCandidateResolution(
    val candidate: CompletedSeriesCandidate,
    val attempt: HomeNextUpResolutionAttempt,
)

private data class HomeNextUpResolutionAttempt(
    val resolved: Pair<String, Pair<Long, ContinueWatchingItem>>?,
    val isConclusive: Boolean,
) {
    companion object {
        fun success(
            resolved: Pair<String, Pair<Long, ContinueWatchingItem>>,
        ): HomeNextUpResolutionAttempt =
            HomeNextUpResolutionAttempt(
                resolved = resolved,
                isConclusive = true,
            )

        fun conclusiveNone(): HomeNextUpResolutionAttempt =
            HomeNextUpResolutionAttempt(
                resolved = null,
                isConclusive = true,
            )

        fun transientFailure(): HomeNextUpResolutionAttempt =
            HomeNextUpResolutionAttempt(
                resolved = null,
                isConclusive = false,
            )
    }
}

private fun saveContinueWatchingSnapshots(
    profileId: Int,
    source: WatchProgressSource,
    cacheGeneration: Int,
    nextUpItemsBySeries: Map<String, Pair<Long, ContinueWatchingItem>>,
    visibleContinueWatchingEntries: List<WatchProgressEntry>,
    todayIsoDate: String,
    seedLastWatchedMap: Map<String, Long>,
) {
    val nextUpCache = nextUpItemsBySeries.mapNotNull { (contentId, pair) ->
        val item = pair.second
        CachedNextUpItem(
            contentId = contentId,
            contentType = item.parentMetaType,
            name = item.title,
            poster = item.poster,
            backdrop = item.background,
            logo = item.logo,
            videoId = item.videoId,
            season = item.seasonNumber,
            episode = item.episodeNumber,
            episodeTitle = item.episodeTitle,
            episodeThumbnail = item.episodeThumbnail,
            pauseDescription = item.pauseDescription,
            released = item.released,
            hasAired = item.released?.let { released ->
                isReleasedBy(todayIsoDate = todayIsoDate, releasedDate = released)
            } ?: true,
            lastWatched = seedLastWatchedMap[contentId] ?: pair.first,
            sortTimestamp = pair.first,
            seedSeason = item.nextUpSeedSeasonNumber,
            seedEpisode = item.nextUpSeedEpisodeNumber,
            isReleaseAlert = item.isReleaseAlert,
            isNewSeasonRelease = item.isNewSeasonRelease,
        )
    }
    val inProgressCache = buildHomeInProgressCacheSnapshot(
        visibleEntries = visibleContinueWatchingEntries,
        cachedEntries = ContinueWatchingEnrichmentCache.getInProgressSnapshot(
            profileId = profileId,
            source = source,
        ),
    )
    ContinueWatchingEnrichmentCache.saveSnapshots(
        profileId = profileId,
        source = source,
        generation = cacheGeneration,
        nextUp = nextUpCache,
        inProgress = inProgressCache,
    )
}

private fun CompletedSeriesCandidate.toContinueWatchingSeed(meta: com.nuvio.app.core.metadata.MetaDetails) =
    WatchProgressEntry(
        contentType = content.type,
        parentMetaId = content.id,
        parentMetaType = content.type,
        videoId = "${content.id}:${seasonNumber}:${episodeNumber}",
        title = meta.name,
        logo = meta.logo,
        poster = meta.poster,
        background = meta.background,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        lastPositionMs = 0L,
        durationMs = 0L,
        lastUpdatedEpochMs = markedAtEpochMs,
        isCompleted = true,
    )

private fun WatchProgressEntry.isCloudLibraryProgressEntry(): Boolean =
    contentType.equals(CloudLibraryContentType, ignoreCase = true) ||
        parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)
