package io.github.dimitrysaf.provenio.p2p

enum class TorrentProfile(val label: String) {
    Slow("Slow"),
    Balanced("Balanced"),
    Fast("Fast"),
}

enum class CacheSize(val label: String, val bytes: Long) {
    None("No caching", 0L),
    Gb2("2G", 2L * 1024 * 1024 * 1024),
    Gb5("5G", 5L * 1024 * 1024 * 1024),
    Gb10("10G", 10L * 1024 * 1024 * 1024),
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

enum class P2pServiceState(val label: String) {
    Disabled("Disabled"),
    NotBuilt("Not built"),
    Offline("Offline"),
    Online("Online"),
}

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
