package io.github.dimitrysaf.provenio.core.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.updates_download_failed_http
import provenio.composeapp.generated.resources.updates_downloaded_file_missing
import provenio.composeapp.generated.resources.updates_empty_download_body
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.compose.resources.getString
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AndroidAppUpdaterPlatform {
    private const val preferencesName = "provenio_updater"
    private const val ignoredTagKey = "ignored_release_tag"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var appContext: Context? = null

    private var installedByHand: Boolean? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Whether this copy was installed by hand, from an APK or over adb, rather than by an app store
     * such as F-Droid that keeps it up to date itself. Only a copy installed by hand updates itself.
     */
    fun isInstalledByHand(): Boolean {
        installedByHand?.let { return it }
        val context = appContext ?: return false
        val installer = runCatching { installerPackageName(context) }.getOrNull()
        return (installer == null || installer !in AppStoreInstallers).also { installedByHand = it }
    }

    private fun installerPackageName(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }

    /** App stores and update managers that install and update apps on their own. */
    private val AppStoreInstallers = setOf(
        "org.fdroid.fdroid",
        "org.fdroid.basic",
        "org.fdroid.fdroid.privileged",
        "com.looker.droidify",
        "com.machiav3lli.fdroid",
        "dev.imranr.obtainium",
        "dev.imranr.obtainium.fdroid",
        "com.aurora.store",
        "com.android.vending",
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps",
        "com.huawei.appmarket",
    )

    fun getSupportedAbis(): List<String> = Build.SUPPORTED_ABIS?.toList().orEmpty()

    fun isDebugBuild(): Boolean {
        val context = appContext ?: return false
        return context.packageName.endsWith(BetaPackageSuffix)
    }

    fun getIgnoredTag(): String? =
        preferences().getString(ignoredTagKey, null)

    fun setIgnoredTag(tag: String?) {
        preferences().edit().apply {
            if (tag == null) remove(ignoredTagKey) else putString(ignoredTagKey, tag)
        }.apply()
    }

    suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val destination = updateFile(assetName)
            if (destination.exists()) {
                onProgress(destination.length(), destination.length())
                return@runCatching destination.absolutePath
            }
            clearUpdateFiles(destination.name)
            val partial = File(destination.parentFile, destination.name + PartialSuffix)

            var attempt = 0
            while (true) {
                attempt += 1
                val resumeFrom = partial.takeIf { it.exists() }?.length() ?: 0L
                val request = Request.Builder()
                    .url(assetUrl)
                    .apply { if (resumeFrom > 0L) header("Range", "bytes=$resumeFrom-") }
                    .build()

                val restart = httpClient.newCall(request).execute().use { response ->
                    if (response.code == HttpRangeNotSatisfiable && attempt == 1) {
                        partial.delete()
                        return@use true
                    }
                    if (!response.isSuccessful) {
                        error(runBlocking { getString(Res.string.updates_download_failed_http, response.code) })
                    }

                    val body = response.body ?: error(runBlocking { getString(Res.string.updates_empty_download_body) })
                    val appending = response.code == HttpPartialContent && resumeFrom > 0L
                    val alreadyDownloaded = if (appending) resumeFrom else 0L
                    val totalBytes = body.contentLength().takeIf { it > 0L }?.let { it + alreadyDownloaded }
                    body.byteStream().use { input ->
                        FileOutputStream(partial, appending).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloadedBytes = alreadyDownloaded
                            onProgress(downloadedBytes, totalBytes)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                onProgress(downloadedBytes, totalBytes)
                            }
                            output.flush()
                        }
                    }
                    false
                }
                if (!restart) break
            }

            check(partial.renameTo(destination)) { runBlocking { getString(Res.string.updates_downloaded_file_missing) } }
            destination.absolutePath
        }
    }

    fun completedUpdatePath(fileName: String): String? =
        updateFile(fileName).takeIf { it.exists() }?.absolutePath

    fun hasPartialUpdate(fileName: String): Boolean {
        val file = updateFile(fileName)
        return File(file.parentFile, file.name + PartialSuffix).exists()
    }

    fun clearUpdateFiles(keepFileName: String?) {
        val keep = keepFileName?.let { updateFile(it).name }
        updatesDirectory().listFiles()?.forEach { file ->
            if (keep == null || (file.name != keep && file.name != keep + PartialSuffix)) {
                file.delete()
            }
        }
    }

    private fun updatesDirectory(): File =
        File(requireContext().cacheDir, "updates").apply { mkdirs() }

    private fun updateFile(fileName: String): File =
        File(updatesDirectory(), fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_"))

    fun canRequestPackageInstalls(): Boolean {
        val context = appContext ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                context.packageManager.canRequestPackageInstalls()
            } catch (_: SecurityException) {
            
                true
            }
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings() {
        val context = appContext ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun installDownloadedApk(path: String): Result<Unit> = runCatching {
        val context = requireContext()
        val apkFile = File(path)
        check(apkFile.exists()) { runBlocking { getString(Res.string.updates_downloaded_file_missing) } }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    private fun preferences() = requireContext().getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private const val PartialSuffix = ".part"
    private const val HttpPartialContent = 206
    private const val HttpRangeNotSatisfiable = 416
    private const val BetaPackageSuffix = ".debug"

    private fun requireContext(): Context =
        requireNotNull(appContext) { "AndroidAppUpdaterPlatform.initialize must be called before use." }
}
