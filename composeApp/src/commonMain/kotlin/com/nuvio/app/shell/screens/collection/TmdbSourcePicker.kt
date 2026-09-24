package com.nuvio.app.shell.screens.collection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.nuvio.app.shell.components.NuvioScreen
import com.nuvio.app.shell.components.NuvioSearchField
import com.nuvio.app.shell.components.PlatformBackHandler
import com.nuvio.app.shell.components.SingleChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.nuvio.app.core.collection.CollectionEditorRepository
import com.nuvio.app.core.collection.CollectionEditorUiState
import com.nuvio.app.core.collection.CollectionSource
import com.nuvio.app.core.collection.TmdbBuilderMode
import com.nuvio.app.core.collection.TmdbCollectionMediaType
import com.nuvio.app.core.collection.TmdbCollectionSort
import com.nuvio.app.core.collection.TmdbCollectionSourceResolver
import com.nuvio.app.core.collection.TmdbCollectionSourceType

@Composable
internal fun TmdbSourcePickerScreen(
    state: CollectionEditorUiState,
    onBack: () -> Unit,
) {
    val sourceType = when (state.tmdbBuilderMode) {
        TmdbBuilderMode.PRESETS -> TmdbCollectionSourceType.DISCOVER
        TmdbBuilderMode.LIST -> TmdbCollectionSourceType.LIST
        TmdbBuilderMode.COLLECTION -> TmdbCollectionSourceType.COLLECTION
        TmdbBuilderMode.PRODUCTION -> TmdbCollectionSourceType.COMPANY
        TmdbBuilderMode.NETWORK -> TmdbCollectionSourceType.NETWORK
        TmdbBuilderMode.PERSON -> TmdbCollectionSourceType.PERSON
        TmdbBuilderMode.DIRECTOR -> TmdbCollectionSourceType.DIRECTOR
        TmdbBuilderMode.DISCOVER -> TmdbCollectionSourceType.DISCOVER
    }
    val requiresId = sourceType != TmdbCollectionSourceType.DISCOVER
    // These two are found by typing a name and searching, not by pasting an id.
    val searchesByName = sourceType == TmdbCollectionSourceType.COMPANY ||
        sourceType == TmdbCollectionSourceType.COLLECTION
    val showMediaControls = state.tmdbBuilderMode == TmdbBuilderMode.PRODUCTION ||
        state.tmdbBuilderMode == TmdbBuilderMode.PERSON ||
        state.tmdbBuilderMode == TmdbBuilderMode.DIRECTOR ||
        state.tmdbBuilderMode == TmdbBuilderMode.DISCOVER
    val showSortControls = state.tmdbBuilderMode == TmdbBuilderMode.PRODUCTION ||
        state.tmdbBuilderMode == TmdbBuilderMode.NETWORK ||
        state.tmdbBuilderMode == TmdbBuilderMode.PERSON ||
        state.tmdbBuilderMode == TmdbBuilderMode.DIRECTOR ||
        state.tmdbBuilderMode == TmdbBuilderMode.DISCOVER
    val showFilterControls = state.tmdbBuilderMode == TmdbBuilderMode.DISCOVER
    // Presets are added by tapping one, so that mode has no action to dock and gets no bar; the
    // scaffold then falls back to the window's own bottom inset for the content's padding.
    val tmdbActionBar: (@Composable () -> Unit)? = if (state.tmdbBuilderMode == TmdbBuilderMode.PRESETS) {
        null
    } else {
        {
            CollectionEditorActionBar {
                Button(
                    onClick = { CollectionEditorRepository.addTmdbSourceFromInput() },
                    modifier = Modifier.weight(1f),
                    enabled = !requiresId || state.tmdbInput.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.collections_editor_add_source))
                }
            }
        }
    }

    PlatformBackHandler(enabled = true) {
        onBack()
    }

    NuvioScreen(
        title = stringResource(Res.string.collections_editor_tmdb_sources),
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        bottomBar = tmdbActionBar,
    ) {

        item {
            PickerTabRow(
                tabs = TmdbBuilderMode.entries,
                selected = state.tmdbBuilderMode,
                label = { tmdbBuilderModeLabel(it) },
                onSelect = { CollectionEditorRepository.setTmdbBuilderMode(it) },
            )
        }

        item {
            // Only where the mode still needs explaining: the tab already names it, so a line
            // that restates the tab is noise. Read here rather than in the builder lambda around
            // this item, which is not a composable scope.
            val modeHelp = tmdbModeHelpText(state.tmdbBuilderMode)
            if (modeHelp.isNotBlank()) {
                Text(
                    text = modeHelp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Production and Collection are searched by name, so their input is a search bar rather
        // than a plain field, in the spec's baseline style: the divider under it separates the bar
        // from the results it returns, which stay in this page's list below.
        if (searchesByName) {
            item {
                NuvioSearchField(
                    query = state.tmdbInput,
                    onQueryChange = { CollectionEditorRepository.setTmdbInput(it) },
                    placeholder = tmdbInputPlaceholder(state.tmdbBuilderMode),
                    divided = true,
                    onSearch = {
                        if (sourceType == TmdbCollectionSourceType.COMPANY) {
                            CollectionEditorRepository.searchTmdbCompanies()
                        } else {
                            CollectionEditorRepository.searchTmdbCollections()
                        }
                    },
                )
            }
        }

        if (state.tmdbBuilderMode != TmdbBuilderMode.PRESETS) {
            item {
                // The form is one segmented group, fields included, so it reads as one thing
                // rather than fields floating inside a card.
                CollectionEditorGroup {
                    if (requiresId && !searchesByName) {
                        row { shape ->
                            PresetFieldRow(
                                label = tmdbInputLabel(state.tmdbBuilderMode),
                                value = state.tmdbInput,
                                onValueChange = { CollectionEditorRepository.setTmdbInput(it) },
                                shape = shape,
                                supportingText = tmdbInputHelper(state.tmdbBuilderMode),
                                placeholder = tmdbInputPlaceholder(state.tmdbBuilderMode),
                            )
                        }
                    }
                    row { shape ->
                        PresetFieldRow(
                            label = stringResource(Res.string.collections_editor_tmdb_display_title),
                            value = state.tmdbTitleInput,
                            onValueChange = { CollectionEditorRepository.setTmdbTitleInput(it) },
                            shape = shape,
                            supportingText = stringResource(Res.string.collections_editor_tmdb_title_helper),
                            placeholder = tmdbTitlePlaceholder(state.tmdbBuilderMode),
                        )
                    }
                }
            }

            if (state.tmdbSearchError != null) {
                item {
                    Text(
                        text = state.tmdbSearchError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Media type and sort are one choice each out of three or four, which is a row that opens
        // the app's single-choice sheet, not a row of chips.
        if (showMediaControls || showSortControls) {
            item {
                var showMediaSheet by remember { mutableStateOf(false) }
                var showSortSheet by remember { mutableStateOf(false) }
                val mediaOptions = tmdbMediaChoices()
                val sortOptions = tmdbSortChoices(state)
                val currentMedia = tmdbCurrentMediaChoice(state)

                CollectionEditorGroup {
                    if (showMediaControls) {
                        row { shape ->
                            CollectionEditorChoiceRow(
                                title = stringResource(Res.string.collections_editor_tmdb_type),
                                value = mediaOptions.first { it.value == currentMedia }.label,
                                onClick = { showMediaSheet = true },
                                shape = shape,
                            )
                        }
                    }
                    if (showSortControls) {
                        row { shape ->
                            CollectionEditorChoiceRow(
                                title = stringResource(Res.string.collections_editor_tmdb_sort),
                                value = sortOptions.firstOrNull { it.value == state.tmdbSortBy }?.label
                                    ?: sortOptions.first().label,
                                onClick = { showSortSheet = true },
                                shape = shape,
                            )
                        }
                    }
                }

                if (showMediaSheet) {
                    SingleChoiceBottomSheet(
                        title = stringResource(Res.string.collections_editor_tmdb_type),
                        options = mediaOptions,
                        isSelected = { it == currentMedia },
                        onSelected = { choice ->
                            CollectionEditorRepository.setTmdbMediaBoth(choice == TmdbMediaChoice.BOTH)
                            when (choice) {
                                TmdbMediaChoice.MOVIE ->
                                    CollectionEditorRepository.setTmdbMediaType(TmdbCollectionMediaType.MOVIE)
                                TmdbMediaChoice.TV ->
                                    CollectionEditorRepository.setTmdbMediaType(TmdbCollectionMediaType.TV)
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
                        isSelected = { it == state.tmdbSortBy },
                        onSelected = { CollectionEditorRepository.setTmdbSortBy(it) },
                        onDismiss = { showSortSheet = false },
                    )
                }
            }
        }

        if (showFilterControls) {
            item {
                PickerSectionLabel(stringResource(Res.string.collections_editor_tmdb_filters))
            }
            item {
                Text(
                    text = stringResource(Res.string.collections_editor_tmdb_filters_helper),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                TmdbDiscoverFilters(state = state)
            }
        }

        if (state.tmdbBuilderMode == TmdbBuilderMode.PRODUCTION && state.tmdbCompanyResults.isNotEmpty()) {
            item {
                PickerSectionLabel(stringResource(Res.string.collections_editor_tmdb_search_results))
            }
            item {
                val movieSuffix = stringResource(Res.string.collections_editor_tmdb_movies)
                val seriesSuffix = stringResource(Res.string.collections_editor_tmdb_series)
                CollectionEditorGroup {
                    state.tmdbCompanyResults.forEach { result ->
                        row { shape ->
                        val title = result.name ?: stringResource(Res.string.collections_editor_tmdb_company_fallback, result.id)
                        PickerOptionRow(
                            title = title,
                            subtitle = listOfNotNull(
                                stringResource(Res.string.collections_editor_tmdb_subtitle_production),
                                result.originCountry,
                            ).joinToString(" • "),
                            shape = shape,
                            onClick = {
                        val sources = tmdbSelectedMediaTypes(state).map { mediaType ->
                            CollectionSource(
                                provider = "tmdb",
                                tmdbSourceType = TmdbCollectionSourceType.COMPANY.name,
                                title = tmdbTitleForMedia(title, mediaType, state.tmdbMediaBoth, movieSuffix, seriesSuffix),
                                tmdbId = result.id,
                                mediaType = mediaType.name,
                                sortBy = state.tmdbSortBy,
                                filters = state.tmdbFilters,
                            )
                        }
                                CollectionEditorRepository.addTmdbSourcesFromPicker(sources)
                            },
                        )
                        }
                    }
                }
            }
        }

        if (state.tmdbBuilderMode == TmdbBuilderMode.COLLECTION && state.tmdbCollectionResults.isNotEmpty()) {
            item {
                PickerSectionLabel(stringResource(Res.string.collections_editor_tmdb_search_results))
            }
            item {
                CollectionEditorGroup {
                    state.tmdbCollectionResults.forEach { result ->
                        row { shape ->
                            val title = result.name
                                ?: stringResource(Res.string.collections_editor_tmdb_collection_fallback, result.id)
                            PickerOptionRow(
                                title = title,
                                subtitle = stringResource(Res.string.collections_editor_tmdb_collection),
                                shape = shape,
                                onClick = {
                                    CollectionEditorRepository.addTmdbSource(
                                        CollectionSource(
                                            provider = "tmdb",
                                            tmdbSourceType = TmdbCollectionSourceType.COLLECTION.name,
                                            title = title,
                                            tmdbId = result.id,
                                            mediaType = TmdbCollectionMediaType.MOVIE.name,
                                            sortBy = state.tmdbSortBy,
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (state.tmdbBuilderMode == TmdbBuilderMode.PRESETS) {
            item {
                // One group in one item. Emitted as separate lazy items they took the screen's
                // list gap between them and stopped reading as a list at all.
                CollectionEditorGroup {
                    TmdbCollectionSourceResolver.presets().forEach { preset ->
                        row { shape ->
                            PickerOptionRow(
                                title = preset.label,
                                subtitle = tmdbSourceSubtitle(preset.source),
                                shape = shape,
                                onClick = { CollectionEditorRepository.addTmdbPreset(preset.source) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Which of the three media choices a mode is on. The state holds it as a flag plus a type. */
internal enum class TmdbMediaChoice { MOVIE, TV, BOTH }

@Composable
internal fun tmdbMediaChoices(): List<SingleChoiceOption<TmdbMediaChoice>> = listOf(
    SingleChoiceOption(
        value = TmdbMediaChoice.MOVIE,
        label = stringResource(Res.string.collections_editor_tmdb_movies),
    ),
    SingleChoiceOption(
        value = TmdbMediaChoice.TV,
        label = stringResource(Res.string.collections_editor_tmdb_series),
    ),
    SingleChoiceOption(
        value = TmdbMediaChoice.BOTH,
        label = stringResource(Res.string.collections_editor_tmdb_both),
    ),
)

internal fun tmdbCurrentMediaChoice(state: CollectionEditorUiState): TmdbMediaChoice = when {
    state.tmdbMediaBoth -> TmdbMediaChoice.BOTH
    state.tmdbMediaType == TmdbCollectionMediaType.TV -> TmdbMediaChoice.TV
    else -> TmdbMediaChoice.MOVIE
}

@Composable
internal fun tmdbSortChoices(state: CollectionEditorUiState): List<SingleChoiceOption<String>> {
    // The date sort means a different field for series than for films, so only the one that
    // applies is offered rather than both.
    val dateSort = if (state.tmdbMediaType == TmdbCollectionMediaType.TV && !state.tmdbMediaBoth) {
        TmdbCollectionSort.FIRST_AIR_DATE_DESC
    } else {
        TmdbCollectionSort.RELEASE_DATE_DESC
    }
    return listOf(
        TmdbCollectionSort.POPULAR_DESC,
        TmdbCollectionSort.VOTE_AVERAGE_DESC,
        TmdbCollectionSort.VOTE_COUNT_DESC,
        dateSort,
    ).map { SingleChoiceOption(value = it.value, label = tmdbSortLabel(it)) }
}

/**
 * Choosing a catalog's genre. One value out of a list, which is what the app's single-choice
 * sheet is for, so it is that sheet rather than a second one built by hand: the selected row's
 * container and its 4dp-to-16dp corner morph come with it.
 */
@Composable
internal fun GenrePickerSheet(
    title: String,
    selectedGenre: String?,
    genreOptions: List<String>,
    allowAll: Boolean,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    // "All genres" is the absence of a genre. The sheet deals only in option values, so it is
    // carried as the empty string and turned back into null on the way out.
    val allLabel = stringResource(Res.string.collections_editor_all_genres)
    val options = buildList {
        if (allowAll) add(SingleChoiceOption(value = "", label = allLabel))
        genreOptions.forEach { add(SingleChoiceOption(value = it, label = it)) }
    }

    SingleChoiceBottomSheet(
        title = stringResource(Res.string.collections_editor_genre_filter),
        options = options,
        isSelected = { it == selectedGenre.orEmpty() },
        onSelected = { onSelect(it.ifBlank { null }) },
        onDismiss = onDismiss,
        description = title,
    )
}

@Composable
internal fun tmdbGenreQuickChips(mediaType: TmdbCollectionMediaType): List<Pair<String, String>> =
    when (mediaType) {
        TmdbCollectionMediaType.MOVIE -> listOf(
            stringResource(Res.string.collections_editor_tmdb_genre_action) to "28",
            stringResource(Res.string.collections_editor_tmdb_genre_adventure) to "12",
            stringResource(Res.string.collections_editor_tmdb_genre_animation) to "16",
            stringResource(Res.string.collections_editor_tmdb_genre_comedy) to "35",
            stringResource(Res.string.collections_editor_tmdb_genre_horror) to "27",
            stringResource(Res.string.collections_editor_tmdb_genre_scifi) to "878",
        )
        TmdbCollectionMediaType.TV -> listOf(
            stringResource(Res.string.collections_editor_tmdb_genre_drama) to "18",
            stringResource(Res.string.collections_editor_tmdb_genre_comedy) to "35",
            stringResource(Res.string.collections_editor_tmdb_genre_animation) to "16",
            stringResource(Res.string.collections_editor_tmdb_genre_crime) to "80",
            stringResource(Res.string.collections_editor_tmdb_genre_scifi) to "10765",
            stringResource(Res.string.collections_editor_tmdb_genre_reality) to "10764",
        )
    }

internal fun tmdbSelectedMediaTypes(state: CollectionEditorUiState): List<TmdbCollectionMediaType> =
    if (state.tmdbMediaBoth) {
        listOf(TmdbCollectionMediaType.MOVIE, TmdbCollectionMediaType.TV)
    } else {
        listOf(state.tmdbMediaType)
    }

internal fun tmdbTitleForMedia(
    title: String,
    mediaType: TmdbCollectionMediaType,
    addSuffix: Boolean,
    movieSuffix: String,
    seriesSuffix: String,
): String {
    if (!addSuffix) return title
    val suffix = when (mediaType) {
        TmdbCollectionMediaType.MOVIE -> movieSuffix
        TmdbCollectionMediaType.TV -> seriesSuffix
    }
    return "$title $suffix"
}

@Composable
internal fun tmdbBuilderModeLabel(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.PRESETS -> stringResource(Res.string.collections_editor_tmdb_presets)
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_public_list_mode)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_production_mode)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_mode)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_mode)
        TmdbBuilderMode.PERSON -> stringResource(Res.string.collections_editor_tmdb_person_mode)
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_director_mode)
        TmdbBuilderMode.DISCOVER -> stringResource(Res.string.collections_editor_tmdb_custom_mode)
    }

@Composable
internal fun tmdbModeHelpText(mode: TmdbBuilderMode): String =
    when (mode) {
        // The tab is named Presets and the list below it is the presets; nothing to add.
        TmdbBuilderMode.PRESETS -> ""
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_help_list)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_help_production)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_help_network)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_help_collection)
        TmdbBuilderMode.PERSON -> stringResource(Res.string.collections_editor_tmdb_help_person)
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_help_director)
        TmdbBuilderMode.DISCOVER -> stringResource(Res.string.collections_editor_tmdb_help_discover)
    }

@Composable
internal fun tmdbInputLabel(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_public_list)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_id)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_id)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_company_search)
        TmdbBuilderMode.PERSON,
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_person_id)
        else -> stringResource(Res.string.collections_editor_tmdb_id_or_url)
    }

@Composable
internal fun tmdbInputPlaceholder(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.LIST -> stringResource(Res.string.collections_editor_tmdb_list_placeholder)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_placeholder)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_placeholder)
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_company_placeholder)
        TmdbBuilderMode.PERSON,
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_person_placeholder)
        else -> stringResource(Res.string.collections_editor_tmdb_id_or_url)
    }

@Composable
internal fun tmdbInputHelper(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.PRODUCTION -> stringResource(Res.string.collections_editor_tmdb_search_helper)
        TmdbBuilderMode.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_collection_helper)
        TmdbBuilderMode.NETWORK -> stringResource(Res.string.collections_editor_tmdb_network_helper)
        // The field's own label and placeholder already say what goes in it.
        TmdbBuilderMode.LIST -> ""
        TmdbBuilderMode.PERSON,
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_person_helper)
        else -> ""
    }

@Composable
internal fun tmdbTitlePlaceholder(mode: TmdbBuilderMode): String =
    when (mode) {
        TmdbBuilderMode.DISCOVER -> stringResource(Res.string.collections_editor_tmdb_discover_title_placeholder)
        TmdbBuilderMode.PERSON -> stringResource(Res.string.collections_editor_tmdb_person_title_placeholder)
        TmdbBuilderMode.DIRECTOR -> stringResource(Res.string.collections_editor_tmdb_director_title_placeholder)
        else -> stringResource(Res.string.collections_editor_tmdb_title_placeholder)
    }

@Composable
internal fun tmdbSortLabel(sort: TmdbCollectionSort): String =
    when (sort) {
        TmdbCollectionSort.ORIGINAL -> stringResource(Res.string.collections_editor_tmdb_sort_original)
        TmdbCollectionSort.POPULAR_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_popular)
        TmdbCollectionSort.VOTE_AVERAGE_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_top_rated)
        TmdbCollectionSort.VOTE_COUNT_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_vote_count)
        TmdbCollectionSort.RELEASE_DATE_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_recent)
        TmdbCollectionSort.FIRST_AIR_DATE_DESC -> stringResource(Res.string.collections_editor_tmdb_sort_recent)
    }

@Composable
internal fun tmdbSourceSubtitle(source: CollectionSource): String {
    val media = when (TmdbCollectionMediaType.fromString(source.mediaType)) {
        TmdbCollectionMediaType.MOVIE -> stringResource(Res.string.collections_editor_tmdb_movies)
        TmdbCollectionMediaType.TV -> stringResource(Res.string.collections_editor_tmdb_series)
    }
    val sort = source.sortBy?.let { value ->
        TmdbCollectionSort.entries.firstOrNull { it.value == value }?.let { sort ->
            tmdbSortLabel(sort)
        }
    } ?: stringResource(Res.string.collections_editor_tmdb_sort_popular)
    val sourceType = runCatching {
        TmdbCollectionSourceType.valueOf(source.tmdbSourceType.orEmpty())
    }.getOrDefault(TmdbCollectionSourceType.DISCOVER)
    return when (sourceType) {
        TmdbCollectionSourceType.LIST -> stringResource(Res.string.collections_editor_tmdb_subtitle_list)
        TmdbCollectionSourceType.COLLECTION -> stringResource(Res.string.collections_editor_tmdb_subtitle_movie_collection)
        TmdbCollectionSourceType.COMPANY -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_production),
            media,
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.NETWORK -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_network),
            stringResource(Res.string.collections_editor_tmdb_series),
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.PERSON -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_person),
            media,
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.DIRECTOR -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_director),
            media,
            sort,
        ).joinToString(" • ")
        TmdbCollectionSourceType.DISCOVER -> listOf(
            stringResource(Res.string.collections_editor_tmdb_subtitle_discover),
            media,
            sort,
        ).joinToString(" • ")
    }
}

/**
 * Discover's filters, one row each.
 *
 * Every filter that had a shortlist used to render that shortlist as a row of chips and then a
 * text field underneath for the same value, so the panel said everything twice and ran to eight
 * screens. Here a filter is one row: the field holds the value, and where a shortlist exists it
 * sits behind the field's own trailing button.
 */
@Composable
internal fun TmdbDiscoverFilters(state: CollectionEditorUiState) {
    val filters = state.tmdbFilters

    CollectionEditorGroup {
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_genres),
                value = filters.withGenres.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withGenres = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_genres_helper),
                placeholder = if (state.tmdbMediaType == TmdbCollectionMediaType.MOVIE) {
                    stringResource(Res.string.collections_editor_tmdb_genres_movie_placeholder)
                } else {
                    stringResource(Res.string.collections_editor_tmdb_genres_series_placeholder)
                },
                presets = tmdbGenreQuickChips(state.tmdbMediaType),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_genres),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_genres),
                value = filters.withoutGenres.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutGenres = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_genres_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_genres_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_date_from),
                value = filters.releaseDateGte.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(releaseDateGte = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_date_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_date_from_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_date_to),
                value = filters.releaseDateLte.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(releaseDateLte = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_date_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_date_to_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_rating_min),
                value = filters.voteAverageGte?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(voteAverageGte = value.toDoubleOrNull())
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_rating_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_rating_min_placeholder),
                keyboardType = KeyboardType.Decimal,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_rating_max),
                value = filters.voteAverageLte?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(voteAverageLte = value.toDoubleOrNull())
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_rating_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_rating_max_placeholder),
                keyboardType = KeyboardType.Decimal,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_votes_min),
                value = filters.voteCountGte?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(voteCountGte = value.toIntOrNull())
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_votes_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_votes_min_placeholder),
                keyboardType = KeyboardType.Number,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_year),
                value = filters.year?.toString().orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters { it.copy(year = value.toIntOrNull()) }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_year_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_year_placeholder),
                keyboardType = KeyboardType.Number,
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_language),
                value = filters.withOriginalLanguage.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withOriginalLanguage = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_language_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_language_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_language_english) to "en",
                    stringResource(Res.string.collections_editor_tmdb_language_korean) to "ko",
                    stringResource(Res.string.collections_editor_tmdb_language_japanese) to "ja",
                    stringResource(Res.string.collections_editor_tmdb_language_hindi) to "hi",
                    stringResource(Res.string.collections_editor_tmdb_language_spanish) to "es",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_languages),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_country),
                value = filters.withOriginCountry.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withOriginCountry = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_country_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_country_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_country_us) to "US",
                    stringResource(Res.string.collections_editor_tmdb_country_korea) to "KR",
                    stringResource(Res.string.collections_editor_tmdb_country_japan) to "JP",
                    stringResource(Res.string.collections_editor_tmdb_country_india) to "IN",
                    stringResource(Res.string.collections_editor_tmdb_country_uk) to "GB",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_countries),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_keywords),
                value = filters.withKeywords.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withKeywords = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_keywords_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_keywords_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_keyword_superhero) to "9715",
                    stringResource(Res.string.collections_editor_tmdb_keyword_based_on_novel) to "818",
                    stringResource(Res.string.collections_editor_tmdb_keyword_time_travel) to "4379",
                    stringResource(Res.string.collections_editor_tmdb_keyword_space) to "9882",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_keywords),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_keywords),
                value = filters.withoutKeywords.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutKeywords = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_keywords_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_keywords_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_companies),
                value = filters.withCompanies.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withCompanies = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_companies_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_companies_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_studio_marvel) to "420",
                    stringResource(Res.string.collections_editor_tmdb_studio_disney) to "2",
                    stringResource(Res.string.collections_editor_tmdb_studio_pixar) to "3",
                    stringResource(Res.string.collections_editor_tmdb_studio_lucasfilm) to "1",
                    stringResource(Res.string.collections_editor_tmdb_studio_warner) to "174",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_studios),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_companies),
                value = filters.withoutCompanies.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutCompanies = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_companies_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_companies_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_networks),
                value = filters.withNetworks.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withNetworks = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_networks_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_networks_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_network_netflix) to "213",
                    stringResource(Res.string.collections_editor_tmdb_network_hbo) to "49",
                    stringResource(Res.string.collections_editor_tmdb_network_disney_plus) to "2739",
                    stringResource(Res.string.collections_editor_tmdb_network_prime_video) to "1024",
                    stringResource(Res.string.collections_editor_tmdb_network_hulu) to "453",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_networks),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_watch_providers),
                value = filters.withWatchProviders.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withWatchProviders = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_watch_providers_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_watch_providers_placeholder),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_netflix) to "8",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_prime) to "119",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_disney) to "337",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_apple) to "350",
                    stringResource(Res.string.collections_editor_tmdb_watch_provider_hulu) to "15",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_watch_providers),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_without_watch_providers),
                value = filters.withoutWatchProviders.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(withoutWatchProviders = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_without_watch_providers_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_without_watch_providers_placeholder),
            )
        }
        row { shape ->
            PresetFieldRow(
                label = stringResource(Res.string.collections_editor_tmdb_watch_region),
                value = filters.watchRegion.orEmpty(),
                onValueChange = { value ->
                    CollectionEditorRepository.updateTmdbFilters {
                        it.copy(watchRegion = value.ifBlank { null })
                    }
                },
                shape = shape,
                supportingText = stringResource(Res.string.collections_editor_tmdb_watch_region_helper),
                placeholder = stringResource(Res.string.collections_editor_tmdb_country_us),
                presets = listOf(
                    stringResource(Res.string.collections_editor_tmdb_country_us) to "US",
                    stringResource(Res.string.collections_editor_tmdb_country_uk) to "GB",
                    stringResource(Res.string.collections_editor_tmdb_country_ca) to "CA",
                    stringResource(Res.string.collections_editor_tmdb_country_au) to "AU",
                    stringResource(Res.string.collections_editor_tmdb_country_de) to "DE",
                ),
                presetSheetTitle = stringResource(Res.string.collections_editor_tmdb_quick_watch_regions),
            )
        }
    }
}
