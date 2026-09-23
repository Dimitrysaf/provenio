package com.nuvio.app.core.downloads

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

internal actual object DownloadsClock {
    @OptIn(ExperimentalForeignApi::class)
    actual fun nowEpochMs(): Long = time(null) * 1000L
}
