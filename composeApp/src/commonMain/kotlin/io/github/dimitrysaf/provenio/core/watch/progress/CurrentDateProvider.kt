package io.github.dimitrysaf.provenio.core.watch.progress

expect object CurrentDateProvider {
    fun todayIsoDate(): String
    fun localStartOfDayEpochMs(isoDate: String): Long?
}
