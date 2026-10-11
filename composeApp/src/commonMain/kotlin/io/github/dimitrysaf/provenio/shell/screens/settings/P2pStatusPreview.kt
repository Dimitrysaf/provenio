package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.p2p.ExternalTorrentServer
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingState
import io.github.dimitrysaf.provenio.core.p2p.P2pStreams
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.theme.provenio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** How the torrent server answers a check: not asked yet, answering, or not answering. */
private sealed interface ServerReach {
    data object Checking : ServerReach
    data class Reachable(val version: String) : ServerReach
    data class Unreachable(val reason: String) : ServerReach
}

/** What the status panel shows, from calmest to most urgent. */
private enum class P2pStatusTone { Neutral, Busy, Ok, Error }

private class P2pStatusView(
    val tone: P2pStatusTone,
    val icon: ImageVector?,
    val title: String,
    val subtitle: String?,
    val transfer: P2pStatusTransfer? = null,
)

private class P2pStatusTransfer(
    val downloadBytesPerSecond: Long,
    val uploadBytesPerSecond: Long,
    val peers: Int,
    val seeds: Int,
)

/**
 * The P2P Streaming page's preview: instead of a picture of a screen, the state of the torrent
 * server, whether Provenio's own engine or an external server. A green badge while it is ready
 * or streaming, the error badge when it fails or cannot be reached, and the loading indicator
 * while it starts. An external server is checked when the page opens and again every so often.
 */
@Composable
internal fun P2pStatusPreview() {
    val settings = remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle().value
    val state = P2pStreams.state.collectAsStateWithLifecycle().value
    val downloads = P2pStreamingEngine.downloadStreamCount.collectAsStateWithLifecycle().value
    val server = settings.externalServer
    var reach by remember(server) { mutableStateOf<ServerReach>(ServerReach.Checking) }
    LaunchedEffect(server, settings.p2pEnabled) {
        if (server == null || !settings.p2pEnabled) return@LaunchedEffect
        while (true) {
            reach = try {
                ServerReach.Reachable(ExternalTorrentServer.probe(server))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ServerReach.Unreachable(error.message.orEmpty())
            }
            delay(if (reach is ServerReach.Reachable) RecheckReachableMillis else RecheckUnreachableMillis)
        }
    }

    val engineName = stringResource(Res.string.settings_p2p_status_builtin_engine)
    val where = server ?: engineName
    val view = when {
        !settings.p2pEnabled -> P2pStatusView(
            tone = P2pStatusTone.Neutral,
            icon = Icons.Rounded.CloudOff,
            title = stringResource(Res.string.settings_p2p_status_off),
            subtitle = stringResource(Res.string.settings_p2p_status_off_hint),
        )
        state is P2pStreamingState.Error -> P2pStatusView(
            tone = P2pStatusTone.Error,
            icon = Icons.Rounded.PriorityHigh,
            title = stringResource(Res.string.settings_p2p_status_stream_failed),
            subtitle = state.message.ifBlank { where },
        )
        state is P2pStreamingState.Streaming -> P2pStatusView(
            tone = P2pStatusTone.Ok,
            icon = Icons.Rounded.PlayArrow,
            title = stringResource(Res.string.settings_p2p_status_streaming),
            subtitle = where,
            transfer = P2pStatusTransfer(state.downloadSpeed, state.uploadSpeed, state.peers, state.seeds),
        )
        state is P2pStreamingState.Connecting -> P2pStatusView(
            tone = P2pStatusTone.Busy,
            icon = null,
            title = stringResource(Res.string.settings_p2p_status_starting),
            subtitle = where,
            transfer = P2pStatusTransfer(state.downloadSpeed, state.uploadSpeed, state.peers, state.seeds),
        )
        server != null -> when (val current = reach) {
            ServerReach.Checking -> P2pStatusView(
                tone = P2pStatusTone.Busy,
                icon = null,
                title = stringResource(Res.string.settings_p2p_status_checking),
                subtitle = server,
            )
            is ServerReach.Reachable -> P2pStatusView(
                tone = P2pStatusTone.Ok,
                icon = Icons.Rounded.Check,
                title = stringResource(Res.string.settings_p2p_status_server_ready),
                subtitle = if (current.version.isBlank()) {
                    server
                } else {
                    stringResource(Res.string.settings_p2p_status_server_version, server, current.version)
                },
            )
            is ServerReach.Unreachable -> P2pStatusView(
                tone = P2pStatusTone.Error,
                icon = Icons.Rounded.PriorityHigh,
                title = stringResource(Res.string.settings_p2p_status_server_unreachable),
                subtitle = server,
            )
        }
        downloads > 0 -> P2pStatusView(
            tone = P2pStatusTone.Ok,
            icon = Icons.Rounded.Download,
            title = stringResource(Res.string.settings_network_p2p_downloading, downloads),
            subtitle = engineName,
        )
        else -> P2pStatusView(
            tone = P2pStatusTone.Ok,
            icon = Icons.Rounded.Check,
            title = stringResource(Res.string.settings_p2p_status_engine_ready),
            subtitle = engineName,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(P2pStatusPreviewHeight)
            .clip(RoundedCornerShape(OuterCorner + 8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusBadge(tone = view.tone, icon = view.icon)
            Text(
                text = view.title,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            view.subtitle?.takeIf(String::isNotBlank)?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            view.transfer?.let { transfer ->
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransferFigure(Icons.Rounded.ArrowDownward, formatTransferRate(transfer.downloadBytesPerSecond))
                    TransferFigure(Icons.Rounded.ArrowUpward, formatTransferRate(transfer.uploadBytesPerSecond))
                    TransferFigure(
                        Icons.Rounded.Groups,
                        stringResource(Res.string.settings_network_p2p_peers_value, transfer.peers, transfer.seeds),
                    )
                }
            }
        }
    }
}

/**
 * The status in a Material expressive shape: a green cookie for ok, the red burst for an error,
 * the loading indicator while busy, and a plain circle while off.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusBadge(tone: P2pStatusTone, icon: ImageVector?) {
    val colors = MaterialTheme.colorScheme
    val success = MaterialTheme.provenio.colors.success
    val container by animateColorAsState(
        targetValue = when (tone) {
            P2pStatusTone.Neutral -> colors.surfaceContainerHighest
            P2pStatusTone.Busy -> colors.primaryContainer
            P2pStatusTone.Ok -> success.copy(alpha = 0.32f).compositeOver(colors.surfaceContainerHigh)
            P2pStatusTone.Error -> colors.errorContainer
        },
        label = "p2pStatusBadgeContainer",
    )
    val content = when (tone) {
        P2pStatusTone.Neutral -> colors.onSurfaceVariant
        P2pStatusTone.Busy -> colors.onPrimaryContainer
        P2pStatusTone.Ok -> success
        P2pStatusTone.Error -> colors.onErrorContainer
    }
    val shape: Shape = when (tone) {
        P2pStatusTone.Neutral, P2pStatusTone.Busy -> MaterialShapes.Circle.toShape()
        P2pStatusTone.Ok -> MaterialShapes.Cookie9Sided.toShape()
        P2pStatusTone.Error -> MaterialShapes.SoftBurst.toShape()
    }
    Box(
        modifier = Modifier
            .size(BadgeSize)
            .clip(shape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = tone to icon,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
            label = "p2pStatusBadgeIcon",
        ) { (shownTone, shownIcon) ->
            if (shownTone == P2pStatusTone.Busy || shownIcon == null) {
                LoadingSpinner(color = content, size = 64.dp)
            } else {
                Icon(
                    imageVector = shownIcon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = content,
                )
            }
        }
    }
}

@Composable
private fun TransferFigure(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private val P2pStatusPreviewHeight = 280.dp
private val BadgeSize = 112.dp
private const val RecheckReachableMillis = 15_000L
private const val RecheckUnreachableMillis = 5_000L
