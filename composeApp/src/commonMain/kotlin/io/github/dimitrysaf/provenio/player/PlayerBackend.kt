package io.github.dimitrysaf.provenio.player

import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.player_backend_builtin
import io.github.dimitrysaf.provenio.resources.player_backend_builtin_summary
import io.github.dimitrysaf.provenio.resources.player_backend_external
import io.github.dimitrysaf.provenio.resources.player_backend_external_summary
import org.jetbrains.compose.resources.StringResource

/**
 * A player the app can hand a URL to.
 *
 * Each entry has to be a general purpose player. Choosing a backend per container or per
 * codec would push that decision onto the user, who has no way of knowing what a stream
 * contains before it opens.
 */
enum class PlayerBackend(val label: StringResource, val summary: StringResource) {
    Builtin(Res.string.player_backend_builtin, Res.string.player_backend_builtin_summary),
    External(Res.string.player_backend_external, Res.string.player_backend_external_summary),
}

/** Backends this platform can actually use. */
expect fun availablePlayerBackends(): List<PlayerBackend>
