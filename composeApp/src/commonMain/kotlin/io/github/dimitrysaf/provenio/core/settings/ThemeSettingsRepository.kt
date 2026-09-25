package io.github.dimitrysaf.provenio.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeSettingsRepository {
    private val _amoledEnabled = MutableStateFlow(false)
    val amoledEnabled: StateFlow<Boolean> = _amoledEnabled.asStateFlow()

    private val _selectedAppLanguage = MutableStateFlow(AppLanguage.DEVICE)
    val selectedAppLanguage: StateFlow<AppLanguage> = _selectedAppLanguage.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _amoledEnabled.value = false
        _selectedAppLanguage.value = AppLanguage.DEVICE
    }

    private fun loadFromDisk() {
        hasLoaded = true
        _amoledEnabled.value = ThemeSettingsStorage.loadAmoledEnabled() ?: false
        val appLanguage = AppLanguage.fromCode(ThemeSettingsStorage.loadSelectedAppLanguage())
        ThemeSettingsStorage.applySelectedAppLanguage(appLanguage.code)
        _selectedAppLanguage.value = appLanguage
    }

    fun setAmoled(enabled: Boolean) {
        ensureLoaded()
        if (_amoledEnabled.value == enabled) return
        _amoledEnabled.value = enabled
        ThemeSettingsStorage.saveAmoledEnabled(enabled)
    }

    fun setAppLanguage(language: AppLanguage) {
        ensureLoaded()
        if (_selectedAppLanguage.value == language) return
        ThemeSettingsStorage.saveSelectedAppLanguage(language.code)
        ThemeSettingsStorage.applySelectedAppLanguage(language.code)
        _selectedAppLanguage.value = language
    }
}
