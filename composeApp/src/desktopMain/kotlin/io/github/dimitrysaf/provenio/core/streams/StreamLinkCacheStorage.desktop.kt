package io.github.dimitrysaf.provenio.core.streams

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey

actual object StreamLinkCacheStorage {
    private const val preferencesName = "provenio_stream_link_cache"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEntry(hashedKey: String): String? =
        preferences?.getString(ProfileScopedKey.of(hashedKey), null)

    actual fun saveEntry(hashedKey: String, payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(hashedKey), payload)
            ?.apply()
    }

    actual fun removeEntry(hashedKey: String) {
        preferences
            ?.edit()
            ?.remove(ProfileScopedKey.of(hashedKey))
            ?.apply()
    }
}
