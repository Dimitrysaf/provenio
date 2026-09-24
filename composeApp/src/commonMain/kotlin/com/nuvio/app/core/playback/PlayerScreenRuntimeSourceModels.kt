package com.nuvio.app.core.playback

import com.nuvio.app.core.streams.StreamItem

internal fun StreamItem.playerSourceIdentityKey(): String? {
    p2pInfoHash?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { hash ->
        return "torrent:$hash:${p2pFileIdx ?: -1}"
    }

    clientResolve?.let { resolve ->
        val raw = resolve.stream?.raw
        val keyParts = listOf(
            addonId,
            resolve.service,
            resolve.serviceIndex?.toString(),
            resolve.infoHash?.trim()?.lowercase(),
            resolve.fileIdx?.toString(),
            resolve.magnetUri,
            resolve.torrentName,
            resolve.filename,
            raw?.torrentName,
            raw?.filename,
            raw?.size?.toString(),
            behaviorHints.filename,
            behaviorHints.videoSize?.toString(),
            streamLabel,
            streamSubtitle,
        ).map { it.orEmpty().trim() }
        if (keyParts.any { it.isNotBlank() }) {
            return "resolve:${keyParts.joinToString("|")}"
        }
    }

    behaviorHints.videoHash?.trim()?.takeIf { it.isNotBlank() }?.let { hash ->
        return "hash:$addonId:$hash:${behaviorHints.videoSize ?: ""}:${behaviorHints.filename.orEmpty()}"
    }

    playableDirectUrl?.trim()?.takeIf { it.isNotBlank() }?.let { url ->
        return "url:$url"
    }

    val fallbackParts = listOf(
        addonId,
        addonName,
        streamLabel,
        streamSubtitle.orEmpty(),
        behaviorHints.filename.orEmpty(),
        behaviorHints.videoSize?.toString().orEmpty(),
        sourceName.orEmpty(),
        sources.joinToString(","),
    ).map { it.trim() }
    return fallbackParts
        .takeIf { parts -> parts.any { it.isNotBlank() } }
        ?.joinToString(separator = "|", prefix = "meta:")
}

internal data class EpisodeResume(val positionMs: Long, val fraction: Float?)
