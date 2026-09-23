package com.nuvio.app.core.settings

internal expect object SentrySettingsPlatform {
    val crashReportsSupported: Boolean
}

internal expect object SentrySettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
}
