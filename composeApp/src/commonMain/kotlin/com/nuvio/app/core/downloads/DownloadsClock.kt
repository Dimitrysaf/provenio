package com.nuvio.app.core.downloads

internal expect object DownloadsClock {
    fun nowEpochMs(): Long
}
