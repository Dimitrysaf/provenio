package io.github.dimitrysaf.provenio.core.downloads

internal actual object DownloadsLocationPlatform {
    actual val isConfigurable: Boolean = true

    actual fun currentPath(): String? = DesktopDownloadsLocation.directory().absolutePath

    actual fun choose(): Boolean = DesktopDownloadsLocation.choose()
}
