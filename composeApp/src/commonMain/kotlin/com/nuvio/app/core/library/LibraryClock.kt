package com.nuvio.app.core.library

internal expect object LibraryClock {
    fun nowEpochMs(): Long
}
