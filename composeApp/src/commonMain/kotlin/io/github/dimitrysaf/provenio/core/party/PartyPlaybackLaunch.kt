package io.github.dimitrysaf.provenio.core.party

import io.github.dimitrysaf.provenio.core.playback.PlayerLaunch

/** The host's source as this device opens it, at [positionMs]. */
internal fun PartyPlayback.toPlayerLaunch(profileId: Int, positionMs: Long): PlayerLaunch = PlayerLaunch(
    profileId = profileId,
    title = title,
    sourceUrl = sourceUrl,
    sourceAudioUrl = sourceAudioUrl,
    sourceHeaders = sourceHeaders,
    sourceResponseHeaders = sourceResponseHeaders,
    streamType = streamType,
    logo = logo,
    poster = poster,
    background = background,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    episodeTitle = episodeTitle,
    episodeThumbnail = episodeThumbnail,
    streamTitle = streamTitle,
    streamSubtitle = streamSubtitle,
    providerName = providerName,
    providerAddonId = providerAddonId,
    contentType = contentType,
    videoId = videoId,
    parentMetaId = parentMetaId,
    parentMetaType = parentMetaType,
    torrentInfoHash = torrentInfoHash,
    torrentFileIdx = torrentFileIdx,
    torrentFilename = torrentFilename,
    torrentTrackers = torrentTrackers,
    initialPositionMs = positionMs,
)

/**
 * Whether guests can open [sourceUrl] themselves: a torrent, which each device streams on its own,
 * or an address on the internet. A downloaded file or this device's own server is not.
 */
internal fun isShareablePartySource(sourceUrl: String, torrentInfoHash: String?): Boolean {
    if (!torrentInfoHash.isNullOrBlank() || sourceUrl.startsWith("torrent://")) return true
    val lower = sourceUrl.lowercase()
    if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
    val host = lower.substringAfter("://").substringBefore('/').substringBefore('?').substringBeforeLast(':')
    return host != "localhost" && host != "127.0.0.1" && host != "[::1]" && !host.startsWith("10.") &&
        !host.startsWith("192.168.")
}
