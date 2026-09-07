package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.Serializable

@Serializable
data class Manifest(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: String,
    val resources: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val catalogs: List<ManifestCatalog> = emptyList(),
    val idPrefixes: List<String>? = null,
    val background: String? = null,
    val logo: String? = null,
    val contactEmail: String? = null,
    val behaviorHints: BehaviorHints? = null,
)

@Serializable
data class ManifestCatalog(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<CatalogExtra>? = null,
)

@Serializable
data class CatalogExtra(
    val name: String,
    val isRequired: Boolean? = null,
    val options: List<String>? = null,
    val optionsLimit: Int? = null,
)

@Serializable
data class BehaviorHints(
    val adult: Boolean? = null,
    val p2p: Boolean? = null,
    val configurable: Boolean? = null,
    val configurationRequired: Boolean? = null,
)
