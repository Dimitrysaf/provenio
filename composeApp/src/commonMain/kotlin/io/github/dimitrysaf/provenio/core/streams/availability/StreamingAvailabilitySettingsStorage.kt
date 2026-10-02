package io.github.dimitrysaf.provenio.core.streams.availability

import kotlinx.serialization.json.JsonObject

internal expect object StreamingAvailabilitySettingsStorage {
    fun loadEnabled(): Boolean?
    fun saveEnabled(enabled: Boolean)
    fun loadApiKey(): String?
    fun saveApiKey(apiKey: String)
    fun exportToSyncPayload(): JsonObject
    fun replaceFromSyncPayload(payload: JsonObject)
}
