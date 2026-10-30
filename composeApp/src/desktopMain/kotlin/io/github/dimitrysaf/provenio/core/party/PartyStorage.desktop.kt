package io.github.dimitrysaf.provenio.core.party

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences

internal actual object PartyStorage {
    private const val preferencesName = "provenio_watch_party"
    private const val settingsKey = "settings"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadSettings(): String? =
        preferences?.getString(settingsKey, null)

    actual fun saveSettings(payload: String) {
        preferences?.edit()?.putString(settingsKey, payload)?.apply()
    }
}
