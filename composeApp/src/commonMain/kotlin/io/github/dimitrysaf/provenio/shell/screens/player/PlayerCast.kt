package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.core.cast.CastController
import io.github.dimitrysaf.provenio.core.cast.CastDevice
import io.github.dimitrysaf.provenio.core.cast.CastFailure
import io.github.dimitrysaf.provenio.core.cast.CastMedia
import io.github.dimitrysaf.provenio.core.cast.CastRelay
import io.github.dimitrysaf.provenio.core.cast.CastState
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.SmallLoadingSpinner
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import io.github.dimitrysaf.provenio.shell.components.consumeAllPointerEvents
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** Why what is playing cannot be cast right now, shown in the cast sheet instead of devices. */
internal enum class CastBlock { NoLocalNetwork, TorrentStarting, NeedsHeaders, LocalSource }

/** The reason casting is blocked, or null when it can go ahead. Null [networkAvailable] means not checked yet. */
internal fun PlayerScreenRuntime.castBlock(networkAvailable: Boolean?): CastBlock? {
    val torrent = activeTorrentInfoHash != null
    return when {
        torrent && p2pResolvedSourceUrl.isNullOrBlank() -> CastBlock.TorrentStarting
        !torrent && activeSourceHeaders.keys.any { !it.equals("User-Agent", ignoreCase = true) } -> CastBlock.NeedsHeaders
        !torrent && !activeSourceUrl.trim().let { it.isCastableUrl() || it.isLocalFile() } -> CastBlock.LocalSource
        networkAvailable == false -> CastBlock.NoLocalNetwork
        else -> null
    }
}

/**
 * What to send the cast device. A torrent from the built-in engine is only reachable on this
 * device, so it goes through the cast relay; anything else is sent as it is. Null when it cannot
 * be cast, the relay included.
 */
private fun PlayerScreenRuntime.castMediaOrNull(): CastMedia? {
    val torrent = activeTorrentInfoHash != null
    val source = (if (torrent) p2pResolvedSourceUrl else activeSourceUrl)?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val url = when {
        source.isCastableUrl() -> source
        torrent -> CastRelay.start(source, activeTorrentFilename?.takeIf(String::isNotBlank) ?: "stream.mp4")
        source.isLocalFile() -> CastRelay.start(source, source.substringBefore('?').substringAfterLast('/'))
        else -> null
    } ?: return null
    val episodeLabel = listOfNotNull(
        activeSeasonNumber?.let { season -> activeEpisodeNumber?.let { "S$season E$it" } },
        activeEpisodeTitle?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").takeIf { it.isNotBlank() }
    return CastMedia(
        url = url,
        title = title,
        subtitle = episodeLabel,
        imageUrl = background ?: poster,
        startPositionMs = playbackSnapshot.positionMs.coerceAtLeast(0L),
    )
}

/** Whether what is playing would reach the cast device through the relay: a torrent from the built-in engine, or a download. */
internal val PlayerScreenRuntime.castUsesRelay: Boolean
    get() = if (activeTorrentInfoHash != null) {
        p2pResolvedSourceUrl?.trim()?.isCastableUrl() == false
    } else {
        activeSourceUrl.trim().isLocalFile()
    }

/** Whether a relayed cast plays a downloaded file rather than a torrent. */
internal val PlayerScreenRuntime.castsDownload: Boolean
    get() = activeTorrentInfoHash == null && activeSourceUrl.trim().isLocalFile()

/** Sends what is playing to [device]; [onUnreachable] runs when the relay finds no local network address. */
internal fun PlayerScreenRuntime.startCasting(device: CastDevice, onUnreachable: () -> Unit) {
    scope.launch {
        // The relay opens a socket, which stays off the main thread.
        val media = withContext(Dispatchers.Default) { castMediaOrNull() }
        if (media == null) {
            onUnreachable()
            return@launch
        }
        showCastSheet = false
        shouldPlay = false
        playerController?.pause()
        CastController.cast(device, media)
    }
}

internal fun PlayerScreenRuntime.stopCasting() {
    val positionMs = (CastController.state.value as? CastState.Active)?.playback?.positionMs
    CastController.stop()
    CastRelay.stop()
    positionMs?.let { playerController?.seekTo(it) }
    shouldPlay = true
    playerController?.play()
}

internal fun PlayerScreenRuntime.resumeAfterCastFailure() {
    CastController.dismissFailure()
    CastRelay.stop()
    shouldPlay = true
    playerController?.play()
}

@Composable
internal fun rememberCastNetworkAvailable(): Boolean? {
    val available by produceState<Boolean?>(initialValue = null) {
        while (true) {
            value = withContext(Dispatchers.Default) { CastController.hasLocalNetwork() }
            delay(CastNetworkCheckMillis)
        }
    }
    return available
}

@Composable
internal fun PlayerScreenRuntime.RenderCastLayer() {
    if (!CastController.isSupported) return
    val castState by CastController.state.collectAsState()
    val casting = castState !is CastState.Idle

    LaunchedEffect(casting, playerController) {
        if (casting) {
            shouldPlay = false
            playerController?.pause()
        }
    }
    LaunchedEffect(casting) {
        if (!casting) CastRelay.stop()
    }
    DisposableEffect(Unit) {
        onDispose {
            if (CastController.state.value !is CastState.Idle) CastController.disconnect()
            CastRelay.stop()
        }
    }

    if (casting) {
        CastRemote(
            state = castState,
            title = title,
            artwork = background ?: poster,
            onPlayPause = { playing -> if (playing) CastController.pause() else CastController.play() },
            onSeek = { CastController.seekTo(it) },
            onStop = { stopCasting() },
            onResumeHere = { resumeAfterCastFailure() },
            viaRelay = castUsesRelay,
        )
    }
    if (showCastSheet) {
        val networkAvailable = rememberCastNetworkAvailable()
        var relayFailed by remember { mutableStateOf(false) }
        CastDeviceSheet(
            block = if (relayFailed) CastBlock.NoLocalNetwork else castBlock(networkAvailable),
            viaRelay = castUsesRelay,
            download = castsDownload,
            onDeviceSelected = { device -> startCasting(device) { relayFailed = true } },
            onDismiss = { showCastSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastDeviceSheet(
    block: CastBlock?,
    viaRelay: Boolean,
    download: Boolean,
    onDeviceSelected: (CastDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val devices by CastController.devices.collectAsState()
    var searchedLong by remember { mutableStateOf(false) }
    val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss) } }

    val castable = block == null
    DisposableEffect(castable) {
        if (castable) CastController.startDiscovery()
        onDispose { CastController.stopDiscovery() }
    }
    LaunchedEffect(Unit) {
        delay(CastSearchHintMillis)
        searchedLong = true
    }

    ModalSheet(onDismissRequest = close, sheetState = sheetState) {
        SheetHeader(
            title = stringResource(Res.string.cast_sheet_title),
            navigation = SheetNavigation.Close,
            onNavigate = close,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = BottomSheetBodyMargin)) {
            if (block != null) {
                CastBlockedNotice(block)
                return@Column
            }
            val vpnActive = remember { CastRelay.vpnActive() }
            if (vpnActive && devices.isNotEmpty()) {
                CastHelp(
                    title = stringResource(Res.string.cast_help_vpn_title),
                    points = listOf(stringResource(Res.string.cast_help_vpn_allow_lan)),
                    warning = true,
                )
            }
            if (viaRelay && devices.isNotEmpty()) {
                CastHelp(
                    title = stringResource(
                        if (download) Res.string.cast_help_download_title else Res.string.cast_help_torrent_title,
                    ),
                    points = listOfNotNull(
                        if (download) {
                            stringResource(Res.string.cast_help_download_sent)
                        } else {
                            stringResource(Res.string.cast_help_torrent_keeps_downloading)
                        },
                        stringResource(Res.string.cast_help_torrent_keep_open),
                    ),
                )
            }
            devices.forEach { device ->
                ListItem(
                    headlineContent = { Text(device.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = device.model?.let { model -> { Text(model, maxLines = 1) } },
                    leadingContent = { Icon(imageVector = Icons.Rounded.Cast, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(enabled = castable) { onDeviceSelected(device) },
                )
            }
            if (devices.isEmpty() && searchedLong) {
                CastHelp(
                    title = stringResource(Res.string.cast_no_devices),
                    points = noDevicesHelp(vpnActive),
                    warning = vpnActive,
                )
            } else if (devices.isEmpty() || !searchedLong) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = BottomSheetBodyMargin, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!searchedLong) SmallLoadingSpinner(size = 20.dp)
                    Text(
                        text = stringResource(
                            if (devices.isEmpty() && searchedLong) Res.string.cast_no_devices else Res.string.cast_searching,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Why casting cannot happen, in place of the device list, with what to do about it. */
@Composable
private fun CastBlockedNotice(block: CastBlock) {
    when (block) {
        CastBlock.TorrentStarting -> CastHelp(
            title = stringResource(Res.string.cast_blocked_torrent_starting_title),
            points = listOf(stringResource(Res.string.cast_blocked_torrent_starting_wait)),
        )
        CastBlock.NeedsHeaders -> CastHelp(
            title = stringResource(Res.string.cast_blocked_stream_title),
            points = listOf(
                stringResource(Res.string.cast_blocked_headers_reason),
                stringResource(Res.string.cast_blocked_pick_other),
            ),
            warning = true,
        )
        CastBlock.LocalSource -> CastHelp(
            title = stringResource(Res.string.cast_blocked_stream_title),
            points = listOf(
                stringResource(Res.string.cast_blocked_local_reason),
                stringResource(Res.string.cast_blocked_pick_online),
            ),
            warning = true,
        )
        CastBlock.NoLocalNetwork -> CastHelp(
            title = stringResource(Res.string.cast_blocked_no_network_title),
            points = listOfNotNull(
                stringResource(Res.string.cast_blocked_no_network_connect),
                stringResource(Res.string.cast_help_same_network),
                if (CastRelay.vpnActive()) stringResource(Res.string.cast_help_vpn_allow_lan) else null,
            ),
            warning = true,
        )
    }
}

/** What to try when no cast device turns up, the VPN first when one is on. */
@Composable
private fun noDevicesHelp(vpnActive: Boolean): List<String> = listOfNotNull(
    if (vpnActive) stringResource(Res.string.cast_help_vpn_hiding) else null,
    stringResource(Res.string.cast_help_same_network),
    stringResource(Res.string.cast_help_guest_wifi),
    stringResource(Res.string.cast_help_tv_on),
    stringResource(Res.string.cast_help_firewall),
)

/** A short heading and a bulleted list: a reason, a warning, or things to try. */
@Composable
private fun CastHelp(
    title: String,
    points: List<String>,
    warning: Boolean = false,
    onDark: Boolean = false,
) {
    val titleColor = when {
        onDark -> Color.White
        warning -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val textColor = if (onDark) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BottomSheetBodyMargin, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = titleColor)
        points.forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "•", style = MaterialTheme.typography.bodyMedium, color = textColor)
                Text(text = point, style = MaterialTheme.typography.bodyMedium, color = textColor)
            }
        }
    }
}

@Composable
private fun CastRemote(
    state: CastState,
    title: String,
    artwork: String?,
    onPlayPause: (playing: Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    onStop: () -> Unit,
    onResumeHere: () -> Unit,
    viaRelay: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .consumeAllPointerEvents(),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.25f,
            )
        }
        Column(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = if (state is CastState.Active) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = castStatusText(state),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            when (state) {
                is CastState.Connecting -> SmallLoadingSpinner(size = 32.dp, color = Color.White)
                is CastState.Active -> {
                    CastSeekBar(
                        positionMs = state.playback.positionMs,
                        durationMs = state.playback.durationMs,
                        onSeek = onSeek,
                    )
                    val playLabel = stringResource(
                        if (state.playback.isPlaying) Res.string.compose_action_pause else Res.string.detail_btn_play,
                    )
                    WithTooltip(playLabel) {
                        FilledIconButton(
                            onClick = { onPlayPause(state.playback.isPlaying) },
                            modifier = Modifier.size(64.dp),
                        ) {
                            PlayPauseIcon(
                                playing = state.playback.isPlaying,
                                contentDescription = playLabel,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
                else -> Unit
            }
            if (state is CastState.Failed && viaRelay) {
                val vpnActive = remember { CastRelay.vpnActive() }
                CastHelp(
                    title = stringResource(Res.string.cast_help_relay_failed_title),
                    points = listOfNotNull(
                        if (vpnActive) stringResource(Res.string.cast_help_vpn_allow_lan) else null,
                        stringResource(Res.string.cast_help_same_network),
                        stringResource(Res.string.cast_help_guest_wifi),
                        stringResource(Res.string.cast_help_firewall),
                    ),
                    onDark = true,
                )
            }
            if (state is CastState.Failed) {
                FilledTonalButton(onClick = onResumeHere) {
                    Text(stringResource(Res.string.cast_resume_here))
                }
            } else {
                FilledTonalButton(onClick = onStop) {
                    Text(stringResource(Res.string.cast_stop))
                }
            }
        }
    }
}

@Composable
private fun CastSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var dragPositionMs by remember { mutableStateOf<Long?>(null) }
    val shownMs = dragPositionMs ?: positionMs
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (durationMs > 0) shownMs.coerceIn(0L, durationMs).toFloat() else 0f,
            onValueChange = { dragPositionMs = it.toLong() },
            onValueChangeFinished = {
                dragPositionMs?.let(onSeek)
                dragPositionMs = null
            },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            enabled = durationMs > 0,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatPlaybackTime(shownMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
            if (durationMs > 0) {
                Text(formatPlaybackTime(durationMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun castStatusText(state: CastState): String = when (state) {
    is CastState.Connecting -> stringResource(Res.string.cast_connecting, state.device.name)
    is CastState.Active -> stringResource(Res.string.cast_casting_to, state.device.name)
    is CastState.Failed -> when (state.reason) {
        CastFailure.Unreachable -> stringResource(Res.string.cast_failed_unreachable, state.device.name)
        CastFailure.Unsupported -> stringResource(Res.string.cast_failed_unsupported, state.device.name)
        CastFailure.Disconnected -> stringResource(Res.string.cast_failed_disconnected, state.device.name)
    }
    CastState.Idle -> ""
}

private fun String.isLocalFile(): Boolean = lowercase().startsWith("file:")

private fun String.isCastableUrl(): Boolean {
    val lower = lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
    val authority = lower.substringAfter("://").substringBefore('/').substringBefore('?').substringAfterLast('@')
    val host = if (authority.startsWith("[")) authority.substringAfter('[').substringBefore(']') else authority.substringBefore(':')
    return host.isNotBlank() && host != "localhost" && !host.startsWith("127.") && host != "::1" && host != "0.0.0.0"
}

private const val CastSearchHintMillis = 8_000L
private const val CastNetworkCheckMillis = 5_000L
