package io.github.dimitrysaf.provenio.core.library

internal expect object LibraryStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
}
