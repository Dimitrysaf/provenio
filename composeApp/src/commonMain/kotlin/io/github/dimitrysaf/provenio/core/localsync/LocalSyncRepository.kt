package io.github.dimitrysaf.provenio.core.localsync

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.build.AppVersionConfig
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.sync.SyncClientIdentity
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.random.Random

enum class LocalSyncError {
    NOT_ON_WIFI,
    UNREACHABLE,
    REJECTED,
    INVALID_CODE,
    VERSION_MISMATCH,
    FAILED,
}

// Something the person should hear about once; routine background syncs are not reported.
sealed interface LocalSyncActivity {
    data object Idle : LocalSyncActivity
    data class Paired(val peerName: String) : LocalSyncActivity
    data class Failed(val error: LocalSyncError, val peerName: String? = null) : LocalSyncActivity
}

data class LocalSyncUiState(
    val deviceName: String = "",
    val peers: List<LocalSyncPeer> = emptyList(),
    val pairingCode: String? = null,
    val pairingQr: List<BooleanArray>? = null,
    val syncingPeerIds: Set<String> = emptySet(),
    val joining: Boolean = false,
    val activity: LocalSyncActivity = LocalSyncActivity.Idle,
)

@Serializable
private data class LocalSyncBeacon(
    val app: String,
    val id: String,
    val port: Int,
)

// Keeps the active profile in step with paired devices on the same Wi-Fi: it listens while the app runs, announces itself, and syncs whenever something changes on either side.
object LocalSyncRepository {
    private const val DEFAULT_PORT = 47_631
    private const val BEACON_PORT = 47_632
    private const val BEACON_APP = "provenio-sync"
    private const val BEACON_INTERVAL_MS = 30_000L
    private const val CHANGE_DEBOUNCE_MS = 8_000L
    private const val CHANGE_CHECK_INTERVAL_MS = 60_000L
    private const val STARTUP_SYNC_DELAY_MS = 3_000L
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val PORT_WAIT_MS = 5_000L
    private const val INCOMING_WAIT_MS = 1_500L
    private const val RETRY_MIN_MS = 1_000L
    private const val RETRY_MAX_MS = 4_000L
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
    private val beaconAddresses = mutableMapOf<String, Pair<String, Int>>()
    private var pairingSecret: ByteArray? = null
    private var started = false

    val isSupported: Boolean get() = LocalSyncPlatform.isSupported

    // Called once the app is running; everything after this happens on its own.
    fun start() {
        if (!isSupported || started) return
        started = true
        _uiState.update { it.copy(deviceName = localSyncDeviceName(), peers = loadPeers()) }
        scope.launch { listen() }
        scope.launch { receiveBeacons() }
        scope.launch { announce() }
        scope.launch { observeLocalChanges() }
        scope.launch {
            delay(STARTUP_SYNC_DELAY_MS)
            syncAll()
        }
    }

    // Coming back to the app catches up with whatever the other devices changed meanwhile.
    fun onForeground() {
        if (!started) return
        scope.launch {
            sendBeacon()
            syncAll()
        }
    }

    // Shows a pairing code for another device to scan or type in.
    fun startPairing() {
        if (!isSupported) return
        start()
        scope.launch {
            val host = LocalSyncPlatform.localIpv4Address()
                ?: return@launch fail(LocalSyncError.NOT_ON_WIFI)
            val port = withTimeoutOrNull(PORT_WAIT_MS) { serverPort.filterNotNull().first() }
                ?: return@launch fail(LocalSyncError.FAILED)
            val secret = LocalSyncPlatform.randomBytes(SECRET_SIZE)
            pairingSecret = secret
            val code = listOf(CODE_PREFIX, CODE_VERSION, host, port.toString(), encodeSyncBytes(secret))
                .joinToString(":")
            _uiState.update { it.copy(pairingCode = code, pairingQr = localSyncQrMatrix(code)) }
        }
    }

    fun stopPairing() {
        pairingSecret = null
        _uiState.update { it.copy(pairingCode = null, pairingQr = null) }
    }

    // Pairs with the device showing [code] and syncs with it straight away.
    fun join(code: String) {
        val pairing = parsePairingCode(code.trim()) ?: return fail(LocalSyncError.INVALID_CODE)
        start()
        scope.launch {
            _uiState.update { it.copy(joining = true) }
            try {
                val paired = connectAndSync(
                    candidates = listOf(pairing.host to pairing.port),
                    secret = pairing.secret,
                    expectedPeerId = null,
                    hasSyncedBefore = false,
                    userInitiated = true,
                )
                paired?.let { name -> report(LocalSyncActivity.Paired(name)) }
            } finally {
                _uiState.update { it.copy(joining = false) }
            }
        }
    }

    fun syncNow(peer: LocalSyncPeer) {
        scope.launch { syncWith(peer, userInitiated = true) }
    }

    fun forget(peer: LocalSyncPeer) {
        savePeers(loadPeers().filterNot { it.deviceId == peer.deviceId })
    }

    fun clearActivity() {
        _uiState.update { it.copy(activity = LocalSyncActivity.Idle) }
    }

    private suspend fun syncAll() {
        loadPeers().forEach { peer -> syncWith(peer, userInitiated = false) }
    }

    private suspend fun syncWith(peer: LocalSyncPeer, userInitiated: Boolean) {
        val secret = runCatching { decodeSyncBytes(peer.secret) }.getOrNull() ?: return
        repeat(if (userInitiated) 1 else 2) { attempt ->
            if (attempt > 0) delay(Random.nextLong(RETRY_MIN_MS, RETRY_MAX_MS))
            val current = loadPeers().firstOrNull { it.deviceId == peer.deviceId } ?: return
            // The saved address first, then wherever the device last announced itself from.
            val candidates = listOfNotNull(current.host to current.port, beaconAddresses[peer.deviceId]).distinct()
            val synced = connectAndSync(
                candidates = candidates,
                secret = secret,
                expectedPeerId = peer.deviceId,
                hasSyncedBefore = current.lastSyncedAtEpochMs != null,
                userInitiated = userInitiated,
            )
            if (synced != null) return
        }
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
        serverPort.value = listening.port
        try {
            while (currentCoroutineContext().isActive) {
                val connection = listening.accept()
                scope.launch { serve(connection) }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Stopped listening for sync" }
        } finally {
            listening.close()
            serverPort.value = null
        }
    }

    private suspend fun announce() {
        while (currentCoroutineContext().isActive) {
            sendBeacon()
            delay(BEACON_INTERVAL_MS)
        }
    }

    private suspend fun sendBeacon() {
        val port = serverPort.value ?: return
        val beacon = LocalSyncBeacon(app = BEACON_APP, id = SyncClientIdentity.currentClientId(), port = port)
        runCatching {
            LocalSyncPlatform.sendBeacon(json.encodeToString(LocalSyncBeacon.serializer(), beacon).encodeToByteArray(), BEACON_PORT)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.d { "Could not announce this device: ${error.message}" }
        }
    }

    private suspend fun receiveBeacons() {
        LocalSyncWifiLock.acquire()
        try {
            LocalSyncPlatform.receiveBeacons(BEACON_PORT) { payload, host -> onBeacon(payload, host) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Could not listen for other devices" }
        } finally {
            LocalSyncWifiLock.release()
        }
    }

    // A paired device announcing itself from a new address, or for the first time since launch, gets a sync.
    private fun onBeacon(payload: ByteArray, host: String) {
        val beacon = runCatching {
            json.decodeFromString(LocalSyncBeacon.serializer(), payload.decodeToString())
        }.getOrNull() ?: return
        if (beacon.app != BEACON_APP || beacon.id == SyncClientIdentity.currentClientId()) return
        val peer = loadPeers().firstOrNull { it.deviceId == beacon.id } ?: return
        val address = host to beacon.port
        val previous = beaconAddresses.put(beacon.id, address)
        if (previous == address) return
        // Both devices hear each other, so only one of them starts the sync.
        if (SyncClientIdentity.currentClientId() > beacon.id) return
        scope.launch { syncWith(peer, userInitiated = false) }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeLocalChanges() {
        scope.launch {
            while (currentCoroutineContext().isActive) {
                delay(CHANGE_CHECK_INTERVAL_MS)
                syncIfChangedLocally()
            }
        }
        merge(
            LibraryRepository.uiState.map { },
            WatchedRepository.uiState.map { },
            WatchProgressRepository.uiState.map { },
            AddonRepository.uiState.map { },
            CollectionRepository.collections.map { },
            HomeCatalogSettingsRepository.uiState.map { },
            ProfileRepository.state.map { it.profiles },
        )
            .drop(1)
            .debounce(CHANGE_DEBOUNCE_MS)
            .collect { syncIfChangedLocally() }
    }

    private suspend fun syncIfChangedLocally() {
        if (loadPeers().isEmpty()) return
        val changed = sessionMutex.withLock {
            val before = loadLedger().lamport
            refreshedLedger().lamport != before
        }
        if (changed) syncAll()
    }

    private suspend fun serve(connection: LocalSyncConnection) {
        // Two devices syncing each other at once would deadlock here, so the incoming one is turned away quickly.
        withTimeoutOrNull(INCOMING_WAIT_MS) { sessionMutex.lock() } ?: run {
            connection.close()
            return
        }
        try {
            var peerId: String? = null
            try {
                val outcome = runServerSession(
                    connection = connection,
                    identity = identity(),
                    secretFor = ::candidateSecrets,
                ) { remoteId, remote, policy ->
                    peerId = remoteId
                    markSyncing(remoteId, true)
                    commitMerge(refreshedLedger(), remote, policy)
                }
                val pairedNow = pairingSecret?.contentEquals(outcome.secret) == true
                rememberPeer(outcome, fallbackHost = null, fallbackPort = null)
                if (pairedNow) {
                    stopPairing()
                    report(LocalSyncActivity.Paired(outcome.peerHello.name))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: LocalSyncRejectedException) {
                log.i { "Refused a sync from a device without the right code" }
            } catch (error: LocalSyncVersionMismatchException) {
                report(LocalSyncActivity.Failed(LocalSyncError.VERSION_MISMATCH, error.peerName))
            } catch (error: Throwable) {
                log.w(error) { "Sync from another device failed" }
            } finally {
                peerId?.let { markSyncing(it, false) }
                connection.close()
            }
        } finally {
            sessionMutex.unlock()
        }
    }

    // Returns the other device's name once synced, or null if it could not be reached or refused.
    private suspend fun connectAndSync(
        candidates: List<Pair<String, Int>>,
        secret: ByteArray,
        expectedPeerId: String?,
        hasSyncedBefore: Boolean,
        userInitiated: Boolean,
    ): String? = sessionMutex.withLock {
        expectedPeerId?.let { markSyncing(it, true) }
        try {
            val (connection, address) = connectAny(candidates) ?: run {
                if (userInitiated) report(LocalSyncActivity.Failed(LocalSyncError.UNREACHABLE))
                return@withLock null
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
                ) { _, remote, policy ->
                    commitMerge(ledger, remote, policy)
                }
                rememberPeer(outcome, fallbackHost = address.first, fallbackPort = address.second)
                outcome.peerHello.name
            } catch (error: CancellationException) {
                throw error
            } catch (error: LocalSyncRejectedException) {
                if (userInitiated) report(LocalSyncActivity.Failed(LocalSyncError.REJECTED))
                null
            } catch (error: LocalSyncVersionMismatchException) {
                // Always said, since a background sync that can never succeed would otherwise fail in silence.
                report(LocalSyncActivity.Failed(LocalSyncError.VERSION_MISMATCH, error.peerName))
                null
            } catch (error: Throwable) {
                log.w(error) { "Sync with ${address.first} failed" }
                if (userInitiated) report(LocalSyncActivity.Failed(LocalSyncError.FAILED))
                null
            } finally {
                connection.close()
            }
        } finally {
            expectedPeerId?.let { markSyncing(it, false) }
        }
    }

    private suspend fun connectAny(candidates: List<Pair<String, Int>>): Pair<LocalSyncConnection, Pair<String, Int>>? {
        candidates.filter { (host, _) -> host.isNotBlank() }.forEach { address ->
            try {
                return LocalSyncPlatform.connect(address.first, address.second, CONNECT_TIMEOUT_MS) to address
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.d { "Could not reach ${address.first}:${address.second}: ${error.message}" }
            }
        }
        return null
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
        // A device with no profiles yet is being set up, so it takes the other device's data.
        fresh = ProfileRepository.state.value.profiles.isEmpty(),
        appVersion = localSyncAppVersion(),
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
        // Changes this device could not take in, because no source here knows them or applying failed.
        val unapplied = mutableSetOf<String>()
        changesBySource.forEach { (source, entries) ->
            val keys = entries.map { it.key }
            if (source == null) {
                unapplied += keys
                return@forEach
            }
            runCatching { source.apply(entries.associate { it.key to it.value }) }
                .onFailure { error ->
                    unapplied += keys
                    log.w(error) { "Could not apply synced ${source.prefix}" }
                }
            runCatching { current.putAll(source.snapshot()) }
        }
        // They keep this device's own record, so the next sync offers them again instead of treating them as received.
        val records = merge.ledger.records.toMutableMap()
        unapplied.forEach { key ->
            val ours = local.records[key]
            if (ours != null) records[key] = ours else records.remove(key)
        }
        val kept = merge.ledger.copy(records = records)
        val adopted = adoptAppliedValues(kept, current, merge.changes.keys - unapplied)
        saveLedger(adopted)
        return merge.copy(ledger = adopted, changes = merge.changes - unapplied)
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
            appVersion = hello.appVersion.ifBlank { null },
        )
        savePeers(listOf(peer) + peers.filterNot { it.deviceId == hello.deviceId })
    }

    private fun markSyncing(peerId: String, syncing: Boolean) {
        _uiState.update {
            it.copy(syncingPeerIds = if (syncing) it.syncingPeerIds + peerId else it.syncingPeerIds - peerId)
        }
    }

    private fun report(activity: LocalSyncActivity) {
        _uiState.update { it.copy(activity = activity) }
    }

    private fun fail(error: LocalSyncError) = report(LocalSyncActivity.Failed(error))

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

// How this build names itself to the other device: the version, and the commit for beta builds that share one.
private fun localSyncAppVersion(): String {
    val commit = AppVersionConfig.BUILD_COMMIT.take(7)
    return if (commit.isBlank()) AppVersionConfig.VERSION_NAME else "${AppVersionConfig.VERSION_NAME} ($commit)"
}
