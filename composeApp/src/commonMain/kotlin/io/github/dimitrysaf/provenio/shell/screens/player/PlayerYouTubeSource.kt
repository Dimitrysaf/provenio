package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.core.trailer.TrailerPlaybackResolver
import io.github.dimitrysaf.provenio.core.trailer.TrailerPlaybackSource
import io.github.dimitrysaf.provenio.core.trailer.isYouTubeVideoUrl
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.player_error_unable_to_play_stream

internal class PlayerSurfaceSource(
    val url: String,
    val audioUrl: String?,
    val isYouTube: Boolean,
)

// What the surface actually plays: the source itself, or for a YouTube link its resolved video and audio.
@Composable
internal fun PlayerScreenRuntime.rememberPlayerSurfaceSource(sourceUrl: String?): PlayerSurfaceSource? {
    if (sourceUrl == null) return null
    if (!isYouTubeVideoUrl(sourceUrl)) return PlayerSurfaceSource(sourceUrl, activeSourceAudioUrl, isYouTube = false)
    val failedText = stringResource(Res.string.player_error_unable_to_play_stream)
    var resolved by remember(sourceUrl) { mutableStateOf<TrailerPlaybackSource?>(null) }
    LaunchedEffect(sourceUrl) {
        val source = runCatching { TrailerPlaybackResolver.resolveFromYouTubeUrl(sourceUrl) }.getOrNull()
        if (source == null) errorMessage = failedText else resolved = source
    }
    return resolved?.let { PlayerSurfaceSource(it.videoUrl, it.audioUrl, isYouTube = true) }
}
