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
    val guestPause: PartyGuestPause = PartyGuestPause.SUGGEST,
    val guestSeek: PartyGuestSeek = PartyGuestSeek.SUGGEST,
    val loading: PartyLoading = PartyLoading.PAUSE_EVERYONE,
    /** How long the room waits for someone loading; null is as long as it takes. */
    val loadingWaitSeconds: Int? = null,
    val syncTolerance: PartySyncTolerance = PartySyncTolerance.TIGHT,
    /** Someone with the link waits until the host lets them in. */
    val askToJoin: Boolean = true,
    /** Guests can vote on each other's suggestions in the chat. */
    val pollsEnabled: Boolean = false,
    /** Guests switch to the subtitle and audio languages the host picks. */
    val matchTracks: Boolean = false,
    /** Under [PartyGuestSeek.SEEK_EVERYONE], guests can change the speed for everyone too. */
    val guestSpeed: Boolean = false,
    /** Guests open the host's own stream links, such as a debrid link, rather than finding their own source. */
    val shareStreamLinks: Boolean = false,
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

    /** The guest's pause or play asks the host, who answers with Yes in the chat. */
    SUGGEST,
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

    /** The guest's seek asks the host, who answers with Yes in the chat. */
    SUGGEST,
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
enum class PartyControlAction { PLAY, PAUSE, SEEK, SPEED }

/** A guest's play, pause, seek or speed, which the host's player carries out for the room. */
data class PartyControl(
    val action: PartyControlAction,
    val positionMs: Long,
    val memberName: String,
    val speed: Float = 1f,
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
    /** The host's link isn't shared: each guest plays the same title from a source of their own. */
    val ownSource: Boolean = false,
    /** The release the host plays, by its add-on's binge group, which guests finding their own source pick first. */
    val bingeGroup: String? = null,
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
    val speed: Float = 1f,
    /** The host's audio language, for guests who match it; null when unknown. */
    val audioLanguage: String? = null,
    /** The host's subtitle language, "" for none; null when unknown. */
    val subtitleLanguage: String? = null,
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
    /** Set when a guest asks the host for something, which the host answers with Yes. */
    val request: PartyRequest? = null,
    /** Where a seek note moved the room to. */
    val positionMs: Long? = null,
    /** The speed a speed note set. */
    val speed: Float? = null,
    /** Who voted for a suggestion, when the room has polls. */
    val votes: List<String> = emptyList(),
)

/** What a guest asks the host for: a title to watch, or to play, pause or seek the room. */
@Serializable
data class PartyRequest(
    val playbackId: Long? = null,
    val action: PartyControlAction? = null,
    val positionMs: Long = 0L,
    val suggestion: PartySuggestion? = null,
    val speed: Float = 1f,
    /** Someone waiting to join; only the host sees this one. */
    val join: Boolean = false,
)

/** What the room says in the chat about [PartyChatMessage.name]. */
@Serializable
enum class PartyChatEvent { JOINED, LEFT, REMOVED, PLAYED, PAUSED, SEEKED, SPEED }

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
    /** This guest lost the connection and is trying to get back in. */
    val reconnecting: Boolean = false,
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

    /** The room asks the host before letting this guest in. */
    data object Waiting : PartyPhase
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

    /** The host didn't let this guest in. */
    DENIED,
    FAILED,
}

data class PartyUiState(
    val phase: PartyPhase = PartyPhase.Idle,
    /** Something to say once, then clear. */
    val notice: PartyError? = null,
)
