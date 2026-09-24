package io.github.dimitrysaf.provenio.core.tracking.simkl

import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object SimklSyncStorage {
    private const val PAYLOAD_KEY = "simkl_sync_snapshot"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(PAYLOAD_KEY))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(PAYLOAD_KEY))
    }

    actual fun removeProfile(profileId: Int) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(PAYLOAD_KEY, profileId))
    }
}
