package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.stremio.model.Manifest
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog

/**
 * An addon the user has added, paired with the manifest fetched from it.
 *
 * [transportUrl] is kept as the user supplied it so it can be shown and re-fetched;
 * [AddonUrl.base] normalises it whenever a request is built.
 */
data class InstalledAddon(
    val transportUrl: String,
    val manifest: Manifest,
    val enabled: Boolean = true,
)

/**
 * The installed addons, in priority order.
 *
 * Order is meaningful: the protocol has no notion of a "best" addon for a request, so the
 * first one in this list that advertises a resource is the first one asked. Routing lives
 * here rather than in the client because it is a property of the collection, not of any
 * single addon.
 */
class AddonCollection(private val addons: List<InstalledAddon> = emptyList()) {

    val all: List<InstalledAddon> get() = addons
    val active: List<InstalledAddon> get() = addons.filter { it.enabled }

    /** Every enabled addon that advertises [resource] for this type and id, in order. */
    fun providersOf(resource: String, type: String, id: String): List<InstalledAddon> =
        active.filter { it.manifest.supports(resource, type, id) }

    fun streamProviders(type: String, id: String): List<InstalledAddon> =
        providersOf("stream", type, id)

    fun metaProviders(type: String, id: String): List<InstalledAddon> =
        providersOf("meta", type, id)

    fun subtitleProviders(type: String, id: String): List<InstalledAddon> =
        providersOf("subtitles", type, id)

    /** Every catalog offered by every enabled addon, paired with its owner. */
    fun catalogs(): List<Pair<InstalledAddon, ManifestCatalog>> =
        active.flatMap { addon -> addon.manifest.catalogs.map { addon to it } }

    /**
     * Catalogs that can be browsed without the caller supplying anything, of [type] or of
     * every type when it is null.
     *
     * A catalog with a required extra needs data we do not have. Cinemeta's `last-videos`
     * and `calendar-videos` want a list of ids the user is already following, so they are
     * feeds for a signed in client rather than shelves to browse.
     */
    fun browsableCatalogs(type: String? = null): List<Pair<InstalledAddon, ManifestCatalog>> =
        catalogs().filter { (_, catalog) ->
            (type == null || catalog.type == type) &&
                catalog.normalizedExtra().none { it.isRequired == true }
        }

    /** Catalogs that accept a `search` extra, which is what a search query can query. */
    fun searchableCatalogs(): List<Pair<InstalledAddon, ManifestCatalog>> =
        catalogs().filter { (_, catalog) ->
            catalog.normalizedExtra().any { it.name == "search" }
        }

    fun with(addon: InstalledAddon): AddonCollection =
        AddonCollection(addons.filterNot { it.manifest.id == addon.manifest.id } + addon)

    fun without(addonId: String): AddonCollection =
        AddonCollection(addons.filterNot { it.manifest.id == addonId })

    /** Moves an addon by [delta] places, clamped to the ends. Order is priority. */
    fun move(addonId: String, delta: Int): AddonCollection {
        val index = addons.indexOfFirst { it.manifest.id == addonId }
        if (index < 0) return this
        val target = (index + delta).coerceIn(0, addons.lastIndex)
        if (target == index) return this
        val reordered = addons.toMutableList()
        reordered.add(target, reordered.removeAt(index))
        return AddonCollection(reordered)
    }

    fun setEnabled(addonId: String, enabled: Boolean): AddonCollection =
        AddonCollection(
            addons.map { if (it.manifest.id == addonId) it.copy(enabled = enabled) else it },
        )
}
