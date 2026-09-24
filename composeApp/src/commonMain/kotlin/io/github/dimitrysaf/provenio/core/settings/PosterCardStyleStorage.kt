package io.github.dimitrysaf.provenio.core.settings

internal expect object PosterCardStyleStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
