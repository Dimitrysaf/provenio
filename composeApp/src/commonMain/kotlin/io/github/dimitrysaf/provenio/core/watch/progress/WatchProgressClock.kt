package io.github.dimitrysaf.provenio.core.watch.progress

internal expect object WatchProgressClock {
    fun nowEpochMs(): Long
}
