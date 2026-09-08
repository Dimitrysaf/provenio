package io.github.dimitrysaf.provenio.player

import io.github.dimitrysaf.provenio.data.PlayerStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which player the app hands URLs to. */
object PlayerRepository {

    private val _backend = MutableStateFlow(PlayerBackend.Builtin)
    val backend: StateFlow<PlayerBackend> = _backend.asStateFlow()

    private var store: PlayerStore? = null
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { PlayerStore(createDatabaseDriver()) }.getOrNull()
        store?.let { existing ->
            runCatching { existing.load() }.getOrNull()?.let { _backend.value = it }
        }
    }

    fun setBackend(backend: PlayerBackend) {
        _backend.value = backend
        store?.let { runCatching { it.save(backend) } }
    }
}
