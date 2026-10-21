package io.github.dimitrysaf.provenio.core.settings

import kotlinx.serialization.json.JsonObject

internal expect object ThemeSettingsStorage {
    fun loadAmoledEnabled(): Boolean?
    fun saveAmoledEnabled(enabled: Boolean)
    fun loadThemeMode(): String?
    fun saveThemeMode(code: String)
    fun loadColorPalette(): String?
    fun saveColorPalette(key: String)
    fun loadDisplayScale(): String?
    fun saveDisplayScale(key: String)

    /** Whether this device is a television, which takes the TV layout until it is set. */
    val isTelevision: Boolean
    fun loadTvLayout(): Boolean?
    fun saveTvLayout(enabled: Boolean)
    fun loadScrollbarFocus(): Boolean?
    fun saveScrollbarFocus(enabled: Boolean)
    fun loadSelectedAppLanguage(): String?
    fun saveSelectedAppLanguage(languageCode: String)
    fun applySelectedAppLanguage(languageCode: String)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
