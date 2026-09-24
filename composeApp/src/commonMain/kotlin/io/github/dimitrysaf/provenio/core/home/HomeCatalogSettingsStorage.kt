package io.github.dimitrysaf.provenio.core.home

internal expect object HomeCatalogSettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
