package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.SkeletonBlock
import com.nuvio.app.shell.theme.nuvio
import com.nuvio.app.shell.components.nuvioHorizontalScrollBleed
import com.nuvio.app.shell.components.withDuplicateSafeLazyKeys
import com.nuvio.app.features.trakt.TraktCommentReview
import kotlinx.coroutines.flow.distinctUntilChanged
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailCommentsSection(
    comments: List<TraktCommentReview>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, comments.size, canLoadMore, isLoadingMore, isLoading, error) {
        if (isLoading || !error.isNullOrBlank()) return@LaunchedEffect
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            canLoadMore && !isLoadingMore && totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) onLoadMore()
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showHeader) {
            DetailSectionTitle(title = stringResource(Res.string.detail_comments_title), fullWidth = false)
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                LazyRow(
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(3) {
                        LoadingCommentCard()
                    }
                }
            }

            !error.isNullOrBlank() -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(onClick = onRetry) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }

            comments.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.detail_comments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                LazyRow(
                    modifier = Modifier
                        .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = comments.withDuplicateSafeLazyKeys { it.id },
                        key = { it.lazyKey },
                    ) { keyedReview ->
                        val review = keyedReview.value
                        CommentCard(
                            review = review,
                            onClick = { onCommentClick(review) },
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "loading_more_comments") {
                            LoadingCommentCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(
    review: TraktCommentReview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bodyText = if (review.hasSpoilerContent) {
        stringResource(Res.string.detail_comments_spoiler_card)
    } else {
        review.comment
    }

    BoxWithConstraints {
        val size = commentCardSize(maxWidth)

        Card(
            onClick = onClick,
            modifier = modifier
                .width(size.width)
                .height(size.height),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = review.authorDisplayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (review.review) {
                    CommentChip(text = stringResource(Res.string.detail_comments_badge_review))
                }

                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (review.hasSpoilerContent) {
                        MaterialTheme.nuvio.colors.warning
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    review.rating?.let { rating ->
                        Text(
                            text = stringResource(Res.string.detail_comments_badge_rating, rating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.detail_comments_likes, review.likes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentChip(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun LoadingCommentCard() {
    BoxWithConstraints {
        val size = commentCardSize(maxWidth)

        Card(modifier = Modifier.width(size.width).height(size.height)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBlock(width = 96.dp, height = 14.dp)
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 12.dp)
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.94f), height = 12.dp)
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.68f), height = 12.dp)
                Spacer(modifier = Modifier.weight(1f))
                SkeletonBlock(width = 64.dp, height = 10.dp)
            }
        }
    }
}

private data class CommentCardSize(val width: Dp, val height: Dp)

private fun commentCardSize(maxWidth: Dp): CommentCardSize =
    if (maxWidth >= 720.dp) CommentCardSize(340.dp, 210.dp) else CommentCardSize(280.dp, 190.dp)
