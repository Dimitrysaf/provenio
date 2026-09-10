package io.github.dimitrysaf.provenio.navigation

import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.search_filter_all
import io.github.dimitrysaf.provenio.resources.search_filter_movies
import io.github.dimitrysaf.provenio.resources.search_filter_people
import io.github.dimitrysaf.provenio.resources.search_filter_tv
import org.jetbrains.compose.resources.StringResource

/**
 * Narrows a search to one kind of result.
 *
 * [type] is matched against a catalog's Stremio type. `person` is not one of the protocol's
 * standard types, so People stays empty unless an addon chooses to serve it.
 */
enum class SearchFilter(val label: StringResource, val type: String?) {
    All(Res.string.search_filter_all, null),
    Movies(Res.string.search_filter_movies, "movie"),
    Tv(Res.string.search_filter_tv, "series"),
    People(Res.string.search_filter_people, "person"),
}
