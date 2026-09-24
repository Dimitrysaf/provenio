package io.github.dimitrysaf.provenio.core.downloads

internal expect object DownloadsLiveStatusPlatform {
    fun onItemsChanged(items: List<DownloadItem>)
}
