package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.Serializable

@Serializable
data class Subtitle(
    val id: String,
    val url: String,
    /** ISO 639-2 where the addon provides one; otherwise free text to show as-is. */
    val lang: String,
)

@Serializable
data class SubtitlesResponse(
    val subtitles: List<Subtitle> = emptyList(),
    val cacheMaxAge: Int? = null,
)
