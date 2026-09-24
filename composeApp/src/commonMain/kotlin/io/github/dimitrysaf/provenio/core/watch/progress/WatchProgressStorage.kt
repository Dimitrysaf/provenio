package io.github.dimitrysaf.provenio.core.watch.progress

internal expect object WatchProgressStorage {
    fun loadPayload(profileId: Int): String?
    fun savePayload(profileId: Int, payload: String)
}
