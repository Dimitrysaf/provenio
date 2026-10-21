package io.github.dimitrysaf.provenio.core.updater

expect object AppUpdaterPlatform {
    val isSupported: Boolean
    val isDebugBuild: Boolean

    fun getSupportedAbis(): List<String>

    fun getIgnoredTag(): String?

    fun setIgnoredTag(tag: String?)

    /**
     * Downloads the update, or reuses one already downloaded, and returns its path once it matches
     * [sha256]. [onVerifying] is called as each check of the file starts, [onProgress] as it
     * downloads.
     */
    suspend fun downloadApk(
        assetUrl: String,
        assetName: String,
        sha256: String?,
        onVerifying: () -> Unit,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
    ): Result<String>

    fun hasPartialUpdate(fileName: String): Boolean

    fun clearUpdateFiles(keepFileName: String?)

    fun canRequestPackageInstalls(): Boolean

    fun openUnknownSourcesSettings()

    fun installDownloadedApk(path: String): Result<Unit>
}
