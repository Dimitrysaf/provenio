package io.github.dimitrysaf.provenio.core.tracking.trakt

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey

internal actual object TraktSettingsStorage {
    private const val preferencesName = "provenio_trakt_settings"
    private const val payloadKey = "trakt_settings_payload"
    private const val pendingWatchProgressSourceKey = "pending_watch_progress_source"

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

    actual fun loadPendingWatchProgressSourcePayload(profileId: Int): String? =
        preferences?.getString(ProfileScopedKey.of(pendingWatchProgressSourceKey, profileId), null)

    actual fun savePendingWatchProgressSourcePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(pendingWatchProgressSourceKey, profileId), payload)
            ?.commit()
    }

    actual fun clearPendingWatchProgressSourcePayload(profileId: Int) {
        preferences
            ?.edit()
            ?.remove(ProfileScopedKey.of(pendingWatchProgressSourceKey, profileId))
            ?.apply()
    }
}
