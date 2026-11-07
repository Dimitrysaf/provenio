package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorRepository
import io.github.dimitrysaf.provenio.core.collection.CollectionEditorUiState
import io.github.dimitrysaf.provenio.core.collection.CollectionSource
import io.github.dimitrysaf.provenio.core.collection.TmdbBuilderMode
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionMediaType
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionSort
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionSourceType

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
