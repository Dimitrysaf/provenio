package io.github.dimitrysaf.provenio.simkl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for `POST /sync/history` and `POST /sync/history/remove`, which share the same
 * shape: both endpoints accept a list of shows, each carrying the seasons and episodes to
 * add to (or remove from) the signed in user's watch history.
 *
 * https://api.simkl.org/guides/sync
 */
@Serializable
data class SimklHistoryRequest(
    @SerialName("shows") val shows: List<SimklHistoryShow> = emptyList(),
)

@Serializable
data class SimklHistoryShow(
    @SerialName("ids") val ids: SimklIds,
    @SerialName("seasons") val seasons: List<SimklHistorySeason>,
)

@Serializable
data class SimklHistorySeason(
    @SerialName("number") val number: Int,
    @SerialName("episodes") val episodes: List<SimklHistoryEpisode>,
)

@Serializable
data class SimklHistoryEpisode(
    @SerialName("number") val number: Int,
)

/** One episode to add to, or remove from, Simkl's watch history for [imdbId]. */
fun singleEpisodeHistoryRequest(
    imdbId: String,
    season: Int,
    episode: Int,
): SimklHistoryRequest = SimklHistoryRequest(
    shows = listOf(
        SimklHistoryShow(
            ids = SimklIds(imdb = imdbId),
            seasons = listOf(
                SimklHistorySeason(
                    number = season,
                    episodes = listOf(SimklHistoryEpisode(episode)),
                ),
            ),
        ),
    ),
)
