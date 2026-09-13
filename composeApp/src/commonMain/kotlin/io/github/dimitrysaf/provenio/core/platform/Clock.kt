package io.github.dimitrysaf.provenio.core.platform

/** Wall clock, used to throttle syncing and to expire cached responses. */
expect fun currentTimeMillis(): Long

/**
 * Today, as `YYYY-MM-DD` — the same shape an addon's own `released` date comes in, so
 * telling whether an episode has aired yet is a plain string comparison rather than
 * something that needs a date-parsing dependency.
 */
expect fun todayIso(): String
