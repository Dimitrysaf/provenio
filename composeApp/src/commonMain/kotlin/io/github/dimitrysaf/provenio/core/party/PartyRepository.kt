package io.github.dimitrysaf.provenio.core.party

import co.touchlab.kermit.Logger
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncConnection
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncFirewall
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncFirewallPrompt
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncFirewallState
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncPlatform
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncServer
import io.github.dimitrysaf.provenio.core.localsync.localSyncDeviceName
import io.github.dimitrysaf.provenio.core.localsync.localSyncQrMatrix
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.sync.SyncClientIdentity
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A watch party: one device hosts a room and is its server, and the others join it with its
 * link. There is no server in between. The host asks its router to forward the room's port, so
 * the link works from anywhere; the link carries the room's secret, which every guest proves.
 *
 * Everything runs on one thread, so the room's state needs no locks; the sockets block on
 * their own threads.
 */
object PartyRepository {
    private const val DEFAULT_PORT = 47_640
    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val HANDSHAKE_TIMEOUT_MS = 10_000L
    private const val PING_INTERVAL_MS = 20_000L
    private const val FAREWELL_TIMEOUT_MS = 1_500L
    private const val MIN_RENEW_INTERVAL_MS = 60_000L
    private const val MAX_NAME_LENGTH = 40
    /** The longest chat message; fields stop taking text there. */
    const val MAX_CHAT_LENGTH = 225
    private const val CHAT_HISTORY = 100
    private const val WELCOME_CHAT = 50

    // A guest who keeps tapping play and pause moves the room at most this often.
    private const val MIN_CONTROL_INTERVAL_MS = 500L

    // A guest's requests to the host come at most this often, so the chat isn't flooded.
    private const val MIN_REQUEST_INTERVAL_MS = 1_000L

    private val log = Logger.withTag("WatchParty")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    private val _uiState = MutableStateFlow(PartyUiState())
    val uiState: StateFlow<PartyUiState> = _uiState.asStateFlow()

    /** What the host is doing: set by the host's own app, and heard by every guest. */
    private val _activity = MutableStateFlow(PartyActivity())
    val activity: StateFlow<PartyActivity> = _activity.asStateFlow()

    /** The source the host has open, which guests open too; null while the host is not watching. */
    private val _playback = MutableStateFlow<PartyPlayback?>(null)
    val playback: StateFlow<PartyPlayback?> = _playback.asStateFlow()

    private val _playbackState = MutableStateFlow<PartyPlaybackState?>(null)
    val playbackState: StateFlow<PartyPlaybackState?> = _playbackState.asStateFlow()

    private val _chat = MutableStateFlow<List<PartyChatMessage>>(emptyList())
    val chat: StateFlow<List<PartyChatMessage>> = _chat.asStateFlow()

    /** When this device may send its next chat message, under the room's slow mode. */
    private val _nextChatAtEpochMs = MutableStateFlow(0L)
    val nextChatAtEpochMs: StateFlow<Long> = _nextChatAtEpochMs.asStateFlow()

    /** The requests in the chat the host already said Yes to. */
    private val _answeredRequests = MutableStateFlow<Set<String>>(emptySet())
    val answeredRequests: StateFlow<Set<String>> = _answeredRequests.asStateFlow()

    /** Titles the host said Yes to, for the app to play as it plays anything. */
    private val _acceptedSuggestions = MutableSharedFlow<PartySuggestion>(extraBufferCapacity = 4)
    val acceptedSuggestions: SharedFlow<PartySuggestion> = _acceptedSuggestions.asSharedFlow()

    /** Guests' allowed plays, pauses and seeks, for the host's player to carry out. */
    private val _controls = MutableSharedFlow<PartyControl>(extraBufferCapacity = 8)
    val controls: SharedFlow<PartyControl> = _controls.asSharedFlow()

    /** The guests the host's player waits for while they load, under [PartyLoading.PAUSE_EVERYONE]. */
    private val _waitingFor = MutableStateFlow<List<PartyMember>>(emptyList())
    val waitingFor: StateFlow<List<PartyMember>> = _waitingFor.asStateFlow()

    val isSupported: Boolean get() = LocalSyncPlatform.isSupported

    val currentRoom: PartyRoom? get() = (_uiState.value.phase as? PartyPhase.InRoom)?.room
    val isHost: Boolean get() = currentRoom?.isHost == true
    val isGuest: Boolean get() = currentRoom?.isHost == false

    // The room being hosted or attended; only one at a time.
    private var sessionJob: Job? = null
    private var hosted: HostedRoom? = null
    private var guestConnection: LocalSyncConnection? = null
    private var guestChannel: PartyChannel? = null
    private var nextPlaybackId = 1L
    private var nextChatNumber = 1L

    // What this guest last told the host about loading, so it is said once.
    private var reportedLoading: Pair<Long, Boolean>? = null

    private class PartyGuest(
        val member: PartyMember,
        val channel: PartyChannel,
    ) {
        // Set once the host sends them off; whoever did that also closes the connection.
        var removed = false
        var lastChatAtEpochMs = 0L
        var lastControlAtEpochMs = 0L

        // Since when their player has been loading the host's source, while it is.
        var loadingSinceEpochMs: Long? = null

        // The host chose not to wait for this load.
        var waitSkipped = false
    }

    private class HostedRoom(
        val secret: ByteArray,
        val server: LocalSyncServer,
        val host: PartyMember,
    ) {
        var settings = PartySettings()
        var closed = false
        var inviteLink: String? = null
        var inviteQr: List<BooleanArray>? = null
        var mapping: PartyPortMapping? = null
        var firewall: LocalSyncFirewallPrompt? = null
        var waitExpiry: Job? = null
        var address: String? = null
        val guests = LinkedHashMap<String, PartyGuest>()
        val banned = LinkedHashMap<String, String>()

        // Connections still saying hello, closed with the room.
        val handshakes = mutableSetOf<LocalSyncConnection>()

        val members: List<PartyMember>
            get() = listOf(host) + guests.values.map { it.member }

        val isFull: Boolean
            get() = settings.memberLimit?.let { members.size >= it } == true
    }

    // ---- What the app asks for ----

    fun createRoom() {
        if (!isSupported) return notify(PartyError.NOT_SUPPORTED)
        launchSession { hostRoom() }
    }

    /** Joins with an invite link, as scanned, pasted or opened. */
    fun join(input: String) {
        if (!isSupported) return notify(PartyError.NOT_SUPPORTED)
        val invite = parsePartyInvite(input) ?: return notify(PartyError.INVALID_LINK)
        launchSession { attendRoom(invite) }
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
            end(notice = null)
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    // ---- The host's controls ----

    fun updateSettings(transform: (PartySettings) -> PartySettings) {
        scope.launch {
            val room = hosted ?: return@launch
            val settings = transform(room.settings)
            if (settings == room.settings) return@launch
            room.settings = settings
            broadcast(room, PartyMessage.Settings(settings))
            publishHost()
            refreshWaiting(room)
        }
    }

    fun kick(memberId: String) {
        scope.launch {
            val room = hosted ?: return@launch
            room.guests[memberId]?.let { guest -> removeGuest(room, guest, PartyRemoval.KICKED) }
        }
    }

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

    fun allowThroughFirewall() {
        scope.launch {
            val room = hosted ?: return@launch
            val prompt = room.firewall?.takeIf { !it.allowing } ?: return@launch
            room.firewall = prompt.copy(allowing = true, allowFailed = false)
            publishHost()
            val allowed = runCatching { LocalSyncFirewall.allow(prompt.tcpPort, prompt.udpPort) }.getOrDefault(false)
            if (hosted !== room) return@launch
            room.firewall = if (allowed) null else prompt.copy(allowing = false, allowFailed = true)
            publishHost()
        }
    }

    fun dismissFirewallPrompt() {
        scope.launch {
            val room = hosted ?: return@launch
            room.firewall = null
            publishHost()
        }
    }

    /** What the host's app is showing, for the guests' line at the top. */
    fun setActivity(activity: PartyActivity) {
        scope.launch {
            val room = hosted ?: return@launch
            if (_activity.value == activity) return@launch
            _activity.value = activity
            broadcast(room, PartyMessage.Activity(activity))
        }
    }

    /**
     * The host opened [playback]; every guest opens it too. Returns the id it is shared under,
     * the same one when it is already shared, or null when this device does not host a room.
     */
    fun startPlayback(playback: PartyPlayback): Long? {
        if (!isHost) return null
        val current = _playback.value
        if (current != null && current.copy(id = 0) == playback.copy(id = 0)) return current.id
        // Called from the player on the main thread, so the id is handed out there.
        val id = nextPlaybackId++
        scope.launch {
            val room = hosted ?: return@launch
            val shared = playback.copy(id = id)
            _playback.value = shared
            _playbackState.value = null
            forgetLoading(room)
            broadcast(room, PartyMessage.Playback(shared))
        }
        return id
    }

    /** The host left the player. */
    fun stopPlayback(playbackId: Long) {
        scope.launch {
            val room = hosted ?: return@launch
            if (_playback.value?.id != playbackId) return@launch
            _playback.value = null
            _playbackState.value = null
            forgetLoading(room)
            broadcast(room, PartyMessage.Playback(null))
        }
    }

    /** Where the host is in the shared source, which guests follow. */
    fun updatePlaybackState(state: PartyPlaybackState) {
        scope.launch {
            val room = hosted ?: return@launch
            if (_playback.value?.id != state.playbackId) return@launch
            _playbackState.value = state
            broadcast(room, PartyMessage.PlaybackState(state))
        }
    }

    /** The host plays on without waiting for whoever is loading now. */
    fun stopWaiting() {
        scope.launch {
            val room = hosted ?: return@launch
            room.guests.values.filter { it.loadingSinceEpochMs != null }.forEach { it.waitSkipped = true }
            refreshWaiting(room)
        }
    }

    /** The host's Yes to a guest's request in the chat: the title plays, or the room plays, pauses or seeks. */
    fun acceptRequest(message: PartyChatMessage) {
        val request = message.request ?: return
        if (!isHost || message.id in _answeredRequests.value) return
        _answeredRequests.update { it + message.id }
        request.suggestion?.let { _acceptedSuggestions.tryEmit(it) }
        val action = request.action ?: return
        scope.launch {
            if (_playback.value?.id != request.playbackId) return@launch
            _controls.tryEmit(PartyControl(action, request.positionMs, message.name))
        }
    }

    // ---- Everyone ----

    /** Sends [text] to the room; false when chat is off or slow mode asks to wait. */
    fun sendChat(text: String): Boolean {
        val message = text.trim().take(MAX_CHAT_LENGTH)
        val room = currentRoom ?: return false
        if (message.isEmpty() || !room.settings.chatEnabled) return false
        val now = EpisodeReleaseDatePlatform.nowEpochMs()
        if (!room.isHost && now < _nextChatAtEpochMs.value) return false
        if (!room.isHost) _nextChatAtEpochMs.value = now + room.settings.slowModeSeconds * 1_000L
        scope.launch {
            val hostedRoom = hosted
            if (hostedRoom != null) {
                postChat(hostedRoom, hostedRoom.host, message)
            } else {
                guestChannel?.let { channel -> runCatching { channel.send(PartyMessage.ChatSend(message)) } }
            }
        }
        return true
    }

    /** A guest suggests a title; false when the host turned suggestions off. */
    fun suggest(suggestion: PartySuggestion): Boolean {
        val room = currentRoom?.takeIf { !it.isHost } ?: return false
        if (!room.settings.suggestionsEnabled) return false
        scope.launch {
            guestChannel?.let { channel -> runCatching { channel.send(PartyMessage.Suggest(suggestion)) } }
        }
        return true
    }

    /** A guest plays, pauses or seeks for everyone; the host decides whether the room lets them. */
    fun requestControl(playbackId: Long, action: PartyControlAction, positionMs: Long) {
        if (!isGuest) return
        scope.launch {
            guestChannel?.let { channel ->
                runCatching { channel.send(PartyMessage.Control(playbackId, action, positionMs.coerceAtLeast(0L))) }
            }
        }
    }

    /** Tells the host whether this guest's player is loading [playbackId], for a room that waits. */
    fun reportLoading(playbackId: Long, loading: Boolean) {
        if (!isGuest) return
        val report = playbackId to loading
        if (reportedLoading == report) return
        reportedLoading = report
        scope.launch {
            guestChannel?.let { channel -> runCatching { channel.send(PartyMessage.Loading(playbackId, loading)) } }
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

    // Back to the start, with everything the room shared forgotten.
    private fun end(notice: PartyError?) {
        _activity.value = PartyActivity()
        _playback.value = null
        _playbackState.value = null
        _chat.value = emptyList()
        _answeredRequests.value = emptySet()
        _waitingFor.value = emptyList()
        reportedLoading = null
        _nextChatAtEpochMs.value = 0L
        _uiState.update { PartyUiState(phase = PartyPhase.Idle, notice = notice ?: it.notice) }
    }

    private fun myId(): String = SyncClientIdentity.currentClientId()

    private fun myName(): String =
        ProfileRepository.state.value.activeProfile?.name?.takeIf { it.isNotBlank() } ?: localSyncDeviceName()

    private fun cleanName(name: String): String = name.filterNot { it.isISOControl() }.trim().take(MAX_NAME_LENGTH)

    private fun appendChat(message: PartyChatMessage) {
        _chat.update { (it + message).takeLast(CHAT_HISTORY) }
    }

    // ---- Hosting ----

    private suspend fun hostRoom() {
        setPhase(PartyPhase.Creating)
        val lanAddresses = LocalSyncPlatform.localIpv4Addresses()
        val ipv6Addresses = partyGlobalIpv6Addresses()
        if (lanAddresses.isEmpty() && ipv6Addresses.isEmpty()) return end(PartyError.NO_NETWORK)
        val server = try {
            LocalSyncPlatform.listen(DEFAULT_PORT)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Could not open a watch party" }
            return end(PartyError.FAILED)
        }
        try {
            // The room only opens once the internet can reach it; a stopped open still gives the port back.
            val mapping = withContext(NonCancellable) { PartyPortMapper.map(server.port) }
            val publicAddress = mapping?.externalAddress?.takeIf { isPublicIpv4(it) }
            if (!currentCoroutineContext().isActive || mapping == null || publicAddress == null) {
                mapping?.let { withContext(NonCancellable) { PartyPortMapper.unmap(it) } }
                log.w { "Watch party not opened: ${mapping?.method ?: "no port"}, external ${mapping?.externalAddress}" }
                currentCoroutineContext().ensureActive()
                return end(if (mapping == null) PartyError.PORT_CLOSED else PartyError.SHARED_ADDRESS)
            }
            log.i { "Watch party open at $publicAddress:${mapping.externalPort} with ${mapping.method}" }
            coroutineScope {
                val room = HostedRoom(
                    secret = LocalSyncPlatform.randomBytes(PARTY_SECRET_SIZE),
                    server = server,
                    host = PartyMember(id = myId(), name = myName(), isHost = true),
                )
                room.mapping = mapping
                room.address = PartyEndpoint(publicAddress, mapping.externalPort).toString()
                // Public first; the local ones last, for guests whose router won't loop back to its own address.
                val endpoints = buildList {
                    add(PartyEndpoint(publicAddress, mapping.externalPort))
                    ipv6Addresses.forEach { add(PartyEndpoint(it, server.port)) }
                    lanAddresses.forEach { add(PartyEndpoint(it, server.port)) }
                }
                room.inviteLink = buildPartyLink(endpoints, room.secret)
                room.inviteQr = room.inviteLink?.let(::localSyncQrMatrix)
                hosted = room
                publishHost()
                launch { pingGuests(room) }
                launch { checkFirewall(room) }
                launch { keepMapping(room, mapping) }
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

    // Renews the router's lease before it runs out, for as long as the room is open.
    private suspend fun keepMapping(room: HostedRoom, initial: PartyPortMapping) {
        var mapping = initial
        while (currentCoroutineContext().isActive && !room.closed && mapping.leaseSeconds > 0) {
            delay((mapping.leaseSeconds * 1_000L / 2).coerceAtLeast(MIN_RENEW_INTERVAL_MS))
            if (room.closed) return
            mapping = PartyPortMapper.renew(mapping) ?: PartyPortMapper.map(room.server.port) ?: mapping
            room.mapping = mapping
        }
    }

    private suspend fun checkFirewall(room: HostedRoom) {
        val port = room.server.port
        val state = runCatching { LocalSyncFirewall.state(port, port) }.getOrDefault(LocalSyncFirewallState.UNKNOWN)
        if (state != LocalSyncFirewallState.BLOCKED || room.closed) return
        room.firewall = LocalSyncFirewallPrompt(state = state, tcpPort = port, udpPort = port)
        publishHost()
    }

    // Closes the room: everyone still in it hears so, and the app goes back to the start.
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
        room.mapping?.let { mapping -> scope.launch { PartyPortMapper.unmap(mapping) } }
        room.waitExpiry?.cancel()
        sessionJob?.cancel()
        sessionJob = null
        end(notice)
        guests.forEach { guest -> sendOff(guest, PartyRemoval.CLOSED) }
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
                when (message) {
                    is PartyMessage.ChatSend -> onGuestChat(room, guest, message.text)
                    is PartyMessage.Suggest -> onGuestSuggestion(room, guest, message.suggestion)
                    is PartyMessage.Control -> onGuestControl(room, guest, message)
                    is PartyMessage.Loading -> onGuestLoading(room, guest, message)
                    else -> Unit
                }
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
                    if (!room.closed) {
                        broadcast(room, PartyMessage.Members(room.members))
                        postEvent(room, guest.member, PartyChatEvent.LEFT)
                    }
                    publishHost()
                    refreshWaiting(room)
                    guest.channel.close()
                }
                // Replaced by a newer connection from the same device.
                else -> guest.channel.close()
            }
        }
    }

    // Returns the guest once they are in the room, or null if they were turned away.
    private suspend fun handshake(room: HostedRoom, connection: LocalSyncConnection): PartyGuest? {
        val hello = connection.readPartyHello()
        suspend fun refuse(status: PartyHelloStatus): PartyGuest? {
            connection.writePartyReply(PartyHelloReply(version = PARTY_PROTOCOL_VERSION, status = status))
            return null
        }
        if (hello.version != PARTY_PROTOCOL_VERSION) return refuse(PartyHelloStatus.VERSION_MISMATCH)
        if (room.closed) return null
        if (hello.memberId in room.banned) return refuse(PartyHelloStatus.BANNED)
        val expected = partyProof(room.secret, "guest", hello.memberId, hello.nonce)
        if (hello.proof != expected || hello.memberId == room.host.id) return refuse(PartyHelloStatus.REJECTED)
        val returning = hello.memberId in room.guests
        if (room.isFull && !returning) return refuse(PartyHelloStatus.FULL)
        val hostNonce = encodePartyBytes(LocalSyncPlatform.randomBytes(PARTY_NONCE_SIZE))
        connection.writePartyReply(
            PartyHelloReply(
                version = PARTY_PROTOCOL_VERSION,
                status = PartyHelloStatus.ACCEPTED,
                nonce = hostNonce,
                proof = partyProof(room.secret, "host", hello.nonce, hostNonce),
            ),
        )
        if (room.closed) return null

        val previous = room.guests[hello.memberId]
        val guest = PartyGuest(
            member = PartyMember(
                id = hello.memberId,
                name = previous?.member?.name ?: uniqueName(room, cleanName(hello.name)),
            ),
            channel = PartyChannel(connection, partySessionKey(room.secret, hello.nonce, hostNonce)),
        )
        room.guests[guest.member.id] = guest
        previous?.channel?.close()
        guest.channel.send(
            PartyMessage.Welcome(
                settings = room.settings,
                members = room.members,
                activity = _activity.value,
                playback = _playback.value,
                playbackState = _playbackState.value,
                chat = _chat.value.takeLast(WELCOME_CHAT),
            ),
        )
        broadcast(room, PartyMessage.Members(room.members), except = guest)
        publishHost()
        if (previous == null) postEvent(room, guest.member, PartyChatEvent.JOINED)
        return guest
    }

    // Two people with the same name are told apart by a number: Maria, Maria (2).
    private fun uniqueName(room: HostedRoom, name: String): String {
        val taken = room.members.map { it.name.lowercase() }.toSet()
        if (name.lowercase() !in taken) return name
        var number = 2
        while ("$name ($number)".lowercase() in taken) number += 1
        return "$name ($number)"
    }

    private fun onGuestChat(room: HostedRoom, guest: PartyGuest, text: String) {
        val message = text.trim().take(MAX_CHAT_LENGTH)
        if (message.isEmpty() || !room.settings.chatEnabled) return
        val now = EpisodeReleaseDatePlatform.nowEpochMs()
        // A second's grace, for the guest's clock running a little ahead of this one.
        val wait = room.settings.slowModeSeconds * 1_000L - 1_000L
        if (wait > 0 && now - guest.lastChatAtEpochMs < wait) return
        guest.lastChatAtEpochMs = now
        postChat(room, guest.member, message)
    }

    private fun postChat(room: HostedRoom, member: PartyMember, text: String) {
        val now = EpisodeReleaseDatePlatform.nowEpochMs()
        val message = PartyChatMessage(
            id = "${member.id}-$now-${nextChatNumber++}",
            memberId = member.id,
            name = member.name,
            text = text,
            sentAtEpochMs = now,
        )
        appendChat(message)
        broadcast(room, PartyMessage.Chat(message))
    }

    // A note from the room itself, such as someone joining, which everyone sees whether chat is on or not.
    private fun postEvent(room: HostedRoom, member: PartyMember, event: PartyChatEvent) {
        val now = EpisodeReleaseDatePlatform.nowEpochMs()
        val message = PartyChatMessage(
            id = "event-${member.id}-$now-${nextChatNumber++}",
            memberId = member.id,
            name = member.name,
            text = "",
            sentAtEpochMs = now,
            event = event,
        )
        appendChat(message)
        broadcast(room, PartyMessage.Chat(message))
    }

    private fun onGuestSuggestion(room: HostedRoom, guest: PartyGuest, suggestion: PartySuggestion) {
        if (!room.settings.suggestionsEnabled) return
        val stamped = suggestion.copy(
            id = "${guest.member.id}-${EpisodeReleaseDatePlatform.nowEpochMs()}",
            memberId = guest.member.id,
            memberName = guest.member.name,
        )
        postRequest(room, guest, PartyRequest(suggestion = stamped))
    }

    // A guest's request, in the chat for everyone, with Yes for the host; it goes through whether chat is on or not.
    private fun postRequest(room: HostedRoom, guest: PartyGuest, request: PartyRequest) {
        val now = EpisodeReleaseDatePlatform.nowEpochMs()
        if (now - guest.lastControlAtEpochMs < MIN_REQUEST_INTERVAL_MS) return
        guest.lastControlAtEpochMs = now
        val message = PartyChatMessage(
            id = "request-${guest.member.id}-$now-${nextChatNumber++}",
            memberId = guest.member.id,
            name = guest.member.name,
            text = "",
            sentAtEpochMs = now,
            request = request,
        )
        appendChat(message)
        broadcast(room, PartyMessage.Chat(message))
    }

    private fun onGuestControl(room: HostedRoom, guest: PartyGuest, control: PartyMessage.Control) {
        if (_playback.value?.id != control.playbackId) return
        val suggests = when (control.action) {
            PartyControlAction.PLAY, PartyControlAction.PAUSE -> room.settings.guestPause == PartyGuestPause.SUGGEST
            PartyControlAction.SEEK -> room.settings.guestSeek == PartyGuestSeek.SUGGEST
        }
        if (suggests) {
            return postRequest(room, guest, PartyRequest(control.playbackId, control.action, control.positionMs))
        }
        val allowed = when (control.action) {
            PartyControlAction.PLAY, PartyControlAction.PAUSE -> room.settings.guestPause == PartyGuestPause.PAUSE_EVERYONE
            PartyControlAction.SEEK -> room.settings.guestSeek == PartyGuestSeek.SEEK_EVERYONE
        }
        if (!allowed) return
        // Seeks all go through, so the room ends where the guest let go.
        if (control.action != PartyControlAction.SEEK) {
            val now = EpisodeReleaseDatePlatform.nowEpochMs()
            if (now - guest.lastControlAtEpochMs < MIN_CONTROL_INTERVAL_MS) return
            guest.lastControlAtEpochMs = now
        }
        _controls.tryEmit(PartyControl(control.action, control.positionMs, guest.member.name))
    }

    private fun onGuestLoading(room: HostedRoom, guest: PartyGuest, loading: PartyMessage.Loading) {
        if (_playback.value?.id != loading.playbackId) return
        if (loading.loading) {
            if (guest.loadingSinceEpochMs == null) guest.loadingSinceEpochMs = EpisodeReleaseDatePlatform.nowEpochMs()
        } else {
            guest.loadingSinceEpochMs = null
            guest.waitSkipped = false
        }
        refreshWaiting(room)
    }

    // A new source starts everyone's loading afresh.
    private fun forgetLoading(room: HostedRoom) {
        room.guests.values.forEach { guest ->
            guest.loadingSinceEpochMs = null
            guest.waitSkipped = false
        }
        refreshWaiting(room)
    }

    // Who the room waits for now; the wait ends by itself once its limit runs out.
    private fun refreshWaiting(room: HostedRoom) {
        room.waitExpiry?.cancel()
        room.waitExpiry = null
        val settings = room.settings
        if (room.closed || settings.loading != PartyLoading.PAUSE_EVERYONE) {
            _waitingFor.value = emptyList()
            return
        }
        val now = EpisodeReleaseDatePlatform.nowEpochMs()
        val limitMs = settings.loadingWaitSeconds?.let { it * 1_000L }
        val loading = room.guests.values.filter { guest ->
            val since = guest.loadingSinceEpochMs ?: return@filter false
            !guest.waitSkipped && (limitMs == null || now - since < limitMs)
        }
        val waiting = loading.map { it.member }
        if (_waitingFor.value != waiting) _waitingFor.value = waiting
        if (limitMs != null && loading.isNotEmpty()) {
            val nextExpiry = loading.minOf { it.loadingSinceEpochMs!! } + limitMs
            room.waitExpiry = scope.launch {
                delay((nextExpiry - now).coerceAtLeast(0L))
                room.waitExpiry = null
                refreshWaiting(room)
            }
        }
    }

    private fun removeGuest(room: HostedRoom, guest: PartyGuest, reason: PartyRemoval) {
        if (room.guests[guest.member.id] !== guest) return
        room.guests.remove(guest.member.id)
        guest.removed = true
        broadcast(room, PartyMessage.Members(room.members))
        postEvent(room, guest.member, PartyChatEvent.REMOVED)
        publishHost()
        refreshWaiting(room)
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
        room.guests.values.filter { it !== except }.forEach { guest ->
            scope.launch { runCatching { guest.channel.send(message) } }
        }
    }

    private fun publishHost() {
        val room = hosted ?: return
        setPhase(
            PartyPhase.InRoom(
                PartyRoom(
                    isHost = true,
                    myMemberId = room.host.id,
                    settings = room.settings,
                    members = room.members,
                    inviteLink = room.inviteLink,
                    inviteQr = room.inviteQr,
                    address = room.address,
                    firewall = room.firewall,
                    banned = room.banned.map { (id, name) -> PartyMember(id = id, name = name) },
                ),
            ),
        )
    }

    // ---- Joining ----

    private suspend fun attendRoom(invite: PartyInvite) {
        val error = try {
            setPhase(PartyPhase.Connecting)
            val (connection, endpoint) = connectFirst(invite.endpoints) ?: return end(PartyError.UNREACHABLE)
            guestConnection = connection
            try {
                converse(connection, invite.secret, endpoint)
            } finally {
                if (guestConnection === connection) guestConnection = null
                guestChannel = null
                connection.close()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            log.w(error) { "Lost the watch party" }
            if (_uiState.value.phase is PartyPhase.InRoom) PartyError.CONNECTION_LOST else PartyError.UNREACHABLE
        }
        end(error)
    }

    // Says hello and then follows the room until it ends; returns why it ended.
    private suspend fun converse(connection: LocalSyncConnection, secret: ByteArray, endpoint: PartyEndpoint): PartyError {
        val memberId = myId()
        val nonce = encodePartyBytes(LocalSyncPlatform.randomBytes(PARTY_NONCE_SIZE))
        connection.writePartyHello(
            PartyHello(
                version = PARTY_PROTOCOL_VERSION,
                memberId = memberId,
                name = myName(),
                nonce = nonce,
                proof = partyProof(secret, "guest", memberId, nonce),
            ),
        )
        val reply = connection.readPartyReply()
        if (reply.version != PARTY_PROTOCOL_VERSION) return PartyError.VERSION_MISMATCH
        when (reply.status) {
            PartyHelloStatus.ACCEPTED -> Unit
            PartyHelloStatus.FULL -> return PartyError.ROOM_FULL
            PartyHelloStatus.BANNED -> return PartyError.BANNED
            PartyHelloStatus.REJECTED -> return PartyError.REJECTED
            PartyHelloStatus.VERSION_MISMATCH -> return PartyError.VERSION_MISMATCH
        }
        // Only the room that holds the link's secret can answer this, so no one stands in for the host.
        if (reply.proof != partyProof(secret, "host", nonce, reply.nonce)) return PartyError.REJECTED
        val channel = PartyChannel(connection, partySessionKey(secret, nonce, reply.nonce))
        guestChannel = channel
        var room: PartyRoom? = null
        while (true) {
            when (val message = channel.receive()) {
                is PartyMessage.Welcome -> {
                    room = PartyRoom(
                        isHost = false,
                        myMemberId = memberId,
                        settings = message.settings,
                        members = message.members,
                        address = endpoint.toString(),
                    )
                    _activity.value = message.activity
                    _playback.value = message.playback
                    _playbackState.value = message.playbackState
                    _chat.value = message.chat
                }
                is PartyMessage.Members -> room = room?.copy(members = message.members)
                is PartyMessage.Settings -> room = room?.copy(settings = message.settings)
                is PartyMessage.Activity -> _activity.value = message.activity
                is PartyMessage.Playback -> {
                    _playback.value = message.playback
                    _playbackState.value = null
                    reportedLoading = null
                }
                is PartyMessage.PlaybackState -> _playbackState.value = message.state
                is PartyMessage.Chat -> appendChat(message.message)
                is PartyMessage.Removed -> return message.reason.toError()
                PartyMessage.Ping -> channel.send(PartyMessage.Pong)
                is PartyMessage.ChatSend, is PartyMessage.Suggest, is PartyMessage.Control, is PartyMessage.Loading,
                PartyMessage.Pong, PartyMessage.Leave, null -> Unit
            }
            room?.let { setPhase(PartyPhase.InRoom(it)) }
        }
    }

    private fun PartyRemoval.toError(): PartyError = when (this) {
        PartyRemoval.KICKED -> PartyError.KICKED
        PartyRemoval.BANNED -> PartyError.BANNED
        PartyRemoval.FULL -> PartyError.ROOM_FULL
        PartyRemoval.CLOSED -> PartyError.CLOSED
    }

    // Tries every address at once and keeps the first that answers; the rest are closed as they come in.
    private suspend fun connectFirst(endpoints: List<PartyEndpoint>): Pair<LocalSyncConnection, PartyEndpoint>? {
        if (endpoints.isEmpty()) return null
        val winner = CompletableDeferred<Pair<LocalSyncConnection, PartyEndpoint>?>()
        var remaining = endpoints.size
        var abandoned = false
        endpoints.forEach { endpoint ->
            scope.launch {
                val connection = try {
                    LocalSyncPlatform.connect(endpoint.host, endpoint.port, CONNECT_TIMEOUT_MS)
                } catch (error: Throwable) {
                    log.d { "Could not reach $endpoint: ${error.message}" }
                    null
                }
                remaining -= 1
                when {
                    connection != null -> if (abandoned || !winner.complete(connection to endpoint)) connection.close()
                    remaining == 0 -> winner.complete(null)
                }
            }
        }
        return try {
            winner.await()
        } catch (error: CancellationException) {
            abandoned = true
            if (!winner.complete(null)) withContext(NonCancellable) { winner.await()?.first?.close() }
            throw error
        }
    }
}

// Not one of the private, shared or reserved ranges, so the internet can reach it.
internal fun isPublicIpv4(address: String): Boolean {
    val parts = address.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4 || parts.any { it !in 0..255 }) return false
    val (a, b) = parts
    return when {
        a == 10 || a == 127 || a == 0 || a >= 224 -> false
        a == 172 && b in 16..31 -> false
        a == 192 && b == 168 -> false
        a == 169 && b == 254 -> false
        a == 100 && b in 64..127 -> false
        else -> true
    }
}
