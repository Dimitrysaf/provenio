package io.github.dimitrysaf.provenio.shell.screens.details.components

import io.github.dimitrysaf.provenio.shell.components.horizontalShelfColumns
import io.github.dimitrysaf.provenio.shell.components.snapsHorizontalShelves
import io.github.dimitrysaf.provenio.shell.components.horizontalShelfRows
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.horizontalScrollBleed
import io.github.dimitrysaf.provenio.shell.components.ShelfSection
import io.github.dimitrysaf.provenio.shell.components.PosterLandscapeAspectRatio
import io.github.dimitrysaf.provenio.shell.components.SkeletonPoster
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.PosterShape
import io.github.dimitrysaf.provenio.shell.screens.home.components.HomePosterCard
import io.github.dimitrysaf.provenio.core.home.stableKey
import io.github.dimitrysaf.provenio.core.watch.watching.application.WatchingState
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbMetadataService
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsRepository

@Composable
fun DetailPosterRailSection(
    title: String,
    items: List<MetaPreview>,
    watchedKeys: Set<String>,
    modifier: Modifier = Modifier,
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    showHeader: Boolean = true,
    headerHorizontalPadding: Dp = 0.dp,
    horizontalScrollPadding: Dp = 0.dp,
    sourceLabel: String? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val posterCardStyle = rememberPosterCardStyleUiState()
    val tmdbSettings by remember {
        TmdbSettingsRepository.ensureLoaded()
        TmdbSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth()) {
        ShelfSection(
            title = if (showHeader) title else "",
            entries = items,
            headerHorizontalPadding = headerHorizontalPadding,
            rowContentPadding = PaddingValues(
                horizontal = headerHorizontalPadding + horizontalScrollPadding,
            ),
            rowModifier = Modifier.horizontalScrollBleed(horizontalScrollPadding),
            key = { item -> item.stableKey() },
            rows = posterCardStyle.horizontalShelfRows,
            columns = posterCardStyle.horizontalShelfColumns,
            snapToItems = posterCardStyle.snapsHorizontalShelves,
        ) { item ->
            val landscape = posterCardStyle.catalogLandscapeModeEnabled || item.posterShape == PosterShape.Landscape
            val localizedBackdrop = rememberDetailBackdrop(
                item = item,
                language = tmdbSettings.language,
                enabled = landscape && (item.id.startsWith("tmdb:") || tmdbSettings.enabled && tmdbSettings.useArtwork),
            )
            if (localizedBackdrop.isLoading) {
                SkeletonPoster(
                    modifier = Modifier.width(landscapePosterWidth(posterCardStyle.widthDp)),
                    aspectRatio = PosterLandscapeAspectRatio,
                    cornerRadius = posterCardStyle.cornerRadiusDp.dp,
                    showLabels = !posterCardStyle.hideLabelsEnabled,
                    showDetail = false,
                )
            } else {
                val cardItem = remember(item, localizedBackdrop.url) {
                    localizedBackdrop.url?.let { item.copy(banner = it) } ?: item
                }
                HomePosterCard(
                    item = cardItem,
                    useLandscapeBackdropMode = landscape,
                    showLandscapeOverlay = false,
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

        sourceLabel
            ?.takeIf { it.isNotBlank() }
            ?.let { label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = headerHorizontalPadding, top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

@Composable
private fun rememberDetailBackdrop(item: MetaPreview, language: String, enabled: Boolean): DetailBackdrop {
    var backdrop by remember(item.id, item.type, language, enabled) {
        mutableStateOf(DetailBackdrop(isLoading = enabled))
    }
    LaunchedEffect(item.id, item.type, language, enabled) {
        if (enabled) {
            backdrop = DetailBackdrop(
                url = TmdbMetadataService.fetchLocalizedBackdrop(item.id, item.type, language),
                isLoading = false,
            )
        }
    }
    return backdrop
}

private data class DetailBackdrop(val url: String? = null, val isLoading: Boolean)
