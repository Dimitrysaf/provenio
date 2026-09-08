package io.github.dimitrysaf.provenio.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

@Composable
actual fun PlayerScreen(
    url: String,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val backend by PlayerRepository.backend.collectAsState()

    when (backend) {
        PlayerBackend.Builtin -> BuiltinPlayer(url = url, modifier = modifier)
        PlayerBackend.External -> ExternalPlayer(url = url, onBack = onBack)
    }
}

@Composable
private fun BuiltinPlayer(url: String, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember {
        // The default renderers only reach the device's own decoders, which on most
        // phones cannot handle AC-3, E-AC-3, DTS or TrueHD. This factory adds FFmpeg
        // software decoders behind them, and prefers the hardware path when there is one.
        val renderers = NextRenderersFactory(context)
            .setExtensionRendererMode(
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER,
            )
        ExoPlayer.Builder(context, renderers).build()
    }

    DisposableEffect(url) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true

        // Releasing is not optional. A leaked codec surfaces later as a decoder failure
        // on an unrelated video, which looks random and is miserable to trace back.
        onDispose { player.release() }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    useController = true
                }
            },
        )
    }
}

/**
 * Hands the URL to whatever the device has installed.
 *
 * This is the widest format coverage available without bundling a second decoder stack,
 * because it reaches VLC, mpv and every other installed player. The chooser opens once and
 * this screen pops itself, so back does not land on an empty black surface.
 */
@Composable
private fun ExternalPlayer(url: String, onBack: () -> Unit) {
    val context = LocalContext.current

    LaunchedEffect(url) {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(view, "Play with"))
        }
        onBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}
