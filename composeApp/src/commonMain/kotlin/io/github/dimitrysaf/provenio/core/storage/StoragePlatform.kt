package io.github.dimitrysaf.provenio.core.storage

/** The kinds of data the Storage page measures, in the order its bar shows them. */
enum class StorageCategory {
    Downloads,
    TorrentCache,
    Images,
    AddonCache,
    Other,
    AppData,
}

/** One torrent the torrent cache holds: its [name] and the space its pieces take. */
data class CachedTorrent(val infoHash: String, val name: String, val bytes: Long)

/** A disk's size and how much of it is free. */
data class StorageSpace(val totalBytes: Long, val freeBytes: Long)

/** A place downloads and the torrent cache can go, such as the phone's own storage or an SD card. */
data class StorageVolume(
    val id: String,
    val label: String,
    val removable: Boolean,
    val totalBytes: Long,
    val freeBytes: Long,
)

/** The app's folders that can be moved somewhere else. */
enum class StorageFolder {
    Cache,
    AppData,
}

/** What the Storage page needs from the device: folder sizes, free space and where things go. */
internal expect object StoragePlatform {
    /**
     * Bytes in the folders behind the categories the platform keeps itself. Downloads and images
     * are measured from their own records, so they are left out.
     */
    suspend fun measure(): Map<StorageCategory, Long>

    /** The bytes the files at [uris] take on disk, together; files that cannot be read count as nothing. */
    suspend fun filesSize(uris: List<String>): Long

    /** The size of the disk the app keeps most of its data on, and its free space. */
    suspend fun space(): StorageSpace?

    /** The torrents in the torrent cache, largest first. */
    suspend fun cachedTorrents(): List<CachedTorrent>

    /** Empties the folders torrent pieces were kept in before a move to another volume. */
    suspend fun clearStaleTorrentCaches()

    suspend fun clearAddonCache()

    /** Deletes logs, cached subtitles and old update files. */
    suspend fun clearOther()

    /** The volumes downloads and the torrent cache can go on; empty where the platform offers no choice. */
    fun volumes(): List<StorageVolume>

    fun selectedVolumeId(): String?

    fun selectVolume(id: String)

    /** The folders that can be moved on this platform. */
    val movableFolders: List<StorageFolder>

    fun folderPath(folder: StorageFolder): String?

    /**
     * Lets the user pick a new place for [folder]. Returns true when one was picked; its contents
     * move there the next time the app starts.
     */
    suspend fun chooseFolder(folder: StorageFolder): Boolean

    /** Whether [restart] can start the app again by itself, rather than only closing it. */
    val canRestart: Boolean

    fun restart()

    /** The titles torrents were last played under, saved as one JSON object. */
    fun loadTorrentTitles(): String?

    fun saveTorrentTitles(payload: String)
}
