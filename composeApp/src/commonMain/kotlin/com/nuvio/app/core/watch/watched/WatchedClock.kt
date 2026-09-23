package com.nuvio.app.core.watch.watched

expect object WatchedClock {
    fun nowEpochMs(): Long
}

