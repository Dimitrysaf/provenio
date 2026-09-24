package io.github.dimitrysaf.provenio.core.tracking.simkl

internal expect object SimklSyncStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
    fun removeProfile(profileId: Int)
}
