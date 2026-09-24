package io.github.dimitrysaf.provenio.core.metadata

internal expect object MetaScreenSettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}