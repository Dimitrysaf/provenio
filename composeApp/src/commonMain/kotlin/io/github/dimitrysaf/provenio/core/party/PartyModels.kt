package io.github.dimitrysaf.provenio.core.party

import io.github.dimitrysaf.provenio.core.localsync.LocalSyncFirewallPrompt
import kotlinx.serialization.Serializable

/** The room's rules. The host sets them; everyone else sees them as they are. */
@Serializable
data class PartySettings(
    /** Everyone in the room, the host included; null is no limit. */
    val memberLimit: Int? = null,
    val chatEnabled: Boolean = true,
    val suggestionsEnabled: Boolean = true,
    /** How long each member waits between chat messages; 0 is off. */
    val slowModeSeconds: Int = 0,
)

@Serializable
data class PartyMember(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
)

/** What the host is doing, which everyone else sees in the line at the top. */
@Serializable
enum class PartyActivityKind { IDLE, BROWSING, CHOOSING, WATCHING }

@Serializable
data class PartyActivity(
    val kind: PartyActivityKind = PartyActivityKind.IDLE,
    val title: String? = null,
)

/** The source the host opened, which every guest opens too. */
@Serializable
data class PartyPlayback(
    val id: Long,
    val title: String,
    val sourceUrl: String,
    val sourceAudioUrl: String? = null,
    val sourceHeaders: Map<String, String> = emptyMap(),
    val sourceResponseHeaders: Map<String, String> = emptyMap(),
    val streamType: String? = null,
    val logo: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeThumbnail: String? = null,
    val streamTitle: String,
    val streamSubtitle: String? = null,
    val providerName: String,
    val providerAddonId: String? = null,
    val contentType: String? = null,
    val videoId: String? = null,
    val parentMetaId: String,
    val parentMetaType: String,
    val torrentInfoHash: String? = null,
    val torrentFileIdx: Int? = null,
    val torrentFilename: String? = null,
    val torrentTrackers: List<String> = emptyList(),
)

/** Where the host is in [PartyPlayback] [playbackId], and whether it plays. */
@Serializable
data class PartyPlaybackState(
    val playbackId: Long,
    val isPlaying: Boolean,
    val positionMs: Long,
)

@Serializable
data class PartyChatMessage(
    val id: String,
    val memberId: String,
    val name: String,
    val text: String,
    val sentAtEpochMs: Long,
)

/** A title a guest would like to watch; the host picks the source. */
@Serializable
data class PartySuggestion(
    val id: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val type: String,
    val videoId: String,
    val parentMetaId: String,
    val parentMetaType: String,
    val title: String,
    val logo: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTitle: String? = null,
    val episodeThumbnail: String? = null,
)

/** How guests outside the host's network can reach the room. */
enum class PartyReachability {
    CHECKING,

    /** The router forwards the room's port, and its address is public. */
    OPEN,

    /** The router forwards the port, but sits behind another one, as with a carrier's shared address. */
    SHARED_ADDRESS,

    /** No router would forward the port; only an IPv6 address is offered. */
    IPV6_ONLY,

    /** Neither: only guests on the host's own network can join. */
    LOCAL_ONLY,
}

data class PartyRoom(
    val isHost: Boolean,
    val myMemberId: String,
    val settings: PartySettings,
    val members: List<PartyMember>,
    // The rest is only known to the host.
    val inviteLink: String? = null,
    val inviteQr: List<BooleanArray>? = null,
    val reachability: PartyReachability = PartyReachability.CHECKING,
    val firewall: LocalSyncFirewallPrompt? = null,
    val banned: List<PartyMember> = emptyList(),
) {
    val isFull: Boolean get() = settings.memberLimit?.let { members.size >= it } == true
    val hostName: String? get() = members.firstOrNull { it.isHost }?.name
}

sealed interface PartyPhase {
    data object Idle : PartyPhase
    data object Creating : PartyPhase
    data object Connecting : PartyPhase
    data class InRoom(val room: PartyRoom) : PartyPhase
}

enum class PartyError {
    NOT_SUPPORTED,
    NO_NETWORK,
    INVALID_LINK,
    UNREACHABLE,
    REJECTED,
    ROOM_FULL,
    BANNED,
    KICKED,
    CLOSED,
    CONNECTION_LOST,
    VERSION_MISMATCH,
    FAILED,
}

data class PartyUiState(
    val phase: PartyPhase = PartyPhase.Idle,
    /** Something to say once, then clear. */
    val notice: PartyError? = null,
)
