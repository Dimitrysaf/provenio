package io.github.dimitrysaf.provenio.feature.player

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import io.github.dimitrysaf.provenio.core.platform.MatchHostSystemBars
import io.github.dimitrysaf.provenio.designsystem.theme.dynamicColorScheme
import io.github.dimitrysaf.provenio.feature.detail.components.EpisodesSheet
import io.github.dimitrysaf.provenio.feature.detail.components.SourcesSheet
import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.player.PlaybackPositionRepository
import io.github.dimitrysaf.provenio.player.PlayerBackend
import io.github.dimitrysaf.provenio.player.PlayerRepository
import io.github.dimitrysaf.provenio.player.ScrobbleTarget
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.streamId
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.simkl.SimklScrobbler
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.back
import io.github.dimitrysaf.provenio.resources.player_cause
import io.github.dimitrysaf.provenio.resources.player_close
import io.github.dimitrysaf.provenio.resources.player_copy
import io.github.dimitrysaf.provenio.resources.player_error
import io.github.dimitrysaf.provenio.resources.player_message
import io.github.dimitrysaf.provenio.resources.player_no_message
import io.github.dimitrysaf.provenio.resources.player_pause
import io.github.dimitrysaf.provenio.resources.player_play
import io.github.dimitrysaf.provenio.resources.player_play_with
import io.github.dimitrysaf.provenio.resources.player_playback_failed
import io.github.dimitrysaf.provenio.resources.player_source
import io.github.dimitrysaf.provenio.resources.player_aspect
import io.github.dimitrysaf.provenio.resources.player_aspect_fill
import io.github.dimitrysaf.provenio.resources.player_aspect_fit
import io.github.dimitrysaf.provenio.resources.player_aspect_zoom
import io.github.dimitrysaf.provenio.resources.player_audio
import io.github.dimitrysaf.provenio.resources.player_back_5
import io.github.dimitrysaf.provenio.resources.player_cast
import io.github.dimitrysaf.provenio.resources.player_episode_number
import io.github.dimitrysaf.provenio.resources.player_episodes
import io.github.dimitrysaf.provenio.resources.player_forward_5
import io.github.dimitrysaf.provenio.resources.player_lock
import io.github.dimitrysaf.provenio.resources.player_sources
import io.github.dimitrysaf.provenio.resources.player_speed
import io.github.dimitrysaf.provenio.resources.player_stats_down
import io.github.dimitrysaf.provenio.resources.player_stats_peer
import io.github.dimitrysaf.provenio.resources.player_stats_seed
import io.github.dimitrysaf.provenio.resources.player_stats_up
import io.github.dimitrysaf.provenio.resources.player_subtitles
import io.github.dimitrysaf.provenio.resources.player_track_off
import io.github.dimitrysaf.provenio.resources.player_unlock
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun PlayerScreen(
    url: String,
    scrobbleTarget: ScrobbleTarget?,
    onBack: () -> Unit,
    modifier: Modifier,
    title: String?,
    videoId: String?,
    season: Int?,
    episode: Int?,
    episodeTitle: String?,
    streamId: String?,
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
            title = title,
            videoId = videoId,
            season = season,
            episode = episode,
            episodeTitle = episodeTitle,
            streamId = streamId,
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
    title: String?,
    videoId: String?,
    season: Int?,
    episode: Int?,
    episodeTitle: String?,
    streamId: String?,
) {
    val context = LocalContext.current
    val player = remember {
        // The default renderers only reach the device's own decoders, which on most
        // phones cannot handle AC-3, E-AC-3, DTS or TrueHD. This factory adds FFmpeg
        // software decoders behind them.
        //
        // ON, not PREFER: PREFER puts FFmpeg ahead of the platform decoders for video as
        // well, so a 4K stream that the phone has silicon for gets software decoded
        // instead. ON keeps hardware first and leaves FFmpeg as the fallback for the
        // formats it is here for.
        val renderers = NextRenderersFactory(context)
            .setExtensionRendererMode(
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
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
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    // Which video the sources sheet is fetching for, or null when it is closed. Usually the
    // episode already playing, but picking a different one from the episodes sheet points
    // this at that episode instead, before anything about the player itself changes.
    var sourcesTarget by remember { mutableStateOf<EpisodeTarget?>(null) }
    var episodesOpen by remember { mutableStateOf(false) }
    PauseWhileCovered(player = player, covered = sourcesTarget != null || episodesOpen)
    // The stream the player is on, which starts as the one navigated to and changes when
    // a different source is picked from the side sheet.
    var streamUrl by remember(url) { mutableStateOf(url) }
    // That stream's own identity (see SourceOption.streamId), kept alongside streamUrl so
    // the position recorder can remember it too. Starts as whatever the details page
    // already resolved this source to.
    var currentStreamId by remember(url) { mutableStateOf(streamId) }
    // Which episode is actually playing. Starts as the one navigated to and moves to
    // whatever is picked from the episodes sheet, the same way streamUrl moves to whatever
    // is picked from the sources sheet.
    var currentVideoId by remember(url) { mutableStateOf(videoId) }
    var currentSeason by remember(url) { mutableStateOf(season) }
    var currentEpisode by remember(url) { mutableStateOf(episode) }
    var currentEpisodeTitle by remember(url) { mutableStateOf(episodeTitle) }
    val currentScrobbleTarget = scrobbleTarget?.copy(season = currentSeason, episode = currentEpisode)

    // Changing source only changes what is loaded. This used to be a DisposableEffect
    // keyed on the URL, which meant picking a new source disposed the old effect first —
    // releasing the player — and then prepared a player that no longer existed. Nothing
    // happened at all: no buffering state, no new frame, the last one frozen on screen.
    // The player's lifetime belongs to the effect below, which is keyed on the player.
    LaunchedEffect(streamUrl) {
        playbackError = null
        // Capture exactly where the old source left off before it is torn down, tagged
        // with the source now loading rather than the one abandoned — a viewer who backs
        // out from here on should resume with what is about to play, not what they just
        // left. The periodic recorder below can be up to a few seconds stale, and a source
        // swap is exactly the moment ResumeWhereItStopped is about to read this value back.
        currentVideoId?.let {
            PlaybackPositionRepository.save(it, player.currentPosition, player.duration, currentStreamId)
        }
        // Stop before swapping so the surface lets go of the frame it is holding; without
        // it the previous source stays on screen until the new one has decoded enough to
        // paint over it.
        player.stop()
        player.setMediaItem(MediaItem.fromUri(streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            // Releasing is not optional. A leaked codec surfaces later as a decoder
            // failure on an unrelated video, which looks random and is miserable to trace
            // back. Tied to the player itself, so it happens when the screen goes away and
            // never merely because the source changed.
            player.release()
        }
    }

    // A real frame on screen is the one honest signal that loading is over — buffering
    // state alone can't tell "still fetching metadata" from "briefly stalled mid-episode".
    // Reset per streamUrl, so switching source shows the loading art again for the new
    // source's own start rather than leaving it permanently dismissed after the first ever.
    var hasRenderedFirstFrame by remember(streamUrl) { mutableStateOf(false) }
    DisposableEffect(player, streamUrl) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Backdrop and logo for the loading art below — fetched once per title, not per
    // source, since switching sources mid-episode should not re-fetch metadata already on
    // screen.
    var loadingMeta by remember { mutableStateOf<Meta?>(null) }
    LaunchedEffect(currentScrobbleTarget?.imdbId) {
        val target = currentScrobbleTarget ?: return@LaunchedEffect
        loadingMeta = AddonRepository.meta(target.mediaType, target.imdbId)
    }

    ImmersiveLandscapeEffect()
    ResumeWhereItStopped(
        player = player,
        streamUrl = streamUrl,
        videoId = currentVideoId,
        fallbackPercent = scrobbleTarget?.resumeProgressPercent,
    )
    PlaybackPositionRecorder(player = player, videoId = currentVideoId, streamId = currentStreamId)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    this.player = player
                    // media3's own overlay is switched off entirely; PlayerControls draws
                    // the whole thing in Compose instead, sharing the app's own theme and
                    // touch targets rather than the stock media3 skin.
                    useController = false
                }
            },
            update = { it.resizeMode = resizeMode },
        )
        // Sits above the video surface and below the controls — drawn here, between the
        // two, purely by z-order — and carries no click handling of its own, so a tap
        // still reaches PlayerControls underneath it exactly as if this were not here.
        if (!hasRenderedFirstFrame) {
            LoadingBackdrop(
                backdrop = loadingMeta?.background,
                logo = loadingMeta?.logo,
                modifier = Modifier.fillMaxSize(),
            )
        }
        PlayerChrome {
            PlayerControls(
                player = player,
                onBack = onBack,
                streamUrl = streamUrl,
                title = title,
                season = currentSeason,
                episode = currentEpisode,
                episodeTitle = currentEpisodeTitle,
                resizeMode = resizeMode,
                onResizeMode = { resizeMode = it },
                onOpenSources = currentVideoId?.let { id ->
                    {
                        sourcesTarget = EpisodeTarget(id, currentSeason, currentEpisode, currentEpisodeTitle)
                    }
                },
                onOpenEpisodes = currentScrobbleTarget?.imdbId?.let { { episodesOpen = true } },
            )
        }
        if (currentScrobbleTarget != null) {
            ScrobbleReporter(player = player, target = currentScrobbleTarget)
        }
    }

    val target = sourcesTarget
    if (target != null) {
        SourcesSheet(
            type = scrobbleTarget?.mediaType ?: MovieType,
            id = target.videoId,
            title = title,
            onDismiss = { sourcesTarget = null },
            onPlay = { source ->
                source.playableUrl?.let { streamUrl = it }
                currentStreamId = source.streamId
                if (target.videoId != currentVideoId) {
                    currentVideoId = target.videoId
                    currentSeason = target.season
                    currentEpisode = target.episode
                    currentEpisodeTitle = target.title
                }
                sourcesTarget = null
            },
            currentUrl = streamUrl,
        )
    }

    if (episodesOpen) {
        val target = currentScrobbleTarget
        if (target?.imdbId != null) {
            EpisodesSheet(
                type = target.mediaType,
                imdbId = target.imdbId,
                currentVideoId = currentVideoId,
                onDismiss = { episodesOpen = false },
                onSelectEpisode = { video ->
                    episodesOpen = false
                    sourcesTarget = EpisodeTarget(video.id, video.season, video.episode, video.title)
                },
            )
        }
    }

    playbackError?.let { error ->
        PlaybackErrorDialog(
            info = error.toDebugInfo(url, stringResource(Res.string.player_no_message)),
            onDismiss = { playbackError = null },
        )
    }
}

/** Which episode the sources sheet should fetch for — not always the one playing now. */
private data class EpisodeTarget(
    val videoId: String,
    val season: Int?,
    val episode: Int?,
    val title: String?,
)

/**
 * Stands in for the black screen while the stream itself is still loading — fetching
 * metadata, or (behind a torrent) waiting for the first pieces to arrive. A still black
 * frame reads as broken; the show's own art reads as "getting there".
 *
 * There is no real byte-level progress to show for either wait, so the logo itself
 * breathes instead of a determinate bar sitting under it — the plain logo, unmasked,
 * pulsing between dim and bright on a loop.
 */
@Composable
private fun LoadingBackdrop(backdrop: String?, logo: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color.Black)) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        if (logo != null) {
            val infinite = rememberInfiniteTransition(label = "loadingLogoPulse")
            val logoAlpha by infinite.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "loadingLogoAlpha",
            )
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.5f).alpha(logoAlpha),
            )
        }
    }
}

/**
 * Starts a title where it was left off.
 *
 * The local position wins over [fallbackPercent], which is what Simkl last recorded: the
 * local one is exact rather than a percentage, is written as recently as a few seconds
 * ago, and exists whether or not anyone is signed in. Simkl's is the fallback for a title
 * watched on another device.
 *
 * Seeks once per source. Doing it on every ready state would drag playback back to the
 * resume point every time the swarm stalled long enough to rebuffer.
 */
@Composable
private fun ResumeWhereItStopped(
    player: ExoPlayer,
    streamUrl: String,
    videoId: String?,
    fallbackPercent: Float?,
) {
    DisposableEffect(player, streamUrl, videoId, fallbackPercent) {
        var seeked = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_READY || seeked) return
                seeked = true

                val saved = PlaybackPositionRepository.positionFor(videoId)
                if (saved != null && saved.positionMillis > 0) {
                    player.seekTo(saved.positionMillis)
                    return
                }

                val duration = player.duration
                if (fallbackPercent != null && fallbackPercent > 0f && duration > 0) {
                    player.seekTo((duration * (fallbackPercent / 100f)).toLong())
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
}

/**
 * Keeps a local resume point for what is playing.
 *
 * Written on a timer rather than only on the way out, because the way out is not always
 * taken: a process killed in the background, a battery running flat and a crash all skip
 * every tidy-up path there is. A row written every few seconds costs one upsert and means
 * the worst case is losing that handful of seconds rather than the whole position.
 *
 * Seeking is deliberately not a trigger of its own — the timer catches it within a few
 * seconds, and writing on every scrub would hammer the database while a thumb is moving.
 */
@Composable
private fun PlaybackPositionRecorder(player: ExoPlayer, videoId: String?, streamId: String?) {
    if (videoId == null) return

    // The effects below are keyed on (player, videoId) only, so they keep running across a
    // source switch rather than restarting — but that means their closures would otherwise
    // freeze on whichever stream was current when they first launched. rememberUpdatedState
    // is exactly the fix Compose offers for that: the same running effect, reading the
    // latest stream id every time it saves.
    val latestStreamId by rememberUpdatedState(streamId)

    DisposableEffect(player, videoId) {
        onDispose {
            PlaybackPositionRepository.save(videoId, player.currentPosition, player.duration, latestStreamId)
        }
    }

    LaunchedEffect(player, videoId) {
        while (true) {
            delay(PositionSaveMillis)
            if (player.isPlaying) {
                PlaybackPositionRepository.save(videoId, player.currentPosition, player.duration, latestStreamId)
            }
        }
    }
}

/**
 * Holds playback while something is covering the picture.
 *
 * A sheet sits on top of the video, so whatever plays behind it is missed. It pauses on
 * the way in and picks up again on the way out — but only if it was playing to begin with,
 * so a film someone paused deliberately stays paused when they close the sheet.
 */
@Composable
private fun PauseWhileCovered(player: ExoPlayer, covered: Boolean) {
    var resumeWhenUncovered by remember { mutableStateOf(false) }

    LaunchedEffect(covered) {
        if (covered) {
            resumeWhenUncovered = player.isPlaying
            player.pause()
        } else if (resumeWhenUncovered) {
            resumeWhenUncovered = false
            player.play()
        }
    }
}

/**
 * The colour scheme the controls are drawn in.
 *
 * A video is always a dark surface whatever the rest of the app is set to, so the overlay
 * takes the dark scheme rather than the ambient one — still the user's own wallpaper
 * colours through [dynamicColorScheme], just the half of them that can be read on top of a
 * picture. Nothing here picks a literal colour; the roles do the work.
 */
@Composable
private fun PlayerChrome(content: @Composable () -> Unit) {
    val scheme = dynamicColorScheme(useDarkTheme = true) ?: darkColorScheme()
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
    ) {
        // A bare IconButton takes its colour from LocalContentColor, which outside a
        // Surface is black — which is how the back arrow ended up painting black on a
        // dark picture and looking like a missing icon. Nothing here draws on a Surface,
        // so the content colour has to be stated once for the whole overlay.
        CompositionLocalProvider(
            LocalContentColor provides scheme.onSurface,
            content = content,
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
        var hasStarted = false

        fun progressPercent(): Float {
            val duration = player.duration
            if (duration <= 0) return 0f
            return (player.currentPosition.toFloat() / duration.toFloat() * 100f)
                .coerceIn(0f, 100f)
        }

        val listener = object : Player.Listener {
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
 * Everything drawn over the video.
 *
 * One visibility flag governs the lot — titles, transport, seek bar, the torrent readout.
 * A tap toggles it. Nothing is exempt: a readout that stays up while the controls fade is
 * a permanent smudge on the picture, and the person watching asked for a clean frame.
 */
@Composable
private fun PlayerControls(
    player: ExoPlayer,
    onBack: () -> Unit,
    streamUrl: String,
    title: String?,
    season: Int?,
    episode: Int?,
    episodeTitle: String?,
    resizeMode: Int,
    onResizeMode: (Int) -> Unit,
    onOpenSources: (() -> Unit)?,
    onOpenEpisodes: (() -> Unit)?,
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var playbackState by remember { mutableStateOf(player.playbackState) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var position by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var bufferedPosition by remember { mutableLongStateOf(player.bufferedPosition.coerceAtLeast(0L)) }
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    // Held here rather than inside the group: the sheets are drawn outside the fading
    // controls, so one stays open when the controls time out underneath it.
    var speedOpen by remember { mutableStateOf(false) }
    var subtitlesOpen by remember { mutableStateOf(false) }
    var audioOpen by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf(player.currentTracks) }
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
                    // Picks up whatever ResumeWhereItStopped just seeked to for the new
                    // source — zero for a fresh episode, or a resume point for one picked
                    // back up. Without this the bar kept showing the previous episode's
                    // position for as long as playback stayed paused/buffering after a
                    // switch, since the poll below only runs while isPlaying is true.
                    position = player.currentPosition.coerceAtLeast(0L)
                }
            }

            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Zeroed the instant a new source is picked, rather than waiting on the state
    // listener above — switching source pauses for buffering, and the READY callback
    // that would otherwise sync this can be a moment away, which is exactly the window
    // where the bar was left showing the previous episode's leftover position.
    LaunchedEffect(streamUrl) {
        position = 0L
        bufferedPosition = 0L
    }

    // media3 has no push callback for playback position, so polling while playing is the
    // standard way to keep a seek bar live.
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (seekPreviewMillis == null) position = player.currentPosition.coerceAtLeast(0L)
            delay(SeekBarPollMillis)
        }
    }

    // Unlike position, this keeps polling while paused: behind a torrent, pieces keep
    // arriving whether or not playback is moving, and the bar showing what has already
    // loaded should say so. A coarser interval than the position poll — how far ahead the
    // buffer reaches does not need to be frame-accurate.
    LaunchedEffect(player) {
        while (true) {
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
            delay(BufferedPositionPollMillis)
        }
    }

    // Controls fade out on their own during playback, as in every other video app, so the
    // video is not left permanently covered by a bar nobody is touching.
    val seekBy: (Long) -> Unit = { delta ->
        val target = (player.currentPosition + delta).coerceAtLeast(0L)
            .let { if (duration > 0) it.coerceAtMost(duration) else it }
        player.seekTo(target)
        position = target
    }

    val selectTrack: (Int, TrackSelectionOverride?) -> Unit = { type, override ->
        val builder = player.trackSelectionParameters.buildUpon()
        if (override == null) {
            builder.setTrackTypeDisabled(type, true)
        } else {
            builder.setTrackTypeDisabled(type, false)
            builder.setOverrideForType(override)
        }
        player.trackSelectionParameters = builder.build()
    }

    val anySheetOpen = speedOpen || subtitlesOpen || audioOpen
    PauseWhileCovered(player = player, covered = anySheetOpen)
    LaunchedEffect(controlsVisible, isPlaying, seekPreviewMillis, anySheetOpen) {
        if (controlsVisible && isPlaying && seekPreviewMillis == null && !anySheetOpen) {
            delay(AutoHideMillis)
            controlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // A tap anywhere shows or hides the controls; a double tap on one side or the
            // other skips, which is the gesture every video app has taught people to
            // expect and which works without the controls being up at all. Locked means
            // locked, so the skip is off then and only the toggle remains.
            .pointerInput(locked) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { offset ->
                        if (!locked) {
                            val forward = offset.x > size.width / 2f
                            seekBy(if (forward) SeekStepMillis else -SeekStepMillis)
                        }
                    },
                )
            },
    ) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Two gradients rather than a flat wash: the picture keeps its contrast in the
            // middle, where nothing is drawn, and darkens only under the rows that carry
            // text. This is the scrim M3 specifies for controls over media.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                            0.28f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                        ),
                    ),
            ) {
                if (locked) {
                    // Locked means locked: one way out and nothing else to press.
                    FilledTonalIconButton(
                        onClick = { locked = false },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = stringResource(Res.string.player_unlock),
                        )
                    }
                    return@Box
                }

                TopRow(
                    onBack = onBack,
                    streamUrl = streamUrl,
                    title = title,
                    season = season,
                    episode = episode,
                    episodeTitle = episodeTitle,
                    modifier = Modifier.align(Alignment.TopStart),
                )

                TransportRow(
                    isPlaying = isPlaying,
                    // Anything that is not a frame on screen reads as loading, not just a
                    // mid-playback stall: IDLE is the player preparing, and BUFFERING is
                    // also the long wait while the torrent's metadata is fetched, since
                    // the loopback server holds that first request open until the swarm
                    // answers.
                    isBuffering = playbackState == Player.STATE_IDLE ||
                        playbackState == Player.STATE_BUFFERING,
                    onSeekBy = seekBy,
                    onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                    modifier = Modifier.align(Alignment.Center),
                )

                BottomRows(
                    position = seekPreviewMillis ?: position,
                    duration = duration,
                    bufferedPosition = bufferedPosition,
                    isPlaying = isPlaying,
                    onSeek = { seekPreviewMillis = it },
                    onSeekFinished = {
                        val target = seekPreviewMillis ?: return@BottomRows
                        player.seekTo(target)
                        position = target
                        seekPreviewMillis = null
                    },
                    resizeMode = resizeMode,
                    onResizeMode = onResizeMode,
                    onLock = { locked = true },
                    onOpenSources = onOpenSources,
                    onOpenEpisodes = onOpenEpisodes,
                    onOpenSpeed = { speedOpen = true },
                    onOpenSubtitles = { subtitlesOpen = true },
                    onOpenAudio = { audioOpen = true },
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }

        // Outside the AnimatedVisibility above: a sheet is its own window and should not
        // be torn down because the controls behind it timed out.
        if (speedOpen) {
            ChoiceSheet(
                title = stringResource(Res.string.player_speed),
                choices = PlaybackSpeeds.map { option ->
                    Choice(
                        label = formatSpeed(option),
                        selected = option == speed,
                        onSelect = {
                            speed = option
                            player.setPlaybackSpeed(option)
                        },
                    )
                },
                onDismiss = { speedOpen = false },
            )
        }
        if (subtitlesOpen) {
            ChoiceSheet(
                title = stringResource(Res.string.player_subtitles),
                choices = trackChoices(tracks, C.TRACK_TYPE_TEXT, selectTrack),
                onDismiss = { subtitlesOpen = false },
            )
        }
        if (audioOpen) {
            ChoiceSheet(
                title = stringResource(Res.string.player_audio),
                choices = trackChoices(tracks, C.TRACK_TYPE_AUDIO, selectTrack),
                onDismiss = { audioOpen = false },
            )
        }
    }
}

/** Back, what is playing, and how the swarm is doing. */
@Composable
private fun TopRow(
    onBack: () -> Unit,
    streamUrl: String,
    title: String?,
    season: Int?,
    episode: Int?,
    episodeTitle: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Built from whichever parts exist: a film has no episode line at all, and a
            // show whose add-on never named the episode still gets its number.
            val episodeLine = listOfNotNull(
                if (season != null && episode != null) {
                    stringResource(Res.string.player_episode_number, season, episode)
                } else {
                    null
                },
                episodeTitle?.takeIf { it.isNotBlank() },
            ).joinToString("  ")
            if (episodeLine.isNotEmpty()) {
                Text(
                    text = episodeLine,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TorrentStats(url = streamUrl)
    }
}

/** Back five, play or pause, forward five. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TransportRow(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onSeekBy: (Long) -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The same scrim-filled container the button groups below use, so the skip buttons
    // read as part of the same control family rather than bare icons floating over the
    // picture.
    val scheme = MaterialTheme.colorScheme
    val skipColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = scheme.scrim.copy(alpha = GroupContainerAlpha),
        contentColor = scheme.onSurface,
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        FilledIconButton(
            onClick = { onSeekBy(-SeekStepMillis) },
            modifier = Modifier.size(GroupButtonSize),
            colors = skipColors,
        ) {
            Icon(
                imageVector = Icons.Filled.Replay5,
                contentDescription = stringResource(Res.string.player_back_5),
                modifier = Modifier.size(SkipIconSize),
            )
        }
        // The loading state takes the play button's place rather than sitting beside it,
        // so the row never reflows and nothing moves under a thumb mid-press.
        Box(
            modifier = Modifier.size(PlayButtonSize),
            contentAlignment = Alignment.Center,
        ) {
            if (isBuffering) {
                // The expressive indicator, which morphs through the shape library while
                // it spins rather than tracing a circle. This is what waiting should look
                // like when the wait is a swarm rather than a spinner.
                LoadingIndicator(modifier = Modifier.size(PlayButtonSize))
            } else {
                // Playing is a circle; paused squares off. The shape carries the state, so
                // the transport reads correctly even from across a room where the icon
                // itself is too small to make out.
                val corner by animateDpAsState(
                    targetValue = if (isPlaying) PlayButtonSize / 2 else PausedCorner,
                    label = "playButtonCorner",
                )
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(PlayButtonSize)
                        // Lifts the primary control off the picture, the way the rest of
                        // the transport reads as sitting on a surface rather than painted
                        // straight onto the video.
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(corner)),
                    shape = RoundedCornerShape(corner),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) Res.string.player_pause else Res.string.player_play,
                        ),
                        modifier = Modifier.size(PlayIconSize),
                    )
                }
            }
        }
        FilledIconButton(
            onClick = { onSeekBy(SeekStepMillis) },
            modifier = Modifier.size(GroupButtonSize),
            colors = skipColors,
        ) {
            Icon(
                imageVector = Icons.Filled.Forward5,
                contentDescription = stringResource(Res.string.player_forward_5),
                modifier = Modifier.size(SkipIconSize),
            )
        }
    }
}

/** Times, the seek bar, and the two button groups under it. */
@Composable
private fun BottomRows(
    position: Long,
    duration: Long,
    bufferedPosition: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    resizeMode: Int,
    onResizeMode: (Int) -> Unit,
    onLock: () -> Unit,
    onOpenSources: (() -> Unit)?,
    onOpenEpisodes: (() -> Unit)?,
    onOpenSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatPlaybackTime(position),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatPlaybackTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WavySeekBar(
            position = position,
            duration = duration,
            bufferedPosition = bufferedPosition,
            isPlaying = isPlaying,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewingGroup(
                resizeMode = resizeMode,
                onResizeMode = onResizeMode,
                onLock = onLock,
                onOpenSpeed = onOpenSpeed,
                onOpenSubtitles = onOpenSubtitles,
                onOpenAudio = onOpenAudio,
            )
            LibraryGroup(onOpenSources = onOpenSources, onOpenEpisodes = onOpenEpisodes)
        }
    }
}

/**
 * A seek bar that waves while playing and holds still the instant it is not — a moving
 * line reads as "this is alive" on its own, with no separate indicator needed for it.
 *
 * Built from scratch rather than styling the standard [Slider]: the wave is drawn as a
 * sampled sine path, which is track content, not a track colour Slider has a slot for.
 * Dragging and tapping both seek, the same gestures the standard control offers.
 */
@Composable
private fun WavySeekBar(
    position: Long,
    duration: Long,
    /** How far the player has actually loaded ahead of playback, drawn as a plain gray
     * fill between the wavy played portion and the untouched track. */
    bufferedPosition: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxValue = duration.coerceAtLeast(1L).toFloat()
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val shownValue = dragValue ?: position.toFloat()
    val fraction = (shownValue / maxValue).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedPosition.toFloat() / maxValue).coerceIn(0f, 1f)

    val infinite = rememberInfiniteTransition(label = "wavySeekPhase")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "wavySeekPhasePosition",
    )
    // Freezes the wave the instant playback stops, rather than fading it out, so a still
    // line and a moving one map directly onto paused and playing.
    val amplitudeFraction by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        label = "wavySeekAmplitude",
    )

    val activeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    // Between the two: not yet played, but already sitting on the device, so it reads as
    // "ready" rather than "unknown" the way the plain track does.
    val bufferedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    // The stop indicator M3 draws at the far end of a determinate track, marking where it
    // finishes — it disappears once the active indicator actually reaches it.
    val stopIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    fun valueAt(x: Float, width: Float): Float =
        (x / width.coerceAtLeast(1f) * maxValue).coerceIn(0f, maxValue)

    Box(
        modifier = modifier
            .height(WavySeekBarHeight)
            .pointerInput(duration) {
                detectTapGestures(
                    onTap = { offset ->
                        val value = valueAt(offset.x, size.width.toFloat())
                        onSeek(value.toLong())
                        onSeekFinished()
                    },
                )
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragValue = valueAt(offset.x, size.width.toFloat())
                    },
                    onDragEnd = {
                        dragValue?.let { onSeek(it.toLong()) }
                        onSeekFinished()
                        dragValue = null
                    },
                    onDragCancel = { dragValue = null },
                ) { change, _ ->
                    val value = valueAt(change.position.x, size.width.toFloat())
                    dragValue = value
                    onSeek(value.toLong())
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2f
            // M3 tapers the wave to flat over the first and last tenth of the track,
            // rather than waving right up to a fixed thumb — the amplitude used to just
            // cut off at the very ends, which is where a full-height wave looked worst
            // sitting against the track's flat cap.
            val edgeTaper = minOf(fraction / 0.1f, (1f - fraction) / 0.1f).coerceIn(0f, 1f)
            val waveAmplitude = WavySeekAmplitude.toPx() * amplitudeFraction * edgeTaper
            val wavelength = WavySeekWavelength.toPx()
            val strokeWidth = WavySeekStrokeWidth.toPx()
            val gap = WavySeekGapSize.toPx()
            val stopRadius = WavySeekStopIndicatorSize.toPx() / 2f
            val activeWidth = size.width * fraction
            // Never drawn short of the played point — a stall can briefly leave the
            // reported buffer behind the position it is already playing from.
            val bufferedWidth = maxOf(size.width * bufferedFraction, activeWidth)

            // The track and the buffered fill only ever cover what the wave does not: a
            // real gap starts right after the active indicator, exactly the spec's own
            // gap between an indicator and its track, which is also what stops the flat
            // track line from showing through the wave's peaks and troughs. Measured from
            // the dot's own outer edge, not the raw played position underneath it — the
            // dot is drawn with a bigger radius than the gap itself, so starting the gap
            // at the position alone left the dot overlapping straight into it and the
            // gray butting up against the dot with no visible gap at all.
            val thumbRadius = WavySeekThumbRadius.toPx()
            val gappedStart = if (activeWidth > 0f) activeWidth + thumbRadius + gap else 0f
            val bufferedStart = gappedStart.coerceAtMost(size.width)
            // The stop indicator sits fixed at the far end with its own reserved gap —
            // capped here rather than after, so a stream that buffers close to 100%
            // (routine for a direct HTTP source, not just a near-finished torrent) can't
            // stretch the gray fill through that gap and butt it right up against the
            // dot. Without the cap the gap was only ever real while little had loaded.
            val trackRegionEnd = (size.width - stopRadius * 2f - gap).coerceAtLeast(bufferedStart)
            val bufferedEnd = maxOf(bufferedWidth, bufferedStart).coerceAtMost(trackRegionEnd)
            val trackEnd = trackRegionEnd

            if (bufferedEnd > bufferedStart) {
                drawLine(
                    color = bufferedColor,
                    start = Offset(bufferedStart, midY),
                    end = Offset(bufferedEnd, midY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            if (trackEnd > bufferedEnd) {
                drawLine(
                    color = trackColor,
                    start = Offset(bufferedEnd, midY),
                    end = Offset(trackEnd, midY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            // Hidden once playback has actually reached the end — a dot marking a finish
            // line still ahead makes no sense once nothing is ahead of it any more.
            if (fraction < 0.999f) {
                drawCircle(
                    color = stopIndicatorColor,
                    radius = stopRadius,
                    center = Offset(size.width - stopRadius, midY),
                )
            }

            val activePath = Path()
            var x = 0f
            var first = true
            while (x <= activeWidth) {
                val y = midY + sin(x / wavelength * 2f * PI.toFloat() + phase) * waveAmplitude
                if (first) {
                    activePath.moveTo(x, y)
                    first = false
                } else {
                    activePath.lineTo(x, y)
                }
                x += WavySeekSampleStep
            }
            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawCircle(
                color = activeColor,
                radius = WavySeekThumbRadius.toPx(),
                center = Offset(activeWidth, midY),
            )
        }
    }
}

/**
 * A connected button group.
 *
 * The spec calls a button group an invisible container with no colour of its own: it adds
 * padding and it reshapes the buttons, and the buttons themselves carry the container
 * treatment. So there is no surface here — 2dp between items, the run's outer ends fully
 * round and every inner corner 8dp, which is what makes a row of buttons read as one
 * connected control rather than six loose ones.
 *
 * M3's own `ButtonGroup` would do this, but it is expressive-only and `internal` in the
 * material3 build Compose Multiplatform 1.12.0 resolves, so the shape is assembled here.
 */
@Composable
private fun ConnectedButtonGroup(
    count: Int,
    item: @Composable (index: Int, shape: RoundedCornerShape) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GroupInnerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (index in 0 until count) {
            item(index, connectedShape(index, count))
        }
    }
}

/** Fully round where the run ends, square-ish where it meets the next button. */
private fun connectedShape(index: Int, count: Int): RoundedCornerShape {
    val start = if (index == 0) GroupOuterCorner else GroupInnerCorner
    val end = if (index == count - 1) GroupOuterCorner else GroupInnerCorner
    return RoundedCornerShape(
        topStart = start,
        bottomStart = start,
        topEnd = end,
        bottomEnd = end,
    )
}

/** How the picture is shown, and who is speaking. */
@Composable
private fun ViewingGroup(
    resizeMode: Int,
    onResizeMode: (Int) -> Unit,
    onLock: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
) {
    // Order matches the spec's reading direction: how it looks, how fast, who is
    // speaking, where it goes, and finally the one that turns the rest off.
    ConnectedButtonGroup(count = 6) { index, shape ->
        when (index) {
            0 -> {
                val aspectLabel = when (resizeMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FILL ->
                        stringResource(Res.string.player_aspect_fill)
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM ->
                        stringResource(Res.string.player_aspect_zoom)
                    else -> stringResource(Res.string.player_aspect_fit)
                }
                val aspectDescription = stringResource(Res.string.player_aspect)
                GroupButton(
                    icon = Icons.Filled.AspectRatio,
                    description = "$aspectDescription: $aspectLabel",
                    shape = shape,
                    onClick = { onResizeMode(nextResizeMode(resizeMode)) },
                )
            }
            1 -> GroupButton(
                icon = Icons.Filled.Speed,
                description = stringResource(Res.string.player_speed),
                shape = shape,
                onClick = onOpenSpeed,
            )
            2 -> GroupButton(
                icon = Icons.Filled.ClosedCaption,
                description = stringResource(Res.string.player_subtitles),
                shape = shape,
                onClick = onOpenSubtitles,
            )
            3 -> GroupButton(
                icon = Icons.Filled.Audiotrack,
                description = stringResource(Res.string.player_audio),
                shape = shape,
                onClick = onOpenAudio,
            )
            4 -> GroupButton(
                icon = Icons.Filled.Cast,
                description = stringResource(Res.string.player_cast),
                shape = shape,
                onClick = {},
                enabled = false,
            )
            else -> GroupButton(
                icon = Icons.Filled.Lock,
                description = stringResource(Res.string.player_lock),
                shape = shape,
                onClick = onLock,
            )
        }
    }
}

/** Where else this could be played from, and what is next. Both still inert. */
@Composable
private fun LibraryGroup(onOpenSources: (() -> Unit)?, onOpenEpisodes: (() -> Unit)?) {
    ConnectedButtonGroup(count = 2) { index, shape ->
        if (index == 0) {
            GroupButton(
                icon = Icons.Filled.VideoLibrary,
                description = stringResource(Res.string.player_sources),
                shape = shape,
                onClick = { onOpenSources?.invoke() },
                enabled = onOpenSources != null,
            )
        } else {
            GroupButton(
                icon = Icons.Filled.PlaylistPlay,
                description = stringResource(Res.string.player_episodes),
                shape = shape,
                onClick = { onOpenEpisodes?.invoke() },
                enabled = onOpenEpisodes != null,
            )
        }
    }
}

/**
 * One button of a group.
 *
 * A filled container, but filled with scrim rather than a palette colour: the picture
 * still reads through it while the icon gets something dark to sit against. Scrim is the
 * role Material already uses for darkening content under an overlay, so this stays a
 * theme role rather than a literal black — and a group needs some container, since the
 * spec rules out standard icon buttons inside one.
 */
@Composable
private fun GroupButton(
    icon: ImageVector,
    description: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        modifier = Modifier.size(GroupButtonSize),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = scheme.scrim.copy(alpha = GroupContainerAlpha),
            contentColor = scheme.onSurface,
            disabledContainerColor = scheme.scrim.copy(alpha = GroupDisabledContainerAlpha),
            disabledContentColor = scheme.onSurface.copy(alpha = GroupDisabledContentAlpha),
        ),
    ) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

/**
 * One choice in a sheet: what it says, whether it is the current one, what picking it does.
 */
private data class Choice(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/**
 * A list of choices, in the same sheet the sources use.
 *
 * A dropdown pinned to a 40dp button is a poor target on a phone held sideways, and it
 * covers the video it is anchored over. A sheet gives the list the full width and puts it
 * where a thumb already is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(title: String, choices: List<Choice>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // The sheet has its own window; keep it in step with the one it opened over.
        MatchHostSystemBars()
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            for (choice in choices) {
                // The clickable overload, not the headlineContent one: only this
                // signature carries contentPadding, and it takes the click itself rather
                // than needing a modifier for it.
                ListItem(
                    onClick = {
                        choice.onSelect()
                        onDismiss()
                    },
                    trailingContent = if (choice.selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    // A list item paints its own surface by default, which is a different
                    // role from the sheet's container — every row came out as a block in
                    // a slightly wrong colour. Transparent lets the sheet show through.
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    // And it indents its content 16dp of its own accord, which left every
                    // label short of the 24dp the header and the sources sheet use.
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    content = { Text(choice.label) },
                )
            }
        }
    }
}

/** Every track of one kind the file carries, plus the option of none at all. */
@Composable
private fun trackChoices(
    tracks: Tracks,
    trackType: Int,
    onSelectTrack: (Int, TrackSelectionOverride?) -> Unit,
): List<Choice> {
    val choices = mutableListOf(
        Choice(
            label = stringResource(Res.string.player_track_off),
            selected = false,
            onSelect = { onSelectTrack(trackType, null) },
        ),
    )
    for (group in tracks.groups.filter { it.type == trackType }) {
        for (index in 0 until group.length) {
            val format = group.getTrackFormat(index)
            choices += Choice(
                label = describeTrack(format.language, format.label, index),
                selected = group.isTrackSelected(index),
                onSelect = {
                    onSelectTrack(
                        trackType,
                        TrackSelectionOverride(group.mediaTrackGroup, index),
                    )
                },
            )
        }
    }
    return choices
}

/** What the swarm is doing, in icons rather than punctuation. */
@Composable
private fun TorrentStats(url: String, modifier: Modifier = Modifier) {
    val settings by P2pRepository.settings.collectAsState()
    val status by P2pRepository.status.collectAsState()

    val isTorrent = status.baseUrl?.let(url::startsWith) == true
    if (settings.hideStats || !isTorrent) return

    Column(
        modifier = modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End,
    ) {
        StatRow(stringResource(Res.string.player_stats_seed), status.seeds.toString())
        StatRow(stringResource(Res.string.player_stats_peer), status.peers.toString())
        StatRow(
            stringResource(Res.string.player_stats_down),
            formatTransferRate(status.downloadBytesPerSecond),
        )
        StatRow(
            stringResource(Res.string.player_stats_up),
            formatTransferRate(status.uploadBytesPerSecond),
        )
        // No label: a percentage on its own is unambiguous, and naming it only adds a
        // word to read.
        StatRow(null, "${(status.progress * 100f).toInt()}%")
    }
}

@Composable
private fun StatRow(label: String?, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            // Monospaced so the numbers do not shuffle sideways every time they tick.
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
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

/** "1x", "1.5x" — trailing zeroes dropped, because "1.0x" reads like a measurement. */
private fun formatSpeed(speed: Float): String {
    val tenths = (speed * 10f).toInt()
    return if (tenths % 10 == 0) "${tenths / 10}x" else "${tenths / 10}.${tenths % 10}x"
}

/** A track's language, its own label, or failing both its position in the list. */
private fun describeTrack(language: String?, label: String?, index: Int): String =
    label?.takeIf { it.isNotBlank() }
        ?: language?.takeIf { it.isNotBlank() }
        ?: "#${index + 1}"

private fun nextResizeMode(current: Int): Int = when (current) {
    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

private val PlaybackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * The play button, two percent over standard.
 *
 * Enough that the eye lands on it first without it becoming a target you could not miss
 * if you tried. The skip buttons beside it carry no container at all, so the filled
 * treatment is already doing the work of marking which one is the primary action.
 */
private val PlayButtonSize = 56.dp

/** How square the play button goes when paused. */
private val PausedCorner = 16.dp
private val PlayIconSize = 26.dp
private val SkipIconSize = 24.dp
private val GroupButtonSize = 40.dp

/** What a stream is when nothing said otherwise — matches the add-on protocol's types. */
private const val MovieType = "movie"

// Dark enough to carry an icon, sheer enough to keep the frame behind it.
private const val GroupContainerAlpha = 0.45f
private const val GroupDisabledContainerAlpha = 0.25f
private const val GroupDisabledContentAlpha = 0.38f
private val StatIconSize = 14.dp
// Connected group: 2dp between buttons at every size, the run's outer ends fully round,
// every inner corner 8dp.
private val GroupInnerPadding = 2.dp
private val GroupOuterCorner = 24.dp
private val GroupInnerCorner = 8.dp
private const val KilobyteBytes = 1_024L
private const val MegabyteBytes = 1_024L * 1_024L

/**
 * How long a read may take before the player gives up on it.
 *
 * Generous on purpose: behind a torrent, a read waits for the piece it needs, and the
 * honest answer to a slow swarm is to keep waiting rather than to fail the playback.
 */
private const val StreamTimeoutMillis = 60_000

private const val SeekStepMillis = 10_000L
private const val SeekBarPollMillis = 200L
private const val BufferedPositionPollMillis = 1_000L
private const val AutoHideMillis = 3_500L

/** How often the resume point is written while playing. */
private const val PositionSaveMillis = 5_000L

private val WavySeekBarHeight = 24.dp
private val WavySeekAmplitude = 2.dp
private val WavySeekWavelength = 36.dp
// M3's wavy indicator spec: a 4dp stroke for both the active wave and the track, a 4dp
// gap between them, and a 4dp stop indicator dot marking where the track ends.
private val WavySeekStrokeWidth = 4.dp
private val WavySeekGapSize = 4.dp
private val WavySeekStopIndicatorSize = 4.dp
private val WavySeekThumbRadius = 6.dp

/** How far apart the sampled points on the wave are, in pixels — finer than this is wasted. */
private const val WavySeekSampleStep = 4f

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
