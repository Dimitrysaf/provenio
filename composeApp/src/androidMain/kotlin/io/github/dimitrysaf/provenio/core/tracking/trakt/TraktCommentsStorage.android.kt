package io.github.dimitrysaf.provenio.core.tracking.trakt

import android.content.Context
import android.content.SharedPreferences
import io.github.dimitrysaf.provenio.core.sync.decodeSyncBoolean
import io.github.dimitrysaf.provenio.core.sync.encodeSyncBoolean
import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal actual object TraktCommentsStorage {
    private const val preferencesName = "provenio_trakt_comments"
    private const val enabledKey = "comments_enabled"
    private val syncKeys = listOf(enabledKey)

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadEnabled(): Boolean? {
        val prefs = preferences ?: return null
        val key = ProfileScopedKey.of(enabledKey)
        return if (prefs.contains(key)) prefs.getBoolean(key, true) else null
    }

    actual fun saveEnabled(enabled: Boolean) {
        preferences
            ?.edit()
            ?.putBoolean(ProfileScopedKey.of(enabledKey), enabled)
            ?.apply()
    }

    actual fun exportToSyncPayload(): JsonObject = buildJsonObject {
        loadEnabled()?.let { put(enabledKey, encodeSyncBoolean(it)) }
    }

    actual fun replaceFromSyncPayload(payload: JsonObject) {
        preferences?.edit()?.apply {
            syncKeys.forEach { remove(ProfileScopedKey.of(it)) }
        }?.apply()

        payload.decodeSyncBoolean(enabledKey)?.let(::saveEnabled)
    }
}
