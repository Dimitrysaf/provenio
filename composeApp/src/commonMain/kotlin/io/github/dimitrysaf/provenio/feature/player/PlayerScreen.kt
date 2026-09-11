package io.github.dimitrysaf.provenio.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.player.ScrobbleTarget

/**
 * Plays [url] full screen.
 *
 * The URL is the only thing the player knows about the stream itself. A localhost address
 * from the torrent engine, a debrid link and a plain HTTP file all arrive here looking
 * identical, which is what keeps the playback path single. [scrobbleTarget] is separate
 * from that: it is what the player is allowed to tell Simkl about progress, and is null
 * whenever there is nothing to tell it (no Simkl id for this title, or a backend — an
 * external player — this app cannot observe playback events from).
 */
@Composable
expect fun PlayerScreen(
    url: String,
    scrobbleTarget: ScrobbleTarget?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    season: Int? = null,
    episode: Int? = null,
    episodeTitle: String? = null,
)
