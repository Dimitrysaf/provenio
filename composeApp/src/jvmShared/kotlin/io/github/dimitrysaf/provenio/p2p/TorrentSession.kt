package io.github.dimitrysaf.provenio.p2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.settings_pack.bool_types as bools
import org.libtorrent4j.swig.settings_pack.int_types as ints
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** One reading of the whole session, taken on a timer while the engine is up. */
data class TorrentSample(
    val peers: Int = 0,
    val seeds: Int = 0,
    val downloadBytesPerSecond: Long = 0,
    val uploadBytesPerSecond: Long = 0,
    val downloadedBytes: Long = 0,
    val uploadedBytes: Long = 0,
    val activeTorrents: Int = 0,
    /** How much of the file being streamed has arrived, 0..1. */
    val progress: Float = 0f,
)

/**
 * The libtorrent session, and the torrents currently being streamed from it.
 *
 * Nothing here is started on its own — [P2pRepository] decides when the engine may run,
 * and this only does what it is told. What it does own is the mapping from an addon's info
 * hash to a live torrent, because the player asks for a URL long before it asks for bytes
 * and the two have to find each other again in between.
 */
class TorrentSession(private val cacheDir: File) {

    private val session = SessionManager()

    /** Info hash to the magnet it came from, so a stream can be opened on first request. */
    private val requests = ConcurrentHashMap<String, TorrentRequest>()

    /** Info hash to the torrent being read, once metadata has arrived. */
    private val streams = ConcurrentHashMap<String, StreamingTorrent>()

    /** Guards the add-a-torrent path, which must not run twice for the same info hash. */
    private val lock = Any()

    /** The settings the session is currently running under. */
    @Volatile
    private var current: P2pSettings = P2pSettings()

    val isRunning: Boolean get() = session.isRunning

    fun start(settings: P2pSettings) {
        if (session.isRunning) return
        current = settings
        cacheDir.mkdirs()
        p2pLog(
            "session starting: port=${settings.listenPort} profile=${settings.profile} " +
                "upload=${settings.uploadEnabled} cache=${settings.cacheSize} dir=$cacheDir",
        )
        session.start(SessionParams(settingsFor(settings)))
        p2pLog("session started: running=${session.isRunning}")
    }

    fun stop() {
        p2pLog("session stopping")
        streams.values.forEach { it.release() }
        streams.clear()
        requests.clear()
        if (session.isRunning) session.stop()
    }

    /** Re-applies settings that can change without tearing the session down. */
    fun apply(settings: P2pSettings) {
        current = settings
        if (session.isRunning) session.applySettings(settingsFor(settings))
    }

    /**
     * Remembers a torrent so [open] can find it later, and answers with the key the URL
     * is built from.
     *
     * Deliberately does no network work: this runs while the user is still looking at the
     * sources sheet, and fetching metadata here would freeze that sheet for as long as the
     * swarm takes to answer.
     */
    fun register(request: TorrentRequest): String {
        val key = request.infoHash.trim().lowercase()
        requests[key] = request.copy(infoHash = key)
        val trackers = request.sources.count { it.startsWith("tracker:") }
        p2pLog(
            "registered $key: trackers=$trackers of ${request.sources.size} sources, " +
                "fileIndex=${request.fileIndex}",
        )
        return key
    }

    /**
     * The torrent for [key], fetching its metadata on first use.
     *
     * Blocking, and deliberately so — it is called from a request handler that has nothing
     * to do until the metadata lands, and the player is already waiting on the response.
     */
    suspend fun open(key: String): StreamingTorrent? = withContext(Dispatchers.IO) {
        streams[key]?.let { return@withContext it }
        val request = requests[key] ?: return@withContext null
        if (!session.isRunning) return@withContext null

        // Two range requests for the same torrent almost always arrive together, and
        // adding it twice would fetch the metadata twice. Nothing inside suspends — the
        // libtorrent calls block — so holding a plain lock across them is safe.
        synchronized(lock) {
            streams[key]?.let { return@withContext it }

            evictTo(current.cacheSize.bytes)

            val hash = runCatching { Sha1Hash.parseHex(key) }
                .onFailure { p2pLog("not a usable info hash: $key") }
                .getOrNull() ?: return@withContext null

            val magnet = magnetUriOf(request)
            p2pLog("adding $key (dht ${dhtState()})")
            p2pLog("magnet: $magnet")

            // Added straight to the session rather than through fetchMagnet, which adds the
            // torrent in upload mode, waits for metadata and then removes it again. That
            // threw away every peer it had found, so a second attempt started from nothing
            // — and a player that gives up and reconnects is exactly when the peers already
            // found are worth the most. Added this way the torrent stays, and a retry
            // continues where the last one got to.
            session.download(magnet, cacheDir, TorrentFlags.SEQUENTIAL_DOWNLOAD)

            val handle = awaitHandle(hash)
            if (handle == null) {
                p2pLog("torrent added but no handle came back within ${HandleTimeoutMillis}ms")
                return@withContext null
            }

            val info = awaitMetadata(handle) ?: return@withContext null
            p2pLog(
                "torrent \"${info.name()}\": " +
                    "files=${info.numFiles()} pieces=${info.numPieces()}",
            )

            val stream = StreamingTorrent(
                handle = handle,
                info = info,
                savePath = cacheDir.absolutePath,
                preferredFileIndex = request.fileIndex,
            )
            stream.prepare()
            p2pLog("streaming \"${stream.fileName}\" (${stream.length} bytes)")
            streams[key] = stream
            stream
        }
    }

    /**
     * Waits for a torrent to actually be in the session.
     *
     * [SessionManager.download] is `async_add_torrent` underneath, so the torrent is not
     * there the instant it returns. Asking for the handle straight away almost always
     * comes back empty, which looked exactly like a torrent nobody was seeding.
     */
    private fun awaitHandle(hash: Sha1Hash): TorrentHandle? {
        val deadline = System.currentTimeMillis() + HandleTimeoutMillis
        while (System.currentTimeMillis() < deadline) {
            val handle = session.find(hash)
            if (handle != null && handle.isValid) return handle
            Thread.sleep(HandlePollMillis)
        }
        return null
    }

    /**
     * Waits for the swarm to hand over the torrent's file list.
     *
     * A magnet is only an info hash: what the files are called and how the pieces are laid
     * out has to come from a peer before anything can be read. Progress is logged on the
     * way, because "nothing is playing" has two very different causes — no route to the
     * network at all, or a route and no seeds — and the DHT and peer counts tell them
     * apart at a glance.
     */
    private fun awaitMetadata(handle: TorrentHandle): TorrentInfo? {
        val startedAt = System.currentTimeMillis()
        val deadline = startedAt + MetadataTimeoutMillis
        var nextReport = startedAt + ReportIntervalMillis

        while (System.currentTimeMillis() < deadline && handle.isValid) {
            val status = handle.status()
            if (status.hasMetadata()) {
                val info = handle.torrentFile()
                if (info != null) {
                    val waited = (System.currentTimeMillis() - startedAt) / 1000
                    p2pLog("metadata in ${waited}s from ${status.numPeers()} peers")
                    return info
                }
            }

            val now = System.currentTimeMillis()
            if (now >= nextReport) {
                nextReport = now + ReportIntervalMillis
                p2pLog(
                    "looking for peers ${(now - startedAt) / 1000}s in: dht=${dhtState()} " +
                        "peers=${status.numPeers()} seeds=${status.numSeeds()} " +
                        "known=${status.listPeers()} state=${status.state()}",
                )
            }
            Thread.sleep(MetadataPollMillis)
        }

        p2pLog(
            "no metadata after ${MetadataTimeoutMillis / 1000}s — dht=${dhtState()}, " +
                "nobody with this torrent answered",
        )
        return null
    }

    /** Whether the DHT is up, and how many nodes it knows — zero nodes means no route out. */
    private fun dhtState(): String =
        if (session.isDhtRunning) "${session.dhtNodes()} nodes" else "off"

    /** Bytes currently held on disk by torrent data. */
    fun cacheBytes(): Long = cacheDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    /** Drops every cached piece, and returns how much was reclaimed. */
    fun clearCache(): Long {
        val before = cacheBytes()
        streams.values.forEach { it.release() }
        streams.clear()
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        cacheDir.mkdirs()
        return before - cacheBytes()
    }

    /**
     * Keeps the cache under its limit by deleting the least recently touched entries
     * first. A limit of zero means the user asked for no caching at all.
     */
    private fun evictTo(limitBytes: Long) {
        val entries = cacheDir.listFiles()?.toMutableList() ?: return
        var total = cacheBytes()
        if (total <= limitBytes) return

        entries.sortBy { it.lastModified() }
        for (entry in entries) {
            if (total <= limitBytes) break
            val size = entry.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            if (entry.deleteRecursively()) total -= size
        }
    }

    private fun settingsFor(settings: P2pSettings): SettingsPack {
        val pack = SettingsPack()

        // 0 is "ask the operating system for a free port", which is also libtorrent's
        // default, so it is left alone rather than pinned to something arbitrary.
        if (settings.listenPort != 0) {
            pack.listenInterfaces("0.0.0.0:${settings.listenPort},[::]:${settings.listenPort}")
        }

        // libtorrent reads a rate limit of 0 as unlimited, so switching uploading off is
        // the smallest non-zero rate rather than zero. A swarm still needs the occasional
        // byte back or peers stop answering.
        pack.uploadRateLimit(if (settings.uploadEnabled) UnlimitedRate else MinimalUploadRate)

        pack.connectionsLimit(settings.profile.connections)
        pack.activeDownloads(settings.profile.activeDownloads)

        // Every way of finding a peer, all at once. The default is to walk tracker tiers in
        // order and stop at the first that answers, which is the polite thing to do for a
        // download that can take all day and the wrong thing for someone waiting on a
        // video to start. Local discovery costs one multicast packet and finds the other
        // device on the same wifi instantly when it is there.
        pack.setBoolean(bools.announce_to_all_trackers.swigValue(), true)
        pack.setBoolean(bools.announce_to_all_tiers.swigValue(), true)
        pack.setBoolean(bools.prefer_udp_trackers.swigValue(), true)
        pack.setBoolean(bools.enable_dht.swigValue(), true)
        pack.setBoolean(bools.enable_lsd.swigValue(), true)

        // Tuned for reading rather than collecting. A whole-pieces threshold means peers
        // send complete pieces instead of scattered blocks, so a piece finishes when it is
        // asked for rather than eventually; the short timeouts drop a peer that has gone
        // quiet fast, because the read head is waiting on it and a stalled request is
        // worse than no request.
        pack.setInteger(ints.whole_pieces_threshold.swigValue(), WholePiecesThresholdSeconds)
        pack.setInteger(ints.max_out_request_queue.swigValue(), MaxOutRequestQueue)
        pack.setInteger(ints.request_timeout.swigValue(), RequestTimeoutSeconds)
        pack.setInteger(ints.peer_connect_timeout.swigValue(), PeerConnectTimeoutSeconds)
        pack.setInteger(ints.connection_speed.swigValue(), settings.profile.connectionSpeed)
        return pack
    }

    /** A reading of the session as a whole, for the status the settings screen shows. */
    fun sample(): TorrentSample {
        if (!session.isRunning) return TorrentSample()
        val live = streams.keys.mapNotNull { key ->
            runCatching { Sha1Hash.parseHex(key) }.getOrNull()
                ?.let { session.find(it) }
                ?.takeIf { it.isValid }
                ?.status()
        }
        return TorrentSample(
            peers = live.sumOf { it.numPeers() },
            seeds = live.sumOf { it.numSeeds() },
            downloadBytesPerSecond = session.downloadRate(),
            uploadBytesPerSecond = session.uploadRate(),
            downloadedBytes = session.totalDownload(),
            uploadedBytes = session.totalUpload(),
            activeTorrents = live.size,
            // Progress of the wanted data, not of the torrent: every file but the one being
            // played is set to IGNORE, so this is how much of the video has arrived rather
            // than how much of the release.
            progress = live.firstOrNull()?.progress() ?: 0f,
        )
    }

    private companion object {
        const val UnlimitedRate = 0
        const val MinimalUploadRate = 1
        const val HandleTimeoutMillis = 15_000L
        const val HandlePollMillis = 50L
        /**
         * Deliberately shorter than the player's read timeout. The player is waiting on
         * this request, so giving up first lets the server answer with a real error the
         * user can be shown, rather than the player timing out on a silent socket.
         */
        const val MetadataTimeoutMillis = 45_000L
        const val MetadataPollMillis = 200L
        const val ReportIntervalMillis = 5_000L

        /** Ask peers for whole pieces when one is expected within this many seconds. */
        const val WholePiecesThresholdSeconds = 20
        const val MaxOutRequestQueue = 1_500
        const val RequestTimeoutSeconds = 10
        const val PeerConnectTimeoutSeconds = 4
    }
}

/** How hard to work the network. More connections find peers faster and cost more battery. */
private val TorrentProfile.connections: Int
    get() = when (this) {
        TorrentProfile.Slow -> 40
        TorrentProfile.Balanced -> 120
        TorrentProfile.Fast -> 300
    }

private val TorrentProfile.activeDownloads: Int
    get() = when (this) {
        TorrentProfile.Slow -> 1
        TorrentProfile.Balanced -> 3
        TorrentProfile.Fast -> 6
    }

/**
 * How many peers to reach out to per second.
 *
 * libtorrent's default of 10 is paced for a download nobody is waiting on. Opening more at
 * once is how a stream finds someone to read from in the first few seconds, at the cost of
 * a burst of radio traffic — which is exactly the trade the profile is there to make.
 */
private val TorrentProfile.connectionSpeed: Int
    get() = when (this) {
        TorrentProfile.Slow -> 10
        TorrentProfile.Balanced -> 30
        TorrentProfile.Fast -> 80
    }
