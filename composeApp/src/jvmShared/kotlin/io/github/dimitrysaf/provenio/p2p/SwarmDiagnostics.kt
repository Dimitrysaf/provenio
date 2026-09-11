package io.github.dimitrysaf.provenio.p2p

import org.libtorrent4j.AlertListener
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.DhtReplyAlert
import org.libtorrent4j.alerts.ListenFailedAlert
import org.libtorrent4j.alerts.ListenSucceededAlert
import org.libtorrent4j.alerts.TrackerErrorAlert
import org.libtorrent4j.alerts.TrackerReplyAlert
import java.util.concurrent.atomic.AtomicInteger

/**
 * Why there are no peers.
 *
 * "No peers" has several causes that look identical from the outside: the socket never
 * bound, the trackers were never reached, they were reached and had nobody, or they had
 * somebody and the connections failed. Only libtorrent knows which, and it says so in
 * alerts — so this listens to the ones that carry the answer and keeps a tally that can be
 * printed next to the peer count.
 *
 * Counting rather than logging every alert is deliberate: twenty trackers announcing on a
 * timer would bury the log in seconds, and the number of replies is the part worth reading.
 * Failures are the exception and are printed, because the error text names the cause —
 * a timeout, a name that will not resolve, a refused connection.
 */
internal class SwarmDiagnostics : AlertListener {

    private val announces = AtomicInteger()
    private val replies = AtomicInteger()
    private val trackerPeers = AtomicInteger()
    private val errors = AtomicInteger()
    private val dhtReplies = AtomicInteger()
    private val dhtPeers = AtomicInteger()
    private val peerConnects = AtomicInteger()
    private val printedErrors = AtomicInteger()

    override fun types(): IntArray = intArrayOf(
        AlertType.LISTEN_SUCCEEDED.swig(),
        AlertType.LISTEN_FAILED.swig(),
        AlertType.TRACKER_ANNOUNCE.swig(),
        AlertType.TRACKER_REPLY.swig(),
        AlertType.TRACKER_ERROR.swig(),
        AlertType.DHT_BOOTSTRAP.swig(),
        AlertType.DHT_REPLY.swig(),
        AlertType.PEER_CONNECT.swig(),
    )

    override fun alert(alert: Alert<*>) {
        when (alert.type()) {
            // The one failure that explains everything else at once: no socket, no peers,
            // no matter how healthy the swarm is.
            AlertType.LISTEN_FAILED -> (alert as ListenFailedAlert).let {
                p2pLog(
                    "listen FAILED on ${it.listenInterface()} " +
                        "(${it.socketType()}): ${it.error().message}",
                )
            }
            AlertType.LISTEN_SUCCEEDED -> (alert as ListenSucceededAlert).let {
                p2pLog("listening on ${it.address()}:${it.port()} (${it.socketType()})")
            }
            AlertType.DHT_BOOTSTRAP -> p2pLog("dht bootstrap finished")

            AlertType.TRACKER_ANNOUNCE -> announces.incrementAndGet()
            AlertType.TRACKER_REPLY -> (alert as TrackerReplyAlert).let {
                replies.incrementAndGet()
                trackerPeers.addAndGet(it.numPeers())
            }
            AlertType.TRACKER_ERROR -> (alert as TrackerErrorAlert).let {
                errors.incrementAndGet()
                // Capped: with twenty trackers on a network that blocks them, every one
                // fails on every retry and the useful part is the first few messages.
                if (printedErrors.incrementAndGet() <= MaxPrintedErrors) {
                    val reason = it.errorMessage().ifBlank { it.error().message }
                    p2pLog("tracker failed: ${it.trackerUrl()} — $reason")
                }
            }
            AlertType.DHT_REPLY -> (alert as DhtReplyAlert).let {
                dhtReplies.incrementAndGet()
                dhtPeers.addAndGet(it.numPeers())
            }
            AlertType.PEER_CONNECT -> peerConnects.incrementAndGet()
            else -> Unit
        }
    }

    /** One line naming who has answered so far, for printing beside the peer count. */
    fun summary(): String =
        "announced=${announces.get()} replies=${replies.get()} " +
            "trackerPeers=${trackerPeers.get()} trackerErrors=${errors.get()} " +
            "dhtReplies=${dhtReplies.get()} dhtPeers=${dhtPeers.get()} " +
            "connects=${peerConnects.get()}"

    private companion object {
        const val MaxPrintedErrors = 8
    }
}
