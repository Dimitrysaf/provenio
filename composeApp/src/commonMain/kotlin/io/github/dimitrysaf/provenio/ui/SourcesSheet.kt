package io.github.dimitrysaf.provenio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.p2p.canRun
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.SourceKind
import io.github.dimitrysaf.provenio.stremio.SourceOption
import kotlinx.coroutines.launch

/**
 * Every stream every addon offers for one title.
 *
 * Addons are asked in parallel and results appear as each answers, because a single slow
 * provider should not hold up the ones that already replied. They are independent third
 * party servers with no shared budget, unlike Simkl.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSheet(
    type: String,
    id: String,
    title: String?,
    onDismiss: () -> Unit,
    onPlay: (SourceOption) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sources = remember { mutableStateListOf<SourceOption>() }
    var pending by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }

    val p2pSettings by P2pRepository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    // Held while the consent notice is up, so the chosen source can continue afterwards.
    var awaitingConsent by remember { mutableStateOf<SourceOption?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    fun playTorrent(source: SourceOption) {
        val infoHash = source.stream.infoHash ?: return
        resolving = true
        scope.launch {
            val url = P2pRepository.streamUrl(infoHash)
            resolving = false
            if (url != null) {
                onPlay(source.copy(resolvedUrl = url))
            } else {
                failure = "The peer-to-peer service could not open this source."
            }
        }
    }

    fun choose(source: SourceOption) {
        failure = null
        when {
            source.kind != SourceKind.Torrent -> onPlay(source)
            // Off, or consent never given. Ask, rather than showing a dead label.
            !p2pSettings.canRun -> awaitingConsent = source
            else -> playTorrent(source)
        }
    }

    LaunchedEffect(type, id) {
        sources.clear()
        val providers = AddonRepository.streamProviders(type, id)
        started = true
        pending = providers.size
        providers.forEach { addon ->
            launch {
                val found = AddonRepository.streamsFrom(addon, type, id)
                sources.addAll(found)
                pending -= 1
            }
        }
    }

    val pendingConsent = awaitingConsent
    if (pendingConsent != null) {
        P2pConsentDialog(
            onAccept = {
                awaitingConsent = null
                P2pRepository.acceptConsent()
                P2pRepository.setEnabled(true)
                playTorrent(pendingConsent)
            },
            onDismiss = { awaitingConsent = null },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = "Sources",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
            )
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(8.dp))

            // Kept visible while some addons are still answering, so a short list does not
            // look like the final answer.
            if (pending > 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val message = failure
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            if (resolving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                started && pending == 0 && sources.isEmpty() -> NoSources()
                sources.isEmpty() -> Loading()
                else -> LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(sources, key = { it.addon.manifest.id + it.label + it.hashCode() }) {
                        SourceRow(it, p2pSettings.canRun) { choose(it) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(source: SourceOption, p2pReady: Boolean, onClick: () -> Unit) {
    // A torrent is selectable whether or not peer-to-peer is on. Choosing one when it is
    // off opens the consent notice, which is the only way the user can turn it on from
    // here, so greying the row out would be a dead end.
    val playable = source.playableUrl != null || source.kind == SourceKind.Torrent
    val alpha = if (playable) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = playable, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (source.kind) {
                SourceKind.Direct -> Icons.Outlined.PlayCircle
                SourceKind.Torrent -> Icons.Outlined.Share
                SourceKind.YouTube -> Icons.Outlined.Link
                SourceKind.External -> Icons.Outlined.OpenInBrowser
                SourceKind.Unsupported -> Icons.Outlined.CloudOff
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = source.detail
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Provenance. Which addon produced this is the first thing worth knowing
                // once several are installed and disagree.
                AssistChip(onClick = {}, label = { Text(source.addon.manifest.name) })
                if (source.kind == SourceKind.Torrent && !p2pReady) {
                    AssistChip(onClick = {}, label = { Text("Turn on peer-to-peer") })
                } else if (!playable) {
                    AssistChip(onClick = {}, label = { Text(source.kind.label) })
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NoSources() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("No sources found", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "No installed add-on offered a stream for this title.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
