package io.github.dimitrysaf.provenio.core.party

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncConnection
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncPlatform
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncServer
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncWifiLock
import io.github.dimitrysaf.provenio.core.localsync.localSyncDeviceName
import io.github.dimitrysaf.provenio.core.localsync.localSyncQrMatrix
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.sync.SyncClientIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A watch party: one device hosts a room and is its server, and the others join it with its
 * link or its code. There is no server in between. A link says where the room listens and
 * carries its secret, so it works from anywhere the host can be reached; a code is found by
 * listening for the room on the same network, and the host lets each such guest in.
 *
 * Everything runs on one thread, so the room's state needs no locks; the sockets block on
 * their own threads.
 */
object PartyRepository {
    private const val DEFAULT_PORT = 47_640
    private const val BEACON_PORT = 47_641
    private const val BEACON_INTERVAL_MS = 2_000L
    private const val FIND_TIMEOUT_MS = 8_000L
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val HANDSHAKE_TIMEOUT_MS = 10_000L
    private const val PING_INTERVAL_MS = 20_000L
    private const val APPROVAL_TIMEOUT_MS = 120_000L
    private const val FAREWELL_TIMEOUT_MS = 1_500L
    private const val MAX_NAME_LENGTH = 40

    private val log = Logger.withTag("WatchParty")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    private val _uiState = MutableStateFlow(PartyUiState())
    val uiState: StateFlow<PartyUiState> = _uiState.asStateFlow()

    val isSupported: Boolean get() = LocalSyncPlatform.isSupported

    // The room being hosted or attended; only one at a time.
    private var sessionJob: Job? = null
    private var hosted: HostedRoom? = null
    private var guestConnection: LocalSyncConnection? = null
    private var guestChannel: PartyChannel? = null

    private class PartyGuest(
        val member: PartyMember,
        val channel: PartyChannel,
        var approved: Boolean,
    ) {
        // Set once the host sends them off; whoever did that also closes the connection.
        var removed = false
    }

    private class HostedRoom(
        val code: String,
        val secret: ByteArray,
        val server: LocalSyncServer,
        val host: PartyMember,
        val inviteLink: String,
        val inviteQr: List<BooleanArray>?,
    ) {
        var settings = PartySettings()
        var closed = false
        val guests = LinkedHashMap<String, PartyGuest>()
        val banned = LinkedHashMap<String, String>()

        // Connections still saying hello, closed with the room.
        val handshakes = mutableSetOf<LocalSyncConnection>()

        val members: List<PartyMember>
            get() = listOf(host) + guests.values.filter { it.approved }.map { it.member }

        val isFull: Boolean
            get() = settings.memberLimit?.let { members.size >= it } == true
    }

    // ---- What the settings page asks for ----

    fun createRoom() {
        if (!isSupported) return notify(PartyError.NOT_SUPPORTED)
        launchSession { hostRoom() }
    }

    /** Joins with a link or a code, as typed, scanned or opened. */
    fun join(input: String) {
        if (!isSupported) return notify(PartyError.NOT_SUPPORTED)
        val target = parsePartyJoinTarget(input) ?: return notify(PartyError.INVALID_LINK)
        launchSession { attendRoom(target) }
    }

    /** Leaves the room, or closes it for everyone when this device hosts it; also stops a join on its way. */
    fun leave() {
        scope.launch {
            hosted?.let { room ->
                endHosting(room, notice = null)
                return@launch
            }
            val job = sessionJob ?: return@launch
            guestChannel?.let { channel ->
                withTimeoutOrNull(FAREWELL_TIMEOUT_MS) { runCatching { channel.send(PartyMessage.Leave) } }
            }
            // Cancelled first, so the read that the close breaks off ends as a cancellation, not an error.
            job.cancel()
            guestConnection?.close()
            guestConnection = null
            guestChannel = null
            if (sessionJob === job) sessionJob = null
            _uiState.update { it.copy(phase = PartyPhase.Idle) }
        }
    }

    fun updateSettings(transform: (PartySettings) -> PartySettings) {
        scope.launch {
            val room = hosted ?: return@launch
            val settings = transform(room.settings)
            if (settings == room.settings) return@launch
            room.settings = settings
            broadcast(room, PartyMessage.Settings(settings))
            publishHost()
        }
    }

    fun approve(memberId: String) {
        scope.launch {
            val room = hosted ?: return@launch
            val guest = room.guests[memberId]?.takeIf { !it.approved } ?: return@launch
            if (room.isFull) return@launch removeGuest(room, guest, PartyRemoval.FULL)
            guest.approved = true
            val welcome = PartyMessage.Welcome(room.settings, room.members)
            scope.launch { runCatching { guest.channel.send(welcome) } }
            broadcast(room, PartyMessage.Members(room.members), except = guest)
            publishHost()
        }
    }

    fun deny(memberId: String) = removeById(memberId, PartyRemoval.DENIED)

    fun kick(memberId: String) = removeById(memberId, PartyRemoval.KICKED)

    fun ban(memberId: String) {
        scope.launch {
            val room = hosted ?: return@launch
            val guest = room.guests[memberId] ?: return@launch
            room.banned[memberId] = guest.member.name
            removeGuest(room, guest, PartyRemoval.BANNED)
        }
    }

    fun unban(memberId: String) {
        scope.launch {
            val room = hosted ?: return@launch
            if (room.banned.remove(memberId) != null) publishHost()
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun removeById(memberId: String, reason: PartyRemoval) {
        scope.launch {
            val room = hosted ?: return@launch
            room.guests[memberId]?.let { guest -> removeGuest(room, guest, reason) }
        }
    }

    private fun notify(error: PartyError) {
        _uiState.update { it.copy(notice = error) }
    }

    // Starts hosting or joining, unless a room is already open or on its way.
    private fun launchSession(block: suspend () -> Unit) {
        scope.launch {
            if (sessionJob?.isActive == true) return@launch
            sessionJob = scope.launch { block() }
        }
    }

    private fun setPhase(phase: PartyPhase) {
        _uiState.update { it.copy(phase = phase) }
    }

    private fun end(notice: PartyError?) {
        _uiState.update { PartyUiState(phase = PartyPhase.Idle, notice = notice ?: it.notice) }
    }

    private fun myId(): String = SyncClientIdentity.currentClientId()

    private fun myName(): String =
        ProfileRepository.state.value.activeProfile?.name?.takeIf { it.isNotBlank() } ?: localSyncDeviceName()

    private fun cleanName(name: String): String = name.filterNot { it.isISOControl() }.trim().take(MAX_NAME_LENGTH)

    // ---- Hosting ----

    private suspend fun hostRoom() {
        setPhase(PartyPhase.Creating)
        val addresses = LocalSyncPlatform.localIpv4Addresses() + partyGlobalIpv6Addresses()
        if (addresses.isEmpty()) return end(PartyError.NO_NETWORK)
        val server = try {
            LocalSyncPlatform.listen(DEFAULT_PORT)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Could not open a watch party" }
            return end(PartyError.FAILED)
        }
        try {
            coroutineScope {
                val code = LocalSyncPlatform.randomBytes(4)
                    .fold(0L) { value, byte -> (value shl 8) or (byte.toLong() and 0xff) }
                    .rem(1_000_000L)
                    .toString()
                    .padStart(PARTY_CODE_LENGTH, '0')
                val secret = LocalSyncPlatform.randomBytes(PARTY_SECRET_SIZE)
                val link = buildPartyLink(code, server.port, addresses, secret)
                val room = HostedRoom(
                    code = code,
                    secret = secret,
                    server = server,
                    host = PartyMember(id = myId(), name = myName(), isHost = true),
                    inviteLink = link,
                    inviteQr = localSyncQrMatrix(link),
                )
                hosted = room
                log.i { "Watch party open on port ${server.port}" }
                publishHost()
                launch { announce(room) }
                launch { pingGuests(room) }
                while (currentCoroutineContext().isActive) {
                    val connection = try {
                        server.accept()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        if (!room.closed) log.w(error) { "The watch party stopped taking guests" }
                        endHosting(room, PartyError.FAILED)
                        break
                    }
                    launch { admit(room, connection) }
                }
            }
        } finally {
            server.close()
        }
    }

    // Closes the room: everyone still in it hears so, and the page goes back to the start.
    private fun endHosting(room: HostedRoom, notice: PartyError?) {
        if (hosted !== room) return
        hosted = null
        room.closed = true
        val guests = room.guests.values.toList()
        room.guests.clear()
        guests.forEach { it.removed = true }
        room.handshakes.forEach { it.close() }
        room.handshakes.clear()
        room.server.close()
        sessionJob?.cancel()
        sessionJob = null
        end(notice)
        guests.forEach { guest -> sendOff(guest, PartyRemoval.CLOSED) }
    }

    private suspend fun announce(room: HostedRoom) {
        val beacon = PartyBeacon(app = PARTY_BEACON_APP, code = room.code, port = room.server.port, hostId = room.host.id)
        val payload = partyJson.encodeToString(PartyBeacon.serializer(), beacon).encodeToByteArray()
        while (currentCoroutineContext().isActive) {
            try {
                LocalSyncPlatform.sendBeacon(payload, BEACON_PORT)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                log.d { "Could not announce the watch party: ${error.message}" }
            }
            delay(BEACON_INTERVAL_MS)
        }
    }

    // Keeps quiet connections from timing out, and finds the ones that went away.
    private suspend fun pingGuests(room: HostedRoom) {
        while (currentCoroutineContext().isActive) {
            delay(PING_INTERVAL_MS)
            room.guests.values.forEach { guest ->
                scope.launch { runCatching { guest.channel.send(PartyMessage.Ping) } }
            }
        }
    }

    private suspend fun admit(room: HostedRoom, connection: LocalSyncConnection) {
        room.handshakes += connection
        // A read cannot be called off, so a connection that says nothing is closed instead.
        val watchdog = scope.launch {
            delay(HANDSHAKE_TIMEOUT_MS)
            connection.close()
        }
        val guest = try {
            handshake(room, connection)
        } catch (error: CancellationException) {
            connection.close()
            throw error
        } catch (error: Throwable) {
            log.d { "A guest could not join: ${error.message}" }
            null
        } finally {
            watchdog.cancel()
            room.handshakes -= connection
        }
        if (guest == null) {
            connection.close()
            return
        }
        try {
            while (true) {
                val message = guest.channel.receive()
                if (message == PartyMessage.Leave || room.guests[guest.member.id] !== guest) break
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.d { "Lost a guest: ${error.message}" }
        } finally {
            when {
                guest.removed -> Unit
                room.guests[guest.member.id] === guest -> {
                    room.guests.remove(guest.member.id)
                    if (guest.approved && !room.closed) broadcast(room, PartyMessage.Members(room.members))
                    publishHost()
                    guest.channel.close()
                }
                // Replaced by a newer connection from the same device.
                else -> guest.channel.close()
            }
        }
    }

    // Returns the guest once they are in the room or waiting at its door, or null if they were turned away.
    private suspend fun handshake(room: HostedRoom, connection: LocalSyncConnection): PartyGuest? {
        val hello = connection.readPartyHello()
        suspend fun refuse(status: PartyHelloStatus): PartyGuest? {
            connection.writePartyReply(PartyHelloReply(version = PARTY_PROTOCOL_VERSION, status = status))
            return null
        }
        if (hello.version != PARTY_PROTOCOL_VERSION) return refuse(PartyHelloStatus.VERSION_MISMATCH)
        if (room.closed) return null
        if (hello.memberId in room.banned) return refuse(PartyHelloStatus.BANNED)
        if (hello.code != room.code || hello.memberId == room.host.id) return refuse(PartyHelloStatus.REJECTED)
        val hostNonce = encodePartyBytes(LocalSyncPlatform.randomBytes(PARTY_NONCE_SIZE))
        val key: ByteArray
        val reply: PartyHelloReply
        when (hello.mode) {
            PartyJoinMode.LINK -> {
                val expected = partyProof(room.secret, "guest", room.code, hello.memberId, hello.nonce)
                if (hello.proof != expected) return refuse(PartyHelloStatus.REJECTED)
                if (room.isFull && room.guests[hello.memberId]?.approved != true) return refuse(PartyHelloStatus.FULL)
                key = partySessionKey(room.secret, hello.nonce, hostNonce)
                reply = PartyHelloReply(
                    version = PARTY_PROTOCOL_VERSION,
                    status = PartyHelloStatus.ACCEPTED,
                    nonce = hostNonce,
                    proof = partyProof(room.secret, "host", hello.nonce, hostNonce),
                )
            }
            PartyJoinMode.CODE -> {
                if (room.isFull) return refuse(PartyHelloStatus.FULL)
                val agreement = PartyKeyAgreement()
                val shared = hello.publicKey
                    ?.let { runCatching { agreement.sharedSecret(decodePartyBytes(it)) }.getOrNull() }
                    ?: return refuse(PartyHelloStatus.REJECTED)
                key = partySessionKey(shared, room.code, hello.nonce, hostNonce)
                reply = PartyHelloReply(
                    version = PARTY_PROTOCOL_VERSION,
                    status = PartyHelloStatus.PENDING,
                    nonce = hostNonce,
                    publicKey = encodePartyBytes(agreement.publicKey),
                )
            }
        }
        connection.writePartyReply(reply)
        if (room.closed) return null

        val guest = PartyGuest(
            member = PartyMember(id = hello.memberId, name = cleanName(hello.name)),
            channel = PartyChannel(connection, key),
            approved = hello.mode == PartyJoinMode.LINK,
        )
        val previous = room.guests.put(guest.member.id, guest)
        previous?.channel?.close()
        if (guest.approved) {
            guest.channel.send(PartyMessage.Welcome(room.settings, room.members))
            broadcast(room, PartyMessage.Members(room.members), except = guest)
        } else {
            if (previous?.approved == true) broadcast(room, PartyMessage.Members(room.members))
            scope.launch {
                delay(APPROVAL_TIMEOUT_MS)
                if (!guest.approved) removeGuest(room, guest, PartyRemoval.DENIED)
            }
        }
        publishHost()
        return guest
    }

    private fun removeGuest(room: HostedRoom, guest: PartyGuest, reason: PartyRemoval) {
        if (room.guests[guest.member.id] !== guest) return
        room.guests.remove(guest.member.id)
        guest.removed = true
        if (guest.approved) broadcast(room, PartyMessage.Members(room.members))
        publishHost()
        sendOff(guest, reason)
    }

    // Says why, if the guest is still listening, then hangs up.
    private fun sendOff(guest: PartyGuest, reason: PartyRemoval) {
        scope.launch {
            withTimeoutOrNull(FAREWELL_TIMEOUT_MS) { runCatching { guest.channel.send(PartyMessage.Removed(reason)) } }
            guest.channel.close()
        }
    }

    private fun broadcast(room: HostedRoom, message: PartyMessage, except: PartyGuest? = null) {
        room.guests.values.filter { it.approved && it !== except }.forEach { guest ->
            scope.launch { runCatching { guest.channel.send(message) } }
        }
    }

    private fun publishHost() {
        val room = hosted ?: return
        setPhase(
            PartyPhase.InRoom(
                PartyRoom(
                    code = room.code,
                    isHost = true,
                    myMemberId = room.host.id,
                    settings = room.settings,
                    members = room.members,
                    inviteLink = room.inviteLink,
                    inviteQr = room.inviteQr,
                    joinRequests = room.guests.values.filter { !it.approved }.map { PartyJoinRequest(it.member.id, it.member.name) },
                    banned = room.banned.map { (id, name) -> PartyMember(id = id, name = name) },
                ),
            ),
        )
    }

    // ---- Joining ----

    private suspend fun attendRoom(target: PartyJoinTarget) {
        val error = try {
            val (code, candidates, secret) = when (target) {
                is PartyJoinTarget.Code -> {
                    setPhase(PartyPhase.Finding)
                    val found = findRoom(target.code) ?: return end(PartyError.NO_ROOM_FOR_CODE)
                    Triple(target.code, listOf(found), null)
                }
                is PartyJoinTarget.Link -> Triple(target.code, target.addresses.map { it to target.port }, target.secret)
            }
            setPhase(PartyPhase.Connecting)
            val connection = connectFirst(candidates) ?: return end(PartyError.UNREACHABLE)
            guestConnection = connection
            try {
                converse(connection, code, secret)
            } finally {
                if (guestConnection === connection) guestConnection = null
                guestChannel = null
                connection.close()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Lost the watch party" }
            when (_uiState.value.phase) {
                is PartyPhase.InRoom, PartyPhase.AwaitingApproval -> PartyError.CONNECTION_LOST
                else -> PartyError.FAILED
            }
        }
        end(error)
    }

    // Says hello and then follows the room until it ends; returns why it ended.
    private suspend fun converse(connection: LocalSyncConnection, code: String, secret: ByteArray?): PartyError {
        val memberId = myId()
        val nonce = encodePartyBytes(LocalSyncPlatform.randomBytes(PARTY_NONCE_SIZE))
        val agreement = if (secret == null) PartyKeyAgreement() else null
        connection.writePartyHello(
            PartyHello(
                version = PARTY_PROTOCOL_VERSION,
                mode = if (secret != null) PartyJoinMode.LINK else PartyJoinMode.CODE,
                code = code,
                memberId = memberId,
                name = myName(),
                nonce = nonce,
                proof = secret?.let { partyProof(it, "guest", code, memberId, nonce) },
                publicKey = agreement?.let { encodePartyBytes(it.publicKey) },
            ),
        )
        val reply = connection.readPartyReply()
        if (reply.version != PARTY_PROTOCOL_VERSION) return PartyError.VERSION_MISMATCH
        val key = when (reply.status) {
            PartyHelloStatus.ACCEPTED -> {
                // Only the room that holds the link's secret can answer this, so no one stands in for the host.
                if (secret == null || reply.proof != partyProof(secret, "host", nonce, reply.nonce)) return PartyError.REJECTED
                partySessionKey(secret, nonce, reply.nonce)
            }
            PartyHelloStatus.PENDING -> {
                val ownKey = agreement ?: return PartyError.REJECTED
                val peerKey = reply.publicKey ?: return PartyError.REJECTED
                partySessionKey(ownKey.sharedSecret(decodePartyBytes(peerKey)), code, nonce, reply.nonce)
            }
            PartyHelloStatus.FULL -> return PartyError.ROOM_FULL
            PartyHelloStatus.BANNED -> return PartyError.BANNED
            PartyHelloStatus.REJECTED -> return PartyError.REJECTED
            PartyHelloStatus.VERSION_MISMATCH -> return PartyError.VERSION_MISMATCH
        }
        if (reply.status == PartyHelloStatus.PENDING) setPhase(PartyPhase.AwaitingApproval)
        val channel = PartyChannel(connection, key)
        guestChannel = channel
        var room: PartyRoom? = null
        while (true) {
            when (val message = channel.receive()) {
                is PartyMessage.Welcome -> room = PartyRoom(
                    code = code,
                    isHost = false,
                    myMemberId = memberId,
                    settings = message.settings,
                    members = message.members,
                )
                is PartyMessage.Members -> room = room?.copy(members = message.members)
                is PartyMessage.Settings -> room = room?.copy(settings = message.settings)
                is PartyMessage.Removed -> return message.reason.toError()
                PartyMessage.Ping -> channel.send(PartyMessage.Pong)
                PartyMessage.Pong, PartyMessage.Leave, null -> Unit
            }
            room?.let { setPhase(PartyPhase.InRoom(it)) }
        }
    }

    private fun PartyRemoval.toError(): PartyError = when (this) {
        PartyRemoval.KICKED -> PartyError.KICKED
        PartyRemoval.BANNED -> PartyError.BANNED
        PartyRemoval.DENIED -> PartyError.DENIED
        PartyRemoval.FULL -> PartyError.ROOM_FULL
        PartyRemoval.CLOSED -> PartyError.CLOSED
    }

    // Listens for a room on this network that announces [code].
    private suspend fun findRoom(code: String): Pair<String, Int>? {
        val ownId = myId()
        val found = CompletableDeferred<Pair<String, Int>>()
        LocalSyncWifiLock.acquire()
        return try {
            withTimeoutOrNull(FIND_TIMEOUT_MS) {
                val listener = launch {
                    LocalSyncPlatform.receiveBeacons(BEACON_PORT) { payload, host ->
                        val beacon = runCatching {
                            partyJson.decodeFromString(PartyBeacon.serializer(), payload.decodeToString())
                        }.getOrNull()
                        if (beacon != null && beacon.app == PARTY_BEACON_APP && beacon.code == code && beacon.hostId != ownId) {
                            found.complete(host to beacon.port)
                        }
                    }
                }
                found.await().also { listener.cancel() }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Could not listen for watch parties" }
            null
        } finally {
            LocalSyncWifiLock.release()
        }
    }

    // Tries every address at once and keeps the first that answers; the rest are closed as they come in.
    private suspend fun connectFirst(candidates: List<Pair<String, Int>>): LocalSyncConnection? {
        if (candidates.isEmpty()) return null
        val winner = CompletableDeferred<LocalSyncConnection?>()
        var remaining = candidates.size
        var abandoned = false
        candidates.forEach { (host, port) ->
            scope.launch {
                val connection = try {
                    LocalSyncPlatform.connect(host, port, CONNECT_TIMEOUT_MS)
                } catch (error: Throwable) {
                    log.d { "Could not reach $host port $port: ${error.message}" }
                    null
                }
                remaining -= 1
                when {
                    connection != null -> if (abandoned || !winner.complete(connection)) connection.close()
                    remaining == 0 -> winner.complete(null)
                }
            }
        }
        return try {
            winner.await()
        } catch (error: CancellationException) {
            abandoned = true
            if (!winner.complete(null)) withContext(NonCancellable) { winner.await()?.close() }
            throw error
        }
    }
}
