package io.github.dimitrysaf.provenio.core.settings

import android.content.Context
import android.content.SharedPreferences

internal actual object SentrySettingsPlatform {
    actual val crashReportsSupported: Boolean = true
}

internal actual object SentrySettingsStorage {
    private const val preferencesName = "provenio_sentry_settings"
    private const val enabledKey = "enabled"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEnabled(): Boolean? =
        preferences?.let { prefs ->
            if (prefs.contains(enabledKey)) prefs.getBoolean(enabledKey, true) else null
        }

    actual fun saveEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(enabledKey, enabled)
            ?.apply()
    }
}
