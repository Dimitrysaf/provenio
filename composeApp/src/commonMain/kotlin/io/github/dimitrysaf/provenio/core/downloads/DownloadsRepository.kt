package io.github.dimitrysaf.provenio.core.downloads

import io.github.dimitrysaf.provenio.core.playback.addonSubtitleRequests
import io.github.dimitrysaf.provenio.core.streams.StreamItem
import io.github.dimitrysaf.provenio.core.build.AppFeaturePolicy
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamRequest
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.p2p.buildP2pMagnetUri
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

object DownloadsRepository {
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val activeHandles = mutableMapOf<String, DownloadsTaskHandle>()
    // Main, so the handle map and the state flow keep being touched from one thread; the engine
    // does its own work off it.
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var hasLoaded = false
    private var nextDownloadOrdinal = 0L

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        activeHandles.values.forEach(DownloadsTaskHandle::cancel)
        activeHandles.clear()
        _uiState.value.items.filter { it.isTorrentDownload }.map { it.id }.forEach(::releaseTorrentEngine)
        hasLoaded = false
        _uiState.value = DownloadsUiState()
        notifyLiveStatusPlatform()
    }

    fun findPlayableDownloadByVideoId(videoId: String?): DownloadItem? {
        ensureLoaded()
        val normalizedVideoId = videoId?.trim().orEmpty()
        if (normalizedVideoId.isBlank()) return null
        return _uiState.value.items.firstOrNull { item ->
            item.videoId == normalizedVideoId && item.hasPlayableLocalFile()
        }
    }

    fun findPlayableDownload(
        parentMetaId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        videoId: String? = null,
    ): DownloadItem? {
        ensureLoaded()
        val items = _uiState.value.items
        val normalizedParentMetaId = parentMetaId.trim()

        findPlayableDownloadByVideoId(videoId)?.let { return it }

        return if (seasonNumber != null && episodeNumber != null) {
            items.firstOrNull { item ->
                item.parentMetaId == normalizedParentMetaId &&
                    item.seasonNumber == seasonNumber &&
                    item.episodeNumber == episodeNumber &&
                    item.hasPlayableLocalFile()
            }
        } else {
            items.firstOrNull { item ->
                item.parentMetaId == normalizedParentMetaId &&
                    item.seasonNumber == null &&
                    item.episodeNumber == null &&
                    item.hasPlayableLocalFile()
            }
        }
    }

    fun playableLocalFileUri(item: DownloadItem): String? {
        ensureLoaded()
        if (item.status != DownloadStatus.Completed) return null
        val resolvedUri = DownloadsPlatformDownloader.resolveLocalFileUri(
            localFileUri = item.localFileUri,
            destinationFileName = item.fileName,
        ) ?: return null

        if (resolvedUri != item.localFileUri) {
            mutateItem(item.id) { current ->
                if (current.fileName == item.fileName) {
                    current.copy(
                        localFileUri = resolvedUri,
                        updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                    )
                } else {
                    current
                }
            }
        }

        return resolvedUri
    }

    fun enqueueFromStream(
        contentType: String,
        videoId: String,
        parentMetaId: String,
        parentMetaType: String,
        title: String,
        logo: String?,
        poster: String?,
        background: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        episodeTitle: String?,
        episodeThumbnail: String?,
        stream: StreamItem,
    ): DownloadEnqueueResult {
        ensureLoaded()

        val directUrl = stream.playableDirectUrl?.trim()?.takeIf { it.isNotBlank() }

        // A torrent carries no URL to fetch. What it does carry is enough to ask the P2P engine
        // to serve it, so the download keeps the torrent's identity and resolves an address each
        // time the transfer starts.
        val torrentInfoHash = if (directUrl == null) stream.p2pInfoHash else null
        if (directUrl == null && torrentInfoHash == null) {
            return DownloadEnqueueResult.MissingUrl
        }
        if (torrentInfoHash != null && !AppFeaturePolicy.p2pEnabled) {
            return DownloadEnqueueResult.P2pDisabled
        }
        if (torrentInfoHash != null) {
            P2pSettingsRepository.ensureLoaded()
            if (!P2pSettingsRepository.uiState.value.p2pEnabled) {
                return DownloadEnqueueResult.P2pDisabled
            }
        }
        if (directUrl != null && !directUrl.isSupportedDownloadUrl()) {
            return DownloadEnqueueResult.UnsupportedFormat
        }

        val torrentTrackers = if (torrentInfoHash != null) stream.p2pTrackers else emptyList()
        // The magnet is what the download reports as its source; it is never fetched directly.
        // Building it also proves the info hash is one the engine could accept, since a stream
        // may carry anything in that field.
        val sourceUrl = directUrl ?: runCatching {
            buildP2pMagnetUri(torrentInfoHash!!, torrentTrackers)
        }.getOrElse { return DownloadEnqueueResult.MissingUrl }

        val now = DownloadsClock.nowEpochMs()
        val logicalKey = buildLogicalKey(
            parentMetaId = parentMetaId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
        )

        var replacedExisting = false
        val currentItems = _uiState.value.items.toMutableList()
        val existing = currentItems.firstOrNull { it.logicalContentKey == logicalKey }
        if (existing != null) {
            replacedExisting = true
            activeHandles.remove(existing.id)?.cancel()
            releaseTorrentEngine(existing.id)
            DownloadsPlatformDownloader.removeFile(playableLocalFileUri(existing) ?: existing.localFileUri)
            DownloadsPlatformDownloader.removePartialFile(existing.fileName)
            DownloadsArchive.remove(existing.fileName)
            currentItems.removeAll { it.id == existing.id }
        }

        val downloadId = nextDownloadId(now)
        val fileName = buildFileName(
            title = title,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            fallbackTitle = stream.streamLabel,
            sourceUrl = sourceUrl,
            downloadId = downloadId,
        )

        val item = DownloadItem(
            id = downloadId,
            contentType = contentType,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            videoId = videoId,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            episodeThumbnail = episodeThumbnail,
            streamTitle = stream.streamLabel,
            streamSubtitle = stream.streamSubtitle,
            providerName = stream.addonName,
            providerAddonId = stream.addonId,
            sourceUrl = sourceUrl,
            sourceHeaders = sanitizeRequestHeaders(stream.behaviorHints.proxyHeaders?.request),
            sourceResponseHeaders = sanitizeResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
            subtitleRequests = addonSubtitleRequests(contentType, videoId),
            sourceSubtitles = stream.externalSubtitles,
            localFileUri = null,
            torrentInfoHash = torrentInfoHash,
            torrentFileIdx = if (torrentInfoHash != null) stream.p2pFileIdx else null,
            torrentTrackers = torrentTrackers,
            fileName = fileName,
            status = DownloadStatus.Downloading,
            downloadedBytes = 0L,
            totalBytes = null,
            errorMessage = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
        )

        currentItems.add(0, item)
        publish(currentItems)
        persist()
        startDownload(item)

        return if (replacedExisting) {
            DownloadEnqueueResult.Replaced
        } else {
            DownloadEnqueueResult.Started
        }
    }

    fun pauseDownload(downloadId: String) {
        ensureLoaded()
        val item = _uiState.value.items.firstOrNull { it.id == downloadId } ?: return
        if (item.status != DownloadStatus.Downloading) return

        activeHandles.remove(downloadId)?.cancel()
        releaseTorrentEngine(downloadId)
        mutateItem(downloadId) { current ->
            current.copy(
                status = DownloadStatus.Paused,
                updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                errorMessage = null,
            )
        }
    }

    fun pauseActiveDownloads() {
        ensureLoaded()
        _uiState.value.items
            .filter { it.status == DownloadStatus.Downloading }
            .map { it.id }
            .forEach(::pauseDownload)
    }

    fun resumeDownload(downloadId: String) {
        ensureLoaded()
        val item = _uiState.value.items.firstOrNull { it.id == downloadId } ?: return
        if (item.status != DownloadStatus.Paused && item.status != DownloadStatus.Failed) return

        val reset = item.copy(
            status = DownloadStatus.Downloading,
            errorMessage = null,
            localFileUri = null,
            updatedAtEpochMs = DownloadsClock.nowEpochMs(),
        )

        replaceItem(reset)
        persist()
        startDownload(reset)
    }

    fun retryDownload(downloadId: String) {
        resumeDownload(downloadId)
    }

    internal fun reattachBackgroundDownload(downloadId: String) {
        if (!hasLoaded) return
        val item = _uiState.value.items.firstOrNull { it.id == downloadId } ?: return
        activeHandles.remove(downloadId)?.cancel()
        releaseTorrentEngine(downloadId)
        val restored = DownloadsPlatformDownloader.restoreItem(item)
        replaceItem(restored)
        persist()
        if (restored.status == DownloadStatus.Downloading) startDownload(restored)
    }

    fun cancelDownload(downloadId: String) {
        ensureLoaded()
        val item = _uiState.value.items.firstOrNull { it.id == downloadId } ?: return

        activeHandles.remove(downloadId)?.cancel()
        releaseTorrentEngine(downloadId)
        DownloadsPlatformDownloader.removeFile(playableLocalFileUri(item) ?: item.localFileUri)
        DownloadsPlatformDownloader.removePartialFile(item.fileName)
        DownloadsArchive.remove(item.fileName)

        publish(_uiState.value.items.filterNot { it.id == downloadId })
        persist()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val payload = DownloadsStorage.loadPayload().orEmpty().trim()

        var shouldPersistNormalized = false
        val normalized = DownloadsCodec.decodeItems(payload)
            .map { item ->
                val statusNormalized = DownloadsPlatformDownloader.restoreItem(item)

                val localUriNormalized = normalizeCompletedLocalFileUri(statusNormalized)
                if (localUriNormalized != item) {
                    shouldPersistNormalized = true
                }
                localUriNormalized
            }

        val records = DownloadsArchive.records().mapNotNull(DownloadsCodec::decodeArchived)
        val recovered = recoverDownloads(normalized, records)
        val items = if (recovered.isEmpty()) {
            normalized
        } else {
            shouldPersistNormalized = true
            (normalized + recovered).sortedByDescending { it.createdAtEpochMs }
        }

        _uiState.value = DownloadsUiState(items)
        notifyLiveStatusPlatform()
        if (shouldPersistNormalized) {
            persist()
        }
        val archived = records.mapTo(mutableSetOf()) { it.item.fileName }
        items.filter { it.status == DownloadStatus.Completed && it.fileName !in archived }.forEach(::archive)
        items.filter { it.status == DownloadStatus.Downloading && it.id !in activeHandles }
            .forEach(::startDownload)
    }

    private fun archive(item: DownloadItem) {
        if (item.status != DownloadStatus.Completed) return
        val record = ArchivedDownload(profileId = ProfileRepository.activeProfileId, item = item)
        DownloadsArchive.save(item.fileName, DownloadsCodec.encodeArchived(record))
    }

    /**
     * Finished downloads whose files are still there but which this profile no longer lists, as
     * after the app's data was cleared: first from the records kept beside the files, then, for
     * files without one, from what the file name says.
     */
    private fun recoverDownloads(current: List<DownloadItem>, records: List<ArchivedDownload>): List<DownloadItem> {
        val activeProfile = ProfileRepository.activeProfileId
        val knownProfiles = ProfileRepository.state.value.profiles.mapTo(mutableSetOf()) { it.profileIndex }
        val profiles = knownProfiles + activeProfile
        val otherProfileItems = profiles.filter { it != activeProfile }
            .flatMap { profile -> DownloadsCodec.decodeItems(DownloadsStorage.loadPayload(profile).orEmpty()) }
        val claimedFiles = (current + otherProfileItems).flatMapTo(mutableSetOf()) { it.fileNames() }
        val knownIds = current.mapTo(mutableSetOf()) { it.id }
        val recovered = mutableListOf<DownloadItem>()

        for (record in records) {
            val item = record.item
            // A record whose profile is gone, as after clearing the app's data, joins the profile in use.
            val belongsHere = record.profileId == activeProfile ||
                (knownProfiles.isNotEmpty() && record.profileId !in profiles)
            if (!belongsHere || item.id in knownIds || item.fileNames().any { it in claimedFiles }) continue
            val localFileUri = DownloadsPlatformDownloader.resolveLocalFileUri(item.localFileUri, item.fileName)
                ?: continue
            val restored = item.copy(
                status = DownloadStatus.Completed,
                localFileUri = localFileUri,
                errorMessage = null,
            )
            recovered += restored
            knownIds += restored.id
            claimedFiles += restored.fileNames()
        }
        records.forEach { claimedFiles += it.item.fileNames() }

        for (localFileUri in DownloadsArchive.mediaFileUris()) {
            val fileName = localFileUri.fileNameFromUri() ?: continue
            if (fileName in claimedFiles) continue
            val item = downloadFromFileName(fileName, localFileUri) ?: continue
            if (item.id in knownIds) continue
            recovered += item
            knownIds += item.id
            claimedFiles += fileName
        }
        return recovered
    }

    private fun startDownload(item: DownloadItem) {
        if (item.isTorrentDownload) {
            startTorrentDownload(item)
        } else {
            startTransfer(item, resolvedSourceUrl = null)
        }
    }

    /**
     * Asks the P2P engine to serve this torrent, then downloads from the address it gives back.
     */
    private fun startTorrentDownload(item: DownloadItem) {
        val infoHash = item.torrentInfoHash ?: return
        val job = repositoryScope.launch {
            val localUrl = try {
                P2pStreamingEngine.startDownloadStream(
                    downloadId = item.id,
                    request = P2pStreamRequest(
                        infoHash = infoHash,
                        fileIdx = item.torrentFileIdx,
                        trackers = item.torrentTrackers,
                    ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                releaseTorrentEngine(item.id)
                // mutateItem takes a plain lambda, so the message is resolved out here where
                // suspending is allowed.
                val reason = error.message?.takeIf { it.isNotBlank() }
                    ?: getString(Res.string.downloads_error_torrent_engine)
                mutateItem(item.id) { current ->
                    if (current.status != DownloadStatus.Downloading) return@mutateItem current
                    current.copy(
                        status = DownloadStatus.Failed,
                        errorMessage = reason,
                        updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                    )
                }
                return@launch
            }
            startTransfer(item, resolvedSourceUrl = localUrl)
        }
        activeHandles[item.id] = TorrentPreparationHandle(item.id, job)
    }

    private fun releaseTorrentEngine(downloadId: String) {
        P2pStreamingEngine.stopDownloadStream(downloadId)
    }

    private fun startTransfer(item: DownloadItem, resolvedSourceUrl: String?) {
        // The downloaders read the URL off the item they are handed, so a torrent is given one
        // pointing at the engine's local address. The stored item keeps its magnet.
        val transferItem = resolvedSourceUrl?.let { item.copy(sourceUrl = it) } ?: item
        val request = DownloadPlatformRequest(transferItem)

        val handle = DownloadsPlatformDownloader.start(
            request = request,
            onProgress = { downloadedBytes, totalBytes ->
                mutateItem(item.id) { current ->
                    if (current.status != DownloadStatus.Downloading) {
                        current
                    } else {
                        current.copy(
                            downloadedBytes = downloadedBytes.coerceAtLeast(0L),
                            totalBytes = totalBytes?.takeIf { it > 0L },
                            updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                            errorMessage = null,
                        )
                    }
                }
            },
            onSuccess = { localFileUri, totalBytes ->
                activeHandles.remove(item.id)
                releaseTorrentEngine(item.id)
                mutateItem(item.id) { current ->
                    if (current.status != DownloadStatus.Downloading) return@mutateItem current
                    current.copy(
                        status = DownloadStatus.Completed,
                        localFileUri = localFileUri,
                        downloadedBytes = if (totalBytes != null && totalBytes > 0L) {
                            totalBytes
                        } else {
                            current.downloadedBytes
                        },
                        totalBytes = totalBytes?.takeIf { it > 0L } ?: current.totalBytes,
                        errorMessage = null,
                        updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                    )
                }
                _uiState.value.items.firstOrNull { it.id == item.id }?.let(::archive)
            },
            onFailure = { message ->
                activeHandles.remove(item.id)
                releaseTorrentEngine(item.id)
                mutateItem(item.id) { current ->
                    if (current.status != DownloadStatus.Downloading) {
                        current
                    } else {
                        current.copy(
                            status = DownloadStatus.Failed,
                            errorMessage = message.ifBlank { runBlocking { getString(Res.string.download_failed) } },
                            updatedAtEpochMs = DownloadsClock.nowEpochMs(),
                        )
                    }
                }
            },
            onPaused = {
                activeHandles.remove(item.id)
                releaseTorrentEngine(item.id)
                mutateItem(item.id) { current ->
                    if (current.status != DownloadStatus.Downloading) return@mutateItem current
                    current.copy(status = DownloadStatus.Paused, errorMessage = null)
                }
            },
        )

        activeHandles[item.id] = handle
    }

    /** Waiting on the engine is cancellable in its own right, before any bytes are moving. */
    private class TorrentPreparationHandle(
        private val downloadId: String,
        private val job: Job,
    ) : DownloadsTaskHandle {
        override fun cancel() {
            job.cancel()
            DownloadsRepository.releaseTorrentEngine(downloadId)
        }
    }

    private fun mutateItem(downloadId: String, transform: (DownloadItem) -> DownloadItem) {
        var changed = false
        val updated = _uiState.value.items.map { item ->
            if (item.id == downloadId) {
                changed = true
                transform(item)
            } else {
                item
            }
        }

        if (changed) {
            publish(updated)
            persist()
        }
    }

    private fun replaceItem(item: DownloadItem) {
        val updated = _uiState.value.items.map { existing ->
            if (existing.id == item.id) item else existing
        }
        publish(updated)
    }

    private fun publish(items: List<DownloadItem>) {
        _uiState.value = DownloadsUiState(
            items = items,
        )
        notifyLiveStatusPlatform()
    }

    private fun notifyLiveStatusPlatform() {
        runCatching {
            DownloadsLiveStatusPlatform.onItemsChanged(_uiState.value.items)
        }
    }

    private fun persist() {
        DownloadsStorage.savePayload(
            DownloadsCodec.encodeItems(_uiState.value.items),
        )
    }

    private fun nextDownloadId(nowEpochMs: Long): String {
        nextDownloadOrdinal += 1L
        return buildString {
            append(nowEpochMs.toString(36))
            append('_')
            append(nextDownloadOrdinal.toString(36))
        }
    }

    private fun normalizeCompletedLocalFileUri(item: DownloadItem): DownloadItem {
        if (item.status != DownloadStatus.Completed) return item
        val resolvedUri = DownloadsPlatformDownloader.resolveLocalFileUri(
            localFileUri = item.localFileUri,
            destinationFileName = item.fileName,
        ) ?: return item
        return if (resolvedUri != item.localFileUri) {
            item.copy(localFileUri = resolvedUri)
        } else {
            item
        }
    }

    private fun DownloadItem.hasPlayableLocalFile(): Boolean =
        status == DownloadStatus.Completed &&
            DownloadsPlatformDownloader.resolveLocalFileUri(
                localFileUri = localFileUri,
                destinationFileName = fileName,
            ) != null
}

@Serializable
internal data class ArchivedDownload(
    val profileId: Int,
    val item: DownloadItem,
)

@Serializable
private data class StoredDownloadsPayload(
    val items: List<DownloadItem> = emptyList(),
)

private object DownloadsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun decodeItems(payload: String): List<DownloadItem> =
        runCatching {
            json.decodeFromString<StoredDownloadsPayload>(payload).items
        }.getOrDefault(emptyList())

    fun decodeArchived(record: String): ArchivedDownload? =
        runCatching { json.decodeFromString<ArchivedDownload>(record) }.getOrNull()

    fun encodeArchived(record: ArchivedDownload): String = json.encodeToString(record)

    fun encodeItems(items: Collection<DownloadItem>): String =
        json.encodeToString(
            StoredDownloadsPayload(
                items = items.toList(),
            ),
        )
}

private fun sanitizeRequestHeaders(headers: Map<String, String>?): Map<String, String> =
    headers
        .orEmpty()
        .mapNotNull { (key, value) ->
            val normalizedKey = key.trim()
            val normalizedValue = value.trim()
            if (
                normalizedKey.isBlank() ||
                normalizedValue.isBlank() ||
                normalizedKey.equals("Accept-Encoding", ignoreCase = true) ||
                normalizedKey.equals("Range", ignoreCase = true)
            ) {
                null
            } else {
                normalizedKey to normalizedValue
            }
        }
        .toMap()

private fun sanitizeResponseHeaders(headers: Map<String, String>?): Map<String, String> =
    headers
        .orEmpty()
        .mapNotNull { (key, value) ->
            val normalizedKey = key.trim()
            val normalizedValue = value.trim()
            if (normalizedKey.isBlank() || normalizedValue.isBlank()) {
                null
            } else {
                normalizedKey to normalizedValue
            }
        }
        .toMap()

private fun buildLogicalKey(
    parentMetaId: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
): String = if (seasonNumber != null && episodeNumber != null) {
    "${parentMetaId.trim()}|$seasonNumber|$episodeNumber"
} else {
    "${parentMetaId.trim()}|movie"
}

private fun buildFileName(
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    fallbackTitle: String,
    sourceUrl: String,
    downloadId: String,
): String {
    val baseTitle = if (seasonNumber != null && episodeNumber != null) {
        buildString {
            append(title)
            append(" S")
            append(seasonNumber.toString().padStart(2, '0'))
            append('E')
            append(episodeNumber.toString().padStart(2, '0'))
            if (!episodeTitle.isNullOrBlank()) {
                append(' ')
                append(episodeTitle)
            }
        }
    } else {
        title.ifBlank { fallbackTitle }
    }

    val extension = sourceUrl.fileExtensionFromUrl()
    return buildString {
        append(baseTitle.sanitizeFileName().ifBlank { "download" }.take(92))
        append('_')
        append(downloadId)
        append('.')
        append(extension)
    }
}

private fun DownloadItem.fileNames(): Set<String> =
    setOfNotNull(fileName.takeIf { it.isNotBlank() }, localFileUri?.fileNameFromUri())

// The last path segment of a file URI, with its percent escapes decoded.
private fun String.fileNameFromUri(): String? {
    val segment = substringBefore('?').substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
    val bytes = ArrayList<Byte>(segment.length)
    var index = 0
    while (index < segment.length) {
        val char = segment[index]
        val escaped = if (char == '%' && index + 2 < segment.length) {
            segment.substring(index + 1, index + 3).toIntOrNull(16)
        } else {
            null
        }
        if (escaped != null) {
            bytes += escaped.toByte()
            index += 3
        } else {
            char.toString().encodeToByteArray().forEach { bytes += it }
            index++
        }
    }
    return bytes.toByteArray().decodeToString()
}

private val RecoveredFileNamePattern = Regex("""^(.*)_([0-9a-z]+_[0-9a-z]+)$""")
private val RecoveredEpisodePattern = Regex("""^(.*?) S(\d{1,3})E(\d{1,4})(?: (.*))?$""")

// A download rebuilt from a file named the way this app names them: the title, the season and
// episode for a series, then the download's id. Whatever the name does not carry stays empty.
private fun downloadFromFileName(fileName: String, localFileUri: String): DownloadItem? {
    val baseName = fileName.substringBeforeLast('.', missingDelimiterValue = fileName)
    val named = RecoveredFileNamePattern.matchEntire(baseName)
    val downloadId = named?.groupValues?.get(2)
    val label = (named?.groupValues?.get(1) ?: baseName).trim().ifBlank { return null }
    val episode = RecoveredEpisodePattern.matchEntire(label)
    val title = (episode?.groupValues?.get(1) ?: label).trim().ifBlank { label }
    val seasonNumber = episode?.groupValues?.get(2)?.toIntOrNull()
    val episodeNumber = episode?.groupValues?.get(3)?.toIntOrNull()
    val episodeTitle = episode?.groupValues?.get(4)?.trim()?.takeIf { it.isNotBlank() }
    val type = if (seasonNumber != null && episodeNumber != null) "series" else "movie"
    val showId = "local:${title.lowercase()}"
    val id = downloadId ?: "local-${fileName.hashCode().toUInt()}"
    val now = DownloadsClock.nowEpochMs()
    val createdAt = downloadId?.substringBefore('_')?.toLongOrNull(36)?.takeIf { it in 1..now } ?: now
    return DownloadItem(
        id = id,
        contentType = type,
        parentMetaId = showId,
        parentMetaType = type,
        videoId = if (type == "series") "$showId:$seasonNumber:$episodeNumber" else showId,
        title = title,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        episodeTitle = episodeTitle,
        streamTitle = fileName,
        providerName = "",
        sourceUrl = localFileUri,
        localFileUri = localFileUri,
        fileName = fileName,
        status = DownloadStatus.Completed,
        createdAtEpochMs = createdAt,
        updatedAtEpochMs = now,
    )
}

private fun String.sanitizeFileName(): String =
    trim().replace(Regex("[^A-Za-z0-9._ -]"), "_")

private fun String.fileExtensionFromUrl(): String {
    val withoutQuery = substringBefore('?').substringBefore('#')
    val suffix = withoutQuery.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .trim()

    return if (suffix.length in 2..5 && suffix.all { it.isLetterOrDigit() }) {
        suffix
    } else {
        "mp4"
    }
}

private fun String.isSupportedDownloadUrl(): Boolean {
    val normalized = trim().lowercase()
    if (normalized.startsWith("magnet:")) return false
    if (normalized.endsWith(".m3u8") || normalized.contains(".m3u8?")) return false
    if (normalized.endsWith(".mpd") || normalized.contains(".mpd?")) return false
    if (normalized.endsWith(".torrent") || normalized.contains(".torrent?")) return false
    return normalized.startsWith("http://") || normalized.startsWith("https://")
}
