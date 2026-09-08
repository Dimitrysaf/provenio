package io.github.dimitrysaf.provenio.stremio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the installed addons for the app.
 *
 * In memory only for now — nothing survives a restart until there is a database to put it
 * in. The API is written so that swapping the backing store later does not change any
 * caller.
 */
object AddonRepository {

    private val client = StremioAddonClient()

    private val _collection = MutableStateFlow(AddonCollection())
    val collection: StateFlow<AddonCollection> = _collection.asStateFlow()

    /**
     * Fetches the manifest at [url] and adds the addon.
     *
     * Installing by URL is the whole install flow in this protocol: the manifest is both
     * the validation and the description, so a failure here is what tells the user the
     * address was wrong rather than the addon being broken.
     */
    suspend fun install(url: String): AddonResult<InstalledAddon> {
        val result = client.fetchManifest(url)
        return result.map { manifest ->
            val addon = InstalledAddon(transportUrl = AddonUrl.manifest(url), manifest = manifest)
            _collection.value = _collection.value.with(addon)
            addon
        }
    }

    fun remove(addonId: String) {
        _collection.value = _collection.value.without(addonId)
    }

    fun setEnabled(addonId: String, enabled: Boolean) {
        _collection.value = _collection.value.setEnabled(addonId, enabled)
    }

    fun move(addonId: String, delta: Int) {
        _collection.value = _collection.value.move(addonId, delta)
    }
}
