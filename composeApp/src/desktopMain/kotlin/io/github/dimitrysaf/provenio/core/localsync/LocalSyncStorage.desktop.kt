package io.github.dimitrysaf.provenio.core.localsync

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences

internal actual object LocalSyncStorage {
    private const val preferencesName = "provenio_local_sync"
    private const val peersKey = "peers"
    private fun ledgerKey(profileId: Int) = "ledger_$profileId"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadLedger(profileId: Int): String? =
        preferences?.getString(ledgerKey(profileId), null)

    actual fun saveLedger(profileId: Int, payload: String) {
        preferences?.edit()?.putString(ledgerKey(profileId), payload)?.apply()
    }

    actual fun loadPeers(): String? =
        preferences?.getString(peersKey, null)

    actual fun savePeers(payload: String) {
        preferences?.edit()?.putString(peersKey, payload)?.apply()
    }
}
