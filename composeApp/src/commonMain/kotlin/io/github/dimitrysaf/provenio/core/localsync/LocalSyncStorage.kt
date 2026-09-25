package io.github.dimitrysaf.provenio.core.localsync

internal expect object LocalSyncStorage {
    fun loadLedger(profileId: Int): String?
    fun saveLedger(profileId: Int, payload: String)
    fun loadPeers(): String?
    fun savePeers(payload: String)
}
