package com.nuvio.app.shell.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.components.NuvioNetworkOfflineCard
import com.nuvio.app.shell.components.SingleChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption
import com.nuvio.app.core.home.MetaPreview
import com.nuvio.app.shell.screens.home.components.PosterGridRow
import com.nuvio.app.shell.screens.home.components.PosterGridSkeletonRow
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.search.DiscoverEmptyStateReason
import com.nuvio.app.core.search.DiscoverUiState

internal fun LazyListScope.discoverContent(
    state: DiscoverUiState,
    isSourceLoading: Boolean,
    columns: Int,
    networkCondition: NetworkCondition,
    onTypeSelected: (String) -> Unit,
    onCatalogSelected: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onRetry: (() -> Unit)? = null,
    watchedKeys: Set<String> = emptySet(),
    fullyWatchedSeriesKeys: Set<String> = emptySet(),
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
) {
    item {
        DiscoverHeaderRow(
            state = state,
            modifier = Modifier.padding(horizontal = 16.dp),
            onTypeSelected = onTypeSelected,
            onCatalogSelected = onCatalogSelected,
            onGenreSelected = onGenreSelected,
        )
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

/**
 * The heading, and the one control that narrows what is under it.
 *
 * Three chips spent a whole row saying what could be filtered; one button says the same thing and
 * gives the row back to the titles. Pressing it opens the list of filters, and picking one opens
 * that filter's own choices — each a sheet, because each is a list to pick from.
 */
@Composable
private fun DiscoverHeaderRow(
    state: DiscoverUiState,
    onTypeSelected: (String) -> Unit,
    onCatalogSelected: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilters by remember { mutableStateOf(false) }
    var openFilter by remember { mutableStateOf<DiscoverFilter?>(null) }

    val allGenresLabel = stringResource(Res.string.discover_all_genres)
    val selectedCatalog = state.selectedCatalog
    val typeLabel = state.selectedType?.displayTypeLabel() ?: stringResource(Res.string.discover_type)
    val catalogLabel = selectedCatalog?.catalogName ?: stringResource(Res.string.discover_catalog)
    val genreLabel = state.selectedGenre ?: allGenresLabel
    val canFilter = state.typeOptions.isNotEmpty() || state.catalogOptions.isNotEmpty()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.compose_search_discover_title),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FilledTonalIconButton(
            onClick = { showFilters = true },
            enabled = canFilter,
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(Res.string.discover_filters),
            )
        }
    }

    if (showFilters) {
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_filters),
            options = listOf(
                SingleChoiceOption(
                    value = DiscoverFilter.TYPE,
                    label = stringResource(Res.string.discover_type),
                    supportingText = typeLabel,
                    enabled = state.typeOptions.isNotEmpty(),
                ),
                SingleChoiceOption(
                    value = DiscoverFilter.CATALOG,
                    label = stringResource(Res.string.discover_catalog),
                    supportingText = catalogLabel,
                    enabled = state.catalogOptions.isNotEmpty(),
                ),
                SingleChoiceOption(
                    value = DiscoverFilter.GENRE,
                    label = stringResource(Res.string.discover_genre),
                    supportingText = genreLabel,
                    enabled = state.genreOptions.isNotEmpty() || selectedCatalog?.genreRequired == true,
                ),
            ),
            // Nothing here is chosen; each row opens the choices for that filter.
            isSelected = { false },
            onSelected = { filter -> openFilter = filter },
            onDismiss = { showFilters = false },
        )
    }

    when (openFilter) {
        DiscoverFilter.TYPE -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_select_type),
            options = state.typeOptions.map { type ->
                SingleChoiceOption(value = type, label = type.displayTypeLabel())
            },
            isSelected = { it == state.selectedType },
            onSelected = onTypeSelected,
            onDismiss = { openFilter = null },
        )

        DiscoverFilter.CATALOG -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_select_catalog),
            options = state.catalogOptions.map { option ->
                SingleChoiceOption(value = option.key, label = option.catalogName)
            },
            isSelected = { it == state.selectedCatalogKey },
            onSelected = onCatalogSelected,
            onDismiss = { openFilter = null },
        )

        DiscoverFilter.GENRE -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.discover_select_genre),
            options = buildList {
                if (selectedCatalog?.genreRequired != true) {
                    add(SingleChoiceOption(value = "", label = allGenresLabel))
                }
                state.genreOptions.forEach { genre ->
                    add(SingleChoiceOption(value = genre, label = genre))
                }
            },
            isSelected = { it == (state.selectedGenre ?: "") },
            onSelected = { genre -> onGenreSelected(genre.ifBlank { null }) },
            onDismiss = { openFilter = null },
        )

        null -> Unit
    }
}

/** Which filter's choices are open. */
private enum class DiscoverFilter {
    TYPE,
    CATALOG,
    GENRE,
}

@Composable
private fun CatalogLoadingFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        NuvioLoadingIndicator(
            modifier = Modifier.size(22.dp),
        )
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
        NuvioNetworkOfflineCard(
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
private fun String.displayTypeLabel(): String =
    when (lowercase()) {
        "movie" -> stringResource(Res.string.media_movies)
        "series" -> stringResource(Res.string.media_series)
        "anime" -> stringResource(Res.string.media_anime)
        "channel" -> stringResource(Res.string.media_channels)
        "tv" -> stringResource(Res.string.media_tv)
        else -> replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
