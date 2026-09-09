package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.stremio.model.Stream

/** A stream together with the addon that produced it. */
data class SourceOption(
    val addon: InstalledAddon,
    val stream: Stream,
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
        get() = when (kind) {
            SourceKind.Direct -> stream.url
            SourceKind.YouTube -> stream.ytId?.let { "https://www.youtube.com/watch?v=$it" }
            SourceKind.External -> stream.externalUrl
            else -> null
        }
}

enum class SourceKind(val label: String) {
    Direct("Direct"),
    Torrent("Peer-to-peer"),
    YouTube("YouTube"),
    External("Opens elsewhere"),
    Unsupported("Unsupported"),
}
