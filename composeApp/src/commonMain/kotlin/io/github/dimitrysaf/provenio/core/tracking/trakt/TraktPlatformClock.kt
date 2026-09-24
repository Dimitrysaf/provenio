package io.github.dimitrysaf.provenio.core.tracking.trakt

internal expect object TraktPlatformClock {
    fun nowEpochMs(): Long
    fun parseIsoDateTimeToEpochMs(value: String): Long?
    fun availableProcessors(): Int
}
