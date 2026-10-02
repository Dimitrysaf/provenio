package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailActionButtons
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailSecondaryAction
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailAdditionalInfoSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailCastSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailCommentsSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailMetaInfo
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailParentsGuideSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailPosterGridSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailPosterRailSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailProductionSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailEpisodeListRow
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailSectionTitle
import io.github.dimitrysaf.provenio.shell.screens.details.components.EpisodeListEntry
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailTrailersSection
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.playback.ParentalWarning
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentReview
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.metadata.MetaCompany
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaPerson
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionItem
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsUiState
import io.github.dimitrysaf.provenio.core.metadata.MetaTrailer
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.core.metadata.MoreLikeThisSource
import io.github.dimitrysaf.provenio.core.metadata.playLabel

internal fun LazyListScope.configuredMetaSectionItems(
    settings: MetaScreenSettingsUiState,
    meta: MetaDetails,
    isTablet: Boolean,
    contentHorizontalPadding: Dp,
    contentMaxWidth: Dp,
    playButtonLabel: String,
    isPrimaryPlayEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    onPrimaryPlayClick: () -> Unit,
    onPrimaryPlayLongClick: (() -> Unit)?,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    showManualPlayOption: Boolean,
    hasProductionSection: Boolean,
    hasTrailersSection: Boolean,
    hasEpisodes: Boolean,
    hasAdditionalInfoSection: Boolean,
    hasCollectionSection: Boolean,
    moreLikeThisItems: List<MetaPreview>,
    parentalWarnings: List<ParentalWarning>,
    shouldShowComments: Boolean,
    comments: List<TraktCommentReview>,
    isCommentsLoading: Boolean,
    isCommentsLoadingMore: Boolean,
    commentsCurrentPage: Int,
    commentsPageCount: Int,
    commentsError: String?,
    episodeListEntries: List<EpisodeListEntry>,
    todayIsoDate: String,
    onEpisodeSeasonToggle: (Int) -> Unit,
    onRetryComments: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    onTrailerClick: (MetaTrailer) -> Unit,
    progressByVideoId: Map<String, WatchProgressEntry>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    blurUnwatchedEpisodes: Boolean,
    onEpisodeClick: (MetaVideo) -> Unit,
    onEpisodeLongPress: (MetaVideo) -> Unit,
    onSeasonLongPress: (Int) -> Unit,
    onOpenMeta: ((MetaPreview) -> Unit)?,
    onCastClick: ((MetaPerson, String?) -> Unit)?,
    onCompanyClick: ((MetaCompany, String) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val enabledItems = settings.items.filter { it.enabled }
    fun sectionHasContent(key: MetaScreenSectionKey): Boolean =
        metaSectionHasContent(
            key = key,
            meta = meta,
            hasProductionSection = hasProductionSection,
            hasTrailersSection = hasTrailersSection,
            hasEpisodes = hasEpisodes,
            hasAdditionalInfoSection = hasAdditionalInfoSection,
            hasCollectionSection = hasCollectionSection,
            moreLikeThisItems = moreLikeThisItems,
            parentalWarnings = parentalWarnings,
            shouldShowComments = shouldShowComments,
            comments = comments,
            isCommentsLoading = isCommentsLoading,
            commentsError = commentsError,
        )

    fun addSectionItem(
        key: String,
        sectionItems: List<MetaScreenSectionItem>,
    ) {
        item(key = key) {
            DetailSectionContainer(
                horizontalPadding = contentHorizontalPadding,
                contentMaxWidth = contentMaxWidth,
            ) {
                ConfiguredMetaSections(
                    settings = settings.copy(items = sectionItems),
                    meta = meta,
                    isTablet = isTablet,
                    horizontalScrollPadding = contentHorizontalPadding,
                    playButtonLabel = playButtonLabel,
                    isPrimaryPlayEnabled = isPrimaryPlayEnabled,
                    isSaved = isSaved,
                    isWatched = isWatched,
                    onPrimaryPlayClick = onPrimaryPlayClick,
                    onPrimaryPlayLongClick = onPrimaryPlayLongClick,
                    onSaveClick = onSaveClick,
                    onSaveLongClick = onSaveLongClick,
                    onWatchedClick = onWatchedClick,
                    showManualPlayOption = showManualPlayOption,
                    hasProductionSection = hasProductionSection,
                    hasTrailersSection = hasTrailersSection,
                    hasEpisodes = hasEpisodes,
                    hasAdditionalInfoSection = hasAdditionalInfoSection,
                    hasCollectionSection = hasCollectionSection,
                    moreLikeThisItems = moreLikeThisItems,
                    parentalWarnings = parentalWarnings,
                    shouldShowComments = shouldShowComments,
                    comments = comments,
                    isCommentsLoading = isCommentsLoading,
                    isCommentsLoadingMore = isCommentsLoadingMore,
                    commentsCurrentPage = commentsCurrentPage,
                    commentsPageCount = commentsPageCount,
                    commentsError = commentsError,
                    onRetryComments = onRetryComments,
                    onLoadMoreComments = onLoadMoreComments,
                    onCommentClick = onCommentClick,
                    onTrailerClick = onTrailerClick,
                    watchedKeys = watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onOpenMeta = onOpenMeta,
                    onCastClick = onCastClick,
                    onCompanyClick = onCompanyClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }
    }

    fun addLazyEpisodeListItems(key: String) {
        if (episodeListEntries.isEmpty()) return

        item(
            key = "$key-header",
            contentType = "detail-episode-header",
        ) {
            DetailSectionContainer(
                horizontalPadding = contentHorizontalPadding,
                contentMaxWidth = contentMaxWidth,
                bottomPadding = 14.dp,
            ) {
                DetailSectionTitle(title = stringResource(Res.string.details_episodes))
            }
        }
        itemsIndexed(
            items = episodeListEntries,
            key = { _, entry -> "$key-${entry.key}" },
            contentType = { _, entry ->
                if (entry is EpisodeListEntry.Season) "detail-episode-season" else "detail-episode"
            },
        ) { index, entry ->
            DetailSectionContainer(
                horizontalPadding = contentHorizontalPadding,
                contentMaxWidth = contentMaxWidth,
                bottomPadding = if (index == episodeListEntries.lastIndex) 20.dp else ListItemBetweenSpace,
                modifier = Modifier.animateItem(),
            ) {
                DetailEpisodeListRow(
                    entry = entry,
                    index = index,
                    count = episodeListEntries.size,
                    meta = meta,
                    todayIsoDate = todayIsoDate,
                    progressByVideoId = progressByVideoId,
                    watchedKeys = watchedKeys,
                    blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                    onSeasonClick = onEpisodeSeasonToggle,
                    onSeasonLongPress = onSeasonLongPress,
                    onEpisodeClick = onEpisodeClick,
                    onEpisodeLongPress = onEpisodeLongPress,
                )
            }
        }
    }

    fun addStandaloneSection(
        section: MetaScreenSectionItem,
        key: String,
    ) {
        if (section.key == MetaScreenSectionKey.EPISODES) {
            addLazyEpisodeListItems(key)
        } else {
            addSectionItem(
                key = key,
                sectionItems = listOf(section),
            )
        }
    }

    enabledItems
        .filter { sectionHasContent(it.key) }
        .forEach { section ->
            addStandaloneSection(
                section = section,
                key = "detail-section-${section.key.name}",
            )
        }
}

@Composable
internal fun DetailSectionContainer(
    horizontalPadding: Dp,
    contentMaxWidth: Dp,
    bottomPadding: Dp = 20.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (contentMaxWidth == Dp.Unspecified) {
                        Modifier
                    } else {
                        Modifier.widthIn(max = contentMaxWidth)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

internal fun metaSectionHasContent(
    key: MetaScreenSectionKey,
    meta: MetaDetails,
    hasProductionSection: Boolean,
    hasTrailersSection: Boolean,
    hasEpisodes: Boolean,
    hasAdditionalInfoSection: Boolean,
    hasCollectionSection: Boolean,
    moreLikeThisItems: List<MetaPreview>,
    parentalWarnings: List<ParentalWarning>,
    shouldShowComments: Boolean,
    comments: List<TraktCommentReview>,
    isCommentsLoading: Boolean,
    commentsError: String?,
): Boolean =
    when (key) {
        MetaScreenSectionKey.ACTIONS -> true
        MetaScreenSectionKey.OVERVIEW -> true
        MetaScreenSectionKey.PARENTS_GUIDE -> parentalWarnings.isNotEmpty()
        MetaScreenSectionKey.PRODUCTION -> hasProductionSection
        MetaScreenSectionKey.CAST -> meta.cast.isNotEmpty()
        MetaScreenSectionKey.COMMENTS -> shouldShowComments && (isCommentsLoading || comments.isNotEmpty() || !commentsError.isNullOrBlank())
        MetaScreenSectionKey.TRAILERS -> hasTrailersSection
        MetaScreenSectionKey.EPISODES -> hasEpisodes
        MetaScreenSectionKey.DETAILS -> hasAdditionalInfoSection
        MetaScreenSectionKey.COLLECTION -> !hasEpisodes && hasCollectionSection
        MetaScreenSectionKey.MORE_LIKE_THIS -> moreLikeThisItems.isNotEmpty()
    }

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
internal fun ConfiguredMetaSections(
    settings: MetaScreenSettingsUiState,
    meta: MetaDetails,
    isTablet: Boolean,
    horizontalScrollPadding: Dp,
    playButtonLabel: String,
    isPrimaryPlayEnabled: Boolean,
    isSaved: Boolean,
    isWatched: Boolean,
    onPrimaryPlayClick: () -> Unit,
    onPrimaryPlayLongClick: (() -> Unit)?,
    onSaveClick: () -> Unit,
    onSaveLongClick: (() -> Unit)?,
    onWatchedClick: () -> Unit,
    showManualPlayOption: Boolean,
    hasProductionSection: Boolean,
    hasTrailersSection: Boolean,
    hasEpisodes: Boolean,
    hasAdditionalInfoSection: Boolean,
    hasCollectionSection: Boolean,
    moreLikeThisItems: List<MetaPreview>,
    parentalWarnings: List<ParentalWarning>,
    shouldShowComments: Boolean,
    comments: List<TraktCommentReview>,
    isCommentsLoading: Boolean,
    isCommentsLoadingMore: Boolean,
    commentsCurrentPage: Int,
    commentsPageCount: Int,
    commentsError: String?,
    onRetryComments: () -> Unit,
    onLoadMoreComments: () -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    onTrailerClick: (MetaTrailer) -> Unit,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onOpenMeta: ((MetaPreview) -> Unit)?,
    onCastClick: ((MetaPerson, String?) -> Unit)?,
    onCompanyClick: ((MetaCompany, String) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val enabledItems = settings.items.filter { it.enabled }

    @Composable
    fun RenderSection(key: MetaScreenSectionKey) {
        when (key) {
            MetaScreenSectionKey.ACTIONS -> {
                DetailActionButtons(
                    playLabel = if (isPrimaryPlayEnabled) playButtonLabel else stringResource(Res.string.playback_unavailable),
                    playEnabled = isPrimaryPlayEnabled,
                    secondaryActions = buildList {
                        add(DetailSecondaryAction(
                            label = if (isWatched) {
                                stringResource(Res.string.hero_mark_unwatched)
                            } else {
                                stringResource(Res.string.hero_mark_watched)
                            },
                            icon = if (isWatched) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.CheckCircleOutline
                            },
                            isActive = isWatched,
                            onClick = onWatchedClick,
                        ))
                        add(DetailSecondaryAction(
                            label = if (isSaved) {
                                stringResource(Res.string.hero_remove_from_library)
                            } else {
                                stringResource(Res.string.hero_add_to_library)
                            },
                            icon = if (isSaved) {
                                Icons.Default.Check
                            } else {
                                Icons.Default.Add
                            },
                            isActive = isSaved,
                            onClick = onSaveClick,
                        ))
                        onSaveLongClick?.let { openListPicker ->
                            add(DetailSecondaryAction(
                                label = stringResource(Res.string.details_save_to_lists),
                                icon = Icons.Rounded.CollectionsBookmark,
                                onClick = openListPicker,
                            ))
                        }
                    },
                    isTablet = isTablet,
                    onPlayClick = onPrimaryPlayClick,
                    onPlayLongClick = if (showManualPlayOption) onPrimaryPlayLongClick else null,
                )
            }
            MetaScreenSectionKey.OVERVIEW -> {
                DetailMetaInfo(
                    meta = meta,
                    horizontalScrollPadding = horizontalScrollPadding,
                )
            }
            MetaScreenSectionKey.PARENTS_GUIDE -> {
                DetailParentsGuideSection(warnings = parentalWarnings)
            }
            MetaScreenSectionKey.PRODUCTION -> {
                if (hasProductionSection) {
                    DetailProductionSection(meta = meta, onCompanyClick = onCompanyClick)
                }
            }
            MetaScreenSectionKey.CAST -> {
                DetailCastSection(
                    cast = meta.cast,
                    horizontalScrollPadding = horizontalScrollPadding,
                    onCastClick = onCastClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    metaId = meta.id,
                    metaType = meta.type,
                )
            }
            MetaScreenSectionKey.COMMENTS -> {
                if (shouldShowComments && (isCommentsLoading || comments.isNotEmpty() || !commentsError.isNullOrBlank())) {
                    DetailCommentsSection(
                        comments = comments,
                        isLoading = isCommentsLoading,
                        isLoadingMore = isCommentsLoadingMore,
                        canLoadMore = commentsCurrentPage < commentsPageCount,
                        error = commentsError,
                        onRetry = onRetryComments,
                        onLoadMore = onLoadMoreComments,
                        onCommentClick = onCommentClick,
                        horizontalScrollPadding = horizontalScrollPadding,
                    )
                }
            }
            MetaScreenSectionKey.TRAILERS -> {
                if (hasTrailersSection) {
                    DetailTrailersSection(
                        trailers = meta.trailers,
                        onTrailerClick = onTrailerClick,
                        horizontalScrollPadding = horizontalScrollPadding,
                    )
                }
            }
            // Episodes are lazy list items of their own, see addLazyEpisodeListItems.
            MetaScreenSectionKey.EPISODES -> Unit
            MetaScreenSectionKey.DETAILS -> {
                if (hasAdditionalInfoSection) {
                    DetailAdditionalInfoSection(meta = meta)
                }
            }
            MetaScreenSectionKey.COLLECTION -> {
                if (!hasEpisodes && hasCollectionSection) {
                    DetailPosterRailSection(
                        title = meta.collectionName.orEmpty(),
                        items = meta.collectionItems,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        horizontalScrollPadding = horizontalScrollPadding,
                        onPosterClick = onOpenMeta,
                    )
                }
            }
            MetaScreenSectionKey.MORE_LIKE_THIS -> {
                if (moreLikeThisItems.isNotEmpty()) {
                    val sourceLabel = when (meta.moreLikeThisSource) {
                        MoreLikeThisSource.TMDB -> stringResource(Res.string.detail_more_like_this_powered_by_tmdb)
                        MoreLikeThisSource.TRAKT -> stringResource(Res.string.detail_more_like_this_powered_by_trakt)
                        null -> null
                    }
                    DetailPosterGridSection(
                        title = stringResource(Res.string.details_more_like_this),
                        items = moreLikeThisItems,
                        watchedKeys = watchedKeys,
                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        sourceLabel = sourceLabel,
                        onPosterClick = onOpenMeta,
                    )
                }
            }
        }
    }

    enabledItems.forEach { section -> RenderSection(section.key) }
}
