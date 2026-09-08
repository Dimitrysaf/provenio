package io.github.dimitrysaf.provenio.p2p

import io.github.dimitrysaf.provenio.data.P2pStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns peer-to-peer configuration and the engine's lifecycle.
 *
 * Nothing starts on its own: the engine runs only while the user has both accepted the
 * consent notice and switched the service on, and any change that turns either off stops
 * it again.
 */
object P2pRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine: P2pEngine = createP2pEngine()

    private val _settings = MutableStateFlow(P2pSettings())
    val settings: StateFlow<P2pSettings> = _settings.asStateFlow()

    val status: StateFlow<P2pStatus> get() = engine.status

    private var store: P2pStore? = null
    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { P2pStore(createDatabaseDriver()) }.getOrNull()
        store?.let { existing ->
            runCatching { existing.load() }.getOrNull()?.let { _settings.value = it }
        }
        if (_settings.value.canRun) start()
    }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun acceptConsent() = update { it.copy(consentAccepted = true) }

    /** Revoking consent also stops the service — consent is what permits it to run. */
    fun revokeConsent() = update { it.copy(consentAccepted = false, enabled = false) }

    fun setUploadEnabled(enabled: Boolean) = update { it.copy(uploadEnabled = enabled) }

    fun setProfile(profile: TorrentProfile) = update { it.copy(profile = profile) }

    fun setCacheSize(size: CacheSize) = update { it.copy(cacheSize = size) }

    fun setHideStats(hide: Boolean) = update { it.copy(hideStats = hide) }

    fun clearCache() {
        scope.launch { engine.clearCache() }
    }

    private fun update(transform: (P2pSettings) -> P2pSettings) {
        val previous = _settings.value
        val next = transform(previous)
        if (next == previous) return
        _settings.value = next
        store?.let { runCatching { it.save(next) } }

        when {
            !next.canRun && previous.canRun -> stop()
            next.canRun && !previous.canRun -> start()
            // Already running and something it depends on changed: restart to apply it.
            next.canRun -> restart()
        }
    }

    private fun start() {
        scope.launch { engine.start(_settings.value) }
    }

    private fun stop() {
        scope.launch { engine.stop() }
    }

    private fun restart() {
        scope.launch {
            engine.stop()
            engine.start(_settings.value)
        }
    }
}

/** The service may run only when the user has both consented and switched it on. */
val P2pSettings.canRun: Boolean get() = enabled && consentAccepted
