package io.github.dimitrysaf.provenio.core.settings

import io.github.dimitrysaf.provenio.core.sync.decodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.decodeSyncString
import io.github.dimitrysaf.provenio.core.sync.encodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.encodeSyncString
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

actual object ThemeSettingsStorage {
    private const val amoledEnabledKey = "amoled_enabled"
    private const val selectedAppLanguageKey = "selected_app_language"
    private const val themeModeKey = "theme_mode"
    private const val colorPaletteKey = "color_palette"
    private const val displayScaleKey = "display_scale"
    private const val scrollbarFocusKey = "scrollbar_focus"
    private const val showImportsKey = "show_imports"
    private val profileScopedSyncKeys = listOf(
        amoledEnabledKey,
    )

    actual fun loadAmoledEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = ProfileScopedKey.of(amoledEnabledKey)
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            null
        }
    }

    actual fun saveAmoledEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(amoledEnabledKey))
    }

    actual fun loadThemeMode(): String? = NSUserDefaults.standardUserDefaults.stringForKey(themeModeKey)

    actual fun saveThemeMode(code: String) {
        NSUserDefaults.standardUserDefaults.setObject(code, forKey = themeModeKey)
    }

    actual fun loadColorPalette(): String? = NSUserDefaults.standardUserDefaults.stringForKey(colorPaletteKey)

    actual fun saveColorPalette(key: String) {
        NSUserDefaults.standardUserDefaults.setObject(key, forKey = colorPaletteKey)
    }

    actual fun loadDisplayScale(): String? = NSUserDefaults.standardUserDefaults.stringForKey(displayScaleKey)

    actual fun saveDisplayScale(key: String) {
        NSUserDefaults.standardUserDefaults.setObject(key, forKey = displayScaleKey)
    }

    actual fun loadScrollbarFocus(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(scrollbarFocusKey) != null) defaults.boolForKey(scrollbarFocusKey) else null
    }

    actual fun saveScrollbarFocus(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = scrollbarFocusKey)
    }

    actual fun loadShowImports(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        return if (defaults.objectForKey(showImportsKey) != null) defaults.boolForKey(showImportsKey) else null
    }

    actual fun saveShowImports(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = showImportsKey)
    }

    actual fun loadSelectedAppLanguage(): String? {
        val value = NSUserDefaults.standardUserDefaults.stringForKey(selectedAppLanguageKey)
        if (value != null) return value
        val legacy = NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(selectedAppLanguageKey))
        if (legacy != null) saveSelectedAppLanguage(legacy)
        return legacy
    }

    actual fun saveSelectedAppLanguage(languageCode: String) {
        NSUserDefaults.standardUserDefaults.setObject(languageCode, forKey = selectedAppLanguageKey)
    }

    actual fun applySelectedAppLanguage(languageCode: String) {
        if (languageCode.equals("device", ignoreCase = true)) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey("AppleLanguages")
            NSUserDefaults.standardUserDefaults.synchronize()
            return
        }
        val normalizedCode = languageCode
            .trim()
            .takeIf { it.isNotBlank() }
            ?: AppLanguage.ENGLISH.code
        NSUserDefaults.standardUserDefaults.setObject(
            listOf(normalizedCode),
            forKey = "AppleLanguages",
        )
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadAmoledEnabled()?.let { put(amoledEnabledKey, encodeSyncBoolean(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        profileScopedSyncKeys.forEach { key ->
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(key))
        }

        payload.decodeSyncBoolean(amoledEnabledKey)?.let(::saveAmoledEnabled)
        applySelectedAppLanguage(loadSelectedAppLanguage() ?: AppLanguage.DEVICE.code)
    }
}
