package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.Serializable

/** The short form returned inside a catalog. */
@Serializable
data class MetaPreview(
    val id: String,
    val type: String,
    val name: String? = null,
    val poster: String? = null,
    val posterShape: String? = null,
    val banner: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val imdbRating: String? = null,
    val releaseInfo: String? = null,
    val director: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val links: List<MetaLink> = emptyList(),
    val trailers: List<Trailer> = emptyList(),
)

/**
 * The full form returned by the `meta` resource. Notably it carries [videos], which is
 * where a series' episodes live — the catalog preview has no equivalent.
 */
@Serializable
data class Meta(
    val id: String,
    val type: String,
    val name: String? = null,
    val genres: List<String> = emptyList(),
    val poster: String? = null,
    val posterShape: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val director: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val imdbRating: String? = null,
    val released: String? = null,
    val trailers: List<Trailer> = emptyList(),
    val links: List<MetaLink> = emptyList(),
    val videos: List<Video> = emptyList(),
    val runtime: String? = null,
    val language: String? = null,
    val country: String? = null,
    val awards: String? = null,
    val website: String? = null,
    val behaviorHints: MetaBehaviorHints? = null,
)

/** One episode (series) or entry (channel). Stream ids look like `tt0108778:1:1`. */
@Serializable
data class Video(
    val id: String,
    val title: String? = null,
    val released: String? = null,
    val thumbnail: String? = null,
    val streams: List<Stream> = emptyList(),
    val available: Boolean? = null,
    val episode: Int? = null,
    val season: Int? = null,
    val trailers: List<Trailer> = emptyList(),
    val overview: String? = null,
)

@Serializable
data class MetaLink(
    val name: String,
    val category: String,
    val url: String,
)

@Serializable
data class Trailer(
    val source: String,
    val type: String? = null,
)

@Serializable
data class MetaBehaviorHints(
    val defaultVideoId: String? = null,
)

@Serializable
data class CatalogResponse(
    val metas: List<MetaPreview> = emptyList(),
    val cacheMaxAge: Int? = null,
    val staleRevalidate: Int? = null,
    val staleError: Int? = null,
)

@Serializable
data class MetaResponse(
    val meta: Meta,
    val cacheMaxAge: Int? = null,
)
