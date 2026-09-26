package io.github.dimitrysaf.provenio.core.downloads

// The folder downloads are saved to, where the platform lets the user pick it.
internal expect object DownloadsLocationPlatform {
    val isConfigurable: Boolean

    fun currentPath(): String?

    // Asks the user for a folder; true when a new one was chosen.
    fun choose(): Boolean
}
