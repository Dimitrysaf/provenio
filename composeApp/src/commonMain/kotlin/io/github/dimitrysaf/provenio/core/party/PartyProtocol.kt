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
internal const val PARTY_PROTOCOL_VERSION = 4
internal const val PARTY_NONCE_SIZE = 16

internal val partyJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** What a guest says first, before anything is sealed: it proves it holds the link's secret. */
@Serializable
internal data class PartyHello(
    val version: Int,
    val memberId: String,
    val name: String,
    val nonce: String,
    val proof: String = "",
)

@Serializable
internal enum class PartyHelloStatus { ACCEPTED, FULL, BANNED, REJECTED, VERSION_MISMATCH }

@Serializable
internal data class PartyHelloReply(
    val version: Int,
    val status: PartyHelloStatus,
    val nonce: String = "",
    val proof: String = "",
)

@Serializable
internal enum class PartyRemoval { KICKED, BANNED, FULL, CLOSED }

/** Everything after the hello, sealed with the session's key. */
@Serializable
internal sealed interface PartyMessage {
    @Serializable
    @SerialName("welcome")
    data class Welcome(
        val settings: PartySettings,
        val members: List<PartyMember>,
        val activity: PartyActivity,
        val playback: PartyPlayback?,
        val playbackState: PartyPlaybackState?,
        val chat: List<PartyChatMessage>,
    ) : PartyMessage

    @Serializable
    @SerialName("members")
    data class Members(val members: List<PartyMember>) : PartyMessage

    @Serializable
    @SerialName("settings")
    data class Settings(val settings: PartySettings) : PartyMessage

    @Serializable
    @SerialName("activity")
    data class Activity(val activity: PartyActivity) : PartyMessage

    /** The host opened a source, or left the player when [playback] is null. */
    @Serializable
    @SerialName("playback")
    data class Playback(val playback: PartyPlayback?) : PartyMessage

    @Serializable
    @SerialName("playback_state")
    data class PlaybackState(val state: PartyPlaybackState) : PartyMessage

    /** A guest's message, which the host lets through or not. */
    @Serializable
    @SerialName("chat_send")
    data class ChatSend(val text: String) : PartyMessage

    @Serializable
    @SerialName("chat")
    data class Chat(val message: PartyChatMessage) : PartyMessage

    @Serializable
    @SerialName("suggest")
    data class Suggest(val suggestion: PartySuggestion) : PartyMessage

    /** A guest plays, pauses or seeks for the room, which the host does where the room lets them. */
    @Serializable
    @SerialName("control")
    data class Control(
        val playbackId: Long,
        val action: PartyControlAction,
        val positionMs: Long,
    ) : PartyMessage

    /** Whether a guest's player is loading the host's source, for a room that waits for them. */
    @Serializable
    @SerialName("loading")
    data class Loading(val playbackId: Long, val loading: Boolean) : PartyMessage

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
