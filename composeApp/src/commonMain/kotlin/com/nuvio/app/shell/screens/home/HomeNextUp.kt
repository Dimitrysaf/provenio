package com.nuvio.app.shell.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.nuvio.app.core.metadata.MetaDetails
import com.nuvio.app.core.metadata.MetaDetailsRepository
import com.nuvio.app.core.metadata.MetaVideo
import com.nuvio.app.core.metadata.SeriesPrimaryAction
import com.nuvio.app.core.metadata.seriesPrimaryAction
import com.nuvio.app.core.watch.watched.WatchedItem
import com.nuvio.app.core.watch.watched.WatchedRepository
import com.nuvio.app.core.watch.watched.episodePlaybackId
import com.nuvio.app.core.watch.watched.watchedItemKey
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.parseReleaseDateToEpochMs
import com.nuvio.app.core.watch.progress.shouldTreatAsInProgressForContinueWatching
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.core.watch.progress.WatchProgressRepository
import com.nuvio.app.core.watch.progress.toUpNextContinueWatchingItem
import kotlinx.coroutines.CancellationException
import nuvio.composeapp.generated.resources.*
import com.nuvio.app.core.home.CompletedSeriesCandidate
import com.nuvio.app.core.home.HomeNextUpCandidateMetadataOutcome
import com.nuvio.app.core.home.classifyHomeNextUpCandidateMetadata

internal const val NEXT_UP_RESOLUTION_CONCURRENCY = 4

internal const val MAX_NEXT_UP_RESOLUTION_RETRIES = 3

internal const val NEXT_UP_RESOLUTION_RETRY_BASE_DELAY_MS = 1_500L

internal fun filterHomeNextUpCandidatesForContinueWatchingWindow(
    candidates: List<CompletedSeriesCandidate>,
    cutoffEpochMs: Long?,
): List<CompletedSeriesCandidate> = cutoffEpochMs
    ?.let { cutoff -> candidates.filter { candidate -> candidate.markedAtEpochMs >= cutoff } }
    ?: candidates

internal suspend fun resolveHomeNextUpCandidate(
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

internal fun MetaDetails.videoForSeriesAction(action: SeriesPrimaryAction): MetaVideo? {
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

internal fun shouldTreatAsActiveInProgressForNextUpSuppression(
    progress: WatchProgressEntry,
    latestCompletedAt: Long?,
): Boolean {
    if (!progress.shouldTreatAsInProgressForContinueWatching()) return false
    if (latestCompletedAt == null || latestCompletedAt == Long.MIN_VALUE) return true
    return progress.lastUpdatedEpochMs >= latestCompletedAt
}

internal data class HomeNextUpCandidateResolution(
    val candidate: CompletedSeriesCandidate,
    val attempt: HomeNextUpResolutionAttempt,
)

internal data class HomeNextUpResolutionAttempt(
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
