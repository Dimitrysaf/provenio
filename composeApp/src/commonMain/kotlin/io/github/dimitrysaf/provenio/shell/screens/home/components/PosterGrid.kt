package io.github.dimitrysaf.provenio.shell.screens.home.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.PosterLandscapeAspectRatio
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import io.github.dimitrysaf.provenio.shell.components.SkeletonPoster
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import io.github.dimitrysaf.provenio.shell.components.ShelfGridRow
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCellWidth
import androidx.compose.foundation.layout.width

/**
 * How many posters fit across, at the width the person chose for them.
 *
 * Poster Card Style sets a poster's width, so the grid asks how many of those fit rather than
 * picking a count from the screen size and stretching whatever lands in it. Landscape mode makes
 * each one wider, so fewer fit, which is the same arithmetic.
 */
@Composable
internal fun rememberPosterGridColumnCount(availableWidth: Dp): Int {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val tileWidth = if (posterCardStyle.catalogLandscapeModeEnabled) {
        landscapePosterWidth(posterCardStyle.widthDp)
    } else {
        posterCardStyle.widthDp.dp
    }
    return remember(availableWidth, tileWidth) {
        val fits = (availableWidth + PosterGridSpacing).value / (tileWidth + PosterGridSpacing).value
        fits.toInt().coerceAtLeast(1)
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
    ) {
        SkeletonPoster(
            modifier = Modifier.width(cellWidth),
            aspectRatio = if (posterCardStyle.catalogLandscapeModeEnabled) PosterLandscapeAspectRatio else 0.675f,
            cornerRadius = posterCardStyle.cornerRadiusDp.dp,
            showLabels = !posterCardStyle.hideLabelsEnabled,
        )
    }
}
