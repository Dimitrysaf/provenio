package io.github.dimitrysaf.provenio.p2p

import java.net.URLEncoder

/**
 * A magnet URI built from what an addon told us, plus the trackers every torrent client
 * falls back on.
 *
 * The fallbacks are not optional. An addon that hands over an info hash and nothing else
 * leaves the DHT as the only way to find the swarm, and a cold DHT can take longer to
 * answer than anyone is willing to stare at a buffering spinner — if it answers at all on a
 * carrier network. A tracker answers in one round trip. Announcing to a tracker that does
 * not carry this torrent costs one packet and nothing else, so the whole list goes on every
 * magnet and the ones that have peers are the ones that reply.
 */
internal fun magnetUriOf(request: TorrentRequest): String = buildString {
    append("magnet:?xt=urn:btih:")
    append(request.infoHash.trim().lowercase())

    // Stremio's "sources" mixes tracker entries with dht ones. Only the trackers are ours
    // to pass along; a dht entry just repeats the info hash.
    val fromAddon = request.sources
        .filter { it.startsWith(TrackerPrefix) }
        .map { it.removePrefix(TrackerPrefix).trim() }

    for (tracker in (fromAddon + FallbackTrackers).filter { it.isNotEmpty() }.distinct()) {
        append("&tr=")
        append(URLEncoder.encode(tracker, Charsets.UTF_8.name()))
    }
}

private const val TrackerPrefix = "tracker:"

/**
 * Public trackers, open to any info hash.
 *
 * UDP first because it is one packet out and one back. The HTTP entry at the end is the
 * exception that earns its place: on a network that blocks UDP outright the DHT is dead too
 * and this is the only thing left that can find a peer.
 */
private val FallbackTrackers = listOf(
    "udp://zer0day.ch:1337/announce",
    "udp://tracker.publictracker.xyz:6969/announce",
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://open.demonii.com:1337/announce",
    "udp://open.stealth.si:80/announce",
    "http://tracker.renfei.net:8080/announce",
    "udp://udp.tracker.projectk.org:23333/announce",
    "udp://tracker.tryhackx.org:6969/announce",
    "udp://tracker.torrent.eu.org:451/announce",
    "udp://tracker.theoks.net:6969/announce",
    "udp://tracker.startwork.cv:1337/announce",
    "udp://tracker.qu.ax:6969/announce",
    "udp://tracker.plx.im:6969/announce",
    "udp://tracker.nyaa.vc:6969/announce",
    "udp://tracker.iperson.xyz:6969/announce",
    "udp://tracker.gmi.gd:6969/announce",
    "udp://tracker.fnix.net:6969/announce",
    "udp://tracker.flatuslifir.is:6969/announce",
    "udp://tracker.ducks.party:1984/announce",
    "udp://tracker.bluefrog.pw:2710/announce",
)
