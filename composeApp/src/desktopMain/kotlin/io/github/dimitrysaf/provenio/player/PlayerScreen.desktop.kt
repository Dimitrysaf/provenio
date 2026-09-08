package io.github.dimitrysaf.provenio.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Desktop has no player yet. media3 is Android only, so this target needs a separate
 * backend such as mpv or VLC. Saying so beats a blank surface that looks like a bug.
 */
@Composable
actual fun PlayerScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Playback is not available on desktop yet.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
