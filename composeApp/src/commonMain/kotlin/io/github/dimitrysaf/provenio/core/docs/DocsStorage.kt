package io.github.dimitrysaf.provenio.core.docs

/** The last copy of each documentation page, so the documentation opens without a connection. */
internal expect object DocsStorage {
    fun load(key: String): String?
    fun save(key: String, text: String)
}
