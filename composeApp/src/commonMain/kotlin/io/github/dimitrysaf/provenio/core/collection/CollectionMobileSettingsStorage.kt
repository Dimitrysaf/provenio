package io.github.dimitrysaf.provenio.core.collection

internal expect object CollectionMobileSettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
