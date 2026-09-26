package io.github.dimitrysaf.provenio.shell.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.core.i18n.localizedByteUnit
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.screens.p2p.TorrentDetailsSheet
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.downloads.DownloadItem
import io.github.dimitrysaf.provenio.core.downloads.DownloadStatus
import io.github.dimitrysaf.provenio.core.downloads.DownloadsLocationPlatform
import io.github.dimitrysaf.provenio.core.downloads.DownloadsPlatformDownloader
import io.github.dimitrysaf.provenio.core.downloads.DownloadsRepository
import io.github.dimitrysaf.provenio.core.downloads.DownloadsUiState
import io.github.dimitrysaf.provenio.core.downloads.sortedForSeriesDownloads

@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onOpenDownload: (DownloadItem) -> Unit,
    initialShowId: String? = null,
    onNavigateToShow: ((showId: String, title: String) -> Unit)? = null,
    onBackFromShow: (() -> Unit)? = null,
) {
    val uiState by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()

    var selectedShowId by rememberSaveable(initialShowId) { mutableStateOf(initialShowId) }
    var downloadPendingDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    val openDownloadsDirectoryFailedText = stringResource(Res.string.downloads_open_directory_failed)

    val completedEpisodes = remember(uiState.items) {
        uiState.completedItems
            .filter { it.isEpisode }
            .sortedForSeriesDownloads()
    }

    val selectedShowTitle = remember(selectedShowId, completedEpisodes) {
        selectedShowId?.let { showId ->
            completedEpisodes.firstOrNull { it.parentMetaId == showId }?.title
        }
    }

    PlatformBackHandler(enabled = selectedShowId != null && onBackFromShow == null) { selectedShowId = null }

    ScreenScaffold(
        title = if (selectedShowId == null) {
            stringResource(Res.string.compose_settings_root_downloads_title)
        } else {
            selectedShowTitle ?: stringResource(Res.string.downloads_show_downloads)
        },
        onBack = {
            if (selectedShowId != null) {
                onBackFromShow?.invoke() ?: run { selectedShowId = null }
            } else {
                onBack()
            }
        },
        actions = {
            IconButton(
                onClick = {
                    if (!DownloadsPlatformDownloader.openDownloadsDirectory()) {
                        ToastController.show(openDownloadsDirectoryFailedText)
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = stringResource(Res.string.downloads_open_directory),
                )
            }
        },
    ) {

        if (selectedShowId == null) {
            downloadsRootContent(
                uiState = uiState,
                onOpenDownload = onOpenDownload,
                onOpenShow = { showId, title ->
                    onNavigateToShow?.invoke(showId, title) ?: run { selectedShowId = showId }
                },
                onDeleteDownload = { downloadPendingDeletionId = it },
            )
        } else {
            downloadsShowContent(
                showId = selectedShowId.orEmpty(),
                episodes = completedEpisodes,
                onOpenDownload = onOpenDownload,
                onDeleteDownload = { downloadPendingDeletionId = it },
            )
        }
    }

    val pendingDeletionId = downloadPendingDeletionId
    if (pendingDeletionId != null) {
        StatusModal(
            title = stringResource(Res.string.action_delete_confirm_title),
            message = stringResource(Res.string.action_delete_confirm_message),
            isVisible = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                DownloadsRepository.cancelDownload(pendingDeletionId)
                downloadPendingDeletionId = null
            },
            onDismiss = { downloadPendingDeletionId = null },
        )
    }
}

private fun LazyListScope.downloadsRootContent(
    uiState: DownloadsUiState,
    onOpenDownload: (DownloadItem) -> Unit,
    onOpenShow: (showId: String, title: String) -> Unit,
    onDeleteDownload: (String) -> Unit,
) {
    val activeItems = uiState.activeItems
    val completedMovies = uiState.completedItems.filterNot(DownloadItem::isEpisode)
    val completedShows = uiState.completedItems
        .filter(DownloadItem::isEpisode)
        .groupBy { it.parentMetaId }
        .mapNotNull { (_, episodes) ->
            episodes.firstOrNull()?.let { first ->
                first to episodes
            }
        }
        .sortedBy { (item, _) -> item.title.lowercase() }

    if (DownloadsLocationPlatform.isConfigurable) {
        item { DownloadLocationRow() }
    }

    if (activeItems.isNotEmpty()) {
        item { DownloadsSubheader(stringResource(Res.string.downloads_section_active), activeItems.size) }
        item { DownloadGroup(activeItems, onOpenDownload, onDeleteDownload) }
    }

    if (completedMovies.isNotEmpty()) {
        item { DownloadsSubheader(stringResource(Res.string.downloads_section_movies), completedMovies.size) }
        item { DownloadGroup(completedMovies, onOpenDownload, onDeleteDownload) }
    }

    if (completedShows.isNotEmpty()) {
        item { DownloadsSubheader(stringResource(Res.string.downloads_section_shows), completedShows.size) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
            ) {
                completedShows.forEachIndexed { index, (item, episodes) ->
                    DownloadShowRow(
                        title = item.title,
                        episodeCount = episodes.size,
                        shape = segmentShape(index = index, count = completedShows.size),
                        onOpen = { onOpenShow(item.parentMetaId, item.title) },
                    )
                }
            }
        }
    }

    if (uiState.items.isEmpty()) {
        item {
            EmptyState(
                icon = Icons.Rounded.Download,
                title = stringResource(Res.string.downloads_empty_title),
                message = stringResource(Res.string.downloads_empty_subtitle),
            )
        }
    }
}

private fun LazyListScope.downloadsShowContent(
    showId: String,
    episodes: List<DownloadItem>,
    onOpenDownload: (DownloadItem) -> Unit,
    onDeleteDownload: (String) -> Unit,
) {
    val showEpisodes = episodes
        .filter { it.parentMetaId == showId }
        .sortedForSeriesDownloads()

    val seasons = showEpisodes
        .groupBy { it.seasonNumber ?: 0 }
        .toList()
        .sortedWith(
            compareBy<Pair<Int, List<DownloadItem>>> { (season, _) ->
                if (season == 0) 0 else 1
            }.thenBy { (season, _) -> if (season == 0) 0 else season },
        )

    if (seasons.isEmpty()) {
        item {
            EmptyState(
                icon = Icons.Rounded.Download,
                title = stringResource(Res.string.downloads_empty_episodes),
                message = stringResource(Res.string.downloads_empty_episodes_subtitle),
            )
        }
        return
    }

    seasons.forEach { (seasonNumber, entries) ->
        val sortedEpisodes = entries.sortedForSeriesDownloads()

        item {
            DownloadsSubheader(
                text = if (seasonNumber == 0) {
                    stringResource(Res.string.episodes_specials)
                } else {
                    stringResource(Res.string.episodes_season, seasonNumber)
                },
                count = sortedEpisodes.size,
            )
        }

        item { DownloadGroup(sortedEpisodes, onOpenDownload, onDeleteDownload) }
    }
}

/** One section's downloads, as a single segmented group. */
@Composable
private fun DownloadGroup(
    items: List<DownloadItem>,
    onOpenDownload: (DownloadItem) -> Unit,
    onDeleteDownload: (String) -> Unit,
) {
    // fillMaxWidth as SettingsList does, or the group measures to its widest row and sits inset
    // from both edges. The screen margin is ScreenScaffold's to apply, so the group adds none.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        items.forEachIndexed { index, item ->
            DownloadRow(
                item = item,
                shape = segmentShape(index = index, count = items.size),
                onOpen = { onOpenDownload(item) },
                onPause = { DownloadsRepository.pauseDownload(item.id) },
                onResume = { DownloadsRepository.resumeDownload(item.id) },
                onRetry = { DownloadsRepository.retryDownload(item.id) },
                onDelete = { onDeleteDownload(item.id) },
            )
        }
    }
}

/** A section's name, and how many downloads are in it. */
@Composable
private fun DownloadsSubheader(text: String, count: Int) {
    ListSubheader(text = "$text ($count)")
}

/**
 * One download, as a row of its section's list.
 *
 * The artwork leads the row so a download is recognised the way it is everywhere else, and what
 * the download is doing is stated above its title rather than buried under it. Whichever action
 * the state calls for stays on the row; deleting is behind the trailing menu, where an action you
 * cannot undo is harder to hit by accident. A failure reports itself when the row is tapped,
 * since an error long enough to matter is too long to sit in a list.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadRow(
    item: DownloadItem,
    shape: RoundedCornerShape,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var showTorrentDetails by remember { mutableStateOf(false) }
    val displayTitle = item.displayTitle()
    val displaySubtitle = downloadDisplaySubtitle(item = item, displayTitle = displayTitle)
    val failureMessage = item.errorMessage?.takeIf { it.isNotBlank() }

    SegmentedListItem(
        onClick = {
            when {
                failureMessage != null -> showError = true
                item.isPlayable -> onOpen()
            }
        },
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = { DownloadThumbnail(item) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(displaySubtitle)
                Text(downloadProgressText(item))
                if (item.status == DownloadStatus.Downloading) {
                    if (item.totalBytes != null && item.totalBytes > 0L) {
                        LinearProgressIndicator(
                            progress = { item.progressFraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        // One trailing control, as the settings rows have. A second button cost the row nearly
        // fifty more points of width, which the title and its progress line were paying for.
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(Res.string.downloads_row_actions),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    when (item.status) {
                        DownloadStatus.Downloading -> DownloadMenuItem(
                            label = stringResource(Res.string.compose_action_pause),
                            icon = Icons.Rounded.Pause,
                        ) {
                            menuOpen = false
                            onPause()
                        }
                        DownloadStatus.Paused -> DownloadMenuItem(
                            label = stringResource(Res.string.action_resume),
                            icon = Icons.Rounded.PlayArrow,
                        ) {
                            menuOpen = false
                            onResume()
                        }
                        DownloadStatus.Failed -> DownloadMenuItem(
                            label = stringResource(Res.string.action_retry),
                            icon = Icons.Rounded.Refresh,
                        ) {
                            menuOpen = false
                            onRetry()
                        }
                        DownloadStatus.Completed -> if (item.isPlayable) {
                            DownloadMenuItem(
                                label = stringResource(Res.string.action_play),
                                icon = Icons.Rounded.PlayArrow,
                            ) {
                                menuOpen = false
                                onOpen()
                            }
                        }
                    }
                    // Only a running torrent download holds the engine, so only it has live details.
                    if (item.torrentInfoHash != null && item.status == DownloadStatus.Downloading) {
                        DownloadMenuItem(
                            label = stringResource(Res.string.torrent_details_title),
                            icon = Icons.Rounded.Info,
                        ) {
                            menuOpen = false
                            showTorrentDetails = true
                        }
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.action_delete)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        },
    ) {
        Text(displayTitle)
    }

    if (showTorrentDetails) {
        TorrentDetailsSheet(
            infoHash = item.torrentInfoHash,
            onDismiss = { showTorrentDetails = false },
        )
    }

    if (showError && failureMessage != null) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text(stringResource(Res.string.downloads_status_failed)) },
            text = { Text(failureMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showError = false
                    onRetry()
                }) {
                    Text(stringResource(Res.string.action_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { showError = false }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        )
    }
}

/** One action in a download's menu. */
@Composable
private fun DownloadMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

/** The download's own artwork, at the size a list row's leading slot allows. */
@Composable
private fun DownloadThumbnail(item: DownloadItem) {
    val artwork = item.episodeThumbnail?.takeIf { it.isNotBlank() }
        ?: item.poster?.takeIf { it.isNotBlank() }
        ?: item.background?.takeIf { it.isNotBlank() }
    // Episode artwork is a still and a movie's is a poster, so each keeps its own proportions.
    val isStill = item.episodeThumbnail?.isNotBlank() == true
    val width = if (isStill) 64.dp else 44.dp

    Box(
        modifier = Modifier
            .size(width = width, height = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Every download states what it is doing over its own artwork, so the icon is where the
        // state is read, in every state. The scrim keeps it legible on a bright still.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.status.icon(),
                contentDescription = null,
                tint = if (item.status == DownloadStatus.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.White
                },
            )
        }
    }
}

/** A downloaded show, which opens its own list of episodes. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadShowRow(
    title: String,
    episodeCount: Int,
    shape: RoundedCornerShape,
    onOpen: () -> Unit,
) {
    SegmentedListItem(
        onClick = onOpen,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            Icon(imageVector = Icons.Rounded.Folder, contentDescription = null)
        },
        supportingContent = {
            Text(stringResource(Res.string.downloads_episode_count, episodeCount))
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    ) {
        Text(title)
    }
}

// The folder new downloads are saved to, which opens the system folder picker to change.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadLocationRow() {
    var path by remember { mutableStateOf(DownloadsLocationPlatform.currentPath().orEmpty()) }
    val shape = segmentShape(index = 0, count = 1)
    val change = {
        if (DownloadsLocationPlatform.choose()) path = DownloadsLocationPlatform.currentPath().orEmpty()
    }
    SegmentedListItem(
        onClick = change,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            Icon(imageVector = Icons.Rounded.Folder, contentDescription = null)
        },
        supportingContent = { Text(path) },
        trailingContent = {
            TextButton(onClick = change) {
                Text(stringResource(Res.string.downloads_location_change))
            }
        },
    ) {
        Text(stringResource(Res.string.downloads_location_title))
    }
}

/** What each download state looks like at a glance. */
private fun DownloadStatus.icon(): ImageVector = when (this) {
    DownloadStatus.Downloading -> Icons.Rounded.Download
    DownloadStatus.Paused -> Icons.Rounded.Pause
    DownloadStatus.Completed -> Icons.Rounded.CheckCircle
    DownloadStatus.Failed -> Icons.Rounded.ErrorOutline
}

private fun DownloadItem.displayTitle(): String =
    if (isEpisode) {
        episodeTitle?.trim()?.takeIf { it.isNotBlank() } ?: title
    } else {
        title
    }

@Composable
private fun downloadDisplaySubtitle(
    item: DownloadItem,
    displayTitle: String,
): String {
    val seasonNumber = item.seasonNumber
    val episodeNumber = item.episodeNumber
    if (seasonNumber == null || episodeNumber == null) {
        return item.displaySubtitle
    }

    val episodeCode = stringResource(
        Res.string.compose_player_episode_code_full,
        seasonNumber,
        episodeNumber,
    )
    return listOf(
        episodeCode,
        item.episodeTitle?.trim().orEmpty().takeIf { it.isNotBlank() && it != displayTitle },
        item.title.trim().takeIf { it.isNotBlank() && it != displayTitle },
    ).filterNotNull().joinToString(" • ")
}

/**
 * How far along the download is, and nothing else.
 *
 * The state is already on the thumbnail and a failure has its own dialog, so naming either here
 * would only repeat them and push the reason for the failure into a row that cannot hold it.
 */
@Composable
private fun downloadProgressText(item: DownloadItem): String {
    val total = item.totalBytes?.takeIf { it > 0L }
    return when {
        item.status == DownloadStatus.Completed -> formatBytes(total ?: item.downloadedBytes)
        total != null -> stringResource(
            Res.string.downloads_progress_format,
            (item.progressFraction * 100f).toInt().coerceIn(0, 100),
            formatBytes(item.downloadedBytes),
            formatBytes(total),
        )
        else -> formatBytes(item.downloadedBytes)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 ${localizedByteUnit("B")}"
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    val value = bytes.toDouble()
    return when {
        value >= gib -> "${((value / gib) * 10.0).toInt() / 10.0} ${localizedByteUnit("GB")}"
        value >= mib -> "${((value / mib) * 10.0).toInt() / 10.0} ${localizedByteUnit("MB")}"
        value >= kib -> "${((value / kib) * 10.0).toInt() / 10.0} ${localizedByteUnit("KB")}"
        else -> "$bytes ${localizedByteUnit("B")}"
    }
}
