package io.github.dimitrysaf.provenio.core.party

/** The host's room settings, kept for the next room they open. */
internal expect object PartyStorage {
    fun loadSettings(): String?
    fun saveSettings(payload: String)
}
