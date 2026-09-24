package io.github.dimitrysaf.provenio.core.tracking.trakt

import io.github.dimitrysaf.provenio.core.sync.decodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.encodeSyncBoolean
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.Foundation.NSUserDefaults

internal actual object TraktCommentsStorage {
    private const val enabledKey = "comments_enabled"
    private val syncKeys = listOf(enabledKey)

    actual fun loadEnabled(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        val key = ProfileScopedKey.of(enabledKey)
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            null
        }
    }

    actual fun saveEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = ProfileScopedKey.of(enabledKey))
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        syncKeys.forEach { key ->
            NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(key))
        }

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
    }
}
