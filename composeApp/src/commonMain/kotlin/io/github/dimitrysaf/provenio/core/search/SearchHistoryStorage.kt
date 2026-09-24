package io.github.dimitrysaf.provenio.core.search

internal expect object SearchHistoryStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
}
