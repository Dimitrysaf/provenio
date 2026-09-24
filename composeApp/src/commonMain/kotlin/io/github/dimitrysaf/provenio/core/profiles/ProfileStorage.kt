package io.github.dimitrysaf.provenio.core.profiles

internal expect object ProfileStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}