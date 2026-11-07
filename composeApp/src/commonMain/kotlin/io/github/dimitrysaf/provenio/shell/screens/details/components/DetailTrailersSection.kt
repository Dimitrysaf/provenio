package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.SkeletonAsyncImage
import io.github.dimitrysaf.provenio.shell.components.FindableText
import io.github.dimitrysaf.provenio.shell.components.LocalWindowBreakpoint
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.core.metadata.MetaTrailer
import io.github.dimitrysaf.provenio.core.metadata.youtubeThumbnailUrl
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** The trailers grouped by kind, and which kind is showing. */
@Stable
internal class TrailerCategories(
    val grouped: Map<String, List<MetaTrailer>>,
    initial: String,
) {
    var selected by mutableStateOf(initial)
    val selectedTrailers: List<MetaTrailer>
        get() = grouped[selected].orEmpty()
    val hasChoice: Boolean
        get() = grouped.size > 1
}

@Composable
internal fun rememberTrailerCategories(trailers: List<MetaTrailer>): TrailerCategories {
    val trailerLabel = stringResource(Res.string.detail_tab_trailer)
    return remember(trailers, trailerLabel) {
        val grouped = trailers.groupBy { trailer -> trailer.type.ifBlank { trailerLabel } }
        val initial = grouped.keys.firstOrNull { it.equals(trailerLabel, ignoreCase = true) }
            ?: grouped.keys.firstOrNull().orEmpty()
        TrailerCategories(grouped, initial)
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun DetailTrailersSection(
    trailers: List<MetaTrailer>,
    onTrailerClick: (MetaTrailer) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
) {
    if (trailers.isEmpty()) return

    val categories = rememberTrailerCategories(trailers)
    val trailersTitle = stringResource(Res.string.detail_trailers_title)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (categories.hasChoice || showHeader) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrailerCategoryPicker(categories)
                if (showHeader) {
                    DetailSectionTitle(title = trailersTitle, fullWidth = false)
                }
            }
        }

        // Laid out as the episode list is: a still on the left, the title and year beside it.
        val trailersShown = categories.selectedTrailers
        DetailSegmentedColumn {
            trailersShown.forEachIndexed { index, trailer ->
                key(trailerKey(trailer, index)) {
                    TrailerRow(
                        trailer = trailer,
                        shapes = detailSegmentShapes(index = index, count = trailersShown.size),
                        onClick = { onTrailerClick(trailer) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrailerRow(
    trailer: MetaTrailer,
    shapes: ListItemShapes,
    onClick: () -> Unit,
) {
    val title = trailer.displayName?.takeIf { it.isNotBlank() } ?: trailer.name
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            SkeletonAsyncImage(
                model = trailer.youtubeThumbnailUrl(),
                contentDescription = null,
                modifier = Modifier
                    .width(TrailerThumbnailWidth)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Crop,
            )
        },
        supportingContent = trailer.publishedAt?.take(4)?.takeIf { it.isNotBlank() }?.let { year ->
            { FindableText(text = year, maxLines = 1) }
        },
    ) {
        FindableText(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private val TrailerThumbnailWidth = 128.dp

internal fun trailerKey(trailer: MetaTrailer, index: Int): String =
    "${trailer.type}-${trailer.id}-${trailer.seasonNumber ?: 0}#$index"

/** The chip that picks which kind of trailer shows, when there is more than one kind. */
@Composable
internal fun TrailerCategoryPicker(categories: TrailerCategories) {
    if (!categories.hasChoice) return
    var categorySheetVisible by remember { mutableStateOf(false) }
    val trailersTitle = stringResource(Res.string.detail_trailers_title)
    val useCategoryMenu = LocalWindowBreakpoint.current.isTwoPane

    Box {
        FilterChip(
            selected = true,
            onClick = { categorySheetVisible = true },
            label = {
                Text(
                    text = categories.selected,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        if (useCategoryMenu) {
            DropdownMenu(
                expanded = categorySheetVisible,
                onDismissRequest = { categorySheetVisible = false },
            ) {
                categories.grouped.forEach { (category, categoryTrailers) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    Res.string.detail_trailer_category_count,
                                    category,
                                    categoryTrailers.size,
                                ),
                            )
                        },
                        onClick = {
                            categories.selected = category
                            categorySheetVisible = false
                        },
                        leadingIcon = { RadioButton(selected = category == categories.selected, onClick = null) },
                    )
                }
            }
        }
    }

    if (categorySheetVisible && !useCategoryMenu) {
        SingleChoiceBottomSheet(
            title = trailersTitle,
            options = categories.grouped.map { (category, categoryTrailers) ->
                SingleChoiceOption(
                    value = category,
                    label = stringResource(Res.string.detail_trailer_category_count, category, categoryTrailers.size),
                )
            },
            isSelected = { it == categories.selected },
            onSelected = { categories.selected = it },
            onDismiss = { categorySheetVisible = false },
        )
    }
}
