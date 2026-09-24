package io.github.dimitrysaf.provenio.core.library

internal expect object LibraryDisplaySettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
