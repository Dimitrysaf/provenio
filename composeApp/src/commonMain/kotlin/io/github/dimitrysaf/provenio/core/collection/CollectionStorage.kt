package io.github.dimitrysaf.provenio.core.collection

internal expect object CollectionStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
