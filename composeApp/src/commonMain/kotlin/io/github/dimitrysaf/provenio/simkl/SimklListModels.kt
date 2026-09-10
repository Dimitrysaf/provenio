package io.github.dimitrysaf.provenio.simkl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for `POST /sync/add-to-list` and `POST /sync/history/remove` when moving a whole
 * title between the user's lists, rather than ticking individual episodes.
 *
 * The two share a shape: a list of titles, split by kind, each identified by its ids. Only
 * add-to-list carries [SimklListEntry.to] — removing takes the title out of every list, so
 * there is no destination to name.
 *
 * https://api.simkl.org/guides/sync
 */
@Serializable
data class SimklListRequest(
    @SerialName("shows") val shows: List<SimklListEntry> = emptyList(),
    @SerialName("movies") val movies: List<SimklListEntry> = emptyList(),
)

@Serializable
data class SimklListEntry(
    @SerialName("ids") val ids: SimklIds,
    /** One of [SimklStatus]. Omitted entirely when removing. */
    @SerialName("to") val to: String? = null,
)

/**
 * One title, in whichever half of the body Simkl expects for its kind.
 *
 * Anime is a list of its own on Simkl but not a type the addon protocol has, so a title
 * that is anime to Simkl arrives here as a series and is sent as one. Simkl files it under
 * the right list from the id regardless.
 */
fun singleTitleListRequest(
    imdbId: String,
    isMovie: Boolean,
    to: String? = null,
): SimklListRequest {
    val entry = SimklListEntry(ids = SimklIds(imdb = imdbId), to = to)
    return if (isMovie) {
        SimklListRequest(movies = listOf(entry))
    } else {
        SimklListRequest(shows = listOf(entry))
    }
}
