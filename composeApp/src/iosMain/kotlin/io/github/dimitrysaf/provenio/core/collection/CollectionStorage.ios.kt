package io.github.dimitrysaf.provenio.core.collection

import io.github.dimitrysaf.provenio.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

actual object CollectionStorage {
    private const val payloadKey = "collections_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }
}
