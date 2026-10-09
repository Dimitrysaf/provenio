package io.github.dimitrysaf.provenio.shell.screens.details

import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.shell.components.posterGridColumns
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleUiState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import io.github.dimitrysaf.provenio.shell.components.CollapsibleShelfHeader
import io.github.dimitrysaf.provenio.shell.components.ShelfExpansion
import io.github.dimitrysaf.provenio.shell.components.ShelfGridRow
import io.github.dimitrysaf.provenio.shell.components.ShelfGridSpacing
import io.github.dimitrysaf.provenio.shell.components.shelfGridColumns
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomePosterCard

/**
 * Where a details page's grid shelves go and how they behave: the width their rows have, the
 * padding and cap of the column they sit in, whether they start open, and the poster card width.
 */
internal class DetailShelfGridContext(
    val contentWidth: Dp,
    val horizontalPadding: Dp,
    val contentMaxWidth: Dp,
    val expandedByDefault: Boolean,
    val posterCellWidth: Dp,
    val posterCardStyle: PosterCardStyleUiState,
)

/**
 * A details page shelf as a grid: its title, which opens and closes it, then each row of items as
 * a list item of its own, so only the rows on screen are composed and only their images load.
 * Items keep their own width ([cellWidth]); spare width goes into the gaps. [footer] follows the
 * rows, for a source credit or the next page of comments, and shows even with no rows.
 * With [previewRows] above 0, only that many rows show until Show All next to the title opens the rest.
 */
internal fun <T> LazyListScope.detailShelfGrid(
    key: String,
    title: @Composable () -> String,
    entries: List<T>,
    cellWidth: Dp,
    context: DetailShelfGridContext,
    spacing: Dp = ShelfGridSpacing,
    columns: Int = shelfGridColumns(context.contentWidth, cellWidth, spacing),
    fillCells: Boolean = false,
    previewRows: Int = 0,
    headerLeading: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    cell: @Composable (index: Int, entry: T) -> Unit,
) {
    val expansionKey = "details:$key"
    val expanded = ShelfExpansion.isExpanded(expansionKey, context.expandedByDefault)
    val allRows = entries.withIndex().toList().chunked(columns)
    val canShowAll = previewRows > 0 && allRows.size > previewRows
    val showAllKey = "$expansionKey:all"
    val showingAll = canShowAll && ShelfExpansion.isExpanded(showAllKey, false)
    item(key = "$key-header", contentType = "detail-shelf-header") {
        DetailSectionContainer(
            horizontalPadding = context.horizontalPadding,
            contentMaxWidth = context.contentMaxWidth,
            bottomPadding = if (expanded) 14.dp else 20.dp,
            modifier = Modifier.animateItem(),
        ) {
            CollapsibleShelfHeader(
                title = title(),
                expanded = expanded,
                onToggle = { ShelfExpansion.toggle(expansionKey, context.expandedByDefault) },
                onViewAllClick = if (expanded && canShowAll) {
                    { ShelfExpansion.toggle(showAllKey, false) }
                } else {
                    null
                },
                viewAllLabel = stringResource(if (showingAll) Res.string.details_show_less else Res.string.shelf_show_all),
                leading = headerLeading,
            )
        }
    }
    if (!expanded) return

    val rows = if (canShowAll && !showingAll) allRows.take(previewRows) else allRows
    rows.forEachIndexed { rowIndex, row ->
        item(key = "$key-row-$rowIndex", contentType = "detail-shelf-row-$key") {
            DetailSectionContainer(
                horizontalPadding = context.horizontalPadding,
                contentMaxWidth = context.contentMaxWidth,
                bottomPadding = if (rowIndex == rows.lastIndex && footer == null) 20.dp else 16.dp,
                modifier = Modifier.animateItem(),
            ) {
                ShelfGridRow(
                    items = row,
                    columns = columns,
                    cellWidth = cellWidth,
                    spacing = spacing,
                    fillCells = fillCells,
                ) { indexed -> cell(indexed.index, indexed.value) }
            }
        }
    }
    if (footer != null) {
        item(key = "$key-footer", contentType = "detail-shelf-footer") {
            DetailSectionContainer(
                horizontalPadding = context.horizontalPadding,
                contentMaxWidth = context.contentMaxWidth,
                modifier = Modifier.animateItem(),
            ) {
                footer()
            }
        }
    }
}

/** A shelf of posters as a grid, drawn with the same poster card as every other shelf. */
internal fun LazyListScope.detailPosterShelfGrid(
    key: String,
    title: @Composable () -> String,
    items: List<MetaPreview>,
    context: DetailShelfGridContext,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    sourceLabel: (@Composable () -> String?)? = null,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    detailShelfGrid(
        key = key,
        title = title,
        entries = items,
        cellWidth = context.posterCellWidth,
        context = context,
        columns = posterGridColumns(context.contentWidth, context.posterCardStyle),
        fillCells = context.posterCardStyle.dynamicSizeEnabled,
        footer = sourceLabel?.let { label ->
            {
                label()?.takeIf(String::isNotBlank)?.let { text ->
                    Text(
                        text = text,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    ) { _, item ->
        HomePosterCard(
            item = item,
            isWatched = WatchingState.isPosterWatched(
                watchedKeys = watchedKeys,
                item = item,
                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
            ),
            onClick = onPosterClick?.let { { it(item) } },
            onLongClick = onPosterLongClick?.let { { it(item) } },
        )
    }
}
