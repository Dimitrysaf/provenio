package io.github.dimitrysaf.provenio.shell.screens.p2p

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.i18n.localizedByteUnit
import io.github.dimitrysaf.provenio.core.p2p.P2P_SPEED_HISTORY_SIZE
import io.github.dimitrysaf.provenio.core.p2p.P2pPeerDetails
import io.github.dimitrysaf.provenio.core.p2p.P2pPeerSource
import io.github.dimitrysaf.provenio.core.p2p.P2pPieceMap
import io.github.dimitrysaf.provenio.core.p2p.P2pPieceState
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pSpeedSample
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingState
import io.github.dimitrysaf.provenio.core.p2p.P2pTorrentDetails
import io.github.dimitrysaf.provenio.core.p2p.P2pTorrentState
import io.github.dimitrysaf.provenio.core.p2p.P2pTrackerDetails
import io.github.dimitrysaf.provenio.core.p2p.P2pTrackerStatus
import io.github.dimitrysaf.provenio.core.p2p.formatP2pSpeed
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsListScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeSource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * Everything the P2P engine knows about the torrent it is serving: live speed, the file's
 * pieces, transfer totals, the swarm, and every peer and tracker.
 *
 * The engine serves one torrent at a time. [infoHash] names the torrent the caller means; when the
 * engine is serving another one, or none, the sheet says so instead of showing the wrong torrent.
 * The engine only gathers this while the sheet is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailsSheet(
    infoHash: String?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        P2pStreamingEngine.acquireTorrentDetails()
        onDispose { P2pStreamingEngine.releaseTorrentDetails() }
    }
    val details by P2pStreamingEngine.torrentDetails.collectAsStateWithLifecycle()
    val streamingState by P2pStreamingEngine.state.collectAsStateWithLifecycle()
    val settings by remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val shown = details?.takeIf { infoHash == null || it.infoHash.equals(infoHash, ignoreCase = true) }

    ModalSheet(
        onDismissRequest = { scope.launch { dismissBottomSheet(sheetState, onDismiss) } },
        sheetState = sheetState,
        fullHeight = true,
    ) {
        Text(
            text = shown?.name?.takeIf(String::isNotBlank)
                ?: stringResource(Res.string.torrent_details_title),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(bottom = 8.dp),
        )

        if (shown == null) {
            // Nothing reported yet while a stream starts; otherwise the engine is idle or busy
            // with a different torrent.
            val starting = details == null && (
                streamingState is P2pStreamingState.Connecting ||
                    streamingState is P2pStreamingState.Streaming
                )
            if (starting) {
                EmptyState(
                    icon = Icons.Rounded.RocketLaunch,
                    title = stringResource(Res.string.torrent_details_starting),
                    message = stringResource(Res.string.torrent_details_waiting),
                )
                return@ModalSheet
            }
            Text(
                text = stringResource(Res.string.torrent_details_not_active),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = BottomSheetBodyMargin)
                    .padding(top = 8.dp, bottom = 32.dp),
            )
            return@ModalSheet
        }

        TorrentDetailsContent(shown, seedingEnabled = settings.enableUpload)
    }
}

@Composable
private fun TorrentDetailsContent(details: P2pTorrentDetails, seedingEnabled: Boolean) {
    val unknown = stringResource(Res.string.torrent_details_unknown)
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Section {
                shapedRow { shape -> SpeedCard(details, seedingEnabled, shape) }
            }
        }

        details.pieces?.let { pieces ->
            item { Subheader(stringResource(Res.string.torrent_details_section_pieces)) }
            item {
                Section {
                    shapedRow { shape -> PieceMapCard(pieces, shape) }
                }
            }
        }

        item { Subheader(stringResource(Res.string.torrent_details_section_transfer)) }
        item {
            val fileProgress = details.fileProgress
            val progressDescription = if (fileProgress != null && details.fileSize != null) {
                stringResource(
                    Res.string.torrent_details_of,
                    formatBytes((details.fileSize * fileProgress).toLong()),
                    formatBytes(details.fileSize),
                )
            } else {
                null
            }
            val downloaded = stringResource(
                Res.string.torrent_details_session_total,
                formatBytes(details.sessionDownloaded),
                formatBytes(details.allTimeDownloaded),
            )
            val uploaded = if (seedingEnabled) {
                stringResource(
                    Res.string.torrent_details_session_total,
                    formatBytes(details.sessionUploaded),
                    formatBytes(details.allTimeUploaded),
                )
            } else {
                stringResource(Res.string.torrent_details_seeding_off)
            }
            val etaSeconds = rememberTickingSeconds(details.etaSeconds, countDown = true)
            Section {
                detailRow(
                    icon = Icons.Rounded.DataUsage,
                    title = stringResource(Res.string.torrent_details_progress),
                    supporting = progressDescription,
                    value = percent(fileProgress ?: details.progress),
                )
                detailRow(
                    icon = Icons.Rounded.ArrowDownward,
                    title = stringResource(Res.string.torrent_details_downloaded),
                    supporting = downloaded,
                    value = formatP2pSpeed(details.downloadSpeed),
                )
                detailRow(
                    icon = if (seedingEnabled) Icons.Rounded.ArrowUpward else Icons.Rounded.CloudOff,
                    title = stringResource(Res.string.torrent_details_uploaded),
                    supporting = uploaded,
                    value = if (seedingEnabled) formatP2pSpeed(details.uploadSpeed) else null,
                )
                detailRow(
                    icon = Icons.Rounded.Schedule,
                    title = stringResource(Res.string.torrent_details_eta),
                    value = etaSeconds?.let(::formatDuration) ?: "∞",
                )
                detailRow(
                    icon = Icons.Rounded.SwapVert,
                    title = stringResource(Res.string.torrent_details_ratio),
                    value = details.shareRatio?.let { formatDecimal(it, 2) } ?: unknown,
                )
                detailRow(
                    icon = Icons.Rounded.WarningAmber,
                    title = stringResource(Res.string.torrent_details_wasted),
                    supporting = stringResource(
                        Res.string.torrent_details_wasted_value,
                        formatBytes(details.redundantBytes),
                        formatBytes(details.hashFailedBytes),
                    ),
                    value = formatBytes(details.redundantBytes + details.hashFailedBytes),
                )
            }
        }

        item { Subheader(stringResource(Res.string.torrent_details_section_swarm)) }
        item {
            val connected = stringResource(
                Res.string.torrent_details_seeds_peers,
                details.connectedSeeds,
                details.connectedPeers,
            )
            val swarm = stringResource(
                Res.string.torrent_details_swarm_value,
                details.swarmSeeds?.toString() ?: "?",
                details.swarmLeechers?.toString() ?: "?",
            )
            val known = stringResource(
                Res.string.torrent_details_seeds_peers,
                details.knownSeeds,
                details.knownPeers,
            )
            val availability = details.availability?.let {
                stringResource(Res.string.torrent_details_availability_value, formatDecimal(it, 2))
            } ?: unknown
            Section {
                detailRow(
                    icon = Icons.Rounded.Groups,
                    title = stringResource(Res.string.torrent_details_connected),
                    value = connected,
                )
                detailRow(
                    icon = Icons.Rounded.Hub,
                    title = stringResource(Res.string.torrent_details_swarm_size),
                    value = swarm,
                )
                detailRow(title = stringResource(Res.string.torrent_details_known), value = known)
                detailRow(title = stringResource(Res.string.torrent_details_availability), value = availability)
            }
        }

        item { Subheader(stringResource(Res.string.torrent_details_section_torrent)) }
        item {
            val pieces = stringResource(
                Res.string.torrent_details_pieces_value,
                details.pieceCount,
                formatBytes(details.pieceLength.toLong()),
            )
            val state = stateLabel(details.state)
            val nextAnnounce = rememberTickingSeconds(details.nextAnnounceSeconds, countDown = true)?.let {
                stringResource(Res.string.torrent_details_in, formatDuration(it))
            } ?: unknown
            val activeSeconds = rememberTickingSeconds(details.activeSeconds, countDown = false) ?: 0L
            Section {
                details.fileName?.let { fileName ->
                    detailRow(
                        title = stringResource(Res.string.torrent_details_file),
                        supporting = fileName,
                        value = details.fileSize?.let(::formatBytes),
                    )
                }
                detailRow(title = stringResource(Res.string.torrent_details_size), value = formatBytes(details.totalSize))
                detailRow(title = stringResource(Res.string.torrent_details_files), value = details.fileCount.toString())
                detailRow(title = stringResource(Res.string.torrent_details_pieces), value = pieces)
                detailRow(title = stringResource(Res.string.torrent_details_state), value = state)
                detailRow(
                    title = stringResource(Res.string.torrent_details_active_time),
                    value = formatDuration(activeSeconds),
                )
                detailRow(title = stringResource(Res.string.torrent_details_next_announce), value = nextAnnounce)
                details.currentTracker?.let { tracker ->
                    detailRow(
                        title = stringResource(Res.string.torrent_details_current_tracker),
                        supporting = tracker,
                    )
                }
                detailRow(
                    title = stringResource(Res.string.torrent_details_info_hash),
                    supporting = details.infoHash,
                )
            }
        }

        item { Subheader(stringResource(Res.string.torrent_details_section_peers, details.peers.size)) }
        item {
            if (details.peers.isEmpty()) {
                EmptyLine(stringResource(Res.string.torrent_details_no_peers))
            } else {
                Section {
                    details.peers.forEach { peer ->
                        shapedRow { shape -> PeerRow(peer, shape) }
                    }
                }
            }
        }

        item { Subheader(stringResource(Res.string.torrent_details_section_trackers, details.trackers.size)) }
        item {
            if (details.trackers.isEmpty()) {
                EmptyLine(stringResource(Res.string.torrent_details_no_trackers))
            } else {
                Section {
                    details.trackers.forEach { tracker ->
                        shapedRow { shape -> TrackerRow(tracker, shape) }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(BottomSheetBodyMargin)) }
    }
}

@Composable
private fun Subheader(text: String) {
    ListSubheader(
        text = text,
        modifier = Modifier.padding(horizontal = BottomSheetBodyMargin).padding(top = 8.dp),
    )
}

@Composable
private fun Section(content: @Composable SettingsListScope.() -> Unit) {
    SettingsList(
        modifier = Modifier.padding(horizontal = BottomSheetBodyMargin),
        content = content,
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = BottomSheetBodyMargin),
    )
}

/** A stated row with an optional tonal icon, laid out as the settings list's rows are. */
private fun SettingsListScope.detailRow(
    title: String,
    icon: ImageVector? = null,
    supporting: String? = null,
    value: String? = null,
) = shapedRow { shape ->
    RowSurface(shape) {
        if (icon != null) TonalIcon(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            supporting?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RowSurface(
    shape: RoundedCornerShape,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun TonalIcon(
    icon: ImageVector,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.55f))
    }
}

/** Current download and upload, and the last two minutes of both. */
@Composable
private fun SpeedCard(details: P2pTorrentDetails, seedingEnabled: Boolean, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    val downloadColor = colors.primary
    val uploadColor = colors.tertiary
    val history = details.speedHistory
    Surface(modifier = Modifier.fillMaxWidth(), shape = shape, color = colors.surfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeedStat(
                    icon = Icons.Rounded.ArrowDownward,
                    container = colors.primaryContainer,
                    content = colors.onPrimaryContainer,
                    value = formatP2pSpeed(details.downloadSpeed),
                    label = stringResource(Res.string.torrent_details_download),
                    modifier = Modifier.weight(1f),
                )
                SpeedStat(
                    icon = if (seedingEnabled) Icons.Rounded.ArrowUpward else Icons.Rounded.CloudOff,
                    container = colors.tertiaryContainer,
                    content = colors.onTertiaryContainer,
                    value = if (seedingEnabled) formatP2pSpeed(details.uploadSpeed) else "—",
                    label = stringResource(
                        if (seedingEnabled) Res.string.torrent_details_upload else Res.string.torrent_details_seeding_off,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            SpeedGraph(
                history = history,
                downloadColor = downloadColor,
                uploadColor = uploadColor,
                showUpload = seedingEnabled,
                baselineColor = colors.outlineVariant,
            )
        }
    }
}

@Composable
private fun SpeedStat(
    icon: ImageVector,
    container: Color,
    content: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TonalIcon(icon, container = container, content = content)
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** Newest sample on the right; lines are smoothed so one-second jitter doesn't read as noise. */
@Composable
private fun SpeedGraph(
    history: List<P2pSpeedSample>,
    downloadColor: Color,
    uploadColor: Color,
    showUpload: Boolean,
    baselineColor: Color,
) {
    val peak = remember(history) {
        (history.maxOfOrNull { maxOf(it.downloadBytesPerSecond, it.uploadBytesPerSecond) } ?: 0L)
            .coerceAtLeast(1L)
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val stroke = 3.dp.toPx()
        val top = stroke
        val bottom = size.height - stroke / 2
        drawLine(
            color = baselineColor,
            start = Offset(0f, bottom),
            end = Offset(size.width, bottom),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
        if (history.size < 2) return@Canvas
        val step = size.width / (P2P_SPEED_HISTORY_SIZE - 1)
        val startX = size.width - step * (history.size - 1)
        val ceiling = peak * 1.1f
        fun point(index: Int, value: Long) = Offset(
            startX + step * index,
            bottom - (value / ceiling) * (bottom - top),
        )
        fun curve(value: (P2pSpeedSample) -> Long): Path = Path().apply {
            var previous = point(0, value(history[0]))
            moveTo(previous.x, previous.y)
            for (index in 1 until history.size) {
                val next = point(index, value(history[index]))
                val midX = (previous.x + next.x) / 2
                cubicTo(midX, previous.y, midX, next.y, next.x, next.y)
                previous = next
            }
        }
        val download = curve { it.downloadBytesPerSecond }
        val fill = Path().apply {
            addPath(download)
            lineTo(size.width, bottom)
            lineTo(startX, bottom)
            close()
        }
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                colors = listOf(downloadColor.copy(alpha = 0.32f), downloadColor.copy(alpha = 0f)),
                startY = top,
                endY = bottom,
            ),
        )
        drawPath(
            path = download,
            color = downloadColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        if (showUpload) {
            drawPath(
                path = curve { it.uploadBytesPerSecond },
                color = uploadColor,
                style = Stroke(width = stroke * 0.75f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * The file's pieces, first on the left. The marker stands where the file stops being ready to
 * play from its start; the strip beneath shades each piece by how many peers have it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PieceMapCard(pieces: P2pPieceMap, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    val cachedColor = colors.primary
    val seededColor = colors.tertiary
    val downloadingColor = colors.secondary
    val missingColor = colors.surfaceContainerHighest
    val neededColor = colors.error
    val markerColor = colors.onSurface
    val availabilityColor = colors.primary
    val cached = pieces.count(P2pPieceState.HAVE) + pieces.count(P2pPieceState.SEEDED)
    Surface(modifier = Modifier.fillMaxWidth(), shape = shape, color = colors.surfaceContainer) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.torrent_details_pieces_summary, cached, pieces.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(Res.string.torrent_details_ready, percent(pieces.readyFraction)),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                val stripHeight = 4.dp.toPx()
                val gap = 4.dp.toPx()
                val barHeight = size.height - stripHeight - gap
                val width = size.width / pieces.size
                val maxAvailability = (0 until pieces.size).maxOfOrNull(pieces::availability)?.coerceAtLeast(1) ?: 1
                drawRoundRect(
                    color = missingColor,
                    size = Size(size.width, barHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
                for (index in 0 until pieces.size) {
                    val color = when (pieces.state(index)) {
                        P2pPieceState.HAVE -> cachedColor
                        P2pPieceState.SEEDED -> seededColor
                        P2pPieceState.DOWNLOADING -> downloadingColor
                        P2pPieceState.BLOCKING, P2pPieceState.MISSING -> null
                    }
                    if (color != null) {
                        drawRect(color, Offset(index * width, 0f), Size(width + 0.5f, barHeight))
                    }
                    val available = pieces.availability(index)
                    if (available > 0) {
                        drawRect(
                            availabilityColor.copy(alpha = 0.12f + 0.6f * available / maxAvailability),
                            Offset(index * width, barHeight + gap),
                            Size(width + 0.5f, stripHeight),
                        )
                    }
                }
                // At least 2dp wide, so a piece the player waits on never disappears between its
                // neighbours on a long file.
                val minimumWidth = 2.dp.toPx()
                for (index in 0 until pieces.size) {
                    if (pieces.state(index) == P2pPieceState.BLOCKING) {
                        drawRect(neededColor, Offset(index * width, 0f), Size(maxOf(width, minimumWidth), barHeight))
                    }
                }
                val readyX = (size.width * pieces.readyFraction.coerceIn(0f, 1f))
                    .coerceIn(minimumWidth, size.width - minimumWidth)
                drawLine(
                    color = markerColor,
                    start = Offset(readyX, 0f),
                    end = Offset(readyX, barHeight),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LegendDot(cachedColor, stringResource(Res.string.torrent_details_legend_cached))
                LegendDot(seededColor, stringResource(Res.string.torrent_details_legend_seeded))
                LegendDot(downloadingColor, stringResource(Res.string.torrent_details_legend_downloading))
                LegendDot(neededColor, stringResource(Res.string.torrent_details_legend_blocking))
                LegendDot(missingColor, stringResource(Res.string.torrent_details_legend_missing))
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A peer: how much of the torrent it has, who it is, and what it is sending us. */
@Composable
private fun PeerRow(peer: P2pPeerDetails, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    RowSurface(shape) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { peer.progress },
                modifier = Modifier.size(40.dp),
                color = if (peer.isSeed) colors.tertiary else colors.primary,
                trackColor = colors.surfaceContainerHighest,
                strokeWidth = 3.dp,
                gapSize = 0.dp,
            )
            Text(
                text = formatDecimal(peer.progress * 100f, 0),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = peer.client.ifBlank { stringResource(Res.string.torrent_details_unknown_client) },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = peerDescription(peer),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RateLabel(Icons.Rounded.ArrowDownward, formatP2pSpeed(peer.downloadSpeed), colors.primary)
            if (peer.uploadSpeed > 0) {
                RateLabel(Icons.Rounded.ArrowUpward, formatP2pSpeed(peer.uploadSpeed), colors.tertiary)
            }
        }
    }
}

@Composable
private fun RateLabel(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1)
    }
}

@Composable
private fun TrackerRow(tracker: P2pTrackerDetails, shape: RoundedCornerShape) {
    val colors = MaterialTheme.colorScheme
    val (icon, container, content) = when (tracker.status) {
        P2pTrackerStatus.WORKING -> Triple(Icons.Rounded.CheckCircle, colors.primaryContainer, colors.onPrimaryContainer)
        P2pTrackerStatus.UPDATING -> Triple(Icons.Rounded.Sync, colors.secondaryContainer, colors.onSecondaryContainer)
        P2pTrackerStatus.ERROR -> Triple(Icons.Rounded.ErrorOutline, colors.errorContainer, colors.onErrorContainer)
        P2pTrackerStatus.NOT_CONTACTED -> Triple(Icons.Rounded.Schedule, colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
    RowSurface(shape) {
        TonalIcon(icon, container = container, content = content)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = trackerHost(tracker.url),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = trackerDescription(tracker),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** Address, what the peer has, and every flag that explains how it is behaving. */
@Composable
private fun peerDescription(peer: P2pPeerDetails): String {
    val parts = mutableListOf(peer.address)
    if (peer.isSeed) parts += stringResource(Res.string.torrent_details_peer_seed)
    if (peer.rttMs > 0) parts += stringResource(Res.string.torrent_details_rtt, peer.rttMs)
    if (peer.isConnecting) parts += stringResource(Res.string.torrent_details_peer_connecting)
    if (peer.isChokingUs) parts += stringResource(Res.string.torrent_details_peer_choking)
    if (peer.isSnubbed) parts += stringResource(Res.string.torrent_details_peer_snubbed)
    if (peer.isEncrypted) parts += stringResource(Res.string.torrent_details_peer_encrypted)
    if (peer.isUtp) parts += stringResource(Res.string.torrent_details_peer_utp)
    if (peer.isWebSeed) parts += stringResource(Res.string.torrent_details_peer_web_seed)
    peer.sources.sorted().forEach { source ->
        parts += stringResource(
            when (source) {
                P2pPeerSource.TRACKER -> Res.string.torrent_details_source_tracker
                P2pPeerSource.DHT -> Res.string.torrent_details_source_dht
                P2pPeerSource.PEX -> Res.string.torrent_details_source_pex
                P2pPeerSource.LSD -> Res.string.torrent_details_source_lsd
                P2pPeerSource.RESUME_DATA -> Res.string.torrent_details_source_resume
                P2pPeerSource.INCOMING -> Res.string.torrent_details_peer_incoming
            },
        )
    }
    return parts.joinToString(" · ")
}

@Composable
private fun trackerDescription(tracker: P2pTrackerDetails): String {
    val status = stringResource(
        when (tracker.status) {
            P2pTrackerStatus.NOT_CONTACTED -> Res.string.torrent_details_tracker_not_contacted
            P2pTrackerStatus.WORKING -> Res.string.torrent_details_tracker_working
            P2pTrackerStatus.UPDATING -> Res.string.torrent_details_tracker_updating
            P2pTrackerStatus.ERROR -> Res.string.torrent_details_tracker_error
        },
    )
    val scrape = if (tracker.seeds != null || tracker.leechers != null) {
        stringResource(
            Res.string.torrent_details_tracker_scrape,
            tracker.seeds?.toString() ?: "?",
            tracker.leechers?.toString() ?: "?",
            tracker.downloaded?.toString() ?: "?",
        )
    } else {
        null
    }
    val next = rememberTickingSeconds(tracker.nextAnnounceSeconds, countDown = true)?.let {
        stringResource(Res.string.torrent_details_in, formatDuration(it))
    }
    return listOfNotNull(tracker.message ?: status, scrape, next).joinToString(" · ")
}

private fun trackerHost(url: String): String =
    url.substringAfter("://").substringBefore('/').substringBefore('?').ifBlank { url }

@Composable
private fun stateLabel(state: P2pTorrentState): String = stringResource(
    when (state) {
        P2pTorrentState.UNKNOWN -> Res.string.torrent_details_state_unknown
        P2pTorrentState.CHECKING_FILES -> Res.string.torrent_details_state_checking_files
        P2pTorrentState.DOWNLOADING_METADATA -> Res.string.torrent_details_state_downloading_metadata
        P2pTorrentState.DOWNLOADING -> Res.string.torrent_details_state_downloading
        P2pTorrentState.FINISHED -> Res.string.torrent_details_state_finished
        P2pTorrentState.SEEDING -> Res.string.torrent_details_state_seeding
        P2pTorrentState.CHECKING_RESUME_DATA -> Res.string.torrent_details_state_checking_resume_data
    },
)

private fun percent(fraction: Float): String =
    "${formatDecimal(fraction.coerceIn(0f, 1f) * 100f, 1)}%"

private fun formatDecimal(value: Float, decimals: Int): String {
    var scale = 1
    repeat(decimals) { scale *= 10 }
    val scaled = kotlin.math.round(value * scale).toLong()
    val whole = scaled / scale
    if (decimals == 0) return whole.toString()
    val fraction = (kotlin.math.abs(scaled) % scale).toString().padStart(decimals, '0')
    return "$whole.$fraction"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gib -> "${formatDecimal((value / gib).toFloat(), 2)} ${localizedByteUnit("GB")}"
        value >= mib -> "${formatDecimal((value / mib).toFloat(), 1)} ${localizedByteUnit("MB")}"
        value >= kib -> "${formatDecimal((value / kib).toFloat(), 0)} ${localizedByteUnit("KB")}"
        else -> "$bytes ${localizedByteUnit("B")}"
    }
}

// The engine only re-reports a timer when other torrent state changes, so it ticks here in between.
@Composable
private fun rememberTickingSeconds(reported: Long?, countDown: Boolean): Long? {
    var elapsed by remember(reported) { mutableLongStateOf(0L) }
    LaunchedEffect(reported) {
        if (reported == null) return@LaunchedEffect
        val start = TimeSource.Monotonic.markNow()
        while (true) {
            delay(1_000L - start.elapsedNow().inWholeMilliseconds % 1_000L)
            elapsed = start.elapsedNow().inWholeSeconds
        }
    }
    return reported?.let { (if (countDown) it - elapsed else it + elapsed).coerceAtLeast(0L) }
}

private fun formatDuration(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    val days = seconds / 86_400
    val hours = seconds % 86_400 / 3_600
    val minutes = seconds % 3_600 / 60
    val rest = seconds % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${rest}s"
        else -> "${rest}s"
    }
}
