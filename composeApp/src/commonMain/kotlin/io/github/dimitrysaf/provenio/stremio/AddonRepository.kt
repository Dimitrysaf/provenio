package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.data.AddonStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import io.github.dimitrysaf.provenio.stremio.model.Manifest
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the installed addons for the app, backed by the database.
 *
 * Storage failures are swallowed on purpose: if the database cannot be opened the app
 * still works for the session, it just will not remember anything. Losing persistence is
 * worth less than losing the ability to add an addon at all.
 */
object AddonRepository {

    private val client = StremioAddonClient()

    private val _collection = MutableStateFlow(AddonCollection())
    val collection: StateFlow<AddonCollection> = _collection.asStateFlow()

    private var store: AddonStore? = null
    private var loaded = false

    /** Opens the database and restores what was saved. Safe to call repeatedly. */
    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { AddonStore(createDatabaseDriver()) }.getOrNull()
        val saved = store?.let { runCatching { it.load() }.getOrNull() }.orEmpty()
        if (saved.isNotEmpty()) {
            _collection.value = AddonCollection(saved)
        }
    }

    /**
     * Fetches the manifest at [url] without installing anything.
     *
     * Reading the manifest is both the validation and the description in this protocol, so
     * a failure here is what tells the user the address was wrong rather than the addon
     * being broken. It is kept separate from [add] because an addon that declares
     * `configurationRequired` must not be installed in its unconfigured form.
     */
    suspend fun inspect(url: String): AddonResult<Manifest> = client.fetchManifest(url)

    fun add(url: String, manifest: Manifest): InstalledAddon {
        val addon = InstalledAddon(transportUrl = AddonUrl.manifest(url), manifest = manifest)
        commit(_collection.value.with(addon))
        return addon
    }

    /**
     * Runs [query] against every enabled catalog that advertises a `search` extra.
     *
     * Results are merged and de-duplicated by id, because several addons commonly index
     * the same catalogue and would otherwise return the same title repeatedly. Addons that
     * fail are skipped rather than failing the whole search, since one bad addon should
     * not empty the screen.
     */
    suspend fun search(query: String): List<MetaPreview> {
        val found = mutableListOf<MetaPreview>()
        _collection.value.searchableCatalogs().forEach { (addon, catalog) ->
            val page = client.fetchCatalog(
                addonUrl = addon.transportUrl,
                type = catalog.type,
                id = catalog.id,
                extra = mapOf("search" to query),
            )
            page.getOrNull()?.metas?.let { found += it }
        }
        return found.distinctBy { it.id }
    }

    fun remove(addonId: String) = commit(_collection.value.without(addonId))

    fun setEnabled(addonId: String, enabled: Boolean) =
        commit(_collection.value.setEnabled(addonId, enabled))

    fun move(addonId: String, delta: Int) = commit(_collection.value.move(addonId, delta))

    private fun commit(collection: AddonCollection) {
        _collection.value = collection
        store?.let { runCatching { it.replaceAll(collection.all) } }
    }
}
