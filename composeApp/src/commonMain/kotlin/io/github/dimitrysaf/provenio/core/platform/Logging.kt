package io.github.dimitrysaf.provenio.core.platform

/**
 * A debug line, on whatever this platform calls its log.
 *
 * Not println: Android does not reliably forward stdout to logcat, so anything logged that
 * way can vanish exactly when it is needed.
 */
expect fun logDebug(tag: String, message: String)
