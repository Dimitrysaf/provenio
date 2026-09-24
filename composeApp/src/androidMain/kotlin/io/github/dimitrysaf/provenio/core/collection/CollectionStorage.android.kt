package io.github.dimitrysaf.provenio.core.collection

import android.content.Context
import android.content.SharedPreferences
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey

actual object CollectionStorage {
    private const val preferencesName = "provenio_collections"
    private const val payloadKey = "collections_payload"

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
}
