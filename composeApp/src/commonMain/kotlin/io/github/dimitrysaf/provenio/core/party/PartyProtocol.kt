package io.github.dimitrysaf.provenio.core.party

import io.github.dimitrysaf.provenio.core.localsync.LocalSyncConnection
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncPlatform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Raised whenever a message changes in a way an older build would misread; host and guests must match.
internal const val PARTY_PROTOCOL_VERSION = 1
internal const val PARTY_NONCE_SIZE = 16

internal val partyJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal enum class PartyJoinMode { LINK, CODE }

/**
 * What a guest says first, before anything is sealed. With a link it proves it holds the
 * room's secret; with a code it sends its half of a key exchange instead.
 */
@Serializable
internal data class PartyHello(
    val version: Int,
    val mode: PartyJoinMode,
    val code: String,
    val memberId: String,
    val name: String,
    val nonce: String,
    val proof: String? = null,
    val publicKey: String? = null,
)

@Serializable
internal enum class PartyHelloStatus {
    /** In the room; the welcome follows. */
    ACCEPTED,

    /** Waiting for the host to let them in. */
    PENDING,
    FULL,
    BANNED,
    REJECTED,
    VERSION_MISMATCH,
}

@Serializable
internal data class PartyHelloReply(
    val version: Int,
    val status: PartyHelloStatus,
    val nonce: String = "",
    val proof: String? = null,
    val publicKey: String? = null,
)

@Serializable
internal enum class PartyRemoval { KICKED, BANNED, DENIED, FULL, CLOSED }

/** Everything after the hello, sealed with the session's key. */
@Serializable
internal sealed interface PartyMessage {
    @Serializable
    @SerialName("welcome")
    data class Welcome(
        val settings: PartySettings,
        val members: List<PartyMember>,
    ) : PartyMessage

    @Serializable
    @SerialName("members")
    data class Members(val members: List<PartyMember>) : PartyMessage

    @Serializable
    @SerialName("settings")
    data class Settings(val settings: PartySettings) : PartyMessage

    @Serializable
    @SerialName("removed")
    data class Removed(val reason: PartyRemoval) : PartyMessage

    @Serializable
    @SerialName("ping")
    data object Ping : PartyMessage

    @Serializable
    @SerialName("pong")
    data object Pong : PartyMessage

    @Serializable
    @SerialName("leave")
    data object Leave : PartyMessage
}

/** A room announcing its code on this network, for guests who only have the code. */
@Serializable
internal data class PartyBeacon(
    val app: String,
    val code: String,
    val port: Int,
    val hostId: String,
)

internal const val PARTY_BEACON_APP = "provenio-party"

internal fun partyProof(secret: ByteArray, vararg parts: String): String =
    encodePartyBytes(LocalSyncPlatform.hmacSha256(secret, parts.joinToString("|").encodeToByteArray()))

internal fun partySessionKey(secret: ByteArray, vararg parts: String): ByteArray =
    LocalSyncPlatform.hmacSha256(secret, (listOf("key") + parts).joinToString("|").encodeToByteArray())

internal suspend fun LocalSyncConnection.writePartyHello(hello: PartyHello) =
    writeFrame(partyJson.encodeToString(PartyHello.serializer(), hello).encodeToByteArray())

internal suspend fun LocalSyncConnection.readPartyHello(): PartyHello =
    partyJson.decodeFromString(PartyHello.serializer(), readFrame().decodeToString())

internal suspend fun LocalSyncConnection.writePartyReply(reply: PartyHelloReply) =
    writeFrame(partyJson.encodeToString(PartyHelloReply.serializer(), reply).encodeToByteArray())

internal suspend fun LocalSyncConnection.readPartyReply(): PartyHelloReply =
    partyJson.decodeFromString(PartyHelloReply.serializer(), readFrame().decodeToString())

/** One sealed connection between the host and a guest. Sends may come from several places at once. */
internal class PartyChannel(
    private val connection: LocalSyncConnection,
    private val key: ByteArray,
) {
    private val writeLock = Mutex()

    suspend fun send(message: PartyMessage) = writeLock.withLock {
        val plain = partyJson.encodeToString(PartyMessage.serializer(), message).encodeToByteArray()
        connection.writeFrame(LocalSyncPlatform.encrypt(key, plain))
    }

    /** The next message, or null for one this build does not know, which a newer one may send. */
    suspend fun receive(): PartyMessage? {
        val plain = LocalSyncPlatform.decrypt(key, connection.readFrame()).decodeToString()
        return try {
            partyJson.decodeFromString(PartyMessage.serializer(), plain)
        } catch (_: SerializationException) {
            null
        }
    }

    fun close() = connection.close()
}
