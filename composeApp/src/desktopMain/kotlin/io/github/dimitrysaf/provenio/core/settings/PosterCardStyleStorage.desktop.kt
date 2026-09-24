package io.github.dimitrysaf.provenio.core.settings

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey

actual object PosterCardStyleStorage {
    private const val preferencesName = "provenio_poster_card_style"
    private const val payloadKey = "poster_card_style_payload"

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
