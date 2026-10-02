package io.github.dimitrysaf.provenio.core.streams.availability

import android.content.Context
import android.content.SharedPreferences
import io.github.dimitrysaf.provenio.core.sync.decodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.decodeSyncString
import io.github.dimitrysaf.provenio.core.sync.encodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.encodeSyncString
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

actual object StreamingAvailabilitySettingsStorage {
    private const val preferencesName = "provenio_streaming_availability_settings"
    private const val enabledKey = "streaming_availability_enabled"
    private const val apiKey = "streaming_availability_api_key"
    private val syncKeys = listOf(enabledKey, apiKey)

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEnabled(): Boolean? =
        preferences?.let { sharedPreferences ->
            val scopedKey = ProfileScopedKey.of(enabledKey)
            if (sharedPreferences.contains(scopedKey)) {
                sharedPreferences.getBoolean(scopedKey, false)
            } else {
                null
            }
        }

    actual fun saveEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(enabledKey), enabled)
            ?.apply()
    }

    actual fun loadApiKey(): String? =
        preferences?.getString(ProfileScopedKey.of(apiKey), null)

    actual fun saveApiKey(apiKey: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(this.apiKey), apiKey)
            ?.apply()
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        preferences?.edit()?.apply {
            syncKeys.forEach { remove(ProfileScopedKey.of(it)) }
        }?.apply()

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKey)?.let(::saveApiKey)
    }
}
