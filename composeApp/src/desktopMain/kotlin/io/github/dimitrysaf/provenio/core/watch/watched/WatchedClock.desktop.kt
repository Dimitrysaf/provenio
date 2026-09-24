package io.github.dimitrysaf.provenio.core.watch.watched

actual object WatchedClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}

