package io.github.dimitrysaf.provenio.core.watch.progress

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

actual object WatchProgressClock {
    @OptIn(ExperimentalForeignApi::class)
    actual fun nowEpochMs(): Long = time(null) * 1000L
}
