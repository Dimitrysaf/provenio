package io.github.dimitrysaf.provenio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.SourceKind
import io.github.dimitrysaf.provenio.stremio.SourceOption
import kotlinx.coroutines.launch

/** One addon's contribution, and whether it has finished contributing. */
private data class AddonSources(
    val addon: InstalledAddon,
    val loading: Boolean,
    val sources: List<SourceOption>,
)

/**
 * Every stream every addon offers for one title, grouped by the addon that produced it.
 *
 * Grouping carries provenance structurally instead of repeating an addon name on every
 * row, and it means a slow or empty provider is visibly its own section rather than an
 * absence you have to infer.
 *
 * Addons are asked in parallel and each section fills as its addon answers. They are
 * independent third party servers with no shared budget, unlike Simkl.
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
    // Refusing the Hidden value stops a downward drag from dismissing the sheet, which
    // otherwise competes with scrolling the list inside it. The scrim and the back gesture
    // still call onDismissRequest, so there is always a way out.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    val groups = remember { mutableStateListOf<AddonSources>() }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }

    val p2pSettings by P2pRepository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var awaitingConsent by remember { mutableStateOf<SourceOption?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    var reloads by remember { mutableIntStateOf(0) }

    LaunchedEffect(type, id, reloads) {
        groups.clear()
        val providers = AddonRepository.streamProviders(type, id)

        val cached = AddonRepository.cachedSources(type, id)
        if (cached != null) {
            providers.forEach { addon ->
                groups.add(
                    AddonSources(
                        addon = addon,
                        loading = false,
                        sources = cached.filter { it.addon.manifest.id == addon.manifest.id },
                    ),
                )
            }
            return@LaunchedEffect
        }

        providers.forEach { groups.add(AddonSources(it, loading = true, sources = emptyList())) }
        providers.forEachIndexed { index, addon ->
            launch {
                val found = AddonRepository.streamsFrom(addon, type, id)
                groups[index] = AddonSources(addon, loading = false, sources = found)
                // Cached once every addon has answered, so a partial set is never stored.
                if (groups.none { it.loading }) {
                    AddonRepository.cacheSources(type, id, groups.flatMap { it.sources })
                }
            }
        }
    }

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
            // Off, or consent never given. Ask, rather than refusing the press.
            !p2pSettings.canRun -> awaitingConsent = source
            else -> playTorrent(source)
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

    val stillLoading = groups.filter { it.loading }
    val total = groups.sumOf { it.sources.size }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sources", style = MaterialTheme.typography.titleLarge)
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    enabled = stillLoading.isEmpty(),
                    onClick = {
                        AddonRepository.forgetSources(type, id)
                        failure = null
                        reloads += 1
                    },
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh sources")
                }
            }
            Spacer(Modifier.height(8.dp))

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

            if (groups.isEmpty()) {
                NoProviders()
                return@Column
            }
            if (stillLoading.isEmpty() && total == 0) {
                NoSources()
                return@Column
            }

            LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                groups.forEach { group ->
                    val addonId = group.addon.manifest.id
                    val isCollapsed = collapsed[addonId] ?: false

                    item(key = "header:$addonId") {
                        AddonHeader(
                            group = group,
                            collapsed = isCollapsed,
                            onToggle = { collapsed[addonId] = !isCollapsed },
                        )
                    }
                    item(key = "body:$addonId") {
                        AnimatedVisibility(visible = !isCollapsed) {
                            Column {
                                group.sources.forEach { source ->
                                    SourceRow(source) { choose(source) }
                                }
                                if (!group.loading && group.sources.isEmpty()) {
                                    Text(
                                        text = "No sources from this add-on.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            start = 24.dp,
                                            end = 24.dp,
                                            bottom = 12.dp,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddonHeader(
    group: AddonSources,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    Column {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.addon.manifest.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (group.loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text(
                    text = group.sources.size.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (collapsed) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.Filled.KeyboardArrowUp
                    },
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun SourceRow(source: SourceOption, onClick: () -> Unit) {
    // A torrent is selectable whether or not peer-to-peer is on, because pressing it is
    // how the user is offered the switch.
    val playable = source.playableUrl != null || source.kind == SourceKind.Torrent
    val alpha = if (playable) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = playable, onClick = onClick)
            .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // No leading icon. An addon returns one kind of source, so every row in a section
        // carried the same glyph, which distinguishes nothing and only narrows the text.
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
        }
    }
}

@Composable
private fun NoProviders() {
    Message(
        title = "No add-on offers sources",
        body = "None of your installed add-ons serve streams for this kind of title.",
    )
}

@Composable
private fun NoSources() {
    Message(
        title = "No sources found",
        body = "Your add-ons answered, but none had a stream for this title.",
    )
}

@Composable
private fun Message(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
