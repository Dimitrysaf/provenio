package com.nuvio.app.shell.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.nuvio.app.core.cloud.CloudLibraryContentType
import com.nuvio.app.core.metadata.MetaDetails
import com.nuvio.app.shell.screens.home.components.HomeContinueWatchingSection
import com.nuvio.app.shell.screens.home.components.HomeContinueWatchingSectionBottomPadding
import com.nuvio.app.shell.screens.home.components.ContinueWatchingLayout
import com.nuvio.app.core.tracking.WatchProgressSource
import com.nuvio.app.core.watch.progress.CachedNextUpItem
import com.nuvio.app.core.watch.progress.ContinueWatchingEnrichmentCache
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesUiState
import com.nuvio.app.core.watch.progress.ContinueWatchingItem
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.shell.components.DisintegrationRequest
import com.nuvio.app.core.watch.watching.domain.isReleasedBy
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.home.CompletedSeriesCandidate
import com.nuvio.app.core.home.buildHomeInProgressCacheSnapshot

internal fun LazyListScope.homeContinueWatchingSections(
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

internal fun saveContinueWatchingSnapshots(
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

internal fun CompletedSeriesCandidate.toContinueWatchingSeed(meta: com.nuvio.app.core.metadata.MetaDetails) =
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

internal fun WatchProgressEntry.isCloudLibraryProgressEntry(): Boolean =
    contentType.equals(CloudLibraryContentType, ignoreCase = true) ||
        parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)
