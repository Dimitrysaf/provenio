package io.github.dimitrysaf.provenio.core.localsync

import kotlin.io.encoding.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PROTOCOL_VERSION = 1
private const val NONCE_SIZE = 16

private val sessionJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** What each device says about itself before anything is exchanged; the proof shows it holds the secret. */
@Serializable
internal data class SyncHello(
    val version: Int,
    val deviceId: String,
    val name: String,
    val nonce: String,
    val proof: String,
    val host: String? = null,
    val port: Int? = null,
    val hasSyncedBefore: Boolean = false,
)

@Serializable
private data class SyncExchange(val ledger: SyncLedger)

/** This device, as the other one will see it. */
internal data class LocalSyncIdentity(
    val deviceId: String,
    val name: String,
    val host: String?,
    val port: Int?,
)

/** What a finished session learned about the other device. */
internal data class LocalSyncOutcome(
    val peerHello: SyncHello,
    val secret: ByteArray,
    val merge: LocalSyncMerge,
)

internal class LocalSyncRejectedException : Exception("The other device did not accept this pairing")

internal fun encodeSyncBytes(bytes: ByteArray): String = Base64.UrlSafe.encode(bytes)

internal fun decodeSyncBytes(value: String): ByteArray = Base64.UrlSafe.decode(value)

private fun proof(secret: ByteArray, vararg parts: String): String =
    encodeSyncBytes(LocalSyncPlatform.hmacSha256(secret, parts.joinToString("|").encodeToByteArray()))

private fun sessionKey(secret: ByteArray, clientNonce: String, serverNonce: String): ByteArray =
    LocalSyncPlatform.hmacSha256(secret, "key|$clientNonce|$serverNonce".encodeToByteArray())

private suspend fun LocalSyncConnection.writeJson(value: String) = writeFrame(value.encodeToByteArray())

private suspend fun LocalSyncConnection.readJson(): String = readFrame().decodeToString()

private suspend fun LocalSyncConnection.writeSealed(key: ByteArray, ledger: SyncLedger) =
    writeFrame(LocalSyncPlatform.encrypt(key, sessionJson.encodeToString(SyncExchange(ledger)).encodeToByteArray()))

private suspend fun LocalSyncConnection.readSealed(key: ByteArray): SyncLedger =
    sessionJson.decodeFromString<SyncExchange>(LocalSyncPlatform.decrypt(key, readFrame()).decodeToString()).ledger

// The side that connects. It proves it holds [secret], checks the other side does too, sends its ledger first and then merges the one that comes back.
internal suspend fun runClientSession(
    connection: LocalSyncConnection,
    identity: LocalSyncIdentity,
    secret: ByteArray,
    expectedPeerId: String?,
    hasSyncedBefore: Boolean,
    ledger: SyncLedger,
    applyMerge: (SyncLedger, Boolean) -> LocalSyncMerge,
): LocalSyncOutcome {
    val nonce = encodeSyncBytes(LocalSyncPlatform.randomBytes(NONCE_SIZE))
    val hello = SyncHello(
        version = PROTOCOL_VERSION,
        deviceId = identity.deviceId,
        name = identity.name,
        nonce = nonce,
        proof = proof(secret, "client", nonce, identity.deviceId, hasSyncedBefore.toString()),
        host = identity.host,
        port = identity.port,
        hasSyncedBefore = hasSyncedBefore,
    )
    connection.writeJson(sessionJson.encodeToString(hello))

    val reply = sessionJson.decodeFromString<SyncHello>(connection.readJson())
    val expectedProof = proof(secret, "server", nonce, reply.nonce, reply.deviceId, reply.hasSyncedBefore.toString())
    if (reply.version != PROTOCOL_VERSION || reply.proof != expectedProof) throw LocalSyncRejectedException()
    if (expectedPeerId != null && reply.deviceId != expectedPeerId) throw LocalSyncRejectedException()

    val key = sessionKey(secret, nonce, reply.nonce)
    connection.writeSealed(key, ledger)
    val remote = connection.readSealed(key)
    // The server reports whether both sides had synced before; on a first sync the host decides.
    val firstSync = !reply.hasSyncedBefore
    val merge = applyMerge(remote, firstSync)
    return LocalSyncOutcome(peerHello = reply, secret = secret, merge = merge)
}

// The side that listens. [secretFor] offers the secrets the client may be using: the pairing code on show, and the one kept for a device already paired.
internal suspend fun runServerSession(
    connection: LocalSyncConnection,
    identity: LocalSyncIdentity,
    secretFor: (deviceId: String) -> List<Pair<ByteArray, Boolean>>,
    applyMerge: (SyncLedger, Boolean) -> LocalSyncMerge,
): LocalSyncOutcome {
    val hello = sessionJson.decodeFromString<SyncHello>(connection.readJson())
    if (hello.version != PROTOCOL_VERSION) throw LocalSyncRejectedException()
    val (secret, knowsClient) = secretFor(hello.deviceId).firstOrNull { (candidate, _) ->
        hello.proof == proof(candidate, "client", hello.nonce, hello.deviceId, hello.hasSyncedBefore.toString())
    } ?: throw LocalSyncRejectedException()

    val bothSyncedBefore = knowsClient && hello.hasSyncedBefore
    val nonce = encodeSyncBytes(LocalSyncPlatform.randomBytes(NONCE_SIZE))
    val reply = SyncHello(
        version = PROTOCOL_VERSION,
        deviceId = identity.deviceId,
        name = identity.name,
        nonce = nonce,
        proof = proof(secret, "server", hello.nonce, nonce, identity.deviceId, bothSyncedBefore.toString()),
        host = identity.host,
        port = identity.port,
        hasSyncedBefore = bothSyncedBefore,
    )
    connection.writeJson(sessionJson.encodeToString(reply))

    val key = sessionKey(secret, hello.nonce, nonce)
    val remote = connection.readSealed(key)
    // The host is the source of truth the first time two devices sync.
    val merge = applyMerge(remote, !bothSyncedBefore)
    connection.writeSealed(key, merge.ledger)
    return LocalSyncOutcome(peerHello = hello, secret = secret, merge = merge)
}
