package io.github.dimitrysaf.provenio.core.storage

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File

/**
 * The storage volumes the app can keep downloads and the torrent cache on: the phone's own shared
 * storage and any SD card or USB drive, through each volume's app-specific folders, which need no
 * permission. The choice is kept in the app's preferences.
 */
internal object AndroidStorageVolumes {
    const val PrimaryId = "primary"

    private const val PreferencesName = "provenio_storage"
    private const val VolumeKey = "volume"

    /** One mounted volume, with the app's own files and cache folders on it. */
    class Volume(
        val id: String,
        val label: String,
        val removable: Boolean,
        val filesRoot: File,
        val cacheRoot: File,
    )

    fun volumes(context: Context): List<Volume> {
        val manager = context.getSystemService(StorageManager::class.java)
        val files = context.getExternalFilesDirs(null)
        val caches = context.externalCacheDirs
        return files.indices.mapNotNull { index ->
            val filesRoot = files[index] ?: return@mapNotNull null
            val cacheRoot = caches.getOrNull(index) ?: return@mapNotNull null
            if (Environment.getExternalStorageState(filesRoot) != Environment.MEDIA_MOUNTED) return@mapNotNull null
            val volume = runCatching { manager?.getStorageVolume(filesRoot) }.getOrNull()
            val primary = index == 0 || volume?.isPrimary == true
            Volume(
                id = if (primary) PrimaryId else volume?.uuid ?: "volume$index",
                label = volume?.getDescription(context) ?: filesRoot.absolutePath,
                removable = volume?.isRemovable == true,
                filesRoot = filesRoot,
                cacheRoot = cacheRoot,
            )
        }
    }

    fun selectedId(context: Context): String =
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(VolumeKey, null) ?: PrimaryId

    fun select(context: Context, id: String) {
        context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).edit().putString(VolumeKey, id).apply()
    }

    /** The chosen volume when it is not the phone's own storage and is mounted; null otherwise. */
    fun selectedSecondary(context: Context): Volume? {
        val id = selectedId(context)
        if (id == PrimaryId) return null
        return volumes(context).firstOrNull { it.id == id }
    }

    /** Where the torrent engine keeps torrent pieces: the app's cache, or the chosen volume's. */
    fun torrentCacheDirectory(context: Context): File =
        selectedSecondary(context)?.let { File(it.cacheRoot, TorrentPayloadPath) }
            ?: File(context.cacheDir, TorrentPayloadPath)

    /** Every folder torrent pieces can be in, on every volume. */
    fun allTorrentCacheDirectories(context: Context): List<File> =
        (listOf(File(context.cacheDir, TorrentPayloadPath)) +
            volumes(context).filter { it.id != PrimaryId }.map { File(it.cacheRoot, TorrentPayloadPath) })
            .distinctBy { it.absolutePath }

    /** The chosen SD card's Movies/Provenio folder, or null when downloads stay in shared storage. */
    fun downloadsDirectory(context: Context): File? =
        selectedSecondary(context)?.let { File(File(it.filesRoot, Environment.DIRECTORY_MOVIES), DownloadsFolderName) }

    /** Every Movies/Provenio folder on a volume other than the phone's own storage. */
    fun secondaryDownloadsDirectories(context: Context): List<File> =
        volumes(context).filter { it.id != PrimaryId }
            .map { File(File(it.filesRoot, Environment.DIRECTORY_MOVIES), DownloadsFolderName) }

    /** The chosen SD card's downloads folder as the system file browser addresses it. */
    fun downloadsDocumentId(context: Context): String? {
        val volume = selectedSecondary(context) ?: return null
        return "${volume.id}:Android/data/${context.packageName}/files/${Environment.DIRECTORY_MOVIES}/$DownloadsFolderName"
    }

    private const val TorrentPayloadPath = "engine/payload"
    private const val DownloadsFolderName = "Provenio"
}
