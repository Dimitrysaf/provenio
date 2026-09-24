package io.github.dimitrysaf.provenio.core.watch.progress

internal expect object ContinueWatchingPreferencesStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
