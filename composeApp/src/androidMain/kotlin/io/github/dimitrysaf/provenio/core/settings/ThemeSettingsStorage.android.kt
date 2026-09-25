package io.github.dimitrysaf.provenio.core.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.dimitrysaf.provenio.core.sync.decodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.decodeSyncString
import io.github.dimitrysaf.provenio.core.sync.encodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.encodeSyncString
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual object ThemeSettingsStorage {
    private const val preferencesName = "provenio_theme_settings"
    private const val amoledEnabledKey = "amoled_enabled"
    private const val selectedAppLanguageKey = "selected_app_language"
    private val profileScopedSyncKeys = listOf(
        amoledEnabledKey,
    )

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        applySelectedAppLanguage(loadSelectedAppLanguage() ?: AppLanguage.DEVICE.code)
    }

    actual fun loadAmoledEnabled(): Boolean? =
        preferences?.let { prefs ->
            val key = ProfileScopedKey.of(amoledEnabledKey)
            if (prefs.contains(key)) prefs.getBoolean(key, false) else null
        }

    actual fun saveAmoledEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(amoledEnabledKey), enabled)
            ?.apply()
    }

    actual fun loadSelectedAppLanguage(): String? {
        val value = preferences?.getString(selectedAppLanguageKey, null)
        if (value != null) return value
        val legacy = preferences?.getString(ProfileScopedKey.of(selectedAppLanguageKey), null)
        if (legacy != null) saveSelectedAppLanguage(legacy)
        return legacy
    }

    actual fun saveSelectedAppLanguage(languageCode: String) {
        preferences
            ?.edit()
            ?.putString(selectedAppLanguageKey, languageCode)
            ?.apply()
    }

    actual fun applySelectedAppLanguage(languageCode: String) {
        if (languageCode.equals("device", ignoreCase = true)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode),
            )
        }
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadAmoledEnabled()?.let { put(amoledEnabledKey, encodeSyncBoolean(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        preferences?.edit()?.apply {
            profileScopedSyncKeys.forEach { remove(ProfileScopedKey.of(it)) }
        }?.apply()

        payload.decodeSyncBoolean(amoledEnabledKey)?.let(::saveAmoledEnabled)
        applySelectedAppLanguage(loadSelectedAppLanguage() ?: AppLanguage.DEVICE.code)
    }
}
