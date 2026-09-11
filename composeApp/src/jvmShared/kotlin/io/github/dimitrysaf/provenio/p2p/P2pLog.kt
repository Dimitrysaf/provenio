package io.github.dimitrysaf.provenio.p2p

import io.github.dimitrysaf.provenio.core.platform.logDebug

/**
 * Peer-to-peer diagnostics, under one tag so they can be found in a wall of logcat.
 *
 * Every line starts with "provenio-p2p" whether or not the reader can filter, because the
 * person reading it may be scrolling a log viewer on the same phone that is running this.
 */
internal fun p2pLog(message: String) {
    logDebug(P2pLogTag, message)
}

const val P2pLogTag = "provenio-p2p"
