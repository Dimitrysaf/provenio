package io.github.dimitrysaf.provenio.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays [url] full screen.
 *
 * The URL is the only thing the player knows about. A localhost address from the torrent
 * engine, a debrid link and a plain HTTP file all arrive here looking identical, which is
 * what keeps the playback path single.
 */
@Composable
expect fun PlayerScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)
