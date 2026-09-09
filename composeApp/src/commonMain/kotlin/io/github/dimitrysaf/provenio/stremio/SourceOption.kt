package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.stremio.model.Stream

/** A stream together with the addon that produced it. */
data class SourceOption(
    val addon: InstalledAddon,
    val stream: Stream,
    /** Filled in once the peer-to-peer service has turned a torrent into a local URL. */
    val resolvedUrl: String? = null,
) {
    /** How this source is played, which decides whether it can be played at all today. */
    val kind: SourceKind
        get() = when {
            stream.url != null -> SourceKind.Direct
            stream.infoHash != null -> SourceKind.Torrent
            stream.ytId != null -> SourceKind.YouTube
            stream.externalUrl != null -> SourceKind.External
            else -> SourceKind.Unsupported
        }

    /** The short label. Addons put quality and release group here. */
    val label: String
        get() = stream.name
            ?: stream.title
            ?: stream.behaviorHints?.filename
            ?: "Source"

    /** The long line under it. Usually filename, size and seed count. */
    val detail: String?
        get() = stream.description ?: stream.title?.takeIf { it != stream.name }

    val playableUrl: String?
        get() = resolvedUrl ?: when (kind) {
            SourceKind.Direct -> stream.url
            SourceKind.YouTube -> stream.ytId?.let { "https://www.youtube.com/watch?v=$it" }
            SourceKind.External -> stream.externalUrl
            else -> null
        }
}

/** The text a filter looks at: whatever the addon wrote about this source. */
val SourceOption.searchText: String
    get() = (label + " " + detail.orEmpty()).lowercase()

/** True when every whitespace separated term in [query] appears somewhere in the text. */
fun SourceOption.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val text = searchText
    return query.trim().lowercase().split(" ").all { term -> term in text }
}

/** True when the source mentions any of [terms], or when nothing is being filtered on. */
fun SourceOption.hasAny(terms: Collection<String>): Boolean {
    if (terms.isEmpty()) return true
    val text = searchText
    return terms.any { it.lowercase() in text }
}

/**
 * Strings offered in the quality filter.
 *
 * Plain substrings, nothing parsed. The protocol has no quality field, so the only honest
 * thing to do is look for the text and let a source through when it is there. Only terms
 * that actually appear in the current results are ever shown.
 */
val QualityTerms = listOf("2160p", "4K", "1080p", "720p", "480p", "HDR")

enum class SourceKind(val label: String) {
    Direct("Direct"),
    Torrent("Peer-to-peer"),
    YouTube("YouTube"),
    External("Opens elsewhere"),
    Unsupported("Unsupported"),
}
