package io.github.dimitrysaf.provenio.core.downloads

internal actual object DownloadsLocationPlatform {
    actual val isConfigurable: Boolean = false

    actual fun currentPath(): String? = null

    actual fun choose(): Boolean = false
}
