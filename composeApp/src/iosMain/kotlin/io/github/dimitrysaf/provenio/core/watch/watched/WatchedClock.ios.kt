package io.github.dimitrysaf.provenio.core.watch.watched

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

actual object WatchedClock {
    @OptIn(ExperimentalForeignApi::class)
    actual fun nowEpochMs(): Long = time(null) * 1000L
}

