package io.github.dimitrysaf.provenio.core.watch.progress

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences

actual object ContinueWatchingEnrichmentStorage {
    private const val preferencesName = "provenio_cw_enrichment"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(key: String): String? =
        preferences?.getString(key, null)

    actual fun savePayload(key: String, payload: String) {
        preferences
            ?.edit()
            ?.putString(key, payload)
            ?.apply()
    }

    actual fun removePayload(key: String) {
        preferences
            ?.edit()
            ?.remove(key)
            ?.apply()
    }
}
