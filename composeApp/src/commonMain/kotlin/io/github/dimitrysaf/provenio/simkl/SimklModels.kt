package io.github.dimitrysaf.provenio.simkl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimklPin(
    @SerialName("result") val result: String? = null,
    @SerialName("user_code") val userCode: String? = null,
    // The reference calls this verification_uri, older responses use verification_url.
    // Both are read so a change on either side does not break sign in.
    @SerialName("verification_uri") val verificationUri: String? = null,
    @SerialName("verification_url") val verificationUrl: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Int? = null,
    @SerialName("interval") val pollIntervalSeconds: Int? = null,
) {
    val verificationPage: String
        get() = verificationUri ?: verificationUrl ?: "https://simkl.com/pin"
}

@Serializable
data class SimklPinPoll(
    @SerialName("result") val result: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
)

/**
 * One step of the sign in poll.
 *
 * [Unreachable] is deliberately separate from [Rejected]. Sending the user to a browser
 * backgrounds the app, and Android restricts background network, so failing to ask is a
 * normal event during sign in and must not end it. Only Simkl actually refusing does.
 */
sealed interface PinStatus {
    data object Pending : PinStatus
    data class Authorized(val accessToken: String) : PinStatus
    data class Rejected(val message: String?) : PinStatus
    data class Unreachable(val reason: String?) : PinStatus
}

/** What the Simkl settings screen is doing right now. */
sealed interface SimklAuthState {
    data object SignedOut : SimklAuthState
    data object Starting : SimklAuthState
    data class AwaitingUser(
        val userCode: String,
        val verificationPage: String,
        val secondsRemaining: Int,
        /** Set while polls are failing, so the screen can say so without giving up. */
        val note: String? = null,
    ) : SimklAuthState
    data object SignedIn : SimklAuthState
    data class Error(val message: String) : SimklAuthState
}

@Serializable
data class SimklSettings(
    @SerialName("user") val user: SimklUserProfile? = null,
    @SerialName("account") val account: SimklAccount? = null,
)

@Serializable
data class SimklUserProfile(
    @SerialName("name") val name: String? = null,
    // Already a full URL in this response, unlike poster and fanart which are paths.
    @SerialName("avatar") val avatar: String? = null,
)

@Serializable
data class SimklAccount(
    @SerialName("id") val id: Long? = null,
    @SerialName("type") val type: String? = null,
)
