package io.github.dimitrysaf.provenio.core.tracking.trakt

import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object TraktAuthStorage {
    private const val payloadKey = "trakt_auth_payload"

    actual fun loadPayload(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey, profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey, profileId))
    }

    actual fun removeProfile(profileId: Int) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(payloadKey, profileId))
    }
}
