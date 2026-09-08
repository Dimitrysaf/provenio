package io.github.dimitrysaf.provenio.p2p

import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Loopback HTTP server the player reads from.
 *
 * Bound to 127.0.0.1 so nothing outside the device can reach it. Port 0 asks the OS for a
 * free port, which avoids clashing with whatever else is listening.
 */
class LocalStreamServer(private val requestedPort: Int) {

    private var server: EmbeddedServer<*, *>? = null

    var port: Int = 0
        private set

    suspend fun start(): Int {
        val engine = embeddedServer(CIO, port = requestedPort, host = LoopbackHost) {
            routing {
                get("/health") {
                    call.respondText("ok")
                }
                get("/stream/{infoHash}") {
                    // The transport is not connected yet. Answering honestly beats
                    // returning a URL that stalls a player with no explanation.
                    call.respondText(
                        text = "Torrent transport is not connected in this build.",
                        status = HttpStatusCode.ServiceUnavailable,
                    )
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

    fun urlFor(infoHash: String): String = "http://$LoopbackHost:$port/stream/$infoHash"

    private companion object {
        const val LoopbackHost = "127.0.0.1"
    }
}
