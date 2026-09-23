package com.nuvio.app.core.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
