package io.github.dimitrysaf.provenio.shell.screens.player

import io.github.dimitrysaf.provenio.core.playback.PlayerSourceTorrent

data class PlayerHandoff(
    val sourceUrl: String,
    val sourceAudioUrl: String?,
    val sourceHeaders: Map<String, String>,
    val sourceResponseHeaders: Map<String, String>,
    val streamType: String?,
    val streamTitle: String,
    val streamSubtitle: String?,
    val providerName: String,
    val providerAddonId: String?,
    val videoId: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val episodeTitle: String?,
    val episodeThumbnail: String?,
    val pauseDescription: String?,
    val torrentInfoHash: String?,
    val torrentFileIdx: Int?,
    val torrentFilename: String?,
    val torrentTrackers: List<String>,
    val sourceTorrent: PlayerSourceTorrent? = null,
    val positionMs: Long,
)
