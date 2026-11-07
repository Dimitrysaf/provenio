package io.github.dimitrysaf.provenio.core.watch.watching.application

import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedItem
import io.github.dimitrysaf.provenio.core.watch.watched.normalizeWatchedMarkedAtEpochMs
import io.github.dimitrysaf.provenio.core.watch.watched.watchedItemKey
import io.github.dimitrysaf.provenio.core.watch.watched.watchedItemKeys
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.continueWatchingEntries
import io.github.dimitrysaf.provenio.core.watch.progress.shouldUseAsCompletedSeedForContinueWatching
import io.github.dimitrysaf.provenio.core.watch.watching.domain.WatchingCompletedEpisode
import io.github.dimitrysaf.provenio.core.watch.watching.domain.WatchingContentRef
import io.github.dimitrysaf.provenio.core.watch.watching.domain.WatchingProgressRecord
import io.github.dimitrysaf.provenio.core.watch.watching.domain.WatchingWatchedRecord
import io.github.dimitrysaf.provenio.core.watch.watching.domain.latestCompletedSeriesEpisode

object WatchingState {
    fun isPosterWatched(
        watchedKeys: Set<String>,
        item: MetaPreview,
        fullyWatchedSeriesKeys: Set<String> = emptySet(),
    ): Boolean {
        val posterKeys = watchedItemKeys(type = item.type, id = item.id)
        if (posterKeys.any(watchedKeys::contains)) return true
        return item.type.isSeriesLikePosterType() && posterKeys.any(fullyWatchedSeriesKeys::contains)
    }

    /**
     * How far through a poster's title is, from 0 to 1, for the bar under its artwork: a movie by
     * its own progress, a series by [seriesProgress] across all its episodes. Null when it has not
     * been started, or has been finished, which the watched mark shows instead.
     */
    fun posterProgress(
        item: MetaPreview,
        movieProgress: Map<String, Float>,
        seriesProgress: Map<String, Float>,
    ): Float? {
        val posterKeys = watchedItemKeys(type = item.type, id = item.id)
        val progress = if (item.type.isSeriesLikePosterType()) seriesProgress else movieProgress
        return posterKeys.firstNotNullOfOrNull(progress::get)?.takeIf { it > 0f && it < 1f }
    }

    /** The progress of each movie started and not finished, by its watched key, for [posterProgress]. */
    fun movieProgressByKey(entries: List<WatchProgressEntry>): Map<String, Float> =
        entries
            .filter { !it.isEpisode && !it.parentMetaType.isSeriesLikePosterType() && !it.isEffectivelyCompleted }
            .associate { entry -> watchedItemKey(entry.parentMetaType, entry.parentMetaId) to entry.progressFraction }

    fun isEpisodeWatched(
        watchedKeys: Set<String>,
        metaType: String,
        metaId: String,
        episode: MetaVideo,
    ): Boolean {
        val keys = watchedItemKeys(
            type = metaType,
            id = metaId,
            season = episode.season,
            episode = episode.episode,
        )
        if (keys.any(watchedKeys::contains)) return true

        // Fallback for franchise-parent anime: meta.id (e.g. "mal:49233") may differ
        // from the actual entry ID in Simkl. Check via video ID resolution in snapshot.
        // Only for anime-style video IDs (mal:, kitsu:, etc.) — not IMDB/TVDB content.
        val videoId = episode.id
        val episodeNumber = episode.episode
        if (episodeNumber != null) {
            return io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAnimeWatchedFallback.isWatched(videoId, episodeNumber)
        }
        return false
    }

    fun areEpisodesWatched(
        watchedKeys: Set<String>,
        metaType: String,
        metaId: String,
        episodes: Collection<MetaVideo>,
    ): Boolean = episodes.isNotEmpty() && episodes.all { episode ->
        isEpisodeWatched(
            watchedKeys = watchedKeys,
            metaType = metaType,
            metaId = metaId,
            episode = episode,
        )
    }

    fun latestCompletedBySeries(
        progressEntries: List<WatchProgressEntry>,
        watchedItems: List<WatchedItem>,
        preferFurthestEpisode: Boolean = true,
    ): Map<WatchingContentRef, WatchingCompletedEpisode> {
        val contentRefs = buildSet {
            progressEntries.forEach { entry ->
                add(WatchingContentRef(type = entry.parentMetaType, id = entry.parentMetaId))
            }
            watchedItems.forEach { item ->
                add(WatchingContentRef(type = item.type, id = item.id))
            }
        }
        val progressRecordsByContent = progressEntries
            .asSequence()
            .filter { entry -> entry.shouldUseAsCompletedSeedForContinueWatching() }
            .map(WatchProgressEntry::toDomainProgressRecord)
            .groupBy(WatchingProgressRecord::content)
        val watchedRecordsByContent = watchedItems
            .asSequence()
            .map(WatchedItem::toDomainWatchedRecord)
            .groupBy(WatchingWatchedRecord::content)
        return contentRefs.mapNotNull { content ->
            latestCompletedSeriesEpisode(
                content = content,
                progressRecords = progressRecordsByContent[content].orEmpty(),
                watchedRecords = watchedRecordsByContent[content].orEmpty(),
                preferFurthestEpisode = preferFurthestEpisode,
            )?.let { completed -> content to completed }
        }.toMap()
    }

    fun visibleContinueWatchingEntries(
        progressEntries: List<WatchProgressEntry>,
        @Suppress("UNUSED_PARAMETER")
        latestCompletedBySeries: Map<WatchingContentRef, WatchingCompletedEpisode>,
    ): List<WatchProgressEntry> = progressEntries.continueWatchingEntries()
}

private fun String.isSeriesLikePosterType(): Boolean =
    trim().lowercase() in setOf("series", "show", "tv", "tvshow", "anime")

private fun WatchProgressEntry.toDomainProgressRecord(): WatchingProgressRecord =
    normalizedCompletion().let { entry ->
        WatchingProgressRecord(
            content = WatchingContentRef(type = entry.parentMetaType, id = entry.parentMetaId),
            videoId = entry.videoId,
            seasonNumber = entry.seasonNumber,
            episodeNumber = entry.episodeNumber,
            lastUpdatedEpochMs = entry.lastUpdatedEpochMs,
            lastPositionMs = entry.lastPositionMs,
            isCompleted = entry.isCompleted,
            episodeTitle = entry.episodeTitle,
            episodeThumbnail = entry.episodeThumbnail,
        )
    }

private fun WatchedItem.toDomainWatchedRecord(): WatchingWatchedRecord =
    WatchingWatchedRecord(
        content = WatchingContentRef(type = type, id = id),
        seasonNumber = season,
        episodeNumber = episode,
        markedAtEpochMs = normalizeWatchedMarkedAtEpochMs(markedAtEpochMs),
    )
