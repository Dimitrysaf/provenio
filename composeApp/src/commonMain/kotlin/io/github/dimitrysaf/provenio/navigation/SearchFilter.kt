package io.github.dimitrysaf.provenio.navigation

/**
 * Narrows a search to one kind of result.
 *
 * [type] is matched against a catalog's Stremio type. `person` is not one of the protocol's
 * standard types, so People stays empty unless an addon chooses to serve it.
 */
enum class SearchFilter(val label: String, val type: String?) {
    All("All", null),
    Movies("Movies", "movie"),
    Tv("TV", "series"),
    People("People", "person"),
}
