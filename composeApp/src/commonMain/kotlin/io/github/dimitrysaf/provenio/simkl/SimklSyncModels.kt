package io.github.dimitrysaf.provenio.simkl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Timestamps from GET /sync/activities.
 *
 * Simkl requires this to be fetched before any sync, and requires the value to be passed
 * back byte for byte as date_from. It is deliberately kept as the raw string rather than
 * parsed into a date type, because reformatting it is exactly the mistake their rules warn
 * against.
 */
@Serializable
data class SimklActivities(
    @SerialName("all") val all: String? = null,
    @SerialName("tv_shows") val tvShows: SimklActivityBucket? = null,
    @SerialName("anime") val anime: SimklActivityBucket? = null,
    @SerialName("movies") val movies: SimklActivityBucket? = null,
)

@Serializable
data class SimklActivityBucket(
    @SerialName("all") val all: String? = null,
    @SerialName("watching") val watching: String? = null,
    @SerialName("plantowatch") val planToWatch: String? = null,
    @SerialName("completed") val completed: String? = null,
    @SerialName("hold") val hold: String? = null,
    @SerialName("dropped") val dropped: String? = null,
    @SerialName("removed_from_list") val removedFromList: String? = null,
)

@Serializable
data class SimklAllItems(
    @SerialName("shows") val shows: List<SimklEntry> = emptyList(),
    @SerialName("movies") val movies: List<SimklEntry> = emptyList(),
    @SerialName("anime") val anime: List<SimklEntry> = emptyList(),
) {
    fun all(): List<SimklEntry> = shows + movies + anime
}

@Serializable
data class SimklEntry(
    @SerialName("status") val status: String? = null,
    @SerialName("watched_episodes_count") val watchedEpisodes: Int = 0,
    @SerialName("total_episodes_count") val totalEpisodes: Int = 0,
    @SerialName("last_watched_at") val lastWatchedAt: String? = null,
    @SerialName("last_watched") val lastWatched: String? = null,
    @SerialName("next_to_watch") val nextToWatch: String? = null,
    @SerialName("show") val show: SimklMedia? = null,
    @SerialName("movie") val movie: SimklMedia? = null,
    // Only present when the request carries `extended=full`: the actual list of watched
    // episodes, grouped by season. This is the one field the app asks `extended=full`
    // for — an aggregate count and a next-to-watch marker cannot tell which specific
    // episodes were watched, and the details page needs exactly that.
    @SerialName("seasons") val seasons: List<SimklSeasonWatched>? = null,
) {
    val media: SimklMedia? get() = show ?: movie
    val isMovie: Boolean get() = movie != null
}

@Serializable
data class SimklSeasonWatched(
    @SerialName("number") val number: Int,
    @SerialName("episodes") val episodes: List<SimklEpisodeWatched> = emptyList(),
)

@Serializable
data class SimklEpisodeWatched(
    @SerialName("number") val number: Int,
)

@Serializable
data class SimklMedia(
    @SerialName("title") val title: String? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("poster") val poster: String? = null,
    @SerialName("ids") val ids: SimklIds? = null,
)

@Serializable
data class SimklIds(
    @SerialName("simkl") val simkl: Long? = null,
    @SerialName("slug") val slug: String? = null,
    @SerialName("imdb") val imdb: String? = null,
    @SerialName("tmdb") val tmdb: String? = null,
    @SerialName("tvdb") val tvdb: String? = null,
)

/** Simkl watchlist statuses. Movies have no watching or hold. */
object SimklStatus {
    const val Watching = "watching"
    const val PlanToWatch = "plantowatch"
    const val Completed = "completed"
    const val Hold = "hold"
    const val Dropped = "dropped"
}
