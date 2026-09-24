package io.github.dimitrysaf.provenio.core.search

import android.content.Context
import android.content.SharedPreferences
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey

actual object SearchHistoryStorage {
    private const val preferencesName = "provenio_search_history"
    private const val payloadKey = "search_history_payload"
    private const val enabledKey = "recent_searches_enabled"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(payloadKey), null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(payloadKey), payload)
            ?.apply()
    }

    actual fun loadEnabled(): Boolean? =
        preferences?.let { prefs ->
            val key = ProfileScopedKey.of(enabledKey)
            if (prefs.contains(key)) prefs.getBoolean(key, true) else null
        }

    actual fun saveEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(enabledKey), enabled)
            ?.apply()
    }
}
