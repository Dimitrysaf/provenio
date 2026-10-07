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

internal fun PlayerScreenRuntime.castMediaOrNull(): CastMedia? {
    if (activeTorrentInfoHash != null) return null
    if (activeSourceHeaders.keys.any { !it.equals("User-Agent", ignoreCase = true) }) return null
    val url = activeSourceUrl.trim()
    if (!url.isCastableUrl()) return null
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

internal fun PlayerScreenRuntime.startCasting(device: CastDevice) {
    val media = castMediaOrNull() ?: return
    showCastSheet = false
    shouldPlay = false
    playerController?.pause()
    CastController.cast(device, media)
}

internal fun PlayerScreenRuntime.stopCasting() {
    val positionMs = (CastController.state.value as? CastState.Active)?.playback?.positionMs
    CastController.stop()
    positionMs?.let { playerController?.seekTo(it) }
    shouldPlay = true
    playerController?.play()
}

internal fun PlayerScreenRuntime.resumeAfterCastFailure() {
    CastController.dismissFailure()
    shouldPlay = true
    playerController?.play()
}

@Composable
internal fun rememberCastNetworkAvailable(): Boolean {
    val available by produceState(initialValue = false) {
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
    DisposableEffect(Unit) {
        onDispose {
            if (CastController.state.value !is CastState.Idle) CastController.disconnect()
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
        )
    }
    if (showCastSheet) {
        CastDeviceSheet(
            castable = castMediaOrNull() != null,
            onDeviceSelected = { startCasting(it) },
            onDismiss = { showCastSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastDeviceSheet(
    castable: Boolean,
    onDeviceSelected: (CastDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val devices by CastController.devices.collectAsState()
    var searchedLong by remember { mutableStateOf(false) }
    val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss) } }

    DisposableEffect(Unit) {
        CastController.startDiscovery()
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
            if (!castable) {
                Text(
                    text = stringResource(Res.string.cast_stream_unsupported),
                    modifier = Modifier.padding(horizontal = BottomSheetBodyMargin, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            if (devices.isEmpty() || !searchedLong) {
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

@Composable
private fun CastRemote(
    state: CastState,
    title: String,
    artwork: String?,
    onPlayPause: (playing: Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    onStop: () -> Unit,
    onResumeHere: () -> Unit,
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
                            Icon(
                                imageVector = if (state.playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = playLabel,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
                else -> Unit
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

private fun String.isCastableUrl(): Boolean {
    val lower = lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
    val authority = lower.substringAfter("://").substringBefore('/').substringBefore('?').substringAfterLast('@')
    val host = if (authority.startsWith("[")) authority.substringAfter('[').substringBefore(']') else authority.substringBefore(':')
    return host.isNotBlank() && host != "localhost" && !host.startsWith("127.") && host != "::1" && host != "0.0.0.0"
}

private const val CastSearchHintMillis = 8_000L
private const val CastNetworkCheckMillis = 5_000L
