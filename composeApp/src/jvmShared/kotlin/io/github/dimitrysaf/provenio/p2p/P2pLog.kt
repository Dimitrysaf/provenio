package io.github.dimitrysaf.provenio.p2p

/**
 * Peer-to-peer diagnostics.
 *
 * Plain stdout rather than a logging framework: Android surfaces it in logcat under
 * System.out and the desktop prints it to the console, which is all that is wanted from
 * something whose whole job is to explain why a swarm did not answer.
 *
 * Read it with: adb logcat | grep provenio-p2p
 */
internal fun p2pLog(message: String) {
    println("provenio-p2p: $message")
}
