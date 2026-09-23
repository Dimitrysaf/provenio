package com.nuvio.app.core.library

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

actual object LibraryClock {
    @OptIn(ExperimentalForeignApi::class)
    actual fun nowEpochMs(): Long = time(null) * 1000L
}
