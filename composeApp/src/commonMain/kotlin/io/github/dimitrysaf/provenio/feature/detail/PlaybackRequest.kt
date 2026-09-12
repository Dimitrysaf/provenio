package io.github.dimitrysaf.provenio.feature.detail

/**
 * Everything the details screen hands off to the player: the resolved stream, and — only
 * when [imdbId] identifies a title Simkl can match — enough to report playback progress
 * there.
 */
data class PlaybackRequest(
    val url: String,
    val type: String,
    val imdbId: String,
    /** What the player puts in its title bar: the show or film, and the episode if any. */
    val title: String? = null,
    /** The add-on's own id for this video, so the player can ask for its sources again. */
    val videoId: String? = null,
    val episodeTitle: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val resumeProgressPercent: Float? = null,
    /**
     * [url]'s own identity — see [io.github.dimitrysaf.provenio.stremio.SourceOption.streamId]
     * — so the player can remember it alongside the position it records. Null whenever the
     * source could not be identified, which just means the next "resume" for this video
     * goes straight to the source sheet.
     */
    val streamId: String? = null,
)
