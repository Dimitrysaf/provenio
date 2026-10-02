package io.github.dimitrysaf.provenio.core.streams.availability

import io.github.dimitrysaf.provenio.core.sync.decodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.decodeSyncString
import io.github.dimitrysaf.provenio.core.sync.encodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.encodeSyncString
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

actual object StreamingAvailabilitySettingsStorage {
    private const val enabledKey = "streaming_availability_enabled"
    private const val apiKey = "streaming_availability_api_key"
    private val syncKeys = listOf(enabledKey, apiKey)

    actual fun loadEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val scopedKey = ProfileScopedKey.of(enabledKey)
        return if (defaults.objectForKey(scopedKey) != null) {
            defaults.boolForKey(scopedKey)
        } else {
            null
        }
    }

    actual fun saveEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(enabledKey))
    }

    actual fun loadApiKey(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(apiKey))

    actual fun saveApiKey(apiKey: String) {
        NSUserDefaults.standardUserDefaults.setObject(apiKey, forKey = ProfileScopedKey.of(this.apiKey))
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
        loadApiKey()?.let { put(apiKey, encodeSyncString(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        syncKeys.forEach { key ->
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(key))
        }

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
        payload.decodeSyncString(apiKey)?.let(::saveApiKey)
    }
}
