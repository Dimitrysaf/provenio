package io.github.dimitrysaf.provenio.player

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.dimitrysaf.provenio.simkl.SimklScrobbler
import kotlinx.coroutines.delay

@Composable
actual fun PlayerScreen(
    url: String,
    scrobbleTarget: ScrobbleTarget?,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val backend by PlayerRepository.backend.collectAsState()

    when (backend) {
        // Scrobbling is dropped for the external backend: once the URL is handed off,
        // this app never sees another playback event to report progress from.
        PlayerBackend.Builtin -> BuiltinPlayer(
            url = url,
            scrobbleTarget = scrobbleTarget,
            onBack = onBack,
            modifier = modifier,
        )
        PlayerBackend.External -> ExternalPlayer(url = url, onBack = onBack)
    }
}

@Composable
private fun BuiltinPlayer(
    url: String,
    scrobbleTarget: ScrobbleTarget?,
    onBack: () -> Unit,
    modifier: Modifier,
) {
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
    var playbackError by remember { mutableStateOf<PlaybackException?>(null) }

    DisposableEffect(url) {
        playbackError = null
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true

        // Releasing is not optional. A leaked codec surfaces later as a decoder failure
        // on an unrelated video, which looks random and is miserable to trace back.
        onDispose { player.release() }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    // media3's own overlay is switched off entirely; PlayerControls below
                    // draws the whole thing in Compose instead, sharing the app's own
                    // theme and touch targets rather than the stock media3 skin.
                    useController = false
                }
            },
        )
        PlayerControls(player = player, onBack = onBack)
        if (scrobbleTarget != null) {
            ScrobbleReporter(player = player, target = scrobbleTarget)
        }
    }

    playbackError?.let { error ->
        PlaybackErrorDialog(
            info = error.toDebugInfo(url),
            onDismiss = { playbackError = null },
        )
    }
}

/** Everything worth showing about a failed load, in the order it's most useful to read. */
private data class PlaybackDebugInfo(
    val errorCodeName: String,
    val errorCode: Int,
    val message: String,
    val causeSummary: String?,
    val url: String,
) {
    /** Plain text, so "Copy" hands over exactly what the dialog shows. */
    fun toClipboardText(): String = buildString {
        appendLine("Playback failed")
        appendLine("Error: $errorCodeName ($errorCode)")
        appendLine("Message: $message")
        if (causeSummary != null) appendLine("Cause: $causeSummary")
        appendLine("Source: $url")
    }
}

private fun PlaybackException.toDebugInfo(url: String): PlaybackDebugInfo {
    // The immediate cause is usually a wrapper (an ExoPlaybackException, say); the root
    // cause is the one that actually names what went wrong — a 404, a codec the device
    // does not have, a malformed container.
    val root = generateSequence(cause) { it.cause }.lastOrNull() ?: cause
    return PlaybackDebugInfo(
        errorCodeName = errorCodeName,
        errorCode = errorCode,
        message = message ?: "No message",
        causeSummary = root?.let { "${it::class.simpleName}: ${it.message ?: "no detail"}" },
        url = url,
    )
}

/**
 * What "the player failed to load" actually was, since a black screen with no explanation
 * is not something a source-quality problem can be told apart from a real app bug by.
 */
@Composable
private fun PlaybackErrorDialog(info: PlaybackDebugInfo, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
        title = { Text("Playback failed") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            ) {
                DebugRow("Error", "${info.errorCodeName} (${info.errorCode})")
                DebugRow("Message", info.message)
                info.causeSummary?.let { DebugRow("Cause", it) }
                DebugRow("Source", info.url)
            }
        },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(info.toClipboardText())) }) {
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DebugRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Reports this playback's lifecycle to Simkl: `start` when it begins or resumes, `pause`
 * whenever the user pauses, and `stop` when this screen goes away — each carrying the
 * current position as a percentage of the media's duration. Draws nothing; this is a pure
 * side effect riding on the player's own listener rather than a second poll loop.
 *
 * Also seeks once to [ScrobbleTarget.resumeProgressPercent], the first time the player
 * reports a real duration — before that, a percentage cannot be turned into a position.
 */
@Composable
private fun ScrobbleReporter(player: ExoPlayer, target: ScrobbleTarget) {
    DisposableEffect(player, target) {
        var seeked = false
        var hasStarted = false

        fun progressPercent(): Float {
            val duration = player.duration
            if (duration <= 0) return 0f
            return (player.currentPosition.toFloat() / duration.toFloat() * 100f)
                .coerceIn(0f, 100f)
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_READY || seeked) return
                seeked = true
                val resumePercent = target.resumeProgressPercent
                val duration = player.duration
                if (resumePercent != null && resumePercent > 0f && duration > 0) {
                    player.seekTo((duration * (resumePercent / 100f)).toLong())
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (playing) {
                    hasStarted = true
                    SimklScrobbler.start(target, progressPercent())
                } else if (hasStarted) {
                    // Only a real pause counts, not the initial false the player reports
                    // before it has ever played anything.
                    SimklScrobbler.pause(target, progressPercent())
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            if (hasStarted) {
                SimklScrobbler.stop(target, progressPercent())
            }
        }
    }
}

/**
 * The played-video overlay: back button, play/pause, +/-10s, and a seek bar with the
 * elapsed and total time. Everything media3's default [PlayerView] controller drew, redone
 * so it looks like the rest of the app instead of the stock Android styling.
 */
@Composable
private fun PlayerControls(player: ExoPlayer, onBack: () -> Unit) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var playbackState by remember { mutableStateOf(player.playbackState) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var position by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var controlsVisible by remember { mutableStateOf(true) }
    // Set only while the user is dragging the seek bar, so the position poll below does
    // not fight the thumb the user is holding.
    var seekPreviewMillis by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // media3 has no push callback for playback position, so polling while playing is the
    // standard way to keep a seek bar live.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (seekPreviewMillis == null) position = player.currentPosition.coerceAtLeast(0L)
            delay(SeekBarPollMillis)
        }
    }

    // Controls fade out on their own during playback, as in every other video app, so the
    // video is not left permanently covered by a bar nobody is touching.
    LaunchedEffect(controlsVisible, isPlaying, seekPreviewMillis) {
        if (controlsVisible && isPlaying && seekPreviewMillis == null) {
            delay(AutoHideMillis)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { controlsVisible = !controlsVisible },
            ),
    ) {
        if (playbackState == Player.STATE_BUFFERING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    IconButton(
                        onClick = {
                            val target = (player.currentPosition - SeekStepMillis)
                                .coerceAtLeast(0L)
                            player.seekTo(target)
                            position = target
                        },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Back 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    IconButton(
                        onClick = { if (player.isPlaying) player.pause() else player.play() },
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            val target = (player.currentPosition + SeekStepMillis)
                                .let { if (duration > 0) it.coerceAtMost(duration) else it }
                            player.seekTo(target)
                            position = target
                        },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                SeekBar(
                    position = seekPreviewMillis ?: position,
                    duration = duration,
                    onSeek = { seekPreviewMillis = it },
                    onSeekFinished = {
                        val target = seekPreviewMillis ?: return@SeekBar
                        player.seekTo(target)
                        position = target
                        seekPreviewMillis = null
                    },
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxValue = duration.coerceAtLeast(1L).toFloat()
    val value = position.toFloat().coerceIn(0f, maxValue)

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Slider(
            value = value,
            onValueChange = { onSeek(it.toLong()) },
            onValueChangeFinished = onSeekFinished,
            valueRange = 0f..maxValue,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatPlaybackTime(position),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatPlaybackTime(duration),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private const val SeekStepMillis = 10_000L
private const val SeekBarPollMillis = 200L
private const val AutoHideMillis = 3_500L

/** `m:ss`, or `h:mm:ss` once the video runs an hour or longer. */
private fun formatPlaybackTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val paddedSeconds = seconds.toString().padStart(2, '0')
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:$paddedSeconds"
    } else {
        "$minutes:$paddedSeconds"
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
