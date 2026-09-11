package io.github.dimitrysaf.provenio.feature.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.player.PlayerBackend
import io.github.dimitrysaf.provenio.player.PlayerRepository
import io.github.dimitrysaf.provenio.player.ScrobbleTarget
import io.github.dimitrysaf.provenio.simkl.SimklScrobbler
import kotlinx.coroutines.delay
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.back
import io.github.dimitrysaf.provenio.resources.player_back_10
import io.github.dimitrysaf.provenio.resources.player_cause
import io.github.dimitrysaf.provenio.resources.player_close
import io.github.dimitrysaf.provenio.resources.player_copy
import io.github.dimitrysaf.provenio.resources.player_error
import io.github.dimitrysaf.provenio.resources.player_forward_10
import io.github.dimitrysaf.provenio.resources.player_message
import io.github.dimitrysaf.provenio.resources.player_no_message
import io.github.dimitrysaf.provenio.resources.player_pause
import io.github.dimitrysaf.provenio.resources.player_play
import io.github.dimitrysaf.provenio.resources.player_play_with
import io.github.dimitrysaf.provenio.resources.player_playback_failed
import io.github.dimitrysaf.provenio.resources.player_source
import io.github.dimitrysaf.provenio.resources.player_stats_downloaded
import io.github.dimitrysaf.provenio.resources.player_stats_peers
import io.github.dimitrysaf.provenio.resources.player_stats_seeds
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

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

        // The eight second default is fine for a web server and far too short for a
        // torrent: the loopback server answers a read only once the piece it covers has
        // arrived, which after a seek means waiting on the swarm. Timing out there would
        // end playback rather than buffer it.
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(StreamTimeoutMillis)
            .setReadTimeoutMs(StreamTimeoutMillis)

        ExoPlayer.Builder(context, renderers)
            // Wrapped rather than used directly so file and content URIs still resolve;
            // only http goes through the factory above.
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(DefaultDataSource.Factory(context, http)),
            )
            .build()
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

    ImmersiveLandscapeEffect()

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
        // Outside PlayerControls on purpose, so it stays up after the controls fade. The
        // whole point of it is watching the swarm while the video plays.
        TorrentStats(
            url = url,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        if (scrobbleTarget != null) {
            ScrobbleReporter(player = player, target = scrobbleTarget)
        }
    }

    playbackError?.let { error ->
        PlaybackErrorDialog(
            info = error.toDebugInfo(url, stringResource(Res.string.player_no_message)),
            onDismiss = { playbackError = null },
        )
    }
}

/**
 * Turns the whole window over to the video: landscape, no system bars, and a screen that
 * does not go dark on its own.
 *
 * All three are undone on the way out, so the rest of the app keeps the orientation the
 * user was holding the phone in and goes back to sleeping normally. The bars are hidden
 * rather than drawn behind because a video is the one screen where the clock and the
 * gesture pill are pure subtraction — and they come back on a swipe from the edge, which
 * is the behaviour every video app has trained people to expect.
 *
 * The keep-awake flag is a window flag rather than a wake lock on purpose: it needs no
 * permission and Android drops it for us if the app is backgrounded or killed, so there is
 * no way to leak a screen that never sleeps again.
 */
@Composable
private fun ImmersiveLandscapeEffect() {
    val activity = LocalActivity.current ?: return

    DisposableEffect(activity) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val previousOrientation = activity.requestedOrientation

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = previousOrientation
        }
    }
}

/**
 * What the swarm is doing, while it is doing it.
 *
 * Only for torrents: a debrid link or a plain HTTP file has no peers to report, so the
 * readout is matched against the engine's own loopback address rather than shown for every
 * stream. Hidden entirely when the user has switched torrent stats off in settings.
 */
@Composable
private fun TorrentStats(url: String, modifier: Modifier = Modifier) {
    val settings by P2pRepository.settings.collectAsState()
    val status by P2pRepository.status.collectAsState()

    val isTorrent = status.baseUrl?.let(url::startsWith) == true
    if (settings.hideStats || !isTorrent) return

    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            // Without this the rows below would each fill the screen's width rather than
            // the widest row's, and the readout would stretch across the whole video.
            .width(IntrinsicSize.Max),
    ) {
        StatRow(stringResource(Res.string.player_stats_peers), status.peers.toString())
        StatRow(stringResource(Res.string.player_stats_seeds), status.seeds.toString())
        StatRow("↓", formatTransferRate(status.downloadBytesPerSecond))
        StatRow("↑", formatTransferRate(status.uploadBytesPerSecond))
        StatRow(
            stringResource(Res.string.player_stats_downloaded),
            "${(status.progress * 100f).toInt()}%",
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = value,
            color = Color.White,
            // Monospaced so the numbers do not shuffle sideways every time they tick.
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/** A transfer rate at the largest unit that still leaves a number worth reading. */
private fun formatTransferRate(bytesPerSecond: Long): String = when {
    bytesPerSecond >= MegabyteBytes -> {
        val tenths = bytesPerSecond * 10 / MegabyteBytes
        "${tenths / 10}.${tenths % 10} MB/s"
    }
    bytesPerSecond >= KilobyteBytes -> "${bytesPerSecond / KilobyteBytes} KB/s"
    else -> "$bytesPerSecond B/s"
}

private const val KilobyteBytes = 1_024L
private const val MegabyteBytes = 1_024L * 1_024L

/** Everything worth showing about a failed load, in the order it's most useful to read. */
private data class PlaybackDebugInfo(
    val errorCodeName: String,
    val errorCode: Int,
    val message: String,
    val causeSummary: String?,
    val url: String,
) {
    /**
     * Plain text, so "Copy" hands over exactly what the dialog shows.
     *
     * Deliberately not translated. This is pasted into a bug report or a logcat search,
     * where it is read by whoever is fixing the problem rather than by the person who hit
     * it, and an English report is the one they can act on.
     */
    fun toClipboardText(): String = buildString {
        appendLine("Playback failed")
        appendLine("Error: $errorCodeName ($errorCode)")
        appendLine("Message: $message")
        if (causeSummary != null) appendLine("Cause: $causeSummary")
        appendLine("Source: $url")
    }
}

private fun PlaybackException.toDebugInfo(
    url: String,
    fallbackMessage: String,
): PlaybackDebugInfo {
    // The immediate cause is usually a wrapper (an ExoPlaybackException, say); the root
    // cause is the one that actually names what went wrong — a 404, a codec the device
    // does not have, a malformed container.
    val root = generateSequence(cause) { it.cause }.lastOrNull() ?: cause
    return PlaybackDebugInfo(
        errorCodeName = errorCodeName,
        errorCode = errorCode,
        message = message ?: fallbackMessage,
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
        title = { Text(stringResource(Res.string.player_playback_failed)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            ) {
                DebugRow(
                    stringResource(Res.string.player_error),
                    "${info.errorCodeName} (${info.errorCode})",
                )
                DebugRow(stringResource(Res.string.player_message), info.message)
                info.causeSummary?.let { DebugRow(stringResource(Res.string.player_cause), it) }
                DebugRow(stringResource(Res.string.player_source), info.url)
            }
        },
        confirmButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(info.toClipboardText())) }) {
                Text(stringResource(Res.string.player_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.player_close)) }
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
                        contentDescription = stringResource(Res.string.back),
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
                            contentDescription = stringResource(Res.string.player_back_10),
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
                            contentDescription = stringResource(
                                if (isPlaying) Res.string.player_pause else Res.string.player_play,
                            ),
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
                            contentDescription = stringResource(Res.string.player_forward_10),
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

/**
 * How long a read may take before the player gives up on it.
 *
 * Generous on purpose: behind a torrent, a read waits for the piece it needs, and the
 * honest answer to a slow swarm is to keep waiting rather than to fail the playback.
 */
private const val StreamTimeoutMillis = 60_000

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
            val title = getString(Res.string.player_play_with)
            context.startActivity(Intent.createChooser(view, title))
        }
        onBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
}
