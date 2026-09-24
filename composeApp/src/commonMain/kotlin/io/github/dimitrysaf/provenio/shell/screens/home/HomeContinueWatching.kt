package io.github.dimitrysaf.provenio.shell.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.dimitrysaf.provenio.core.cloud.CloudLibraryContentType
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeContinueWatchingSection
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomeContinueWatchingSectionBottomPadding
import io.github.dimitrysaf.provenio.shell.screens.home.components.ContinueWatchingLayout
import io.github.dimitrysaf.provenio.core.tracking.WatchProgressSource
import io.github.dimitrysaf.provenio.core.watch.progress.CachedNextUpItem
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingEnrichmentCache
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesUiState
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingItem
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.shell.components.DisintegrationRequest
import io.github.dimitrysaf.provenio.core.watch.watching.domain.isReleasedBy
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.home.CompletedSeriesCandidate
import io.github.dimitrysaf.provenio.core.home.buildHomeInProgressCacheSnapshot

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

internal fun CompletedSeriesCandidate.toContinueWatchingSeed(meta: io.github.dimitrysaf.provenio.core.metadata.MetaDetails) =
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
