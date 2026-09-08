package io.github.dimitrysaf.provenio.data

import app.cash.sqldelight.db.SqlDriver
import io.github.dimitrysaf.provenio.db.ProvenioDatabase
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.StremioAddonClient
import io.github.dimitrysaf.provenio.stremio.model.Manifest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Persists the installed addons.
 *
 * The manifest is stored as the JSON it arrived as rather than being spread across
 * columns: it is a document owned by the addon, its shape varies between addons, and
 * nothing here queries inside it. Order is stored explicitly because it is priority.
 */
class AddonStore(driver: SqlDriver) {

    private val queries = ProvenioDatabase(driver).installedAddonQueries

    fun load(): List<InstalledAddon> =
        queries.selectAll().executeAsList().mapNotNull { row ->
            // A manifest that no longer parses is dropped rather than failing the load —
            // one bad row should not cost the user every other addon.
            val manifest = runCatching {
                StremioAddonClient.addonJson.decodeFromString<Manifest>(row.manifestJson)
            }.getOrNull() ?: return@mapNotNull null
            InstalledAddon(
                transportUrl = row.transportUrl,
                manifest = manifest,
                enabled = row.enabled != 0L,
            )
        }

    fun replaceAll(addons: List<InstalledAddon>) {
        queries.transaction {
            queries.deleteAll()
            addons.forEachIndexed { index, addon ->
                queries.insert(
                    id = addon.manifest.id,
                    transportUrl = addon.transportUrl,
                    manifestJson = StremioAddonClient.addonJson.encodeToString(addon.manifest),
                    enabled = if (addon.enabled) 1L else 0L,
                    sortIndex = index.toLong(),
                )
            }
        }
    }
}
