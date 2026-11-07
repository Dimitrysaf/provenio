package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.runtime.Composable
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.collection.CollectionSource
import io.github.dimitrysaf.provenio.core.collection.TmdbCollectionMediaType
import io.github.dimitrysaf.provenio.core.collection.TraktListSort
import io.github.dimitrysaf.provenio.core.collection.TraktSortHow

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
