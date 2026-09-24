package io.github.dimitrysaf.provenio.core.watch.watched

expect object WatchedClock {
    fun nowEpochMs(): Long
}

