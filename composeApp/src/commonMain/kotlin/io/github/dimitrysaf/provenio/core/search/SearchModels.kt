package io.github.dimitrysaf.provenio.core.search

import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.catalog.CatalogTarget
import io.github.dimitrysaf.provenio.core.catalog.supportsPagination
import provenio.composeapp.generated.resources.*

enum class SearchEmptyStateReason {
    NoActiveAddons,
    NoSearchCatalogs,
    NoResults,
    RequestFailed,
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val sections: List<HomeCatalogSection> = emptyList(),
    val emptyStateReason: SearchEmptyStateReason? = null,
    val errorMessage: String? = null,
)

enum class DiscoverEmptyStateReason {
    NoActiveAddons,
    NoDiscoverCatalogs,
    NoResults,
    RequestFailed,
}

data class DiscoverCatalogOption(
    val key: String,
    val addonName: String,
    val manifestUrl: String,
    val type: String,
    val catalogId: String,
    val catalogName: String,
    val genreOptions: List<String> = emptyList(),
    val genreRequired: Boolean = false,
    val supportsPagination: Boolean = false,
)

data class DiscoverUiState(
    val typeOptions: List<String> = emptyList(),
    val selectedType: String? = null,
    val catalogOptions: List<DiscoverCatalogOption> = emptyList(),
    val selectedCatalogKey: String? = null,
    val selectedGenre: String? = null,
    val items: List<MetaPreview> = emptyList(),
    val isLoading: Boolean = false,
    val nextSkip: Int? = null,
    val consecutiveDuplicatePages: Int = 0,
    val emptyStateReason: DiscoverEmptyStateReason? = null,
    val errorMessage: String? = null,
) {
    val selectedCatalog: DiscoverCatalogOption?
        get() = catalogOptions.firstOrNull { it.key == selectedCatalogKey }

    val genreOptions: List<String>
        get() = selectedCatalog?.genreOptions.orEmpty()

    val canLoadMore: Boolean
        get() = nextSkip != null
}

internal fun <T> canReuseRequestState(
    forceRefresh: Boolean,
    requestKey: T,
    cachedRequestKey: T?,
): Boolean = !forceRefresh && requestKey == cachedRequestKey

internal fun resolveDiscoverCatalog(
    sources: List<DiscoverCatalogOption>,
    preferredCatalogKey: String?,
    currentCatalogKey: String?,
): DiscoverCatalogOption? =
    sources.firstOrNull { it.key == preferredCatalogKey }
        ?: sources.firstOrNull { it.key == currentCatalogKey }
        ?: sources.firstOrNull()

private fun String.typeSortKey(): String =
    when (lowercase()) {
        "movie" -> "0_movie"
        "series" -> "1_series"
        "anime" -> "2_anime"
        else -> "9_$this"
    }

enum class SearchSortOrder {
    Default,
    NameAscending,
    NameDescending,
    Newest,
    Oldest,
    Rating,
}

data class SearchFilterState(
    val type: String? = null,
    val catalogKey: String? = null,
    val genre: String? = null,
    val sort: SearchSortOrder = SearchSortOrder.Default,
) {
    val hasSearchFilters: Boolean
        get() = type != null || catalogKey != null || genre != null
}

data class SearchCatalogFilterOption(
    val key: String,
    val label: String,
)

/** Identifies the catalog a search section came from, the same for every query. */
fun HomeCatalogSection.searchCatalogKey(): String =
    when (val catalog = target) {
        is CatalogTarget.Addon -> "${catalog.manifestUrl}:${catalog.contentType}:${catalog.catalogId}"
        else -> key
    }

/** Types present in the search results, movies and series first. */
fun List<HomeCatalogSection>.searchTypeOptions(): List<String> =
    map { it.target.contentType }.distinct().sortedBy { it.typeSortKey() }

/** Catalogs present in the search results, limited to [type] when one is picked. */
fun List<HomeCatalogSection>.searchCatalogOptions(type: String?): List<SearchCatalogFilterOption> =
    filter { type == null || it.target.contentType == type }
        .distinctBy { it.searchCatalogKey() }
        .map { SearchCatalogFilterOption(key = it.searchCatalogKey(), label = "${it.title} • ${it.addonName}") }

/** Genres the results carry, limited to [type] and [catalogKey] when picked. */
fun List<HomeCatalogSection>.searchGenreOptions(type: String?, catalogKey: String?): List<String> =
    filter { type == null || it.target.contentType == type }
        .filter { catalogKey == null || it.searchCatalogKey() == catalogKey }
        .flatMap { section -> section.items.flatMap { it.genres } }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

/** Applies type, catalog, genre and sort order to search results, dropping sections left empty. */
fun List<HomeCatalogSection>.applySearchFilters(filters: SearchFilterState): List<HomeCatalogSection> {
    if (!filters.hasSearchFilters && filters.sort == SearchSortOrder.Default) return this
    return filter { filters.type == null || it.target.contentType == filters.type }
        .filter { filters.catalogKey == null || it.searchCatalogKey() == filters.catalogKey }
        .map { section ->
            val items = section.items
                .filter { item -> filters.genre == null || item.genres.any { it.trim().equals(filters.genre, ignoreCase = true) } }
                .sortedFor(filters.sort)
            if (items == section.items) section else section.copy(items = items)
        }
        .filter { it.items.isNotEmpty() }
}

/** Orders titles by [order], keeping the catalog's own order for ties and for [SearchSortOrder.Default]. */
fun List<MetaPreview>.sortedFor(order: SearchSortOrder): List<MetaPreview> =
    when (order) {
        SearchSortOrder.Default -> this
        SearchSortOrder.NameAscending -> sortedBy { it.name.lowercase() }
        SearchSortOrder.NameDescending -> sortedByDescending { it.name.lowercase() }
        SearchSortOrder.Newest -> sortedWith(compareByDescending(nullsFirst<String>()) { it.releaseSortKey() })
        SearchSortOrder.Oldest -> sortedWith(compareBy(nullsLast<String>()) { it.releaseSortKey() })
        SearchSortOrder.Rating -> sortedWith(compareByDescending(nullsFirst<Double>()) { it.imdbRating?.trim()?.toDoubleOrNull() })
    }

/** A sortable release date, from the full date when known and the year otherwise. */
private fun MetaPreview.releaseSortKey(): String? {
    rawReleaseDate?.trim()?.takeIf { it.length >= 4 && it.take(4).all(Char::isDigit) }?.let { return it.take(10) }
    return releaseInfo?.let { YearPrefix.find(it)?.value }
}

private val YearPrefix = Regex("\\d{4}")
