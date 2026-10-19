package io.github.dimitrysaf.provenio.core.storage

import io.github.dimitrysaf.provenio.core.addons.AddonHttpClientProvider
import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.DesktopFolderChooser
import io.github.dimitrysaf.provenio.desktop.DesktopStorageLocations
import io.github.dimitrysaf.provenio.desktop.FolderChoice
import io.github.dimitrysaf.provenio.desktop.SingleInstance
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.storage_location_app_data
import provenio.composeapp.generated.resources.storage_location_cache

internal actual object StoragePlatform {
    private val isFlatpak: Boolean = !System.getenv("FLATPAK_ID").isNullOrBlank()

    actual suspend fun measure(): Map<StorageCategory, Long> = withContext(Dispatchers.IO) {
        val context = Context.app
        val cache = context.cacheDir
        val logs = File(context.filesDir, LogsFolder).sizeOnDisk()
        val otherCache = cache.listFiles().orEmpty()
            .filterNot { it.name in KeptInCache }
            .sumOf { it.sizeOnDisk() }
        mapOf(
            StorageCategory.TorrentCache to File(cache, TorrentPayloadPath).sizeOnDisk(),
            StorageCategory.AddonCache to File(cache, AddonCacheFolder).sizeOnDisk(),
            StorageCategory.Other to otherCache + logs,
            StorageCategory.AppData to (context.filesDir.sizeOnDisk() - logs + context.configDir.sizeOnDisk()).coerceAtLeast(0L),
        )
    }

    actual suspend fun space(): StorageSpace? = withContext(Dispatchers.IO) {
        val root = Context.app.cacheDir
        runCatching { StorageSpace(totalBytes = root.totalSpace, freeBytes = root.usableSpace) }
            .getOrNull()
            ?.takeIf { it.totalBytes > 0L }
    }

    actual suspend fun cachedTorrents(): List<CachedTorrent> = withContext(Dispatchers.IO) {
        cachedTorrentsIn(listOf(File(Context.app.cacheDir, TorrentPayloadPath)), StorageTitles.titles())
    }

    actual suspend fun clearStaleTorrentCaches() = Unit

    actual suspend fun clearAddonCache() = withContext(Dispatchers.IO) {
        runCatching { AddonHttpClientProvider.get().cache?.evictAll() }
        Unit
    }

    actual suspend fun clearOther() = withContext(Dispatchers.IO) {
        val context = Context.app
        context.cacheDir.listFiles().orEmpty()
            .filterNot { it.name in KeptInCache }
            .forEach { runCatching { it.deleteRecursively() } }
        File(context.filesDir, LogsFolder).listFiles().orEmpty()
            .filterNot { it.name == CurrentLogFile }
            .forEach { runCatching { it.deleteRecursively() } }
    }

    actual fun volumes(): List<StorageVolume> = emptyList()

    actual fun selectedVolumeId(): String? = null

    actual fun selectVolume(id: String) = Unit

    actual val movableFolders: List<StorageFolder> =
        if (isFlatpak) emptyList() else listOf(StorageFolder.Cache, StorageFolder.AppData)

    actual fun folderPath(folder: StorageFolder): String? = when (folder) {
        StorageFolder.Cache -> DesktopStorageLocations.cacheRoot()
        StorageFolder.AppData -> DesktopStorageLocations.dataRoot()
    }?.absolutePath

    actual suspend fun chooseFolder(folder: StorageFolder): Boolean {
        val title = getString(
            when (folder) {
                StorageFolder.Cache -> Res.string.storage_location_cache
                StorageFolder.AppData -> Res.string.storage_location_app_data
            },
        )
        val initial = folderPath(folder)?.let(::File)?.parentFile
        val choice = DesktopFolderChooser.chooseFolder(title = title, initial = initial)
        val chosen = (choice as? FolderChoice.Chosen)?.folder ?: return false
        when (folder) {
            StorageFolder.Cache -> DesktopStorageLocations.moveCache(File(chosen, CacheFolderName))
            StorageFolder.AppData -> DesktopStorageLocations.moveData(File(chosen, DataFolderName))
        }
        return true
    }

    actual val canRestart: Boolean =
        !isFlatpak && ProcessHandle.current().info().command().isPresent

    actual fun restart() {
        val info = ProcessHandle.current().info()
        val command = info.command().orElse(null)
        SingleInstance.release()
        if (command != null && !isFlatpak) {
            val arguments = info.arguments().orElse(emptyArray()).toList()
            runCatching { ProcessBuilder(listOf(command) + arguments).inheritIO().start() }
        }
        exitProcess(0)
    }

    actual fun loadTorrentTitles(): String? =
        Context.app.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(TorrentTitlesKey, null)

    actual fun saveTorrentTitles(payload: String) {
        Context.app.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(TorrentTitlesKey, payload)
            .apply()
    }

    private const val PreferencesName = "provenio_storage"
    private const val TorrentTitlesKey = "torrent_titles"
    private const val TorrentPayloadPath = "engine/payload"
    private const val AddonCacheFolder = "addon_http"
    private const val LogsFolder = "logs"
    private const val CurrentLogFile = "provenio.log"
    private const val CacheFolderName = "Provenio Cache"
    private const val DataFolderName = "Provenio Data"
    private val KeptInCache = setOf("engine", AddonCacheFolder, "image_cache")
}
