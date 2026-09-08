package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.Serializable

/**
 * A playable source. Exactly one of the source identifiers is set — [url], [ytId],
 * [infoHash] (BitTorrent), [externalUrl] or one of the archive forms.
 */
@Serializable
data class Stream(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val externalUrl: String? = null,
    val nzbUrl: String? = null,
    val rarUrls: List<String> = emptyList(),
    val zipUrls: List<String> = emptyList(),
    val tgzUrls: List<String> = emptyList(),
    val tarUrls: List<String> = emptyList(),
    val fileIdx: Int? = null,
    val fileMustInclude: String? = null,
    val servers: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val subtitles: List<Subtitle> = emptyList(),
    val behaviorHints: StreamBehaviorHints? = null,
) {
    /** True when playing this stream means joining a swarm, not fetching a URL. */
    val isP2p: Boolean get() = infoHash != null
}

@Serializable
data class StreamBehaviorHints(
    val countryWhitelist: List<String> = emptyList(),
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val proxyHeaders: ProxyHeaders? = null,
    val videoHash: String? = null,
    val videoSize: Long? = null,
    val filename: String? = null,
)

@Serializable
data class ProxyHeaders(
    val request: Map<String, String> = emptyMap(),
    val response: Map<String, String> = emptyMap(),
)

@Serializable
data class StreamResponse(
    val streams: List<Stream> = emptyList(),
    val cacheMaxAge: Int? = null,
)
