package io.github.dimitrysaf.provenio.p2p

import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.p2p_cache_10g
import io.github.dimitrysaf.provenio.resources.p2p_cache_2g
import io.github.dimitrysaf.provenio.resources.p2p_cache_5g
import io.github.dimitrysaf.provenio.resources.p2p_cache_none
import io.github.dimitrysaf.provenio.resources.p2p_profile_balanced
import io.github.dimitrysaf.provenio.resources.p2p_profile_fast
import io.github.dimitrysaf.provenio.resources.p2p_profile_slow
import io.github.dimitrysaf.provenio.resources.p2p_state_disabled
import io.github.dimitrysaf.provenio.resources.p2p_state_not_built
import io.github.dimitrysaf.provenio.resources.p2p_state_offline
import io.github.dimitrysaf.provenio.resources.p2p_state_online
import org.jetbrains.compose.resources.StringResource

enum class TorrentProfile(val label: StringResource) {
    Slow(Res.string.p2p_profile_slow),
    Balanced(Res.string.p2p_profile_balanced),
    Fast(Res.string.p2p_profile_fast),
}

// The sizes read the same in every language, but they still come from the resources so
// every label in the app is looked up the same way and none can be missed.
enum class CacheSize(val label: StringResource, val bytes: Long) {
    None(Res.string.p2p_cache_none, 0L),
    Gb2(Res.string.p2p_cache_2g, 2L * 1024 * 1024 * 1024),
    Gb5(Res.string.p2p_cache_5g, 5L * 1024 * 1024 * 1024),
    Gb10(Res.string.p2p_cache_10g, 10L * 1024 * 1024 * 1024),
}

/**
 * Peer-to-peer configuration.
 *
 * [enabled] is false out of the box and stays false until the user has read and accepted
 * [consentAccepted] — joining a swarm exposes the user's IP address to every other peer,
 * which is not something to switch on for them.
 */
data class P2pSettings(
    val enabled: Boolean = false,
    val consentAccepted: Boolean = false,
    /** Off by default. Distributing is treated far more seriously than receiving. */
    val uploadEnabled: Boolean = false,
    val profile: TorrentProfile = TorrentProfile.Balanced,
    val cacheSize: CacheSize = CacheSize.Gb2,
    /** 0 asks the engine to pick a free port. */
    val listenPort: Int = 0,
    val hideStats: Boolean = false,
)

enum class P2pServiceState(val label: StringResource) {
    Disabled(Res.string.p2p_state_disabled),
    NotBuilt(Res.string.p2p_state_not_built),
    Offline(Res.string.p2p_state_offline),
    Online(Res.string.p2p_state_online),
}

/**
 * A torrent as an addon described it.
 *
 * Addons hand over an info hash rather than a magnet, usually with the trackers they know
 * about and sometimes with the index of the file they mean. All three are worth carrying:
 * the trackers save waiting on the DHT, and the index saves guessing which of the files in
 * a release is the feature.
 */
data class TorrentRequest(
    val infoHash: String,
    val sources: List<String> = emptyList(),
    val fileIndex: Int? = null,
)

/** Live engine readings. All zeroed while the service is not running. */
data class P2pStatus(
    val state: P2pServiceState = P2pServiceState.Disabled,
    val listenPort: Int? = null,
    val portInUse: Boolean = false,
    /** Localhost root the player reads from while the service is up. */
    val baseUrl: String? = null,
    val localAddresses: List<String> = emptyList(),
    val publicAddress: String? = null,
    val peers: Int = 0,
    val seeds: Int = 0,
    val downloadBytesPerSecond: Long = 0,
    val uploadBytesPerSecond: Long = 0,
    val sessionDownloadedBytes: Long = 0,
    val sessionUploadedBytes: Long = 0,
    val cacheUsedBytes: Long = 0,
    val activeTorrents: Int = 0,
    /** Why the engine is in [P2pServiceState.Failed], or any note worth surfacing. */
    val detail: String? = null,
)

/** Human-readable byte count. Binary units, because that is what the engine reports. */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val rounded = (value * 10).toLong() / 10.0
    return "$rounded ${units[unit]}"
}

fun formatSpeed(bytesPerSecond: Long): String =
    if (bytesPerSecond <= 0) "—" else "${formatBytes(bytesPerSecond)}/s"
