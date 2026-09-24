package com.nuvio.app.shell.screens.collection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nuvio.app.shell.components.NuvioScreen
import com.nuvio.app.shell.components.NuvioSearchField
import com.nuvio.app.shell.components.PlatformBackHandler
import com.nuvio.app.shell.components.SingleChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption
import com.nuvio.app.core.tracking.trakt.TraktPublicListSearchResult
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.collection.CollectionEditorRepository
import com.nuvio.app.core.collection.CollectionEditorUiState
import com.nuvio.app.core.collection.CollectionSource
import com.nuvio.app.core.collection.TmdbCollectionMediaType
import com.nuvio.app.core.collection.TraktListSort
import com.nuvio.app.core.collection.TraktSortHow

@Composable
internal fun TraktSourcePickerScreen(
    state: CollectionEditorUiState,
    onBack: () -> Unit,
) {
    val searchResultsTitle = stringResource(Res.string.collections_editor_trakt_search_results)
    val trendingTitle = stringResource(Res.string.collections_editor_trakt_trending)
    val popularTitle = stringResource(Res.string.collections_editor_trakt_popular)

    PlatformBackHandler(enabled = true) {
        onBack()
    }

    NuvioScreen(
        title = if (state.editingTraktSourceIndex != null) {
                    stringResource(Res.string.collections_editor_edit_trakt_source)
                } else {
                    stringResource(Res.string.collections_editor_trakt_sources)
                },
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        bottomBar = {
            CollectionEditorActionBar {
                Button(
                    onClick = { CollectionEditorRepository.addTraktSourceFromInput() },
                    modifier = Modifier.weight(1f),
                    enabled = state.traktInput.isNotBlank(),
                ) {
                    Text(
                        text = if (state.editingTraktSourceIndex != null) {
                            stringResource(Res.string.collections_editor_save)
                        } else {
                            stringResource(Res.string.collections_editor_add_source)
                        },
                    )
                }
            }
        },
    ) {

        item {
            NuvioSearchField(
                query = state.traktInput,
                onQueryChange = { CollectionEditorRepository.setTraktInput(it) },
                placeholder = stringResource(Res.string.collections_editor_trakt_input_placeholder),
                divided = true,
                onSearch = { CollectionEditorRepository.searchTraktLists() },
            )
        }

        item {
            // Same shape as the TMDB form: one segmented group, fields included.
            CollectionEditorGroup {
                row { shape ->
                    PresetFieldRow(
                        label = stringResource(Res.string.collections_editor_tmdb_display_title),
                        value = state.traktTitleInput,
                        onValueChange = { CollectionEditorRepository.setTraktTitleInput(it) },
                        shape = shape,
                        supportingText = stringResource(Res.string.collections_editor_tmdb_title_helper),
                        placeholder = stringResource(Res.string.collections_editor_trakt_title_placeholder),
                    )
                }
            }
        }

        if (state.traktSearchError != null) {
            item {
                Text(
                    text = state.traktSearchError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            var showMediaSheet by remember { mutableStateOf(false) }
            var showSortSheet by remember { mutableStateOf(false) }
            var showDirectionSheet by remember { mutableStateOf(false) }
            val mediaOptions = tmdbMediaChoices()
            val currentMedia = when {
                state.traktMediaBoth -> TmdbMediaChoice.BOTH
                state.traktMediaType == TmdbCollectionMediaType.TV -> TmdbMediaChoice.TV
                else -> TmdbMediaChoice.MOVIE
            }
            val sortOptions = traktSortOptions().map { (value, label) ->
                SingleChoiceOption(value = value, label = label)
            }
            val ascending = stringResource(Res.string.collections_editor_trakt_ascending)
            val descending = stringResource(Res.string.collections_editor_trakt_descending)
            val directionOptions = listOf(
                SingleChoiceOption(value = TraktSortHow.ASC.value, label = ascending),
                SingleChoiceOption(value = TraktSortHow.DESC.value, label = descending),
            )

            CollectionEditorGroup {
                row { shape ->
                    CollectionEditorChoiceRow(
                        title = stringResource(Res.string.collections_editor_tmdb_type),
                        value = mediaOptions.first { it.value == currentMedia }.label,
                        onClick = { showMediaSheet = true },
                        shape = shape,
                    )
                }
                row { shape ->
                    CollectionEditorChoiceRow(
                        title = stringResource(Res.string.collections_editor_tmdb_sort),
                        value = sortOptions.firstOrNull { it.value == state.traktSortBy }?.label
                            ?: sortOptions.first().label,
                        onClick = { showSortSheet = true },
                        shape = shape,
                    )
                }
                row { shape ->
                    CollectionEditorChoiceRow(
                        title = stringResource(Res.string.collections_editor_trakt_direction),
                        value = if (state.traktSortHow == TraktSortHow.DESC.value) descending else ascending,
                        onClick = { showDirectionSheet = true },
                        shape = shape,
                    )
                }
            }

            if (showMediaSheet) {
                SingleChoiceBottomSheet(
                    title = stringResource(Res.string.collections_editor_tmdb_type),
                    options = mediaOptions,
                    isSelected = { it == currentMedia },
                    onSelected = { choice ->
                        CollectionEditorRepository.setTraktMediaBoth(choice == TmdbMediaChoice.BOTH)
                        when (choice) {
                            TmdbMediaChoice.MOVIE ->
                                CollectionEditorRepository.setTraktMediaType(TmdbCollectionMediaType.MOVIE)
                            TmdbMediaChoice.TV ->
                                CollectionEditorRepository.setTraktMediaType(TmdbCollectionMediaType.TV)
                            TmdbMediaChoice.BOTH -> Unit
                        }
                    },
                    onDismiss = { showMediaSheet = false },
                )
            }

            if (showSortSheet) {
                SingleChoiceBottomSheet(
                    title = stringResource(Res.string.collections_editor_tmdb_sort),
                    options = sortOptions,
                    isSelected = { it == state.traktSortBy },
                    onSelected = { CollectionEditorRepository.setTraktSortBy(it) },
                    onDismiss = { showSortSheet = false },
                )
            }

            if (showDirectionSheet) {
                SingleChoiceBottomSheet(
                    title = stringResource(Res.string.collections_editor_trakt_direction),
                    options = directionOptions,
                    isSelected = { it == state.traktSortHow },
                    onSelected = { CollectionEditorRepository.setTraktSortHow(it) },
                    onDismiss = { showDirectionSheet = false },
                )
            }
        }

        TraktResultSection(
            title = searchResultsTitle,
            results = state.traktSearchResults,
        )
        TraktResultSection(
            title = trendingTitle,
            results = state.traktTrendingResults,
        )
        TraktResultSection(
            title = popularTitle,
            results = state.traktPopularResults,
        )
    }
}

internal fun LazyListScope.TraktResultSection(
    title: String,
    results: List<TraktPublicListSearchResult>,
) {
    if (results.isEmpty()) return
    item {
        PickerSectionLabel(title)
    }
    item {
        CollectionEditorGroup {
            results.forEach { result ->
                row { shape ->
                    PickerOptionRow(
                        title = result.title,
                        subtitle = result.subtitle,
                        shape = shape,
                        onClick = { CollectionEditorRepository.addTraktSourceFromResult(result) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun traktSortOptions(): List<Pair<String, String>> =
    listOf(
        TraktListSort.RANK.value to stringResource(Res.string.collections_editor_trakt_sort_list_order),
        TraktListSort.ADDED.value to stringResource(Res.string.collections_editor_trakt_sort_recently_added),
        TraktListSort.TITLE.value to stringResource(Res.string.collections_editor_trakt_sort_title),
        TraktListSort.RELEASED.value to stringResource(Res.string.collections_editor_trakt_sort_released),
        TraktListSort.RUNTIME.value to stringResource(Res.string.collections_editor_trakt_sort_runtime),
        TraktListSort.POPULARITY.value to stringResource(Res.string.collections_editor_trakt_sort_popular),
        TraktListSort.PERCENTAGE.value to stringResource(Res.string.collections_editor_trakt_sort_percentage),
        TraktListSort.VOTES.value to stringResource(Res.string.collections_editor_trakt_sort_votes),
    )

@Composable
internal fun traktSortLabel(value: String?): String =
    when (TraktListSort.normalize(value)) {
        TraktListSort.ADDED.value -> stringResource(Res.string.collections_editor_trakt_sort_recently_added)
        TraktListSort.TITLE.value -> stringResource(Res.string.collections_editor_trakt_sort_title)
        TraktListSort.RELEASED.value -> stringResource(Res.string.collections_editor_trakt_sort_released)
        TraktListSort.RUNTIME.value -> stringResource(Res.string.collections_editor_trakt_sort_runtime)
        TraktListSort.POPULARITY.value -> stringResource(Res.string.collections_editor_trakt_sort_popular)
        TraktListSort.PERCENTAGE.value -> stringResource(Res.string.collections_editor_trakt_sort_percentage)
        TraktListSort.VOTES.value -> stringResource(Res.string.collections_editor_trakt_sort_votes)
        else -> stringResource(Res.string.collections_editor_trakt_sort_list_order)
    }

@Composable
internal fun traktDirectionLabel(value: String?): String =
    when (TraktSortHow.normalize(value)) {
        TraktSortHow.DESC.value -> stringResource(Res.string.collections_editor_trakt_descending)
        else -> stringResource(Res.string.collections_editor_trakt_ascending)
    }

@Composable
internal fun traktSourceSubtitle(source: CollectionSource): String {
    val media = when (TmdbCollectionMediaType.fromString(source.mediaType)) {
        TmdbCollectionMediaType.MOVIE -> stringResource(Res.string.collections_editor_tmdb_movies)
        TmdbCollectionMediaType.TV -> stringResource(Res.string.collections_editor_tmdb_series)
    }
    return listOf(
        media,
        traktSortLabel(source.sortBy),
        traktDirectionLabel(source.sortHow),
        stringResource(Res.string.collections_editor_trakt_list_id_format, source.traktListId ?: ""),
    ).joinToString(" • ")
}
