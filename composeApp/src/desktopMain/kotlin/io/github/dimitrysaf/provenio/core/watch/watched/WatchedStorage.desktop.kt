package io.github.dimitrysaf.provenio.core.watch.watched

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences

actual object WatchedStorage {
    private const val preferencesName = "provenio_watched"
    private fun payloadKey(profileId: Int) = "watched_payload_$profileId"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString(payloadKey(profileId), null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey(profileId), payload)
            ?.apply()
    }
}

