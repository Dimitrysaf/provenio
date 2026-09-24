package io.github.dimitrysaf.provenio.core.profiles

internal expect object AvatarStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}