package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.data.AddonStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import io.github.dimitrysaf.provenio.stremio.model.Manifest
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog
import io.github.dimitrysaf.provenio.stremio.model.Meta
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
    suspend fun search(query: String, type: String? = null): List<MetaPreview> {
        val found = mutableListOf<MetaPreview>()
        _collection.value.searchableCatalogs()
            .filter { (_, catalog) -> type == null || catalog.type == type }
            .forEach { (addon, catalog) ->
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

    /**
     * Full metadata for one title, from the first addon that answers.
     *
     * Addons are asked in the collection's order and the first usable reply wins. Asking
     * all of them and merging would mean deciding which addon's description is correct,
     * which is the seat cascade and is not designed yet.
     */
    suspend fun meta(type: String, id: String): Meta? {
        _collection.value.metaProviders(type, id).forEach { addon ->
            val reply = client.fetchMeta(addon.transportUrl, type, id)
            reply.getOrNull()?.meta?.let { return it }
        }
        return null
    }

    /**
     * One page of a catalog.
     *
     * `skip` is how the protocol paginates. Without it a shelf silently stops at whatever
     * the addon returns first and looks like the end of the catalog.
     */
    suspend fun catalogPage(
        addon: InstalledAddon,
        catalog: ManifestCatalog,
        skip: Int,
    ): List<MetaPreview>? {
        val extra = if (skip > 0) mapOf("skip" to skip.toString()) else emptyMap()
        return client.fetchCatalog(
            addonUrl = addon.transportUrl,
            type = catalog.type,
            id = catalog.id,
            extra = extra,
        ).getOrNull()?.metas
    }

    /** Addons that can answer for this title, so the caller can fetch them as it likes. */
    fun streamProviders(type: String, id: String): List<InstalledAddon> =
        _collection.value.streamProviders(type, id)

    /**
     * Streams from one addon, tagged with where they came from.
     *
     * Provenance travels with the stream rather than being looked up later, because once
     * several addons answer for the same title there is no way to work out afterwards
     * which one produced a given result.
     */
    suspend fun streamsFrom(
        addon: InstalledAddon,
        type: String,
        id: String,
    ): List<SourceOption> {
        val reply = client.fetchStreams(addon.transportUrl, type, id).getOrNull()
        return reply?.streams.orEmpty().map { SourceOption(addon = addon, stream = it) }
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
