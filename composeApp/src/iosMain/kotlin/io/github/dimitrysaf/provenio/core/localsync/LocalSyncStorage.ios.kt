package io.github.dimitrysaf.provenio.core.localsync

import platform.Foundation.NSUserDefaults

internal actual object LocalSyncStorage {
    private const val peersKey = "local_sync_peers"
    private fun ledgerKey(profileId: Int) = "local_sync_ledger_$profileId"

    actual fun loadLedger(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ledgerKey(profileId))

    actual fun saveLedger(profileId: Int, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ledgerKey(profileId))
    }

    actual fun loadPeers(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(peersKey)

    actual fun savePeers(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = peersKey)
    }
}
