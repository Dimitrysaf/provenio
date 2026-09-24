package com.nuvio.app.shell.screens.details

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.home.HomeCatalogSection
import com.nuvio.app.core.home.MetaPreview
import com.nuvio.app.core.library.LibraryRepository
import com.nuvio.app.core.library.toLibraryItem
import com.nuvio.app.core.metadata.MetaDetails
import com.nuvio.app.core.metadata.SeriesPrimaryAction
import com.nuvio.app.core.metadata.groupedEpisodesForDisplay
import com.nuvio.app.core.metadata.moreLikeThisFallback
import com.nuvio.app.core.metadata.seriesPrimaryAction
import com.nuvio.app.core.tracking.TrackingMembershipApplyResult
import com.nuvio.app.core.tracking.TrackingProviderId
import com.nuvio.app.core.watch.progress.ContinueWatchingPreferencesRepository
import com.nuvio.app.core.watch.progress.WatchProgressEntry
import com.nuvio.app.core.watch.progress.WatchProgressUiState
import com.nuvio.app.core.watch.progress.buildPlaybackVideoId
import com.nuvio.app.core.watch.watched.WatchedUiState
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.screens.details.components.EpisodeListEntry
import com.nuvio.app.shell.screens.details.components.buildEpisodeListEntries
import com.nuvio.app.shell.screens.details.components.episodeWatchState
import com.nuvio.app.shell.screens.details.components.rememberEpisodeSeasonExpansion
import com.nuvio.app.shell.screens.details.components.summarizeEpisodeSeasons
import com.nuvio.app.shell.screens.library.PendingTrackingMembershipRemoval
import com.nuvio.app.shell.screens.library.executeTrackingMembershipOperation
import com.nuvio.app.shell.screens.library.showTrackingMembershipRewriteFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tracking_lists_update_failed
import org.jetbrains.compose.resources.stringResource

// What the play button does for this title: the series episode to go to, or where the movie was left.
internal data class DetailPrimaryPlay(
    val seriesAction: SeriesPrimaryAction?,
    val seriesStreamVideoId: String?,
    val seriesPauseDescription: String?,
    val movieProgress: WatchProgressEntry?,
)

@Composable
internal fun rememberDetailPrimaryPlay(
    meta: MetaDetails,
    watchProgressUiState: WatchProgressUiState,
    watchedUiState: WatchedUiState,
    progressByVideoId: Map<String, WatchProgressEntry>,
    todayIsoDate: String,
): DetailPrimaryPlay {
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
    val seriesStreamVideoId = remember(seriesAction, seriesActionVideo) {
        val action = seriesAction ?: return@remember null
        seriesActionVideo?.id?.takeIf { it.isNotBlank() } ?: action.videoId
    }
    return DetailPrimaryPlay(
        seriesAction = seriesAction,
        seriesStreamVideoId = seriesStreamVideoId,
        seriesPauseDescription = seriesActionVideo?.overview,
        movieProgress = movieProgress,
    )
}

// The episode list rows, with each season folded or open.
internal class DetailEpisodeList(
    val entries: List<EpisodeListEntry>,
    val onSeasonToggle: (Int) -> Unit,
)

@Composable
internal fun rememberDetailEpisodeList(
    meta: MetaDetails,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    todayIsoDate: String,
): DetailEpisodeList {
    val groupedEpisodes = remember(meta.videos, meta.type) {
        meta.groupedEpisodesForDisplay()
    }
    val seasonSummary = remember(meta, groupedEpisodes, progressByVideoId, watchedKeys, todayIsoDate) {
        summarizeEpisodeSeasons(groupedEpisodes, todayIsoDate) { episode ->
            episodeWatchState(meta, episode, progressByVideoId, watchedKeys)
        }
    }
    val seasonExpansion = rememberEpisodeSeasonExpansion(meta.id)
    val expandedSeasons = groupedEpisodes.keys.filter { season ->
        seasonExpansion.isExpanded(season, seasonSummary.defaultSeason)
    }.toSet()
    val entries = remember(groupedEpisodes, expandedSeasons, seasonSummary.completedSeasons) {
        buildEpisodeListEntries(
            groupedEpisodes = groupedEpisodes,
            expandedSeasons = expandedSeasons,
            completedSeasons = seasonSummary.completedSeasons,
        )
    }
    return DetailEpisodeList(
        entries = entries,
        onSeasonToggle = { season -> seasonExpansion.toggle(season, seasonSummary.defaultSeason) },
    )
}

// Which optional sections this title has anything to show in.
internal data class DetailSectionContent(
    val hasProduction: Boolean,
    val hasAdditionalInfo: Boolean,
    val hasCollection: Boolean,
    val hasTrailers: Boolean,
    val moreLikeThisItems: List<MetaPreview>,
)

@Composable
internal fun rememberDetailSectionContent(
    meta: MetaDetails,
    homeSections: List<HomeCatalogSection>,
): DetailSectionContent {
    val moreLikeThisItems = remember(meta, homeSections) {
        meta.moreLikeThis.ifEmpty { moreLikeThisFallback(meta, homeSections) }
    }
    return remember(meta, moreLikeThisItems) {
        DetailSectionContent(
            hasProduction = meta.productionCompanies.isNotEmpty() || meta.networks.isNotEmpty(),
            hasAdditionalInfo = meta.status != null ||
                meta.releaseInfo != null ||
                meta.runtime != null ||
                meta.ageRating != null ||
                meta.country != null ||
                meta.language != null,
            hasCollection = meta.collectionName != null && meta.collectionItems.isNotEmpty(),
            hasTrailers = meta.trailers.isNotEmpty(),
            moreLikeThisItems = moreLikeThisItems,
        )
    }
}

// Where the list is scrolled relative to the hero, read lazily so scrolling does not recompose sections.
internal class DetailScrollState(
    val listState: LazyListState,
    val heroHeightPx: MutableIntState,
    val scrollOffsetPx: () -> Float,
    val isHeroCollapsed: State<Boolean>,
)

@Composable
internal fun rememberDetailScrollState(metaId: String): DetailScrollState {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val safeAreaTopPx = with(density) {
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()
            .toPx()
    }
    val heroHeightPx = remember(metaId) { mutableIntStateOf(0) }
    // Keep pixel-by-pixel list state reads out of this composition. Reading the
    // offset here would recompose every metadata section on every scroll frame.
    val scrollOffsetPx = remember(listState, heroHeightPx) {
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
                (listState.firstVisibleItemIndex > 0 || scrollOffsetPx() > thresholdPx)
        }
    }
    return remember(listState, heroHeightPx, scrollOffsetPx, isHeroCollapsed) {
        DetailScrollState(listState, heroHeightPx, scrollOffsetPx, isHeroCollapsed)
    }
}

// Saves the title to the library, or takes it out, asking first when that would remove tracker history.
@Composable
internal fun rememberToggleSaved(
    meta: MetaDetails,
    scope: CoroutineScope,
    onRemovalNeedsConfirmation: (PendingTrackingMembershipRemoval) -> Unit,
): () -> Unit {
    val failedMessage = stringResource(Res.string.tracking_lists_update_failed)
    return remember(meta, failedMessage) {
        {
            val item = meta.toLibraryItem(savedAtEpochMs = 0L)
            scope.launch {
                val toggleMembership: suspend (Set<TrackingProviderId>) -> TrackingMembershipApplyResult =
                    { confirmedProviders ->
                        LibraryRepository.toggleSaved(
                            item = item,
                            confirmedRemovalProviders = confirmedProviders,
                        )
                    }
                executeTrackingMembershipOperation(
                    operation = { toggleMembership(emptySet()) },
                    onSuccess = { result ->
                        if (result.requiresRemovalConfirmation) {
                            onRemovalNeedsConfirmation(
                                PendingTrackingMembershipRemoval(
                                    itemTitle = item.name,
                                    confirmations = result.requiredRemovalConfirmations,
                                    retry = toggleMembership,
                                    onApplied = ::showTrackingMembershipRewriteFeedback,
                                    onFailure = { error -> NuvioToastController.show(error.message ?: failedMessage) },
                                ),
                            )
                        } else {
                            showTrackingMembershipRewriteFeedback(result)
                        }
                    },
                    onFailure = { error -> NuvioToastController.show(error.message ?: failedMessage) },
                )
            }
            Unit
        }
    }
}
