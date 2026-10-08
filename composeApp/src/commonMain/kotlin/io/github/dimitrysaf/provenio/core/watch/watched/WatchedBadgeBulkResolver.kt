package io.github.dimitrysaf.provenio.core.watch.watched

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaDetailsRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklSyncRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.toSimklShowIdSiblings
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderId
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.WatchProgressSource
import io.github.dimitrysaf.provenio.core.tracking.effectiveWatchProgressSource
import io.github.dimitrysaf.provenio.core.tracking.providerId
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktProgressRepository
import io.github.dimitrysaf.provenio.core.watch.progress.CurrentDateProvider
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val BADGE_RESOLUTION_CONCURRENCY = 2
private const val AMBIGUOUS_MARKER = "__ambiguous__"
private const val BADGE_META_CACHE_SIZE = 96
private val BADGE_META_CACHE_TTL = 30.minutes

private val log = Logger.withTag("WatchedBadgeBulk")

private class BadgeMetaEntry(val meta: MetaDetails, val fetchedAt: TimeMark)

private val badgeMetaCache = linkedMapOf<String, BadgeMetaEntry>()
private val badgeMetaCacheLock = Mutex()

private suspend fun cachedBadgeMeta(contentId: String): MetaDetails? = badgeMetaCacheLock.withLock {
    val entry = badgeMetaCache[contentId] ?: return@withLock null
    if (entry.fetchedAt.elapsedNow() > BADGE_META_CACHE_TTL) {
        badgeMetaCache.remove(contentId)
        null
    } else {
        entry.meta
    }
}

private suspend fun rememberBadgeMeta(contentId: String, meta: MetaDetails) = badgeMetaCacheLock.withLock {
    badgeMetaCache.remove(contentId)
    badgeMetaCache[contentId] = BadgeMetaEntry(meta, TimeSource.Monotonic.markNow())
    while (badgeMetaCache.size > BADGE_META_CACHE_SIZE) {
        badgeMetaCache.remove(badgeMetaCache.keys.first())
    }
}

/**
 * Works out which series are fully watched. Series in [alreadyResolvedSeriesIds] are skipped, so a
 * rerun after a partial run only asks the add-ons again for the series that failed, not for all of
 * them. [onSeriesResolved] reports each series as it resolves, so progress survives cancellation.
 * Returns true when every series resolved.
 */
suspend fun resolveWatchedBadgesBulk(
    watchedItems: List<WatchedItem>,
    progressEntries: List<WatchProgressEntry>,
    todayIsoDate: String = CurrentDateProvider.todayIsoDate(),
    alreadyResolvedSeriesIds: Set<String> = emptySet(),
    onSeriesResolved: (String) -> Unit = {},
): Boolean = withContext(Dispatchers.Default) {
    val allSeriesIds = buildSet {
        watchedItems.forEach { item ->
            if (item.type.isSeriesLikeWatchedType() && item.season != null && item.episode != null) {
                add(item.id)
            }
        }
        progressEntries.forEach { entry ->
            if (entry.parentMetaType.isSeriesLikeWatchedType() && entry.isEpisode && entry.isEffectivelyCompleted) {
                add(entry.parentMetaId)
            }
        }
        WatchedRepository.baseFullyWatchedSeriesKeys().mapNotNullTo(this, ::extractContentIdFromWatchedKey)
    }
    val touchedSeriesIds = allSeriesIds - alreadyResolvedSeriesIds
    if (touchedSeriesIds.isEmpty()) return@withContext true

    // Use the full watchedKeys from UI state which includes extra keys from
    // provider alternate IDs (e.g. Simkl anime alternate MAL/Kitsu keys).
    val watchedKeys = WatchedRepository.uiState.value.watchedKeys

    log.i {
        "Bulk badge resolution starting: ${touchedSeriesIds.size} series candidates " +
            "(${alreadyResolvedSeriesIds.size} already resolved)"
    }

    val semaphore = Semaphore(BADGE_RESOLUTION_CONCURRENCY)
    val resolvedIds = mutableSetOf<String>()
    val resolvedStates = linkedMapOf<String, Boolean>()

    try {
        for (contentId in touchedSeriesIds) {
            semaphore.withPermit {
                val meta = try {
                    cachedBadgeMeta(contentId)
                        ?: MetaDetailsRepository.fetch(type = "series", id = contentId, cacheResult = false)
                            ?.also { rememberBadgeMeta(contentId, it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    null
                }
                if (meta != null) {
                    val isFullyWatched = WatchedRepository.calculateFullyWatchedSeriesState(
                        meta = meta,
                        todayIsoDate = todayIsoDate,
                        isEpisodeWatched = { episode ->
                            val keys = watchedItemKeys(meta.type, meta.id, episode.season, episode.episode)
                            if (keys.any(watchedKeys::contains)) {
                                true
                            } else {
                                val episodeNumber = episode.episode
                                if (episodeNumber != null) {
                                    io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAnimeWatchedFallback.isWatched(episode.id, episodeNumber)
                                } else {
                                    false
                                }
                            }
                        },
                        isEpisodeCompleted = { episode ->
                            val playbackId = meta.episodePlaybackId(episode)
                            progressEntries.any { entry ->
                                entry.videoId == playbackId && entry.isEffectivelyCompleted
                            }
                        },
                    )
                    resolvedStates[watchedItemKey(meta.type, meta.id)] = isFullyWatched
                    if (meta.type.isSeriesLikeWatchedType() && meta.videos.isNotEmpty()) {
                        resolvedIds.add(contentId)
                        onSeriesResolved(contentId)
                    }
                }
            }
            yield()
        }
    } finally {
        // Also on cancellation, so the series reported through onSeriesResolved keep their badges.
        WatchedRepository.updateFullyWatchedSeriesStates(resolvedStates)
    }
    log.i { "Bulk badge resolution complete: resolved ${resolvedIds.size}/${touchedSeriesIds.size}" }

    // Sibling expansion
    expandFullyWatchedWithSiblings()
    resolvedIds.size == touchedSeriesIds.size
}

fun expandFullyWatchedWithSiblings() {
    val siblingMap = getActiveProviderSiblingMap()
    if (siblingMap.isEmpty()) {
        WatchedRepository.setExpandedFullyWatchedSeriesKeys(emptySet())
        return
    }

    // Use base keys (without previous sibling expansion) to avoid feedback loops
    val baseKeys = WatchedRepository.baseFullyWatchedSeriesKeys()
    if (baseKeys.isEmpty()) {
        WatchedRepository.setExpandedFullyWatchedSeriesKeys(emptySet())
        return
    }

    // Compute only the sibling-derived keys (not including base keys themselves)
    val siblingKeys = buildSet {
        for (key in baseKeys) {
            val contentId = extractContentIdFromWatchedKey(key) ?: continue
            val siblings = siblingMap[contentId] ?: continue
            siblings.forEach { siblingId ->
                if (siblingId != contentId && !siblingId.startsWith(AMBIGUOUS_MARKER)) {
                    val siblingKey = rebuildWatchedKeyWithSiblingId(key, siblingId)
                    if (siblingKey != null) add(siblingKey)
                }
            }
        }
    }

    if (siblingKeys != WatchedRepository.currentExpandedSiblingKeys()) {
        log.i { "Sibling expansion: ${baseKeys.size} base keys -> +${siblingKeys.size} sibling keys" }
        WatchedRepository.setExpandedFullyWatchedSeriesKeys(siblingKeys)
    }
}

private fun getActiveProviderSiblingMap(): Map<String, Set<String>> {
    val source = TrackingSettingsRepository.uiState.value.watchProgressSource
    val effectiveSource = effectiveWatchProgressSource(
        requestedSource = source,
        isProviderAuthenticated = { providerId ->
            io.github.dimitrysaf.provenio.core.tracking.TrackingProviderRegistry.isAuthenticated(providerId)
        },
    )
    return when (effectiveSource.providerId) {
        TrackingProviderId.TRAKT -> TraktProgressRepository.getShowIdSiblings()
        TrackingProviderId.SIMKL -> {
            SimklSyncRepository.state.value.snapshot.toSimklShowIdSiblings()
        }
        else -> emptyMap()
    }
}

private fun extractContentIdFromWatchedKey(key: String): String? {
    // Format: "type:contentId:season:episode"
    // Split from the end to handle contentIds with colons (like "tmdb:123")
    val parts = key.split(':')
    if (parts.size < 4) return null
    // Last two parts are season and episode (-1:-1)
    // First part is type, everything in between is contentId
    val type = parts.first()
    val season = parts[parts.size - 2]
    val episode = parts.last()
    if (season.toIntOrNull() == null || episode.toIntOrNull() == null) return null
    val contentId = parts.subList(1, parts.size - 2).joinToString(":")
    return contentId.takeIf { it.isNotBlank() }
}

private fun rebuildWatchedKeyWithSiblingId(originalKey: String, siblingId: String): String? {
    val parts = originalKey.split(':')
    if (parts.size < 4) return null
    val type = parts.first()
    return watchedItemKey(type = type, id = siblingId)
}

private fun String.isSeriesLikeWatchedType(): Boolean =
    trim().lowercase() in setOf("series", "show", "tv", "tvshow", "anime")
