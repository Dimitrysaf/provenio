package io.github.dimitrysaf.provenio.shell.screens.home.components

import io.github.dimitrysaf.provenio.shell.components.PosterPortraitAspectRatio
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.PosterLandscapeAspectRatio
import io.github.dimitrysaf.provenio.shell.components.posterGridColumns
import io.github.dimitrysaf.provenio.shell.components.SkeletonPoster
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import io.github.dimitrysaf.provenio.shell.components.ShelfGridRow
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCellWidth
import androidx.compose.foundation.layout.width

/**
 * How many posters fit across, at the width the person chose for them, or the number of cards
 * per row they chose for dynamic sizing. Landscape mode makes each one wider, so fewer fit.
 */
@Composable
internal fun rememberPosterGridColumnCount(availableWidth: Dp): Int {
    val posterCardStyle = rememberPosterCardStyleUiState()
    return remember(availableWidth, posterCardStyle) {
        posterGridColumns(availableWidth, posterCardStyle, PosterGridSpacing)
    }
}

/** The gap between posters, which the column count has to account for. */
internal val PosterGridSpacing = 12.dp

/**
 * One row of a poster grid. Each poster is the card every shelf draws, at the poster card style's
 * width, height and corners, never stretched to fill its column: spare width goes into the gaps.
 */
@Composable
internal fun PosterGridRow(
    items: List<MetaPreview>,
    columns: Int,
    modifier: Modifier = Modifier,
    watchedKeys: Set<String> = emptySet(),
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    ShelfGridRow(
        items = items,
        columns = columns,
        cellWidth = rememberPosterCellWidth(),
        modifier = modifier,
        spacing = PosterGridSpacing,
        fillCells = rememberPosterCardStyleUiState().dynamicSizeEnabled,
    ) { item ->
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

@Composable
internal fun PosterGridSkeletonRow(
    columns: Int,
    modifier: Modifier = Modifier,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val cellWidth = rememberPosterCellWidth()
    ShelfGridRow(
        items = List(columns) { it },
        columns = columns,
        cellWidth = cellWidth,
        modifier = modifier,
        spacing = PosterGridSpacing,
        fillCells = posterCardStyle.dynamicSizeEnabled,
    ) {
        SkeletonPoster(
            modifier = Modifier.width(cellWidth),
            aspectRatio = if (posterCardStyle.catalogLandscapeModeEnabled) PosterLandscapeAspectRatio else PosterPortraitAspectRatio,
            cornerRadius = posterCardStyle.cornerRadiusDp.dp,
            showLabels = !posterCardStyle.hideLabelsEnabled,
        )
    }
}
