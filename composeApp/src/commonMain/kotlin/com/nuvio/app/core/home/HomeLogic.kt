package com.nuvio.app.core.home

import com.nuvio.app.core.watch.watched.WatchedItem
import com.nuvio.app.core.watch.progress.CachedNextUpItem
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.ContinueWatchingSortMode
import com.nuvio.app.core.watch.progress.isMalformedNextUpSeedContentId
import com.nuvio.app.core.watch.progress.isSeriesTypeForContinueWatching
import com.nuvio.app.core.watch.progress.parseReleaseDateToEpochMs
import com.nuvio.app.core.watch.progress.shouldUseAsCompletedSeedForContinueWatching
import com.nuvio.app.core.watch.progress.WatchProgressClock
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.core.watch.watching.application.WatchingState
import com.nuvio.app.core.watch.watching.domain.WatchingContentRef
import nuvio.composeapp.generated.resources.*
import com.nuvio.app.core.cloud.CloudLibraryContentType
import com.nuvio.app.core.cloud.CloudLibraryUiState
import com.nuvio.app.core.cloud.findPlaybackTargetForProgress
import com.nuvio.app.core.watch.progress.CachedInProgressItem
import com.nuvio.app.core.watch.progress.nextUpDismissKey
import com.nuvio.app.core.watch.progress.resolvedProgressKey
import com.nuvio.app.core.watch.progress.buildContinueWatchingEpisodeSubtitle
import com.nuvio.app.core.watch.progress.toContinueWatchingItem
import com.nuvio.app.core.watch.watching.domain.isReleasedBy

internal const val HomeContinueWatchingMaxRecentProgressItems = 300

internal const val HomeNextUpInitialResolutionLimit = 32

internal data class HomeNextUpResolutionPlan(
    val initialCandidates: List<CompletedSeriesCandidate>,
    val deferredCandidates: List<CompletedSeriesCandidate>,
)

internal fun planHomeNextUpResolutionCandidates(
    candidates: List<CompletedSeriesCandidate>,
): HomeNextUpResolutionPlan =
    HomeNextUpResolutionPlan(
        initialCandidates = candidates.take(HomeNextUpInitialResolutionLimit),
        deferredCandidates = candidates.drop(HomeNextUpInitialResolutionLimit),
    )

internal fun filterEntriesForContinueWatchingWindow(
    entries: List<WatchProgressEntry>,
    cutoffEpochMs: Long?,
): List<WatchProgressEntry> = cutoffEpochMs
    ?.let { cutoff -> entries.filter { entry -> entry.lastUpdatedEpochMs >= cutoff } }
    ?: entries

internal fun buildHomeNextUpSeedCandidates(
    progressEntries: List<WatchProgressEntry>,
    watchedItems: List<WatchedItem>,
    providerOwnsCompletedHistory: Boolean,
    preferFurthestEpisode: Boolean,
    nowEpochMs: Long,
    shouldUseProgressSeed: (WatchProgressEntry, Long) -> Boolean = { entry, _ ->
        entry.shouldUseAsCompletedSeedForContinueWatching()
    },
    isContentHidden: (String) -> Boolean = { false },
): List<CompletedSeriesCandidate> {
    val progressSeeds = progressEntries
        .asSequence()
        .filterNot { entry -> isContentHidden(entry.parentMetaId) }
        .filter { entry -> entry.parentMetaType.isSeriesTypeForContinueWatching() }
        .filter { entry -> entry.seasonNumber != null && entry.episodeNumber != null && entry.seasonNumber != 0 }
        .filter { entry -> !isMalformedNextUpSeedContentId(entry.parentMetaId) }
        .filter { entry -> shouldUseProgressSeed(entry, nowEpochMs) }
        .toList()
    val watchedSeeds = if (providerOwnsCompletedHistory) {
        emptyList()
    } else {
        watchedItems.filter { item ->
            !isContentHidden(item.id) &&
                item.type.isSeriesTypeForContinueWatching() &&
                item.season != null &&
                item.episode != null &&
                item.season != 0 &&
                !isMalformedNextUpSeedContentId(item.id)
        }
    }

    return WatchingState.latestCompletedBySeries(
        progressEntries = progressSeeds,
        watchedItems = watchedSeeds,
        preferFurthestEpisode = preferFurthestEpisode,
    ).mapNotNull { (content, completed) ->
        if (!content.type.isSeriesTypeForContinueWatching()) return@mapNotNull null
        if (completed.seasonNumber == 0) return@mapNotNull null
        if (isMalformedNextUpSeedContentId(content.id)) return@mapNotNull null
        CompletedSeriesCandidate(
            content = content,
            seasonNumber = completed.seasonNumber,
            episodeNumber = completed.episodeNumber,
            markedAtEpochMs = completed.markedAtEpochMs,
        )
    }.sortedWith(
        compareByDescending<CompletedSeriesCandidate> { candidate -> candidate.markedAtEpochMs }
            .thenByDescending { candidate -> candidate.seasonNumber }
            .thenByDescending { candidate -> candidate.episodeNumber },
    )
}

internal fun filterNextUpItemsByCurrentSeeds(
    nextUpItemsBySeries: Map<String, Pair<Long, ContinueWatchingItem>>,
    activeSeedContentIds: Set<String>,
    currentSeedByContentId: Map<String, Pair<Int, Int>>,
    shouldDropItemsWithoutActiveSeed: Boolean,
): Map<String, Pair<Long, ContinueWatchingItem>> =
    nextUpItemsBySeries.filter { (contentId, pair) ->
        if (shouldDropItemsWithoutActiveSeed && contentId !in activeSeedContentIds) {
            return@filter false
        }
        val item = pair.second
        val currentSeed = currentSeedByContentId[contentId] ?: return@filter true
        item.nextUpSeedSeasonNumber == currentSeed.first &&
            item.nextUpSeedEpisodeNumber == currentSeed.second
    }

internal fun isHomeNextUpSeedSourceLoaded(
    providerOwnsCompletedHistory: Boolean,
    hasLoadedRemoteProgress: Boolean,
    hasLoadedWatchedItems: Boolean,
    hasLoadedRemoteWatchedItems: Boolean,
): Boolean = hasLoadedRemoteProgress && (
    providerOwnsCompletedHistory || (hasLoadedWatchedItems && hasLoadedRemoteWatchedItems)
)

internal fun cachedNextUpHasAired(
    cached: CachedNextUpItem,
    nowEpochMs: Long = WatchProgressClock.nowEpochMs(),
    releaseEpochMs: Long? = com.nuvio.app.core.watch.progress.parseReleaseDateToEpochMs(cached.released),
): Boolean =
    releaseEpochMs?.let { nowEpochMs >= it }
        ?: cached.hasAired

internal fun hasHomeNextUpSeedChangedFromCache(
    currentSeason: Int,
    currentEpisode: Int,
    cachedSeason: Int?,
    cachedEpisode: Int?,
): Boolean {
    if (cachedSeason == null || cachedEpisode == null) return false
    return currentSeason != cachedSeason || currentEpisode != cachedEpisode
}

internal fun hasUsableHomeNextUpMetadata(item: ContinueWatchingItem): Boolean {
    val hasResolvedTitle = item.title.isNotBlank() &&
        !item.title.equals(item.parentMetaId, ignoreCase = true)
    val hasArtwork = listOf(
        item.imageUrl,
        item.poster,
        item.background,
        item.episodeThumbnail,
    ).any { value -> !value.isNullOrBlank() }
    return hasResolvedTitle && hasArtwork
}

internal enum class HomeNextUpCandidateMetadataOutcome {
    Ready,
    Dismissed,
    Transient,
}

internal data class HomeNextUpCandidateMetadataDecision(
    val item: ContinueWatchingItem,
    val outcome: HomeNextUpCandidateMetadataOutcome,
)

/**
 * Splits unaired next-up episodes into a dedicated row when [ContinueWatchingSortMode.SPLIT_UPCOMING]
 * is active. The main row keeps its recency ordering, while Upcoming is ordered by the nearest
 * release instant with unknown dates last.
 */
internal fun splitUpcomingItems(
    items: List<ContinueWatchingItem>,
    mode: ContinueWatchingSortMode,
    nowEpochMs: Long = WatchProgressClock.nowEpochMs(),
): Pair<List<ContinueWatchingItem>, List<ContinueWatchingItem>> {
    if (mode != ContinueWatchingSortMode.SPLIT_UPCOMING) {
        return items to emptyList()
    }

    val (upcoming, main) = items.partition { item ->
        item.isNextUp && parseReleaseDateToEpochMs(item.released)
            ?.let { releaseEpochMs -> releaseEpochMs > nowEpochMs } == true
    }
    val sortedUpcoming = upcoming.sortedWith { first, second ->
        val firstRelease = parseReleaseDateToEpochMs(first.released)
        val secondRelease = parseReleaseDateToEpochMs(second.released)
        when {
            firstRelease == null && secondRelease == null -> 0
            firstRelease == null -> 1
            secondRelease == null -> -1
            else -> firstRelease.compareTo(secondRelease)
        }
    }
    return main to sortedUpcoming
}

internal data class CompletedSeriesCandidate(
    val content: WatchingContentRef,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val markedAtEpochMs: Long,
)

internal fun mergeHomeNextUpItemsWithCache(
    resolvedItems: Map<String, Pair<Long, ContinueWatchingItem>>,
    cachedItems: Map<String, Pair<Long, ContinueWatchingItem>>,
    conclusivelyProcessedContentIds: Set<String>,
): Map<String, Pair<Long, ContinueWatchingItem>> {
    val retainedCachedItems = cachedItems.filterKeys { contentId ->
        contentId !in conclusivelyProcessedContentIds || contentId in resolvedItems
    }
    val resolvedItemsWithCacheFallback = resolvedItems.mapValues { (contentId, pair) ->
        pair.first to pair.second.withFallbackMetadata(cachedItems[contentId]?.second)
    }
    return retainedCachedItems + resolvedItemsWithCacheFallback
}

internal fun classifyHomeNextUpCandidateMetadata(
    freshItem: ContinueWatchingItem,
    cachedFallbackItem: ContinueWatchingItem?,
    dismissedNextUpKeys: Set<String>,
): HomeNextUpCandidateMetadataDecision {
    val mergedItem = freshItem.withFallbackMetadata(cachedFallbackItem)
    val dismissKey = nextUpDismissKey(
        mergedItem.parentMetaId,
        mergedItem.nextUpSeedSeasonNumber,
        mergedItem.nextUpSeedEpisodeNumber,
    )
    val outcome = when {
        dismissKey in dismissedNextUpKeys -> HomeNextUpCandidateMetadataOutcome.Dismissed
        hasUsableHomeNextUpMetadata(mergedItem) -> HomeNextUpCandidateMetadataOutcome.Ready
        else -> HomeNextUpCandidateMetadataOutcome.Transient
    }
    return HomeNextUpCandidateMetadataDecision(
        item = mergedItem,
        outcome = outcome,
    )
}

internal fun buildHomeContinueWatchingItems(
    visibleEntries: List<WatchProgressEntry>,
    cachedInProgressByProgressKey: Map<String, ContinueWatchingItem> = emptyMap(),
    nextUpItemsBySeries: Map<String, Pair<Long, ContinueWatchingItem>>,
    nextUpSuppressedSeriesIds: Set<String>? = null,
    sortMode: ContinueWatchingSortMode = ContinueWatchingSortMode.DEFAULT,
    todayIsoDate: String = "",
    cloudLibraryUiState: CloudLibraryUiState? = null,
): List<ContinueWatchingItem> {
    val suppressedSeriesIds = nextUpSuppressedSeriesIds
        ?: visibleEntries
            .asSequence()
            .filter { entry -> entry.parentMetaType.isSeriesTypeForContinueWatching() }
            .map { entry -> entry.parentMetaId }
            .filter(String::isNotBlank)
            .toSet()

    val candidates = buildList {
        addAll(
            visibleEntries.map { entry ->
                val liveItem = entry.toContinueWatchingItem()
                HomeContinueWatchingCandidate(
                    lastUpdatedEpochMs = entry.lastUpdatedEpochMs,
                    item = liveItem
                        .withFallbackMetadata(
                            fallback = cachedInProgressByProgressKey[entry.resolvedProgressKey()],
                            preserveFallbackPlaybackIdentity = true,
                        )
                        .withCloudLibraryMetadata(cloudLibraryUiState),
                    isProgressEntry = true,
                )
            },
        )
        addAll(
            nextUpItemsBySeries.values.mapNotNull { (lastUpdatedEpochMs, item) ->
                if (item.parentMetaId in suppressedSeriesIds) return@mapNotNull null
                HomeContinueWatchingCandidate(
                    lastUpdatedEpochMs = lastUpdatedEpochMs,
                    item = item,
                    isProgressEntry = false,
                )
            },
        )
    }

    // Deduplicate by series/content id first (order-stable)
    val seen = mutableSetOf<String>()
    val deduplicated = candidates
        .sortedWith(
            compareByDescending<HomeContinueWatchingCandidate> { it.lastUpdatedEpochMs }
                .thenByDescending { it.isProgressEntry },
        )
        .filter { candidate -> candidate.item.shouldDisplayInContinueWatching() }
        .filter { candidate ->
            val key = candidate.item.parentMetaId.ifBlank { candidate.item.videoId }
            seen.add(key)
        }

    return when (sortMode) {
        ContinueWatchingSortMode.DEFAULT,
        ContinueWatchingSortMode.SPLIT_UPCOMING,
        -> deduplicated.map(HomeContinueWatchingCandidate::item)
        ContinueWatchingSortMode.STREAMING_STYLE -> applyStreamingStyleSort(deduplicated, todayIsoDate)
    }
}

private fun applyStreamingStyleSort(
    candidates: List<HomeContinueWatchingCandidate>,
    todayIsoDate: String,
): List<ContinueWatchingItem> {
    val (released, unreleased) = candidates.partition { candidate ->
        val item = candidate.item
        if (!item.isNextUp) {
            true // in-progress items are always "released"
        } else {
            val itemReleased = item.released
            if (itemReleased.isNullOrBlank() || todayIsoDate.isBlank()) {
                true // no date info → treat as released
            } else {
                isReleasedBy(todayIsoDate = todayIsoDate, releasedDate = itemReleased)
            }
        }
    }

    // Released: most recently watched first (already sorted by dedup pass)
    val sortedReleased = released.map(HomeContinueWatchingCandidate::item)

    // Unaired: soonest air date first; unknown dates go to the end
    val sortedUnreleased = unreleased
        .sortedWith { a, b ->
            val dateA = a.item.released?.takeIf { it.isNotBlank() }
            val dateB = b.item.released?.takeIf { it.isNotBlank() }
            when {
                dateA == null && dateB == null -> 0
                dateA == null -> 1
                dateB == null -> -1
                else -> dateA.compareTo(dateB)
            }
        }
        .map(HomeContinueWatchingCandidate::item)

    return sortedReleased + sortedUnreleased
}

private data class HomeContinueWatchingCandidate(
    val lastUpdatedEpochMs: Long,
    val item: ContinueWatchingItem,
    val isProgressEntry: Boolean,
)

internal fun buildHomeInProgressCacheSnapshot(
    visibleEntries: List<WatchProgressEntry>,
    cachedEntries: List<CachedInProgressItem>,
): List<CachedInProgressItem> {
    val cachedByProgressKey = cachedEntries.associateBy(CachedInProgressItem::resolvedProgressKey)
    return visibleEntries.map { entry ->
        val item = entry
            .toContinueWatchingItem()
            .withFallbackMetadata(
                fallback = cachedByProgressKey[entry.resolvedProgressKey()]?.toContinueWatchingItem(),
                preserveFallbackPlaybackIdentity = true,
            )
        CachedInProgressItem(
            contentId = entry.parentMetaId,
            contentType = entry.contentType,
            name = item.title,
            poster = item.poster,
            backdrop = item.background,
            logo = item.logo,
            videoId = entry.videoId,
            season = entry.seasonNumber,
            episode = entry.episodeNumber,
            episodeTitle = item.episodeTitle,
            episodeThumbnail = item.episodeThumbnail,
            pauseDescription = item.pauseDescription,
            position = entry.lastPositionMs,
            duration = entry.durationMs,
            lastWatched = entry.lastUpdatedEpochMs,
            progressPercent = entry.progressPercent,
            progressKey = entry.resolvedProgressKey(),
        )
    }
}

private fun ContinueWatchingItem.shouldDisplayInContinueWatching(): Boolean =
    isNextUp || progressFraction < 0.995f

private fun CachedNextUpItem.toContinueWatchingItem(
    releaseEpochMs: Long?,
    nowEpochMs: Long,
): ContinueWatchingItem {
    val alertState = com.nuvio.app.core.watch.progress.calculateReleaseAlertState(
        seedLastUpdatedEpochMs = lastWatched,
        seedSeasonNumber = seedSeason,
        nextSeasonNumber = season,
        releasedIso = released,
        releaseEpochMs = releaseEpochMs,
        nowEpochMs = nowEpochMs,
    )
    val resolvedPoster = poster.nonBlankOrNull()
    val resolvedBackdrop = backdrop.nonBlankOrNull()
    val resolvedEpisodeThumbnail = episodeThumbnail.nonBlankOrNull()
    return ContinueWatchingItem(
        parentMetaId = contentId,
        parentMetaType = contentType,
        videoId = videoId,
        title = name,
        subtitle = buildContinueWatchingEpisodeSubtitle(
            seasonNumber = season,
            episodeNumber = episode,
            episodeTitle = episodeTitle,
        ),
        imageUrl = resolvedEpisodeThumbnail ?: resolvedBackdrop ?: resolvedPoster,
        logo = logo.nonBlankOrNull(),
        poster = resolvedPoster,
        background = resolvedBackdrop,
        seasonNumber = season,
        episodeNumber = episode,
        episodeTitle = episodeTitle.nonBlankOrNull(),
        episodeThumbnail = resolvedEpisodeThumbnail,
        pauseDescription = pauseDescription.nonBlankOrNull(),
        released = released.nonBlankOrNull(),
        isNextUp = true,
        nextUpSeedSeasonNumber = seedSeason,
        nextUpSeedEpisodeNumber = seedEpisode,
        resumePositionMs = 0L,
        resumeProgressFraction = null,
        durationMs = 0L,
        progressFraction = 0f,
        isReleaseAlert = alertState.isReleaseAlert,
        isNewSeasonRelease = alertState.isNewSeasonRelease,
    )
}

private fun ContinueWatchingItem.withFallbackMetadata(
    fallback: ContinueWatchingItem?,
    preserveFallbackPlaybackIdentity: Boolean = false,
): ContinueWatchingItem {
    val nonBlankFallbackTitle = fallback?.title?.takeIf { it.isNotBlank() }
    val fallbackHasPlaceholderTitle = fallback?.hasPlaceholderHomeTitle() == true
    val fallbackTitle = nonBlankFallbackTitle
        ?.takeUnless { fallbackHasPlaceholderTitle }

    return copy(
        title = when {
            title.isBlank() && nonBlankFallbackTitle != null -> nonBlankFallbackTitle
            hasPlaceholderHomeTitle() && fallbackTitle != null -> fallbackTitle
            else -> title
        },
        subtitle = when {
            subtitle.isBlank() -> fallback?.subtitle?.takeIf { it.isNotBlank() }.orEmpty()
            preserveFallbackPlaybackIdentity && !fallback?.subtitle.isNullOrBlank() -> fallback.subtitle
            else -> subtitle
        },
        imageUrl = imageUrl.orNonBlank(fallback?.imageUrl),
        logo = logo.orNonBlank(fallback?.logo),
        poster = poster.orNonBlank(fallback?.poster),
        background = background.orNonBlank(fallback?.background),
        videoId = if (preserveFallbackPlaybackIdentity) {
            fallback?.videoId?.takeIf { it.isNotBlank() } ?: videoId
        } else {
            videoId
        },
        episodeTitle = episodeTitle.orNonBlank(fallback?.episodeTitle),
        episodeThumbnail = episodeThumbnail.orNonBlank(fallback?.episodeThumbnail),
        pauseDescription = pauseDescription.orNonBlank(fallback?.pauseDescription),
        released = released.orNonBlank(fallback?.released),
    )
}

internal fun String?.nonBlankOrNull(): String? = this?.takeIf { it.isNotBlank() }

private fun String?.orNonBlank(fallback: String?): String? =
    nonBlankOrNull() ?: fallback.nonBlankOrNull()

private fun ContinueWatchingItem.withCloudLibraryMetadata(
    cloudLibraryUiState: CloudLibraryUiState?,
): ContinueWatchingItem {
    if (!isCloudLibraryContinueWatchingItem() || cloudLibraryUiState == null) return this
    val target = cloudLibraryUiState.findPlaybackTargetForProgress(
        contentId = parentMetaId,
        videoId = videoId,
    ) ?: return this
    val fileName = target.file.name.trim().takeIf { it.isNotBlank() }
        ?: target.item.name.trim().takeIf { it.isNotBlank() }
        ?: return this
    return copy(
        title = fileName,
        pauseDescription = pauseDescription
            ?: target.item.name.takeIf { itemName -> itemName.isNotBlank() && itemName != fileName },
    )
}

private fun ContinueWatchingItem.hasPlaceholderHomeTitle(): Boolean {
    val normalizedTitle = title.trim()
    return normalizedTitle.equals(parentMetaId, ignoreCase = true) ||
        (isCloudLibraryContinueWatchingItem() && normalizedTitle.equals(videoId, ignoreCase = true))
}

private fun ContinueWatchingItem.isCloudLibraryContinueWatchingItem(): Boolean =
    parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)
