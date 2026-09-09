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

/** One step of the sign in poll. */
sealed interface PinStatus {
    data object Pending : PinStatus
    data class Authorized(val accessToken: String) : PinStatus
    data class Failed(val message: String?) : PinStatus
}

/** What the Simkl settings screen is doing right now. */
sealed interface SimklAuthState {
    data object SignedOut : SimklAuthState
    data object Starting : SimklAuthState
    data class AwaitingUser(
        val userCode: String,
        val verificationPage: String,
        val secondsRemaining: Int,
    ) : SimklAuthState
    data object SignedIn : SimklAuthState
    data class Error(val message: String) : SimklAuthState
}
