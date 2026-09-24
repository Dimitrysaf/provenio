package com.nuvio.app.shell.screens.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.addons.ManagedAddon
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.cloud.CloudLibraryRepository
import com.nuvio.app.core.cloud.CloudLibraryUiState
import com.nuvio.app.core.home.CachedNextUpRelease
import com.nuvio.app.core.home.HomeContinueWatchingMaxRecentProgressItems
import com.nuvio.app.core.home.buildHomeContinueWatchingItems
import com.nuvio.app.core.home.buildHomeNextUpSeedCandidates
import com.nuvio.app.core.home.cachedNextUpHasAired
import com.nuvio.app.core.home.filterEntriesForContinueWatchingWindow
import com.nuvio.app.core.home.filterNextUpItemsByCurrentSeeds
import com.nuvio.app.core.home.hasHomeNextUpSeedChangedFromCache
import com.nuvio.app.core.home.isHomeNextUpSeedSourceLoaded
import com.nuvio.app.core.home.mergeHomeNextUpItemsWithCache
import com.nuvio.app.core.home.splitUpcomingItems
import com.nuvio.app.core.home.toContinueWatchingItem
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.watch.progress.ContinueWatchingEnrichmentCache
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesUiState
import com.nuvio.app.core.watch.progress.CurrentDateProvider
import com.nuvio.app.core.watch.progress.WatchProgressClock
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.core.watch.progress.WatchProgressRepository
import com.nuvio.app.core.watch.progress.WatchProgressUiState
import com.nuvio.app.core.watch.progress.continueWatchingEntries
import com.nuvio.app.core.watch.progress.isSeriesTypeForContinueWatching
import com.nuvio.app.core.watch.progress.nextUpDismissKey
import com.nuvio.app.core.watch.progress.resolvedProgressKey
import com.nuvio.app.core.watch.progress.toContinueWatchingItem
import com.nuvio.app.core.watch.watched.WatchedUiState
import com.nuvio.app.shell.components.ScreenActivityEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nuvio.composeapp.generated.resources.*

// The continue watching and upcoming rows: in-progress entries plus the next up episode of finished ones.
internal data class HomeContinueWatchingRows(
    val continueWatching: List<ContinueWatchingItem>,
    val upcoming: List<ContinueWatchingItem>,
)

// Builds the rows from progress and watched history, resolving next up episodes in the background.
@Composable
internal fun rememberHomeContinueWatchingRows(
    activeProfileId: Int,
    watchProgressUiState: WatchProgressUiState,
    watchedUiState: WatchedUiState,
    continueWatchingPreferences: ContinueWatchingPreferencesUiState,
    continueWatchingDaysCap: Int,
    cloudLibraryUiState: CloudLibraryUiState,
    enabledAddons: List<ManagedAddon>,
    networkCondition: NetworkCondition,
): HomeContinueWatchingRows {
    val effectiveWatchProgressSource = watchProgressUiState.source
    val cwCacheGeneration by ContinueWatchingEnrichmentCache.generation.collectAsStateWithLifecycle()

    val progressProviderOwnsCompletedHistory = remember(effectiveWatchProgressSource) {
        WatchProgressRepository.activeProviderOwnsCompletedHistoryProjection()
    }
    val continueWatchingCutoffEpochMs = remember(
        effectiveWatchProgressSource,
        continueWatchingDaysCap,
    ) {
        WatchProgressRepository.activeProviderContinueWatchingCutoffEpochMs(
            daysCap = continueWatchingDaysCap,
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
        networkCondition,
        continueWatchingPreferences.showUnairedNextUp,
        continueWatchingPreferences.upNextFromFurthestEpisode,
        continueWatchingPreferences.dismissedNextUpKeys,
        cwCacheGeneration,
    ) {
        mutableStateOf(0)
    }

    val preparedProjectionGeneration = cachedProjectionGeneration
    ScreenActivityEffect(
        preparedProjectionGeneration,
        completedSeriesCandidates,
        metaProviderKey,
        metaProviderReadinessKey,
        networkCondition,
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

        val seedLastWatchedMap = completedSeriesCandidates.associate { it.content.id to it.markedAtEpochMs }
        val transientContentIds = withContext(Dispatchers.Default) {
            resolveHomeNextUpItems(
                completedSeriesCandidates = completedSeriesCandidates,
                cachedNextUpItems = cachedNextUpItems,
                watchProgressEntries = watchProgressUiState.entries,
                watchedItems = nextUpWatchedItems,
                preferFurthestEpisode = continueWatchingPreferences.upNextFromFurthestEpisode,
                showUnairedNextUp = continueWatchingPreferences.showUnairedNextUp,
                dismissedNextUpKeys = continueWatchingPreferences.dismissedNextUpKeys,
                providerOwnsCompletedHistory = progressProviderOwnsCompletedHistory,
            ) { items, processedContentIds, todayIsoDate ->
                withContext(Dispatchers.Main) {
                    nextUpItemsBySeries = items
                    processedNextUpContentIds = processedContentIds
                }
                saveContinueWatchingSnapshots(
                    profileId = activeProfileId,
                    source = effectiveWatchProgressSource,
                    cacheGeneration = cwCacheGeneration,
                    nextUpItemsBySeries = items,
                    visibleContinueWatchingEntries = visibleContinueWatchingEntries,
                    todayIsoDate = todayIsoDate,
                    seedLastWatchedMap = seedLastWatchedMap,
                )
            }
        }
        if (
            transientContentIds.isNotEmpty() &&
            nextUpResolutionRetryAttempt < MAX_NEXT_UP_RESOLUTION_RETRIES &&
            networkCondition == NetworkCondition.Online
        ) {
            delay(NEXT_UP_RESOLUTION_RETRY_BASE_DELAY_MS * (1L shl nextUpResolutionRetryAttempt))
            nextUpResolutionRetryAttempt += 1
        }
    }

    return HomeContinueWatchingRows(continueWatching = continueWatchingItems, upcoming = upcomingItems)
}

// Holds a row at its first item as items arrive, until the user scrolls it themselves.
@Composable
internal fun KeepRowAtStartUntilScrolled(
    listState: LazyListState,
    profileId: Int,
    hasItems: Boolean,
) {
    var hasUserScrolled by remember(profileId) { mutableStateOf(false) }
    LaunchedEffect(profileId, listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (isScrolling) hasUserScrolled = true
        }
    }
    ScreenActivityEffect(profileId, hasItems, hasUserScrolled) { active ->
        if (!active) return@ScreenActivityEffect
        if (!hasUserScrolled && hasItems) {
            snapshotFlow {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }.collect { (index, offset) ->
                if (
                    !hasUserScrolled &&
                    !listState.isScrollInProgress &&
                    (index != 0 || offset != 0)
                ) {
                    listState.scrollToItem(0)
                }
            }
        }
    }
}
