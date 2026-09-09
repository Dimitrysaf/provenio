package io.github.dimitrysaf.provenio.simkl

/** Wall clock, used only to throttle how often sync may run. */
expect fun currentTimeMillis(): Long
