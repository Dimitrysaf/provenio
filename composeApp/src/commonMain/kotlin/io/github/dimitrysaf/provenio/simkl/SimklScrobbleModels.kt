package io.github.dimitrysaf.provenio.simkl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body shared by `POST /scrobble/start`, `/scrobble/pause` and `/scrobble/stop`: the item
 * being played — a movie, or a show plus the episode within it — and how far into it
 * playback has reached, as a 0-100 percentage. `/scrobble/stop` at 80% or more marks the
 * item watched on Simkl; below that it is saved as a resumable paused session instead.
 */
@Serializable
data class SimklScrobbleRequest(
    @SerialName("progress") val progress: Float,
    @SerialName("movie") val movie: SimklMedia? = null,
    @SerialName("show") val show: SimklMedia? = null,
    @SerialName("episode") val episode: SimklScrobbleEpisode? = null,
)

@Serializable
data class SimklScrobbleEpisode(
    @SerialName("season") val season: Int,
    @SerialName("number") val number: Int,
)

@Serializable
data class SimklScrobbleResponse(
    @SerialName("id") val id: Long? = null,
    @SerialName("action") val action: String? = null,
    @SerialName("progress") val progress: Float? = null,
)

/**
 * One saved, resumable pause point from `GET /sync/playback/{type}`. Not a watchlist
 * record — Simkl keeps these separate, purely to answer "where did the user leave off".
 */
@Serializable
data class SimklPlaybackSession(
    @SerialName("id") val id: Long,
    @SerialName("progress") val progress: Float,
    @SerialName("paused_at") val pausedAt: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("show") val show: SimklMedia? = null,
    @SerialName("movie") val movie: SimklMedia? = null,
    @SerialName("episode") val episode: SimklPlaybackEpisode? = null,
) {
    val media: SimklMedia? get() = show ?: movie
}

/** Unlike [SimklScrobbleEpisode], the episode number here comes back under `episode`. */
@Serializable
data class SimklPlaybackEpisode(
    @SerialName("season") val season: Int,
    @SerialName("episode") val number: Int,
)

fun singleEpisodeScrobbleRequest(
    imdbId: String,
    season: Int,
    episode: Int,
    progress: Float,
): SimklScrobbleRequest = SimklScrobbleRequest(
    progress = progress,
    show = SimklMedia(ids = SimklIds(imdb = imdbId)),
    episode = SimklScrobbleEpisode(season, episode),
)

fun movieScrobbleRequest(imdbId: String, progress: Float): SimklScrobbleRequest =
    SimklScrobbleRequest(progress = progress, movie = SimklMedia(ids = SimklIds(imdb = imdbId)))
