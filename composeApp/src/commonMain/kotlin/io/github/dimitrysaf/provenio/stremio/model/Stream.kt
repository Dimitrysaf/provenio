package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.Serializable

@Serializable
data class Stream(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val title: String? = null,
    val name: String? = null,
    val behaviorHints: StreamBehaviorHints? = null,
)

@Serializable
data class StreamBehaviorHints(
    val filename: String? = null,
    val bingeGroup: String? = null,
)

@Serializable
data class StreamResponse(
    val streams: List<Stream> = emptyList(),
    val cacheMaxAge: Int? = null,
)
