package io.github.dimitrysaf.provenio.core.auth

internal expect object AuthStorage {
    fun loadAnonymousUserId(): String?
    fun saveAnonymousUserId(userId: String)
    fun clearAnonymousUserId()
}
