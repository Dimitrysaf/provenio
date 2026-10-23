package io.github.dimitrysaf.provenio.core.docs

import io.github.dimitrysaf.provenio.desktop.Context
import io.github.dimitrysaf.provenio.desktop.SharedPreferences

internal actual object DocsStorage {
    private const val preferencesName = "provenio_docs"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun load(key: String): String? = preferences?.getString(key, null)

    actual fun save(key: String, text: String) {
        preferences?.edit()?.putString(key, text)?.apply()
    }
}
