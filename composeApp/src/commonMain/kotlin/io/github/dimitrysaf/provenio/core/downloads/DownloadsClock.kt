package io.github.dimitrysaf.provenio.core.downloads

internal expect object DownloadsClock {
    fun nowEpochMs(): Long
}
