package io.github.dimitrysaf.provenio.core.docs

import platform.Foundation.NSUserDefaults

internal actual object DocsStorage {
    private const val keyPrefix = "provenio_docs."

    actual fun load(key: String): String? = NSUserDefaults.standardUserDefaults.stringForKey(keyPrefix + key)

    actual fun save(key: String, text: String) {
        NSUserDefaults.standardUserDefaults.setObject(text, forKey = keyPrefix + key)
    }
}
