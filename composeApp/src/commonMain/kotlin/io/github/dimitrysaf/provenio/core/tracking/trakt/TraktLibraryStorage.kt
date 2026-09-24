package io.github.dimitrysaf.provenio.core.tracking.trakt

internal expect object TraktLibraryStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}