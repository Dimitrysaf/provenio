package io.github.dimitrysaf.provenio.core.p2p

// The newest player screen owns the engine; an older one leaving after a newer one opened must not stop the newer one's stream.
internal object P2pPlaybackOwner {
    private var latest = 0L

    fun claim(): Long = ++latest

    // True when the leaving screen is still the newest, so the engine is its to stop.
    fun release(owner: Long): Boolean = owner == latest
}
