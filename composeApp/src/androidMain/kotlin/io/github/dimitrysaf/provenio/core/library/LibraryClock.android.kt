package io.github.dimitrysaf.provenio.core.library

actual object LibraryClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}
