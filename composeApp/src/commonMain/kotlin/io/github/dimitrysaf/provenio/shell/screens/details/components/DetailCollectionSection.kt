package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.portraitArtworkCandidates
import io.github.dimitrysaf.provenio.core.home.stableKey
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import io.github.dimitrysaf.provenio.shell.components.CompletedOverlay
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.components.ShapedArtworkImage
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.episodes_cd_watched

/**
 * The titles of the collection as a segmented list, laid out as the trailers are: the poster on
 * the left, marked when watched, with the title and its year beside it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailCollectionSection(
    title: String,
    items: List<MetaPreview>,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    onItemClick: ((MetaPreview) -> Unit)?,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    if (items.isEmpty()) return
    val watchedDescription = stringResource(Res.string.episodes_cd_watched)
    DetailSection(title = title, modifier = modifier, showHeader = showHeader) {
        DetailSegmentedColumn {
            items.forEachIndexed { index, item ->
                key(item.stableKey()) {
                    SegmentedListItem(
                        onClick = { onItemClick?.invoke(item) },
                        enabled = onItemClick != null,
                        shapes = detailSegmentShapes(index = index, count = items.size),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .width(CollectionPosterWidth)
                                    .aspectRatio(2f / 3f)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                ShapedArtworkImage(
                                    candidates = item.portraitArtworkCandidates(),
                                    contentDescription = null,
                                    letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.matchParentSize(),
                                )
                                CompletedOverlay(
                                    visible = WatchingState.isPosterWatched(
                                        watchedKeys = watchedKeys,
                                        item = item,
                                        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                    ),
                                    badgeSize = CollectionWatchedBadgeSize,
                                    checkDescription = watchedDescription,
                                )
                            }
                        },
                        supportingContent = item.releaseInfo?.takeIf(String::isNotBlank)?.let { year ->
                            { FindableText(text = year, maxLines = 1) }
                        },
                    ) {
                        FindableText(text = item.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// As tall as a trailer's still beside it in the list.
private val CollectionPosterWidth = 48.dp
private val CollectionWatchedBadgeSize = 28.dp
