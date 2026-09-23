package com.nuvio.app.core.watch.progress

internal expect object WatchProgressClock {
    fun nowEpochMs(): Long
}
