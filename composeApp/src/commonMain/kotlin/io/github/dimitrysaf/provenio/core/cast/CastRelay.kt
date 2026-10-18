package io.github.dimitrysaf.provenio.core.cast

/**
 * Hands a stream that only this device can reach, such as a torrent served by the built-in
 * engine, to a cast device on the local network. The cast device asks the relay for each piece
 * of the video, and the relay fetches it from the stream here and passes it on.
 */
internal expect object CastRelay {
    /**
     * Starts relaying [targetUrl] and returns the address a cast device can open, ending in
     * [fileName] so the device can tell the format. Null when there is no local network address.
     */
    fun start(targetUrl: String, fileName: String): String?

    /** Stops relaying and closes the address handed out. */
    fun stop()

    /** Whether a VPN is up, which can hide cast devices or keep them from reaching this device. */
    fun vpnActive(): Boolean
}
