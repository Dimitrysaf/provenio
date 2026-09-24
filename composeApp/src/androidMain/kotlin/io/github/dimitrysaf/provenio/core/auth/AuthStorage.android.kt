package io.github.dimitrysaf.provenio.core.auth

import android.content.Context
import android.content.SharedPreferences

actual object AuthStorage {
    private const val PREFS_NAME = "provenio_auth"
    private const val KEY_ANONYMOUS_USER_ID = "anonymous_user_id"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadAnonymousUserId(): String? =
        preferences?.getString(KEY_ANONYMOUS_USER_ID, null)

    actual fun saveAnonymousUserId(userId: String) {
        preferences?.edit()?.putString(KEY_ANONYMOUS_USER_ID, userId)?.apply()
    }

    actual fun clearAnonymousUserId() {
        preferences?.edit()?.remove(KEY_ANONYMOUS_USER_ID)?.apply()
    }
}
