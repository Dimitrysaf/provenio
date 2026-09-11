package io.github.dimitrysaf.provenio.p2p

import java.net.URLEncoder

/**
 * A magnet URI built from what an addon told us.
 *
 * Addons hand over an info hash and, usually, a list of trackers. Without at least one of
 * those two the swarm has to be found through the DHT alone, which works but is slow to
 * start — so every tracker offered is worth carrying.
 */
internal fun magnetUriOf(request: TorrentRequest): String = buildString {
    append("magnet:?xt=urn:btih:")
    append(request.infoHash.trim().lowercase())
    request.sources.forEach { entry ->
        // Stremio's "sources" mixes tracker entries with dht ones. Only the trackers are
        // ours to pass along; a dht entry just repeats the info hash.
        if (entry.startsWith(TrackerPrefix)) {
            append("&tr=")
            append(URLEncoder.encode(entry.removePrefix(TrackerPrefix), Charsets.UTF_8.name()))
        }
    }
}

private const val TrackerPrefix = "tracker:"
