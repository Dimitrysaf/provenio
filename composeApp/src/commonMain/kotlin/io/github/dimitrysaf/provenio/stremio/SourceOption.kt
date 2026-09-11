package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.stremio.model.Stream
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.source_kind_direct
import io.github.dimitrysaf.provenio.resources.source_kind_external
import io.github.dimitrysaf.provenio.resources.source_kind_p2p
import io.github.dimitrysaf.provenio.resources.source_kind_unsupported
import io.github.dimitrysaf.provenio.resources.source_kind_youtube
import org.jetbrains.compose.resources.StringResource

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

    /**
     * The short label. Addons put quality and release group here.
     *
     * Null when the addon named the source nothing at all — the stand-in for that is a
     * translated string, so it belongs to whoever is drawing this, not here.
     */
    val label: String?
        get() = stream.name
            ?: stream.title
            ?: stream.behaviorHints?.filename

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

/**
 * Whether this is the source currently playing at [streamUrl].
 *
 * A torrent's playable URL is minted per session by the local server, so it cannot be
 * compared with one from an earlier run — but that URL carries the info hash as its last
 * path segment, and the info hash is the torrent's identity. Anything else is a plain
 * address and compares directly.
 */
fun SourceOption.isPlayingAt(streamUrl: String?): Boolean {
    if (streamUrl.isNullOrBlank()) return false
    val hash = stream.infoHash?.trim()?.lowercase()
    if (hash != null) {
        return streamUrl.substringAfterLast('/').substringBefore('?').startsWith(hash)
    }
    return playableUrl != null && playableUrl == streamUrl
}

/** The text a filter looks at: whatever the addon wrote about this source. */
val SourceOption.searchText: String
    get() = (label.orEmpty() + " " + detail.orEmpty()).lowercase()

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

enum class SourceKind(val label: StringResource) {
    Direct(Res.string.source_kind_direct),
    Torrent(Res.string.source_kind_p2p),
    YouTube(Res.string.source_kind_youtube),
    External(Res.string.source_kind_external),
    Unsupported(Res.string.source_kind_unsupported),
}
