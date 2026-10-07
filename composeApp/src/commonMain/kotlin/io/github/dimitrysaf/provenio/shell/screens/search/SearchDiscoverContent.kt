package io.github.dimitrysaf.provenio.shell.screens.search

import io.github.dimitrysaf.provenio.shell.components.SmallLoadingSpinner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.NetworkOfflineCard
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.shell.screens.home.components.PosterGridRow
import io.github.dimitrysaf.provenio.shell.screens.home.components.PosterGridSkeletonRow
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.search.DiscoverEmptyStateReason
import io.github.dimitrysaf.provenio.core.search.DiscoverUiState

internal fun LazyListScope.discoverContent(
    state: DiscoverUiState,
    isSourceLoading: Boolean,
    columns: Int,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
    watchedKeys: Set<String> = emptySet(),
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    item {
        DiscoverHeaderRow(modifier = Modifier.padding(horizontal = 16.dp))
    }
    state.selectedCatalog?.let { selectedCatalog ->
        item {
            Text(
                text = stringResource(
                    Res.string.discover_catalog_context,
                    selectedCatalog.addonName,
                    selectedCatalog.type.displayTypeLabel(),
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    when {
        (state.isLoading || isSourceLoading) && state.items.isEmpty() -> {
            items(2) {
                PosterGridSkeletonRow(
                    columns = columns,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        state.items.isEmpty() -> {
            item {
                DiscoverEmptyStateCard(
                    reason = state.emptyStateReason,
                    errorMessage = state.errorMessage,
                    networkCondition = networkCondition,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        else -> {
            items(count = (state.items.size + columns - 1) / columns) { rowIndex ->
                val firstIndex = rowIndex * columns
                PosterGridRow(
                    items = state.items.subList(firstIndex, minOf(firstIndex + columns, state.items.size)),
                    columns = columns,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    watchedKeys = watchedKeys,
                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                    onPosterClick = onPosterClick,
                    onPosterLongClick = onPosterLongClick,
                )
            }
            if (state.isLoading) {
                item {
                    CatalogLoadingFooter(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/** The Discover heading; its filters live in the search field's filter button. */
@Composable
private fun DiscoverHeaderRow(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.compose_search_discover_title),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun CatalogLoadingFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        SmallLoadingSpinner(size = 22.dp)
    }
}

@Composable
private fun DiscoverEmptyStateCard(
    reason: DiscoverEmptyStateReason?,
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (
        reason == DiscoverEmptyStateReason.RequestFailed &&
        (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable)
    ) {
        NetworkOfflineCard(
            condition = networkCondition,
            modifier = modifier,
            onRetry = onRetry,
        )
        return
    }

    val title: String
    val message: String
    val icon: ImageVector

    when (reason) {
        DiscoverEmptyStateReason.NoActiveAddons -> {
            title = stringResource(Res.string.compose_search_empty_no_active_addons_title)
            message = stringResource(Res.string.discover_empty_no_active_addons_message)
            icon = Icons.Rounded.Extension
        }

        DiscoverEmptyStateReason.NoDiscoverCatalogs -> {
            title = stringResource(Res.string.discover_empty_no_catalogs_title)
            message = stringResource(Res.string.discover_empty_no_catalogs_message)
            icon = Icons.Rounded.GridView
        }

        DiscoverEmptyStateReason.RequestFailed -> {
            title = stringResource(Res.string.discover_empty_load_failed_title)
            message = errorMessage ?: stringResource(Res.string.discover_empty_load_failed_message)
            icon = Icons.Rounded.ErrorOutline
        }

        DiscoverEmptyStateReason.NoResults, null -> {
            title = stringResource(Res.string.discover_empty_no_results_title)
            message = stringResource(Res.string.discover_empty_no_results_message)
            icon = Icons.Rounded.SearchOff
        }
    }

    EmptyState(
        icon = icon,
        modifier = modifier,
        title = title,
        message = message,
        actionLabel = if (reason == DiscoverEmptyStateReason.RequestFailed) {
            stringResource(Res.string.action_retry)
        } else {
            null
        },
        onActionClick = if (reason == DiscoverEmptyStateReason.RequestFailed) onRetry else null,
    )
}

@Composable
internal fun String.displayTypeLabel(): String =
    when (lowercase()) {
        "movie" -> stringResource(Res.string.media_movies)
        "series" -> stringResource(Res.string.media_series)
        "anime" -> stringResource(Res.string.media_anime)
        "channel" -> stringResource(Res.string.media_channels)
        "tv" -> stringResource(Res.string.media_tv)
        else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
