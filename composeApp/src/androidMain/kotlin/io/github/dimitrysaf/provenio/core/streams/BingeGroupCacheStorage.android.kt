package io.github.dimitrysaf.provenio.core.streams

import android.content.Context
import android.content.SharedPreferences
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey

actual object BingeGroupCacheStorage {
    private const val preferencesName = "provenio_binge_group_cache"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun load(hashedKey: String): String? =
        preferences?.getString(ProfileScopedKey.of(hashedKey), null)

    actual fun save(hashedKey: String, value: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(hashedKey), value)
            ?.apply()
    }

    actual fun remove(hashedKey: String) {
        preferences
            ?.edit()
            ?.remove(ProfileScopedKey.of(hashedKey))
            ?.apply()
    }
}
