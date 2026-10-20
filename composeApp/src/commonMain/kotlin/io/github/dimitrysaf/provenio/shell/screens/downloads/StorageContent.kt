package io.github.dimitrysaf.provenio.shell.screens.downloads

import androidx.compose.foundation.Canvas
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import io.github.dimitrysaf.provenio.core.downloads.DownloadItem
import io.github.dimitrysaf.provenio.core.downloads.DownloadStatus
import io.github.dimitrysaf.provenio.core.downloads.DownloadsLocationPlatform
import io.github.dimitrysaf.provenio.core.downloads.DownloadsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pCacheClearResult
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingState
import io.github.dimitrysaf.provenio.core.p2p.P2pStreams
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.search.SearchHistoryRepository
import io.github.dimitrysaf.provenio.core.storage.CachedTorrent
import io.github.dimitrysaf.provenio.core.storage.StorageCategory
import io.github.dimitrysaf.provenio.core.storage.StorageFolder
import io.github.dimitrysaf.provenio.core.storage.StoragePlatform
import io.github.dimitrysaf.provenio.core.storage.StorageSpace
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingEnrichmentCache
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.SkeletonBlock
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** What the Storage page measured: the bytes in each category, the disk's space and the torrent cache's contents. */
@Immutable
internal data class StorageUsage(
    val bytes: Map<StorageCategory, Long>,
    val space: StorageSpace?,
    val torrents: List<CachedTorrent>,
) {
    val appBytes: Long
        get() = bytes.values.sum()

    operator fun get(category: StorageCategory): Long = bytes[category] ?: 0L
}

/** The bytes downloads take: what has arrived of each, or the whole file once it is done. */
internal fun List<DownloadItem>.storageBytes(): Long = sumOf { item ->
    if (item.status == DownloadStatus.Completed) item.totalBytes ?: item.downloadedBytes else item.downloadedBytes
}

/**
 * Measures storage again whenever [refreshKey] or the downloads change. Finished downloads are
 * measured by their files on disk, so videos found in the folder count even without a known size.
 */
@Composable
internal fun rememberStorageUsage(downloads: List<DownloadItem>, refreshKey: Int): StorageUsage? {
    val imageLoader = SingletonImageLoader.get(LocalPlatformContext.current)
    var usage by remember { mutableStateOf<StorageUsage?>(null) }
    LaunchedEffect(refreshKey, downloads) {
        val measured = StoragePlatform.measure()
        val finished = downloads.filter { it.status == DownloadStatus.Completed }
        val finishedBytes = StoragePlatform.filesSize(finished.mapNotNull { it.localFileUri })
            .takeIf { it > 0L }
            ?: finished.storageBytes()
        val downloadsBytes = finishedBytes + downloads.filterNot { it.status == DownloadStatus.Completed }.storageBytes()
        usage = StorageUsage(
            bytes = measured +
                (StorageCategory.Downloads to downloadsBytes) +
                (StorageCategory.Images to (imageLoader.diskCache?.size ?: 0L)),
            space = StoragePlatform.space(),
            torrents = StoragePlatform.cachedTorrents(),
        )
    }
    return usage
}

/**
 * The page's headline: how much Provenio takes, a tall bar of only that split by what takes it,
 * and a legend naming each part with its size.
 */
@Composable
internal fun StorageUsageCard(usage: StorageUsage?) {
    val categories = StorageCategory.entries.filter { (usage?.get(it) ?: 0L) > 0L }
    val segments = buildList {
        categories.forEach { add(it.color() to (usage?.get(it) ?: 0L)) }
    }
    val labels = categories.associateWith { it.label() }
    val barDescription = categories.joinToString { "${labels.getValue(it)} ${formatBytes(usage?.get(it) ?: 0L)}" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OuterCorner))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .animateContentSize()
            .padding(20.dp),
    ) {
        if (usage == null) {
            StorageUsageSkeleton()
            return@Column
        }
        Text(
            text = formatBytes(usage.appBytes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.storage_used_by_app),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        StorageBar(
            segments = segments,
            totalBytes = usage?.appBytes ?: 0L,
            modifier = Modifier.semantics { contentDescription = barDescription },
        )
        if (categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.forEach { category ->
                    StorageLegendRow(color = category.color(), label = labels.getValue(category), bytes = usage?.get(category) ?: 0L)
                }
            }
        }
    }
}

/**
 * The usage card while storage is first measured: the shapes of the total, the bar and a few
 * legend rows, so the card keeps its place and size instead of showing a placeholder number.
 */
@Composable
private fun StorageUsageSkeleton() {
    Column {
        SkeletonBlock(width = 120.dp, height = 32.dp, cornerRadius = 8.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBlock(width = 96.dp, height = 14.dp)
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth(),
            height = StorageBarHeight,
            cornerRadius = StorageBarHeight / 2,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(88.dp, 120.dp, 72.dp).forEach { labelWidth ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkeletonBlock(width = 12.dp, height = 12.dp, cornerRadius = 6.dp)
                    SkeletonBlock(width = labelWidth, height = 14.dp)
                    Spacer(modifier = Modifier.weight(1f))
                    SkeletonBlock(width = 56.dp, height = 14.dp)
                }
            }
        }
    }
}

/**
 * How much space the device has left, as one list item under the usage card; while storage is
 * first measured, the shape of that item.
 */
@Composable
internal fun StorageFreeSpaceRow(space: StorageSpace?, loading: Boolean = false) {
    if (loading) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 56.dp, cornerRadius = OuterCorner)
        return
    }
    if (space == null) return
    SettingsList {
        infoRow(
            title = stringResource(Res.string.storage_free_on_device),
            value = stringResource(
                Res.string.storage_space_free_of,
                formatBytes(space.freeBytes),
                formatBytes(space.totalBytes),
            ),
        )
    }
}

/**
 * One tall rounded bar split into each part's share of [totalBytes] in its colour, with a 2dp
 * gap of the card's colour between parts. Before anything is measured it is the empty track.
 */
@Composable
private fun StorageBar(
    segments: List<Pair<Color, Long>>,
    totalBytes: Long,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val gap = MaterialTheme.colorScheme.surfaceContainer
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(StorageBarHeight)
            .clip(RoundedCornerShape(StorageBarHeight / 2)),
    ) {
        drawRect(color = track)
        if (totalBytes <= 0L) return@Canvas
        val gapPx = StorageBarGap.toPx()
        val minWidthPx = StorageBarMinSegment.toPx()
        var x = 0f
        segments.forEach { (color, bytes) ->
            if (x >= size.width) return@forEach
            val width = maxOf(size.width * (bytes.toFloat() / totalBytes), minWidthPx)
                .coerceAtMost(size.width - x)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(StorageBarSegmentCorner.toPx()),
            )
            x += width
            drawRect(color = gap, topLeft = Offset(x, 0f), size = Size(gapPx, size.height))
            x += gapPx
        }
    }
}

@Composable
private fun StorageLegendRow(color: Color, label: String, bytes: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatBytes(bytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The torrents the torrent cache holds, with the space each takes. */
@Composable
internal fun CachedTorrentsGroup(torrents: List<CachedTorrent>) {
    if (torrents.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListSubheader(text = stringResource(Res.string.storage_section_torrent_cache, torrents.size))
        SettingsList {
            torrents.forEach { torrent ->
                infoRow(title = torrent.name, value = formatBytes(torrent.bytes))
            }
        }
    }
}

/** Everything that frees space or forgets something, each with what it would free. */
@Composable
internal fun StorageCleanupGroup(
    usage: StorageUsage?,
    downloads: List<DownloadItem>,
    onCleared: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val imageLoader = SingletonImageLoader.get(LocalPlatformContext.current)
    val streamingState = P2pStreams.state.collectAsStateWithLifecycle().value
    val cacheState = P2pStreamingEngine.cacheState.collectAsStateWithLifecycle().value
    val downloadStreams = P2pStreamingEngine.downloadStreamCount.collectAsStateWithLifecycle().value
    var torrentClearResult by remember { mutableStateOf<P2pCacheClearResult?>(null) }
    var torrentClearFailed by remember { mutableStateOf(false) }
    var searchCount by remember { mutableIntStateOf(SearchHistoryRepository.storedCount()) }
    var continueWatchingCleared by remember { mutableStateOf(false) }
    var confirmDeleteDownloads by remember { mutableStateOf(false) }
    var confirmClearSearches by remember { mutableStateOf(false) }
    val torrentClearAvailable = streamingState !is P2pStreamingState.Connecting &&
        streamingState !is P2pStreamingState.Streaming &&
        downloadStreams == 0 &&
        !cacheState.isClearing
    fun clear(category: StorageCategory, action: suspend () -> Unit) {
        val freed = usage?.get(category) ?: 0L
        scope.launch {
            runCatching { action() }
            ToastController.show(getString(Res.string.storage_cleared, formatBytes(freed)))
            onCleared()
        }
    }
    val torrentBytes = usage?.get(StorageCategory.TorrentCache) ?: 0L

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListSubheader(text = stringResource(Res.string.storage_section_clean_up))
        SettingsList {
            navigationRow(
                title = stringResource(Res.string.settings_p2p_clear_cache_title),
                description = when {
                    cacheState.isClearing -> stringResource(Res.string.settings_p2p_clear_cache_clearing)
                    !torrentClearAvailable -> stringResource(Res.string.settings_p2p_clear_cache_playback_active)
                    torrentClearFailed -> stringResource(Res.string.settings_p2p_clear_cache_failed)
                    torrentClearResult != null -> stringResource(
                        Res.string.settings_p2p_clear_cache_done,
                        formatBytes(torrentClearResult!!.reclaimedBytes),
                    )
                    else -> stringResource(Res.string.settings_p2p_clear_cache_usage, formatBytes(torrentBytes))
                },
                enabled = torrentClearAvailable,
                onClick = {
                    torrentClearResult = null
                    torrentClearFailed = false
                    scope.launch {
                        runCatching { P2pStreamingEngine.clearCache() }
                            .onSuccess { torrentClearResult = it }
                            .onFailure { torrentClearFailed = true }
                        runCatching { StoragePlatform.clearStaleTorrentCaches() }
                        onCleared()
                    }
                },
            )
            navigationRow(
                title = stringResource(Res.string.storage_clear_images),
                description = stringResource(
                    Res.string.storage_clear_images_description,
                    formatBytes(usage?.get(StorageCategory.Images) ?: 0L),
                ),
                onClick = {
                    clear(StorageCategory.Images) {
                        imageLoader.memoryCache?.clear()
                        imageLoader.diskCache?.clear()
                    }
                },
            )
            if (usage?.bytes?.containsKey(StorageCategory.AddonCache) == true) {
                navigationRow(
                    title = stringResource(Res.string.storage_clear_addon_cache),
                    description = stringResource(
                        Res.string.storage_clear_addon_cache_description,
                        formatBytes(usage?.get(StorageCategory.AddonCache) ?: 0L),
                    ),
                    onClick = { clear(StorageCategory.AddonCache) { StoragePlatform.clearAddonCache() } },
                )
                navigationRow(
                    title = stringResource(Res.string.storage_clear_other),
                    description = stringResource(
                        Res.string.storage_clear_other_description,
                        formatBytes(usage?.get(StorageCategory.Other) ?: 0L),
                    ),
                    onClick = { clear(StorageCategory.Other) { StoragePlatform.clearOther() } },
                )
            }
            navigationRow(
                title = stringResource(Res.string.storage_clear_search_history),
                description = if (searchCount > 0) {
                    stringResource(Res.string.storage_clear_search_history_description, searchCount)
                } else {
                    stringResource(Res.string.storage_clear_search_history_empty)
                },
                enabled = searchCount > 0,
                onClick = { confirmClearSearches = true },
            )
            navigationRow(
                title = stringResource(Res.string.settings_advanced_clear_cw_cache),
                description = if (continueWatchingCleared) {
                    stringResource(Res.string.settings_advanced_clear_cw_cache_done)
                } else {
                    stringResource(Res.string.settings_advanced_clear_cw_cache_subtitle)
                },
                enabled = !continueWatchingCleared,
                onClick = {
                    val profileId = ProfileRepository.activeProfileId
                    ContinueWatchingEnrichmentCache.clearAll(profileId)
                    continueWatchingCleared = true
                    scope.launch { WatchProgressRepository.clearLocalAndForceSnapshotRefreshFromServer(profileId) }
                },
            )
            navigationRow(
                title = stringResource(Res.string.storage_delete_all_downloads),
                description = if (downloads.isNotEmpty()) {
                    stringResource(
                        Res.string.storage_delete_all_downloads_description,
                        formatBytes(usage?.get(StorageCategory.Downloads) ?: downloads.storageBytes()),
                        downloads.size,
                    )
                } else {
                    stringResource(Res.string.downloads_empty_title)
                },
                enabled = downloads.isNotEmpty(),
                onClick = { confirmDeleteDownloads = true },
            )
        }
    }

    if (confirmClearSearches) {
        StatusModal(
            title = stringResource(Res.string.storage_clear_search_history_confirm_title),
            message = stringResource(Res.string.storage_clear_search_history_confirm_message),
            isVisible = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                SearchHistoryRepository.clearAll()
                searchCount = 0
                confirmClearSearches = false
            },
            onDismiss = { confirmClearSearches = false },
        )
    }

    if (confirmDeleteDownloads) {
        StatusModal(
            title = stringResource(Res.string.storage_delete_all_downloads_confirm_title),
            message = stringResource(Res.string.storage_delete_all_downloads_confirm_message),
            isVisible = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                downloads.forEach { DownloadsRepository.cancelDownload(it.id) }
                confirmDeleteDownloads = false
                onCleared()
            },
            onDismiss = { confirmDeleteDownloads = false },
        )
    }
}

/**
 * Where things are kept: the downloads folder, the cache and app data folders where they can be
 * moved, and the storage volume where there is more than one.
 */
@Composable
internal fun StorageLocationsGroup(onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    val volumes = remember { StoragePlatform.volumes() }
    var selectedVolumeId by remember { mutableStateOf(StoragePlatform.selectedVolumeId()) }
    var downloadsPath by remember { mutableStateOf(DownloadsLocationPlatform.currentPath().orEmpty()) }
    var folderPaths by remember {
        mutableStateOf(StoragePlatform.movableFolders.associateWith { StoragePlatform.folderPath(it).orEmpty() })
    }
    var showVolumeSheet by remember { mutableStateOf(false) }
    var restartPrompt by remember { mutableStateOf(false) }
    val showDownloads = DownloadsLocationPlatform.isConfigurable
    if (!showDownloads && volumes.isEmpty() && folderPaths.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListSubheader(text = stringResource(Res.string.storage_section_locations))
        SettingsList {
            if (volumes.isNotEmpty()) {
                val selected = volumes.firstOrNull { it.id == selectedVolumeId } ?: volumes.first()
                navigationRow(
                    title = stringResource(Res.string.storage_location_volume),
                    description = selected.label,
                    onClick = { showVolumeSheet = true },
                )
            }
            if (showDownloads) {
                val change = {
                    if (DownloadsLocationPlatform.choose()) {
                        downloadsPath = DownloadsLocationPlatform.currentPath().orEmpty()
                        onChanged()
                    }
                }
                navigationRow(
                    title = stringResource(Res.string.downloads_location_title),
                    description = downloadsPath,
                    trailingContent = {
                        TextButton(onClick = change) { Text(stringResource(Res.string.downloads_location_change)) }
                    },
                    onClick = change,
                )
            }
            folderPaths.forEach { (folder, path) ->
                val change: () -> Unit = {
                    scope.launch {
                        if (StoragePlatform.chooseFolder(folder)) {
                            folderPaths = folderPaths + (folder to StoragePlatform.folderPath(folder).orEmpty())
                            restartPrompt = true
                        }
                    }
                }
                navigationRow(
                    title = stringResource(
                        when (folder) {
                            StorageFolder.Cache -> Res.string.storage_location_cache
                            StorageFolder.AppData -> Res.string.storage_location_app_data
                        },
                    ),
                    description = path,
                    trailingContent = {
                        TextButton(onClick = change) { Text(stringResource(Res.string.downloads_location_change)) }
                    },
                    onClick = change,
                )
            }
        }
    }

    if (showVolumeSheet) {
        SingleChoiceBottomSheet(
            title = stringResource(Res.string.storage_location_volume),
            description = stringResource(Res.string.storage_location_volume_description),
            options = volumes.map { volume ->
                SingleChoiceOption(
                    value = volume.id,
                    label = volume.label,
                    supportingText = stringResource(
                        Res.string.storage_space_free_of,
                        formatBytes(volume.freeBytes),
                        formatBytes(volume.totalBytes),
                    ),
                )
            },
            isSelected = { it == (selectedVolumeId ?: volumes.first().id) },
            onSelected = { id ->
                StoragePlatform.selectVolume(id)
                selectedVolumeId = id
                onChanged()
            },
            onDismiss = { showVolumeSheet = false },
        )
    }

    if (restartPrompt) {
        StatusModal(
            title = stringResource(Res.string.storage_restart_title),
            message = stringResource(Res.string.storage_restart_message),
            isVisible = true,
            confirmText = stringResource(
                if (StoragePlatform.canRestart) Res.string.storage_restart_now else Res.string.storage_close_app,
            ),
            dismissText = stringResource(Res.string.storage_restart_later),
            onConfirm = { StoragePlatform.restart() },
            onDismiss = { restartPrompt = false },
        )
    }
}

@Composable
private fun StorageCategory.label(): String = stringResource(
    when (this) {
        StorageCategory.Downloads -> Res.string.storage_category_downloads
        StorageCategory.TorrentCache -> Res.string.storage_category_torrent_cache
        StorageCategory.Images -> Res.string.storage_category_images
        StorageCategory.AddonCache -> Res.string.storage_category_addon_cache
        StorageCategory.Other -> Res.string.storage_category_other
        StorageCategory.AppData -> Res.string.storage_category_app_data
    },
)

/**
 * Each category's colour, from a colour-blind-checked categorical palette taken in a fixed order
 * so a category keeps its colour whichever others are present, stepped for the theme on screen.
 */
@Composable
private fun StorageCategory.color(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val (light, darkValue) = when (this) {
        StorageCategory.Downloads -> 0xFF2A78D6 to 0xFF3987E5
        StorageCategory.TorrentCache -> 0xFFEB6834 to 0xFFD95926
        StorageCategory.Images -> 0xFF1BAF7A to 0xFF199E70
        StorageCategory.AddonCache -> 0xFFEDA100 to 0xFFC98500
        StorageCategory.Other -> 0xFFE87BA4 to 0xFFD55181
        StorageCategory.AppData -> 0xFF008300 to 0xFF008300
    }
    return Color(if (dark) darkValue else light)
}

private val StorageBarHeight = 36.dp
private val StorageBarGap = 2.dp
private val StorageBarMinSegment = 4.dp
private val StorageBarSegmentCorner = 4.dp
