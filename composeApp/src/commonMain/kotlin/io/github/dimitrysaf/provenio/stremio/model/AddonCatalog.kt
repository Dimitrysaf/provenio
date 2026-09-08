package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.Serializable

/** An addon advertised by another addon's `addon_catalog` resource. */
@Serializable
data class AddonCatalogEntry(
    val transportUrl: String,
    val transportName: String? = null,
    val manifest: Manifest? = null,
)

@Serializable
data class AddonCatalogResponse(
    val addons: List<AddonCatalogEntry> = emptyList(),
    val cacheMaxAge: Int? = null,
)
