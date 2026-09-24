package io.github.dimitrysaf.provenio.core.library

internal expect object LibraryClock {
    fun nowEpochMs(): Long
}
