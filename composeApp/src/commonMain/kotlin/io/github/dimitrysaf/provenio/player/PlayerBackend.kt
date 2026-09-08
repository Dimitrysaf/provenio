package io.github.dimitrysaf.provenio.player

/**
 * A player the app can hand a URL to.
 *
 * Each entry has to be a general purpose player. Choosing a backend per container or per
 * codec would push that decision onto the user, who has no way of knowing what a stream
 * contains before it opens.
 */
enum class PlayerBackend(val label: String, val summary: String) {
    Builtin("Built-in", "Hardware decoding for common formats"),
    External("External app", "Hand off to any player installed on the device"),
}

/** Backends this platform can actually use. */
expect fun availablePlayerBackends(): List<PlayerBackend>
