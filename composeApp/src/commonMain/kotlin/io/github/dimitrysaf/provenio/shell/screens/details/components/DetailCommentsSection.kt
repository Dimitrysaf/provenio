package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.components.SkeletonBlock
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentReview
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The comments as a segmented list: the first few, then View all for the comments page when
 * [onViewAll] is given; every loaded comment when it is not.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun DetailCommentsSection(
    comments: List<TraktCommentReview>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onCommentClick: (TraktCommentReview) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onViewAll: (() -> Unit)? = null,
) {
    DetailSection(
        title = stringResource(Res.string.detail_comments_title),
        modifier = modifier,
        showHeader = showHeader,
    ) {
        when {
            isLoading -> DetailListSkeleton()

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

            else -> DetailSegmentedList(
                items = comments,
                previewCount = DetailListPreviewCount,
                showingAll = false,
                onViewAll = onViewAll,
            ) { review, shapes ->
                CommentRow(review = review, shapes = shapes, onClick = { onCommentClick(review) })
            }
        }
    }
}

/** One comment as a list row: its author above, the comment, then its rating and likes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CommentRow(
    review: TraktCommentReview,
    shapes: ListItemShapes,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        overlineContent = {
            FindableText(text = review.authorDisplayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = { CommentStats(rating = review.rating, likes = review.likes) },
        trailingContent = if (review.review) {
            { CommentChip(text = stringResource(Res.string.detail_comments_badge_review)) }
        } else {
            null
        },
    ) {
        if (review.hasSpoilerContent) {
            FindableText(
                text = stringResource(Res.string.detail_comments_spoiler_card),
                color = MaterialTheme.provenio.colors.warning,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            FindableText(text = review.comment, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Rows standing in for a Details list while it loads. */
@Composable
internal fun DetailListSkeleton(count: Int = 3) {
    DetailSegmentedColumn {
        repeat(count) { index ->
            DetailStaticSegment(index = index, count = count) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(width = 96.dp, height = 12.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 12.dp)
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f), height = 12.dp)
                }
            }
        }
    }
}

/** A comment's rating, as a star and its score out of ten, and its likes, as a thumb and their count. */
@Composable
internal fun CommentStats(rating: Int?, likes: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rating?.let {
            CommentStat(icon = Icons.Rounded.Star, text = stringResource(Res.string.detail_comments_badge_rating, it))
        }
        CommentStat(icon = Icons.Rounded.ThumbUp, text = stringResource(Res.string.detail_comments_likes, likes))
    }
}

@Composable
private fun CommentStat(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
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
