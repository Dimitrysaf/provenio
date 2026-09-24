package io.github.dimitrysaf.provenio.core.metadata

import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object MetaScreenSettingsStorage {
    private const val payloadKey = "meta_screen_settings_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }
}