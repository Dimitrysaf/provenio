package io.github.dimitrysaf.provenio.p2p

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap

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

            val magnet = magnetUriOf(request)
            p2pLog("fetching metadata for $key (up to ${MetadataTimeoutSeconds}s)")
            p2pLog("magnet: $magnet")

            val startedAt = System.currentTimeMillis()
            val data = runCatching {
                session.fetchMagnet(magnet, MetadataTimeoutSeconds, cacheDir)
            }.onFailure { p2pLog("fetchMagnet threw: $it") }.getOrNull()
            val elapsed = (System.currentTimeMillis() - startedAt) / 1000

            if (data == null) {
                p2pLog("no metadata after ${elapsed}s — no seeds answered, or the magnet is bad")
                return@withContext null
            }
            p2pLog("metadata: ${data.size} bytes in ${elapsed}s")

            val info = runCatching { TorrentInfo.bdecode(data) }
                .onFailure { p2pLog("could not decode metadata: $it") }
                .getOrNull() ?: return@withContext null

            p2pLog(
                "torrent \"${info.name()}\": " +
                    "files=${info.numFiles()} pieces=${info.numPieces()}",
            )
            session.download(info, cacheDir)

            val handle = awaitHandle(info.infoHash())
            if (handle == null) {
                p2pLog("torrent added but no handle came back within ${HandleTimeoutMillis}ms")
                return@withContext null
            }

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
        return pack
    }

    private companion object {
        const val MetadataTimeoutSeconds = 60
        const val UnlimitedRate = 0
        const val MinimalUploadRate = 1
        const val HandleTimeoutMillis = 15_000L
        const val HandlePollMillis = 50L
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
