package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.Serializable

@Serializable
data class MetaPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val posterShape: String? = null,
    val banner: String? = null,
    val releaseInfo: String? = null,
    val description: String? = null,
)

@Serializable
data class CatalogResponse(
    val metas: List<MetaPreview> = emptyList(),
    val cacheMaxAge: Int? = null,
)

@Serializable
data class MetaResponse(
    val meta: MetaPreview,
)
