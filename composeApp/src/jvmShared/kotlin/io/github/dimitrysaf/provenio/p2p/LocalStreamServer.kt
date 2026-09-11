package io.github.dimitrysaf.provenio.p2p

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Loopback HTTP server the player reads from.
 *
 * Bound to 127.0.0.1 so nothing outside the device can reach it. Port 0 asks the OS for a
 * free port, which avoids clashing with whatever else is listening.
 *
 * Range requests are the whole point. A player seeking through a video asks for the bytes
 * around the new position and nothing else, so answering them properly is what turns a
 * torrent into something watchable rather than something to wait for.
 */
class LocalStreamServer(
    private val requestedPort: Int,
    private val torrents: TorrentSession,
) {
    private var server: EmbeddedServer<*, *>? = null

    var port: Int = 0
        private set

    suspend fun start(): Int {
        val engine = embeddedServer(CIO, port = requestedPort, host = LoopbackHost) {
            routing {
                get("/health") {
                    call.respondText("ok")
                }
                get("/stream/{key}") {
                    // Already open by the time a player asks: the metadata was fetched
                    // when the URL was handed out, so this is a map lookup and the
                    // response headers go out immediately.
                    val key = call.parameters["key"]
                    val stream = key?.let { torrents.open(it) }
                    if (stream == null) {
                        // The engine was stopped underneath us, or this URL outlived the
                        // session that issued it. Either way there is nothing to play.
                        call.respondText(
                            text = "No torrent for that id.",
                            status = HttpStatusCode.ServiceUnavailable,
                        )
                        return@get
                    }
                    serve(call, stream)
                }
            }
        }
        engine.start(wait = false)
        server = engine
        port = engine.engine.resolvedConnectors().first().port
        return port
    }

    fun stop() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 1000)
        server = null
        port = 0
    }

    fun urlFor(key: String): String = "http://$LoopbackHost:$port/stream/$key"

    private suspend fun serve(call: ApplicationCall, stream: StreamingTorrent) {
        val total = stream.length
        val requested = call.request.header(HttpHeaders.Range)?.let { parseRange(it, total) }
        val start = requested?.first ?: 0L
        val endInclusive = requested?.second ?: (total - 1)
        val count = endInclusive - start + 1

        if (start >= total || count <= 0) {
            call.response.header(HttpHeaders.ContentRange, "bytes */$total")
            call.respondText(
                text = "",
                status = HttpStatusCode.RequestedRangeNotSatisfiable,
            )
            return
        }

        // Advertised on every response, including the first one without a Range header —
        // it is how the player learns it is allowed to seek at all.
        call.response.header(HttpHeaders.AcceptRanges, "bytes")
        if (requested != null) {
            call.response.header(HttpHeaders.ContentRange, "bytes $start-$endInclusive/$total")
        }

        call.respondOutputStream(
            contentType = ContentType.Application.OctetStream,
            status = if (requested != null) HttpStatusCode.PartialContent else HttpStatusCode.OK,
            contentLength = count,
        ) {
            stream.writeRange(this, start, count)
        }
    }

    private companion object {
        const val LoopbackHost = "127.0.0.1"
    }
}

/**
 * The one Range form players actually send: `bytes=start-` or `bytes=start-end`.
 *
 * Multi-range requests are legal HTTP and no video player issues them, so anything that
 * is not a single range is treated as no range at all — which answers with the whole file
 * and is always correct, if not always what was asked for.
 */
internal fun parseRange(header: String, total: Long): Pair<Long, Long>? {
    val spec = header.removePrefix("bytes=").trim()
    if (spec.isEmpty() || spec.contains(',')) return null

    val start = spec.substringBefore('-').trim()
    val end = spec.substringAfter('-').trim()

    return when {
        // "bytes=-500" means the last 500 bytes, not a range starting at nothing.
        start.isEmpty() -> {
            val suffix = end.toLongOrNull() ?: return null
            val from = (total - suffix).coerceAtLeast(0)
            from to total - 1
        }
        else -> {
            val from = start.toLongOrNull() ?: return null
            val to = end.toLongOrNull() ?: (total - 1)
            from to minOf(to, total - 1)
        }
    }
}
