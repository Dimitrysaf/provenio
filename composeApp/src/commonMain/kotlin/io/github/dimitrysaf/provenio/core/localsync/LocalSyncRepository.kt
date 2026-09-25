package io.github.dimitrysaf.provenio.core.localsync

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.sync.SyncClientIdentity
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

enum class LocalSyncError {
    NOT_ON_WIFI,
    UNREACHABLE,
    REJECTED,
    INVALID_CODE,
    FAILED,
}

sealed interface LocalSyncActivity {
    data object Idle : LocalSyncActivity
    data class Syncing(val peerName: String?) : LocalSyncActivity
    data class Synced(val peerName: String, val changeCount: Int) : LocalSyncActivity
    data class Failed(val error: LocalSyncError) : LocalSyncActivity
}

data class LocalSyncUiState(
    val deviceName: String = "",
    val peers: List<LocalSyncPeer> = emptyList(),
    val pairingCode: String? = null,
    val pairingQr: List<BooleanArray>? = null,
    val activity: LocalSyncActivity = LocalSyncActivity.Idle,
)

// Syncs the active profile with other devices on the same Wi-Fi. While the sync page is open this device listens, so a paired device can start a sync from its side too.
object LocalSyncRepository {
    private const val DEFAULT_PORT = 47_631
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val PORT_WAIT_MS = 5_000L
    private const val SECRET_SIZE = 32
    private const val CODE_PREFIX = "provenio-sync"
    private const val CODE_VERSION = "1"

    private val log = Logger.withTag("LocalSync")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sessionMutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val peersSerializer = ListSerializer(LocalSyncPeer.serializer())

    private val _uiState = MutableStateFlow(LocalSyncUiState())
    val uiState: StateFlow<LocalSyncUiState> = _uiState.asStateFlow()

    private val serverPort = MutableStateFlow<Int?>(null)
    private var server: LocalSyncServer? = null
    private var listenJob: Job? = null
    private var pairingSecret: ByteArray? = null

    val isSupported: Boolean get() = LocalSyncPlatform.isSupported

    // Starts listening for paired devices; the sync page calls it while it is on screen.
    fun open() {
        if (!isSupported) return
        _uiState.update { it.copy(deviceName = localSyncDeviceName(), peers = loadPeers()) }
        if (listenJob?.isActive == true) return
        listenJob = scope.launch { listen() }
    }

    fun close() {
        stopPairing()
        listenJob?.cancel()
        listenJob = null
        server?.close()
        server = null
        serverPort.value = null
    }

    /** Shows a pairing code for another device to scan or type in. */
    fun startPairing() {
        if (!isSupported) return
        scope.launch {
            val host = LocalSyncPlatform.localIpv4Address()
                ?: return@launch fail(LocalSyncError.NOT_ON_WIFI)
            val port = withTimeoutOrNull(PORT_WAIT_MS) { serverPort.filterNotNull().first() }
                ?: return@launch fail(LocalSyncError.FAILED)
            val secret = LocalSyncPlatform.randomBytes(SECRET_SIZE)
            pairingSecret = secret
            val code = listOf(CODE_PREFIX, CODE_VERSION, host, port.toString(), encodeSyncBytes(secret))
                .joinToString(":")
            _uiState.update {
                it.copy(pairingCode = code, pairingQr = localSyncQrMatrix(code), activity = LocalSyncActivity.Idle)
            }
        }
    }

    fun stopPairing() {
        pairingSecret = null
        _uiState.update { it.copy(pairingCode = null, pairingQr = null) }
    }

    /** Pairs with the device showing [code] and syncs with it straight away. */
    fun join(code: String) {
        val pairing = parsePairingCode(code.trim()) ?: return fail(LocalSyncError.INVALID_CODE)
        scope.launch {
            connectAndSync(
                host = pairing.host,
                port = pairing.port,
                secret = pairing.secret,
                expectedPeerId = null,
                hasSyncedBefore = false,
                peerName = null,
            )
        }
    }

    fun syncWith(peer: LocalSyncPeer) {
        scope.launch {
            connectAndSync(
                host = peer.host,
                port = peer.port,
                secret = decodeSyncBytes(peer.secret),
                expectedPeerId = peer.deviceId,
                hasSyncedBefore = peer.lastSyncedAtEpochMs != null,
                peerName = peer.name,
            )
        }
    }

    fun forget(peer: LocalSyncPeer) {
        savePeers(loadPeers().filterNot { it.deviceId == peer.deviceId })
    }

    fun clearActivity() {
        _uiState.update { it.copy(activity = LocalSyncActivity.Idle) }
    }

    private suspend fun listen() {
        val listening = try {
            LocalSyncPlatform.listen(DEFAULT_PORT)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Could not start listening for sync" }
            return
        }
        server = listening
        serverPort.value = listening.port
        try {
            while (currentCoroutineContext().isActive) {
                val connection = listening.accept()
                scope.launch { serve(connection) }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.d { "Stopped listening for sync: ${error.message}" }
        } finally {
            listening.close()
            if (server === listening) {
                server = null
                serverPort.value = null
            }
        }
    }

    private suspend fun serve(connection: LocalSyncConnection) {
        sessionMutex.withLock {
            try {
                val outcome = runServerSession(
                    connection = connection,
                    identity = identity(),
                    secretFor = ::candidateSecrets,
                ) { remote, firstSync ->
                    _uiState.update { it.copy(activity = LocalSyncActivity.Syncing(null)) }
                    commitMerge(refreshedLedger(), remote, if (firstSync) SyncConflictPolicy.KEEP_LOCAL else SyncConflictPolicy.NEWEST)
                }
                if (pairingSecret?.contentEquals(outcome.secret) == true) stopPairing()
                rememberPeer(outcome, fallbackHost = null, fallbackPort = null)
                succeed(outcome)
            } catch (error: CancellationException) {
                throw error
            } catch (error: LocalSyncRejectedException) {
                log.i { "Refused a sync from a device without the right code" }
            } catch (error: Throwable) {
                log.w(error) { "Sync from another device failed" }
                fail(LocalSyncError.FAILED)
            } finally {
                connection.close()
            }
        }
    }

    private suspend fun connectAndSync(
        host: String,
        port: Int,
        secret: ByteArray,
        expectedPeerId: String?,
        hasSyncedBefore: Boolean,
        peerName: String?,
    ) {
        sessionMutex.withLock {
            _uiState.update { it.copy(activity = LocalSyncActivity.Syncing(peerName)) }
            val connection = try {
                LocalSyncPlatform.connect(host, port, CONNECT_TIMEOUT_MS)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.w(error) { "Could not reach $host:$port" }
                fail(LocalSyncError.UNREACHABLE)
                return@withLock
            }
            try {
                val ledger = refreshedLedger()
                val outcome = runClientSession(
                    connection = connection,
                    identity = identity(),
                    secret = secret,
                    expectedPeerId = expectedPeerId,
                    hasSyncedBefore = hasSyncedBefore,
                    ledger = ledger,
                ) { remote, firstSync ->
                    commitMerge(ledger, remote, if (firstSync) SyncConflictPolicy.TAKE_REMOTE else SyncConflictPolicy.NEWEST)
                }
                rememberPeer(outcome, fallbackHost = host, fallbackPort = port)
                succeed(outcome)
            } catch (error: CancellationException) {
                throw error
            } catch (error: LocalSyncRejectedException) {
                fail(LocalSyncError.REJECTED)
            } catch (error: Throwable) {
                log.w(error) { "Sync with $host:$port failed" }
                fail(LocalSyncError.FAILED)
            } finally {
                connection.close()
            }
        }
    }

    private fun candidateSecrets(deviceId: String): List<Pair<ByteArray, Boolean>> {
        val peer = loadPeers().firstOrNull { it.deviceId == deviceId }
        val syncedBefore = peer?.lastSyncedAtEpochMs != null
        val stored = peer?.secret?.let { runCatching { decodeSyncBytes(it) }.getOrNull() }
        return listOfNotNull(pairingSecret, stored).map { it to syncedBefore }
    }

    private fun identity(): LocalSyncIdentity = LocalSyncIdentity(
        deviceId = SyncClientIdentity.currentClientId(),
        name = localSyncDeviceName(),
        host = LocalSyncPlatform.localIpv4Address(),
        port = serverPort.value,
    )

    // Stamps local edits since the last sync, leaving out any source that could not be read.
    private fun refreshedLedger(): SyncLedger {
        val sources = localSyncSources()
        val snapshot = mutableMapOf<String, JsonElement>()
        val readablePrefixes = mutableListOf<String>()
        sources.forEach { source ->
            runCatching { source.snapshot() }
                .onSuccess { values ->
                    snapshot.putAll(values)
                    readablePrefixes += source.prefix
                }
                .onFailure { error -> log.w(error) { "Could not read ${source.prefix} for sync" } }
        }
        val refreshed = refreshSyncLedger(
            ledger = loadLedger(),
            current = snapshot,
            ownedPrefixes = readablePrefixes,
            deviceId = SyncClientIdentity.currentClientId(),
        )
        saveLedger(refreshed)
        return refreshed
    }

    private fun commitMerge(local: SyncLedger, remote: SyncLedger, policy: SyncConflictPolicy): LocalSyncMerge {
        val merge = mergeSyncLedgers(local, remote, policy)
        val sources = localSyncSources()
        val changesBySource = merge.changes.entries.groupBy { (key, _) ->
            sources.firstOrNull { key.startsWith(it.prefix) }
        }
        val current = mutableMapOf<String, JsonElement>()
        changesBySource.forEach { (source, entries) ->
            source ?: return@forEach
            runCatching { source.apply(entries.associate { it.key to it.value }) }
                .onFailure { error -> log.w(error) { "Could not apply synced ${source.prefix}" } }
            runCatching { current.putAll(source.snapshot()) }
        }
        val adopted = adoptAppliedValues(merge.ledger, current, merge.changes.keys)
        saveLedger(adopted)
        return merge.copy(ledger = adopted)
    }

    private fun rememberPeer(outcome: LocalSyncOutcome, fallbackHost: String?, fallbackPort: Int?) {
        val hello = outcome.peerHello
        val peers = loadPeers()
        val previous = peers.firstOrNull { it.deviceId == hello.deviceId }
        val peer = LocalSyncPeer(
            deviceId = hello.deviceId,
            name = hello.name,
            secret = encodeSyncBytes(outcome.secret),
            host = hello.host ?: fallbackHost ?: previous?.host.orEmpty(),
            port = hello.port ?: fallbackPort ?: previous?.port ?: DEFAULT_PORT,
            lastSyncedAtEpochMs = EpisodeReleaseDatePlatform.nowEpochMs(),
        )
        savePeers(listOf(peer) + peers.filterNot { it.deviceId == hello.deviceId })
    }

    private fun succeed(outcome: LocalSyncOutcome) {
        _uiState.update {
            it.copy(
                activity = LocalSyncActivity.Synced(
                    peerName = outcome.peerHello.name,
                    changeCount = outcome.merge.changes.size,
                ),
            )
        }
    }

    private fun fail(error: LocalSyncError) {
        _uiState.update { it.copy(activity = LocalSyncActivity.Failed(error)) }
    }

    private fun loadLedger(): SyncLedger {
        val payload = LocalSyncStorage.loadLedger(ProfileRepository.activeProfileId)?.trim().orEmpty()
        if (payload.isEmpty()) return SyncLedger()
        return runCatching { json.decodeFromString(SyncLedger.serializer(), payload) }.getOrElse { SyncLedger() }
    }

    private fun saveLedger(ledger: SyncLedger) {
        LocalSyncStorage.saveLedger(ProfileRepository.activeProfileId, json.encodeToString(SyncLedger.serializer(), ledger))
    }

    private fun loadPeers(): List<LocalSyncPeer> {
        val payload = LocalSyncStorage.loadPeers()?.trim().orEmpty()
        if (payload.isEmpty()) return emptyList()
        return runCatching { json.decodeFromString(peersSerializer, payload) }.getOrElse { emptyList() }
    }

    private fun savePeers(peers: List<LocalSyncPeer>) {
        LocalSyncStorage.savePeers(json.encodeToString(peersSerializer, peers))
        _uiState.update { it.copy(peers = peers) }
    }

    private class PairingCode(val host: String, val port: Int, val secret: ByteArray)

    private fun parsePairingCode(code: String): PairingCode? {
        val parts = code.split(':')
        if (parts.size != 5 || parts[0] != CODE_PREFIX || parts[1] != CODE_VERSION) return null
        val port = parts[3].toIntOrNull()?.takeIf { it in 1..65_535 } ?: return null
        val secret = runCatching { decodeSyncBytes(parts[4]) }.getOrNull()
            ?.takeIf { it.size == SECRET_SIZE }
            ?: return null
        return PairingCode(host = parts[2], port = port, secret = secret)
    }
}
