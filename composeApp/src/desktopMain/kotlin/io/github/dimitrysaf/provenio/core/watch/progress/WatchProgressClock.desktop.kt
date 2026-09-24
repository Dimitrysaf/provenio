package io.github.dimitrysaf.provenio.core.watch.progress

actual object WatchProgressClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}
