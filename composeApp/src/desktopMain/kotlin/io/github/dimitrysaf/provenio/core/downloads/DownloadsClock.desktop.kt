package io.github.dimitrysaf.provenio.core.downloads

internal actual object DownloadsClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}
