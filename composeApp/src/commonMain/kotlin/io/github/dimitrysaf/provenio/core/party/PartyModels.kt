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
    val guestPause: PartyGuestPause = PartyGuestPause.SNAP_TO_HOST,
    val guestSeek: PartyGuestSeek = PartyGuestSeek.SNAP_BACK,
    val loading: PartyLoading = PartyLoading.KEEP_UP,
    /** How long the room waits for someone loading; null is as long as it takes. */
    val loadingWaitSeconds: Int? = 30,
    val syncTolerance: PartySyncTolerance = PartySyncTolerance.NORMAL,
)

/** What happens when a guest pauses, and then plays again. */
@Serializable
enum class PartyGuestPause {
    /** The guest's pause pauses the room, and their play plays it. */
    PAUSE_EVERYONE,

    /** The room carries on; the guest picks up where they paused, on their own. */
    CONTINUE_WHERE_LEFT,

    /** The room carries on; when the guest plays, they jump to where the host is now. */
    SNAP_TO_HOST,

    /** Guests have no pause; the room plays and pauses with the host only. */
    HOST_ONLY,
}

/** What happens when a guest moves to another moment. */
@Serializable
enum class PartyGuestSeek {
    /** The room moves there with them. */
    SEEK_EVERYONE,

    /** They watch from there on their own, until they catch up. */
    ON_THEIR_OWN,

    /** Only the host moves the room; the guest goes back to where the host is. */
    SNAP_BACK,
}

/** What the room does while someone's player is loading. */
@Serializable
enum class PartyLoading {
    /** The host pauses until everyone has loaded, for at most [PartySettings.loadingWaitSeconds]. */
    PAUSE_EVERYONE,

    /** The player of whoever is loading keeps up with the host's time, and loads from there. */
    KEEP_UP,

    /** Whoever is loading carries on from where they stopped, behind the room. */
    CONTINUE,
}

/** How far a guest may drift from the host before their player is put back in step. */
@Serializable
enum class PartySyncTolerance(val driftMs: Long) {
    TIGHT(1_000),
    NORMAL(3_000),
    RELAXED(8_000),
}

/** What a guest asks the host's player to do for everyone. */
@Serializable
enum class PartyControlAction { PLAY, PAUSE, SEEK }

/** A guest's play, pause or seek, which the host's player carries out for the room. */
data class PartyControl(
    val action: PartyControlAction,
    val positionMs: Long,
    val memberName: String,
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
    /** Whether the host means to play; it may still be loading. */
    val isPlaying: Boolean,
    val positionMs: Long,
    /** The host's player is loading, so its position stands still. */
    val isLoading: Boolean = false,
    /** Counts the host's jumps, so everyone keeping to the room jumps with it. */
    val seekCount: Int = 0,
    /** Who the room is paused for while they load. */
    val waitingFor: List<String> = emptyList(),
    /** The guest who last played, paused or moved the room; null when it was the host. */
    val changedBy: String? = null,
)

@Serializable
data class PartyChatMessage(
    val id: String,
    val memberId: String,
    val name: String,
    val text: String,
    val sentAtEpochMs: Long,
    /** Set for the room's own notes, such as someone joining, which have no text. */
    val event: PartyChatEvent? = null,
)

/** What the room says in the chat about [PartyChatMessage.name]. */
@Serializable
enum class PartyChatEvent { JOINED, LEFT, REMOVED }

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

data class PartyRoom(
    val isHost: Boolean,
    val myMemberId: String,
    val settings: PartySettings,
    val members: List<PartyMember>,
    /** The room's address on the internet, as the host opened it or as this guest reached it. */
    val address: String? = null,
    // The rest is only known to the host.
    val inviteLink: String? = null,
    val inviteQr: List<BooleanArray>? = null,
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

    /** The router opened no port, so no one outside could reach the room. */
    PORT_CLOSED,

    /** The port is open, but behind an address the provider shares, so no one outside could reach it. */
    SHARED_ADDRESS,
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
