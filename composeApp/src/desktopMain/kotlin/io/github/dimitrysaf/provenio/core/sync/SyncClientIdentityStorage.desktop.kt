package io.github.dimitrysaf.provenio.core.sync

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences

actual object SyncClientIdentityStorage {
    private const val preferencesName = "provenio_sync_client_identity"
    private const val clientIdKey = "client_instance_id"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadClientId(): String? =
        preferences?.getString(clientIdKey, null)

    actual fun saveClientId(clientId: String) {
        preferences
            ?.edit()
            ?.putString(clientIdKey, clientId)
            ?.apply()
    }
}
