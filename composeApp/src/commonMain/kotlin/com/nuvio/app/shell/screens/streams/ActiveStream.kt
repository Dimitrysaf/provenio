package com.nuvio.app.shell.screens.streams

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nuvio.app.core.streams.StreamItem

/**
 * What names a stream, whatever it turns into on the way to the player.
 *
 * A torrent is its info hash, and the file inside it when the add-on names one: that is the
 * identity P2P streams under, and it survives the local URL the engine hands back. Anything else
 * is the URL it points at. Matching on the URL alone never recognised a torrent, which is why the
 * list used to highlight nothing.
 */
internal fun StreamItem.playbackIdentity(): String? {
    val infoHash = p2pInfoHash ?: infoHash ?: clientResolve?.infoHash
    val canonicalHash = infoHash?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    if (canonicalHash != null) {
        val file = p2pFileIdx ?: fileIdx
        return if (file != null) "$canonicalHash:$file" else canonicalHash
    }
    return playableDirectUrl ?: externalOpenUrl
}

/**
 * The stream playback is using right now, remembered against the video it belongs to so a
 * season pack cannot light up the wrong episode's list.
 */
object ActiveStreamStore {
    private var record by mutableStateOf<ActiveStreamRecord?>(null)

    fun set(videoId: String, stream: StreamItem) {
        val identity = stream.playbackIdentity() ?: return
        record = ActiveStreamRecord(videoId = videoId, identity = identity)
    }

    fun clear() {
        record = null
    }

    /** True when [stream] is the one being played for [videoId]. */
    fun isActive(videoId: String, stream: StreamItem): Boolean {
        val current = record ?: return false
        if (current.videoId != videoId) return false
        return current.identity == stream.playbackIdentity()
    }

    /** True when [stream] is the one being played, whatever it was opened from. */
    fun isActive(stream: StreamItem): Boolean {
        val current = record ?: return false
        return current.identity == stream.playbackIdentity()
    }
}

private data class ActiveStreamRecord(
    val videoId: String,
    val identity: String,
)
