package io.github.dimitrysaf.provenio.shell.components

import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleUiState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A short shelf laid out as a grid that wraps to the page's width instead of a row that scrolls
 * sideways, so scrolling the page is never caught by a row on the way.
 *
 * Items keep the width they choose; the first one sets how many fit across, and the gaps grow a
 * little to spread a row over the width. Every item is composed, so this suits shelves of a few
 * items, such as a collection's folders; long shelves give each grid row its own list item so only
 * the rows on screen are drawn.
 */
@Composable
fun <T> ShelfGrid(
    entries: List<T>,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    itemSpacing: Dp = 10.dp,
    rowSpacing: Dp = 16.dp,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val paddingPx = horizontalPadding.roundToPx()
        val spacingPx = itemSpacing.roundToPx()
        val rowSpacingPx = rowSpacing.roundToPx()
        val width = constraints.maxWidth
        val available = (width - paddingPx * 2).coerceAtLeast(0)
        if (entries.isEmpty()) return@SubcomposeLayout layout(width, 0) {}

        val itemConstraints = Constraints(maxWidth = available)
        val placeables = entries.mapIndexed { index, entry ->
            subcompose(key?.invoke(entry) ?: index) { itemContent(entry) }
                .first()
                .measure(itemConstraints)
        }
        val cellWidth = placeables.first().width.coerceAtLeast(1)
        val columns = ((available + spacingPx) / (cellWidth + spacingPx)).coerceAtLeast(1)
        val gap = if (columns > 1) {
            ((available - columns * cellWidth) / (columns - 1)).coerceIn(spacingPx, spacingPx * 3)
        } else {
            0
        }
        val gridWidth = columns * cellWidth + (columns - 1) * gap
        val startX = paddingPx + ((available - gridWidth) / 2).coerceAtLeast(0)
        val rows = placeables.chunked(columns)
        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val height = rowHeights.sum() + rowSpacingPx * (rows.size - 1)

        layout(width, height) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { column, placeable ->
                    placeable.placeRelative(startX + column * (cellWidth + gap), y)
                }
                y += rowHeights[rowIndex] + rowSpacingPx
            }
        }
    }
}

/** The smallest gap between grid cells; spare width widens it, up to three times this. */
val ShelfGridSpacing = 12.dp

/** How many cells of [cellWidth] fit across [availableWidth] with at least [spacing] between them. */
fun shelfGridColumns(availableWidth: Dp, cellWidth: Dp, spacing: Dp = ShelfGridSpacing): Int =
    ((availableWidth + spacing) / (cellWidth + spacing)).toInt().coerceAtLeast(1)

/**
 * How many posters a grid [availableWidth] wide shows across: the number of columns chosen in Card
 * styles when dynamic sizing has one, as long as each poster stays at least [MinPosterWidthDp]
 * wide, otherwise as many of the style's cards as fit.
 */
internal fun posterGridColumns(
    availableWidth: Dp,
    style: PosterCardStyleUiState,
    spacing: Dp = ShelfGridSpacing,
): Int {
    val landscape = style.catalogLandscapeModeEnabled
    if (style.dynamicSizeEnabled && style.cardsPerRow > 0) {
        val narrowest = if (landscape) landscapePosterWidth(MinPosterWidthDp) else MinPosterWidthDp.dp
        return minOf(style.cardsPerRow, shelfGridColumns(availableWidth, narrowest, spacing))
    }
    val cellWidth = if (landscape) landscapePosterWidth(style.widthDp) else style.widthDp.dp
    return shelfGridColumns(availableWidth, cellWidth, spacing)
}

/** How many rows of cards a horizontal poster shelf holds under the card style: at least one. */
internal val PosterCardStyleUiState.horizontalShelfRows: Int
    get() = shelfRows.coerceAtLeast(1)

/** Whether horizontal poster shelves snap to whole columns: when the columns or rows are set by hand. */
internal val PosterCardStyleUiState.snapsHorizontalShelves: Boolean
    get() = (dynamicSizeEnabled && cardsPerRow > 0) || shelfRows > 1

/**
 * One row of a grid shelf whose rows are separate list items, so only the rows on screen are
 * composed. Every cell is [cellWidth] wide, the width the item draws itself at (the poster card
 * style's, for posters), and is never stretched: spare width goes into the gaps instead, and every
 * row uses the same gaps so the columns line up. A short last row starts at the left like the rest.
 * With [fillCells] the cells share the row's width instead and each item is drawn at its cell's
 * width, as dynamic poster sizing wants. Cells never get wider than [columns] of them can fit.
 */
@Composable
fun <T> ShelfGridRow(
    items: List<T>,
    columns: Int,
    cellWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = ShelfGridSpacing,
    fillCells: Boolean = false,
    cell: @Composable (T) -> Unit,
) {
    androidx.compose.ui.layout.Layout(
        content = { items.forEach { item -> cell(item) } },
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val spacingPx = spacing.roundToPx()
        val fittingPx = ((width - spacingPx * (columns - 1)) / columns.coerceAtLeast(1)).coerceAtLeast(1)
        val cellPx = if (fillCells) fittingPx else cellWidth.roundToPx().coerceAtMost(fittingPx)
        val gap = if (columns > 1) {
            ((width - columns * cellPx) / (columns - 1)).coerceIn(spacingPx, spacingPx * 3)
        } else {
            0
        }
        val gridWidth = columns * cellPx + (columns - 1) * gap
        val startX = ((width - gridWidth) / 2).coerceAtLeast(0)
        val itemConstraints = if (fillCells) Constraints.fixedWidth(cellPx) else Constraints(maxWidth = cellPx)
        val placeables = measurables.map { it.measure(itemConstraints) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val x = startX + index * (cellPx + gap) + (cellPx - placeable.width).coerceAtLeast(0) / 2
                placeable.placeRelative(x, 0)
            }
        }
    }
}

/** A poster card's width under the poster card style: the cell width of a poster grid. */
@Composable
internal fun rememberPosterCellWidth(landscape: Boolean = false): Dp {
    val style = rememberPosterCardStyleUiState()
    return if (landscape || style.catalogLandscapeModeEnabled) landscapePosterWidth(style.widthDp) else style.widthDp.dp
}
