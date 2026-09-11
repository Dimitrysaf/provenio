package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.p2p.P2pConsentDialog
import io.github.dimitrysaf.provenio.core.platform.MatchHostSystemBars
import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.p2p.TorrentRequest
import io.github.dimitrysaf.provenio.p2p.canRun
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.SourceKind
import io.github.dimitrysaf.provenio.stremio.SourceOption
import io.github.dimitrysaf.provenio.stremio.isPlayingAt
import io.github.dimitrysaf.provenio.stremio.QualityTerms
import io.github.dimitrysaf.provenio.stremio.hasAny
import io.github.dimitrysaf.provenio.stremio.matches
import io.github.dimitrysaf.provenio.stremio.searchText
import kotlinx.coroutines.launch
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.search_clear
import io.github.dimitrysaf.provenio.resources.source_fallback_name
import io.github.dimitrysaf.provenio.resources.sources_no_addon_offers
import io.github.dimitrysaf.provenio.resources.sources_no_addon_offers_body
import io.github.dimitrysaf.provenio.resources.sources_none_found
import io.github.dimitrysaf.provenio.resources.sources_none_found_body
import io.github.dimitrysaf.provenio.resources.sources_none_from_addon
import io.github.dimitrysaf.provenio.resources.sources_nothing_matches
import io.github.dimitrysaf.provenio.resources.sources_nothing_matches_body
import io.github.dimitrysaf.provenio.resources.sources_p2p_failed
import io.github.dimitrysaf.provenio.resources.sources_quality_filter
import io.github.dimitrysaf.provenio.resources.sources_refresh
import io.github.dimitrysaf.provenio.resources.sources_search
import io.github.dimitrysaf.provenio.resources.sources_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

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
    /** What is playing right now, so the row for it can be marked. */
    currentUrl: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groups = remember { mutableStateListOf<AddonSources>() }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }

    val p2pSettings by P2pRepository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var awaitingConsent by remember { mutableStateOf<SourceOption?>(null) }
    var resolving by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    var reloads by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    // Terms the user ticked. Empty means no restriction.
    val picked = remember { mutableStateListOf<String>() }
    var filterOpen by remember { mutableStateOf(false) }

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
            val url = P2pRepository.streamUrl(
                TorrentRequest(
                    infoHash = infoHash,
                    sources = source.stream.sources,
                    fileIndex = source.stream.fileIdx,
                ),
            )
            resolving = false
            if (url != null) {
                onPlay(source.copy(resolvedUrl = url))
            } else {
                failure = getString(Res.string.sources_p2p_failed)
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

    fun visibleIn(group: AddonSources): List<SourceOption> = group.sources.filter { source ->
        source.matches(query) && source.hasAny(picked)
    }

    // Only terms some source actually mentions. Offering "2160p" when nothing is 4K would
    // be a filter that can only ever empty the list.
    val offered = QualityTerms.filter { term ->
        val needle = term.lowercase()
        groups.any { group -> group.sources.any { needle in it.searchText } }
    }
    val filtering = query.isNotBlank() || picked.isNotEmpty()
    val matches = groups.sumOf { visibleIn(it).size }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // The sheet has its own window; keep it in step with the one it opened over.
        MatchHostSystemBars()
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.sources_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
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
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(Res.string.sources_refresh),
                    )
                }
            }
            // One flat field above the list, so it applies to every section. No outline
            // and no filled container: it is a filter on what is already here, not a form.
            if (total > 0) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.sources_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription =
                                            stringResource(Res.string.search_clear),
                                    )
                                }
                            }
                            if (offered.isNotEmpty()) {
                                Box {
                                    IconButton(onClick = { filterOpen = true }) {
                                        Icon(
                                            imageVector = Icons.Outlined.FilterList,
                                            contentDescription =
                                                stringResource(Res.string.sources_quality_filter),
                                            tint = if (picked.isEmpty()) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = filterOpen,
                                        onDismissRequest = { filterOpen = false },
                                    ) {
                                        offered.forEach { term ->
                                            val on = term in picked
                                            DropdownMenuItem(
                                                text = { Text(term) },
                                                leadingIcon = {
                                                    Checkbox(checked = on, onCheckedChange = null)
                                                },
                                                onClick = {
                                                    if (on) picked.remove(term) else picked.add(term)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
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
            if (filtering && matches == 0) {
                Message(
                    title = stringResource(Res.string.sources_nothing_matches),
                    body = stringResource(Res.string.sources_nothing_matches_body),
                )
                return@Column
            }

            LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                groups.forEach { group ->
                    val addonId = group.addon.manifest.id
                    val shown = visibleIn(group)
                    // While filtering, a section with no matches is noise rather than
                    // information, so it drops out entirely.
                    if (filtering && shown.isEmpty()) return@forEach
                    val isCollapsed = collapsed[addonId] ?: false

                    item(key = "header:$addonId") {
                        AddonHeader(
                            group = group,
                            shownCount = shown.size,
                            collapsed = isCollapsed,
                            onToggle = { collapsed[addonId] = !isCollapsed },
                        )
                    }
                    item(key = "body:$addonId") {
                        AnimatedVisibility(visible = !isCollapsed) {
                            Column {
                                shown.forEach { source ->
                                    SourceRow(
                                        source = source,
                                        isPlaying = source.isPlayingAt(currentUrl),
                                    ) { choose(source) }
                                }
                                if (!group.loading && group.sources.isEmpty()) {
                                    Text(
                                        text = stringResource(Res.string.sources_none_from_addon),
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
        if (isPlaying) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun AddonHeader(
    group: AddonSources,
    shownCount: Int,
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
                    text = shownCount.toString(),
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
private fun SourceRow(source: SourceOption, isPlaying: Boolean, onClick: () -> Unit) {
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
        // No leading icon for the ordinary case. An addon returns one kind of source, so
        // every row in a section carried the same glyph, which distinguishes nothing and
        // only narrows the text. The one that is playing is the exception worth marking.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.label ?: stringResource(Res.string.source_fallback_name),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                },
                fontWeight = if (isPlaying) FontWeight.Bold else null,
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
        title = stringResource(Res.string.sources_no_addon_offers),
        body = stringResource(Res.string.sources_no_addon_offers_body),
    )
}

@Composable
private fun NoSources() {
    Message(
        title = stringResource(Res.string.sources_none_found),
        body = stringResource(Res.string.sources_none_found_body),
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
