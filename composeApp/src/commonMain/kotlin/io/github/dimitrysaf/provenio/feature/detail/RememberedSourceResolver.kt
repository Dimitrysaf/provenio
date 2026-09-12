package io.github.dimitrysaf.provenio.feature.detail

import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.p2p.TorrentRequest
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.SourceKind
import io.github.dimitrysaf.provenio.stremio.isPlayingAt
import io.github.dimitrysaf.provenio.stremio.streamId

/** A remembered source, resolved back into something actually playable right now. */
internal data class ResolvedSource(val url: String, val streamId: String?)

/**
 * Finds the source a previous session played this video with — identified by [streamId],
 * exactly what [io.github.dimitrysaf.provenio.stremio.SourceOption.streamId] saved — among
 * what every installed addon offers today, and turns it back into a playable url. Used to
 * try "resume" against the same source before falling back to the source sheet.
 *
 * Returns null the moment any step comes up short: no addon lists this source any more, or
 * — for a torrent — peer-to-peer is off, consent was never given, or the swarm cannot be
 * reached. Every one of those is exactly when the caller should fall back to letting the
 * source be picked by hand instead.
 */
internal suspend fun resolveRememberedSource(
    type: String,
    videoId: String,
    streamId: String,
): ResolvedSource? {
    val match = AddonRepository.streamProviders(type, videoId)
        .firstNotNullOfOrNull { addon ->
            AddonRepository.streamsFrom(addon, type, videoId).firstOrNull { it.isPlayingAt(streamId) }
        }
        ?: return null

    val url = if (match.kind == SourceKind.Torrent) {
        val hash = match.stream.infoHash ?: return null
        P2pRepository.streamUrl(
            TorrentRequest(
                infoHash = hash,
                sources = match.stream.sources,
                fileIndex = match.stream.fileIdx,
            ),
        )
    } else {
        match.playableUrl
    } ?: return null

    return ResolvedSource(url = url, streamId = match.streamId)
}
