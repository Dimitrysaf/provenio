package io.github.dimitrysaf.provenio.core.storage

import platform.Foundation.NSUserDefaults

internal actual object StoragePlatform {
    actual suspend fun measure(): Map<StorageCategory, Long> = emptyMap()

    actual suspend fun filesSize(uris: List<String>): Long = 0L

    actual suspend fun space(): StorageSpace? = null

    actual suspend fun cachedTorrents(): List<CachedTorrent> = emptyList()

    actual suspend fun clearStaleTorrentCaches() = Unit

    actual suspend fun clearAddonCache() = Unit

    actual suspend fun clearOther() = Unit

    actual fun volumes(): List<StorageVolume> = emptyList()

    actual fun selectedVolumeId(): String? = null

    actual fun selectVolume(id: String) = Unit

    actual val movableFolders: List<StorageFolder> = emptyList()

    actual fun folderPath(folder: StorageFolder): String? = null

    actual suspend fun chooseFolder(folder: StorageFolder): Boolean = false

    actual val canRestart: Boolean = false

    actual fun restart() = Unit

    actual fun loadTorrentTitles(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(TorrentTitlesKey)

    actual fun saveTorrentTitles(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = TorrentTitlesKey)
    }

    private const val TorrentTitlesKey = "provenio_storage_torrent_titles"
}
