package io.github.dimitrysaf.provenio.core.party

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

/** Someone who joined with the room's code and waits for the host to let them in. */
data class PartyJoinRequest(
    val memberId: String,
    val name: String,
)

data class PartyRoom(
    val code: String,
    val isHost: Boolean,
    val myMemberId: String,
    val settings: PartySettings,
    val members: List<PartyMember>,
    // The rest is only known to the host.
    val inviteLink: String? = null,
    val inviteQr: List<BooleanArray>? = null,
    val joinRequests: List<PartyJoinRequest> = emptyList(),
    val banned: List<PartyMember> = emptyList(),
) {
    val isFull: Boolean get() = settings.memberLimit?.let { members.size >= it } == true
}

sealed interface PartyPhase {
    data object Idle : PartyPhase
    data object Creating : PartyPhase

    /** Listening on this network for the room that announces the code typed in. */
    data object Finding : PartyPhase
    data object Connecting : PartyPhase

    /** Joined with a code: the host has to let this device in. */
    data object AwaitingApproval : PartyPhase
    data class InRoom(val room: PartyRoom) : PartyPhase
}

enum class PartyError {
    NOT_SUPPORTED,
    NO_NETWORK,
    INVALID_LINK,
    NO_ROOM_FOR_CODE,
    UNREACHABLE,
    REJECTED,
    ROOM_FULL,
    BANNED,
    DENIED,
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
