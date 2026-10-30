package io.github.dimitrysaf.provenio.core.party

import platform.Foundation.NSUserDefaults

internal actual object PartyStorage {
    private const val settingsKey = "watch_party_settings"

    actual fun loadSettings(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(settingsKey)

    actual fun saveSettings(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = settingsKey)
    }
}
