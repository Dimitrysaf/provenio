package io.github.dimitrysaf.provenio.core.search

internal expect object DiscoverSelectionStorage {
    fun loadCatalogKey(): String?
    fun saveCatalogKey(catalogKey: String)
}
