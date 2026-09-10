package io.github.dimitrysaf.provenio.core.platform

/** Wall clock, used to throttle syncing and to expire cached responses. */
expect fun currentTimeMillis(): Long
