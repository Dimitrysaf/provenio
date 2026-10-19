package io.github.dimitrysaf.provenio.core.storage

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import io.github.dimitrysaf.provenio.core.addons.AddonHttpClientProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual object StoragePlatform {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual suspend fun measure(): Map<StorageCategory, Long> = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext emptyMap()
        val cache = context.cacheDir
        val keptInCache = setOf(EngineFolder, AddonCacheFolder, ImageCacheFolder)
        val otherCache = cache.listFiles().orEmpty()
            .filterNot { it.name in keptInCache }
            .sumOf { it.sizeOnDisk() }
        val logs = File(context.filesDir, LogsFolder).sizeOnDisk()
        val dataRoot = context.dataDir
        val appData = dataRoot.sizeOnDisk() - cache.sizeOnDisk() - logs -
            File(context.filesDir, InProgressDownloadsFolder).sizeOnDisk()
        mapOf(
            StorageCategory.TorrentCache to AndroidStorageVolumes.allTorrentCacheDirectories(context).sumOf { it.sizeOnDisk() },
            StorageCategory.AddonCache to File(cache, AddonCacheFolder).sizeOnDisk(),
            StorageCategory.Other to otherCache + logs,
            StorageCategory.AppData to appData.coerceAtLeast(0L),
        )
    }

    actual suspend fun filesSize(uris: List<String>): Long = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext 0L
        uris.sumOf { uri ->
            fileUriLength(uri) ?: runCatching {
                context.contentResolver.query(Uri.parse(uri), arrayOf(OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
            }.getOrNull() ?: 0L
        }
    }

    actual suspend fun space(): StorageSpace? = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext null
        val root = AndroidStorageVolumes.selectedSecondary(context)?.filesRoot ?: context.filesDir
        runCatching {
            val stats = StatFs(root.absolutePath)
            StorageSpace(totalBytes = stats.totalBytes, freeBytes = stats.availableBytes)
        }.getOrNull()
    }

    actual suspend fun cachedTorrents(): List<CachedTorrent> = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext emptyList()
        cachedTorrentsIn(AndroidStorageVolumes.allTorrentCacheDirectories(context), StorageTitles.titles())
    }

    actual suspend fun clearStaleTorrentCaches() = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext
        val current = AndroidStorageVolumes.torrentCacheDirectory(context).absolutePath
        AndroidStorageVolumes.allTorrentCacheDirectories(context)
            .filter { it.absolutePath != current }
            .forEach { it.deleteContents() }
    }

    actual suspend fun clearAddonCache() = withContext(Dispatchers.IO) {
        runCatching { AddonHttpClientProvider.get().cache?.evictAll() }
        Unit
    }

    actual suspend fun clearOther() = withContext(Dispatchers.IO) {
        val context = appContext ?: return@withContext
        val keptInCache = setOf(EngineFolder, AddonCacheFolder, ImageCacheFolder)
        context.cacheDir.listFiles().orEmpty()
            .filterNot { it.name in keptInCache }
            .forEach { runCatching { it.deleteRecursively() } }
        File(context.filesDir, LogsFolder).deleteContents()
    }

    actual fun volumes(): List<StorageVolume> {
        val context = appContext ?: return emptyList()
        return AndroidStorageVolumes.volumes(context).map { volume ->
            val stats = runCatching { StatFs(volume.filesRoot.absolutePath) }.getOrNull()
            StorageVolume(
                id = volume.id,
                label = volume.label,
                removable = volume.removable,
                totalBytes = stats?.totalBytes ?: 0L,
                freeBytes = stats?.availableBytes ?: 0L,
            )
        }
    }

    actual fun selectedVolumeId(): String? = appContext?.let(AndroidStorageVolumes::selectedId)

    actual fun selectVolume(id: String) {
        val context = appContext ?: return
        AndroidStorageVolumes.select(context, id)
    }

    actual val movableFolders: List<StorageFolder> = emptyList()

    actual fun folderPath(folder: StorageFolder): String? = null

    actual suspend fun chooseFolder(folder: StorageFolder): Boolean = false

    actual val canRestart: Boolean = false

    actual fun restart() = Unit

    actual fun loadTorrentTitles(): String? =
        appContext?.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)?.getString(TorrentTitlesKey, null)

    actual fun saveTorrentTitles(payload: String) {
        appContext?.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(TorrentTitlesKey, payload)
            ?.apply()
    }

    private const val PreferencesName = "provenio_storage"
    private const val TorrentTitlesKey = "torrent_titles"
    private const val EngineFolder = "engine"
    private const val AddonCacheFolder = "addon_http"
    private const val ImageCacheFolder = "image_cache"
    private const val LogsFolder = "logs"
    private const val InProgressDownloadsFolder = "downloads"
}
