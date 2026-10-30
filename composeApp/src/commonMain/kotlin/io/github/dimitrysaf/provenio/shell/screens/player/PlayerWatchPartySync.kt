package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.party.PartyControl
import io.github.dimitrysaf.provenio.core.party.PartyControlAction
import io.github.dimitrysaf.provenio.core.party.PartyGuestPause
import io.github.dimitrysaf.provenio.core.party.PartyGuestSeek
import io.github.dimitrysaf.provenio.core.party.PartyLoading
import io.github.dimitrysaf.provenio.core.party.PartyPlayback
import io.github.dimitrysaf.provenio.core.party.PartyPlaybackState
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.PartyRoom
import io.github.dimitrysaf.provenio.core.party.PartySettings
import io.github.dimitrysaf.provenio.core.party.isShareablePartySource
import io.github.dimitrysaf.provenio.shell.components.ToastController
import kotlin.math.abs
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.*

// How often each player checks itself against the room.
private const val PartySyncTickMs = 250L

// The host says where it is this often, besides whenever it plays, pauses or jumps.
private const val PartyStateIntervalMs = 2_000L

// A position that moves this much more or less than the clock did was a seek.
private const val PartySeekJumpMs = 2_000L

// After the sync seeks, the player settles this long before it is judged again.
private const val PartySettleMs = 2_000L

// Loading this long counts; shorter stalls pass unnoticed.
private const val PartyLoadingGraceMs = 1_000L

// A player loaded this far ahead is ready to play, though paused.
private const val PartyReadyBufferMs = 3_000L

// A player keeping up starts loading this far ahead of the host, twice as far each time the host
// passes it before it is ready.
private const val PartyKeepUpLeadMs = 5_000L
private const val PartyKeepUpMaxLeadMs = 40_000L

// A guest this far ahead of the host or less waits for it, rather than seeking back and loading again.
private const val PartyMaxHoldMs = 10_000L

private val PartyClockOrigin = TimeSource.Monotonic.markNow()

// Milliseconds on a clock that only moves forward, for timing between ticks.
private fun partyClockMs(): Long = PartyClockOrigin.elapsedNow().inWholeMilliseconds

// ---- The host ----

private class HostPartyClock {
    var seekCount = 0
    var changedBy: String? = null

    // A guest's play, pause or seek, until the player shows it, so it is told as theirs.
    var pendingPlay: Pair<Boolean, String>? = null
    var pendingSeek: Pair<Long, String>? = null

    // Paused for someone loading, so it plays on when they have.
    var waitPaused = false

    var lastPositionMs = -1L
    var lastAtMs = 0L
    var lastPlaying = false
    var lastSent: PartyPlaybackState? = null
    var lastSentAtMs = 0L
}

/**
 * The host shares the source it plays and where it is in it. It carries out the plays, pauses
 * and seeks the room lets guests make, and pauses while the room waits for someone loading.
 */
@Composable
internal fun PlayerScreenRuntime.HostPartySync() {
    var sharedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(activeSourceIdentityKey, activeVideoId) {
        sharedId = if (isShareablePartySource(activeSourceUrl, activeTorrentInfoHash)) {
            PartyRepository.startPlayback(currentPartyPlayback())
        } else {
            null
        }
    }
    val id = sharedId
    val clock = remember(id) { HostPartyClock() }
    val waitingFor by PartyRepository.waitingFor.collectAsStateWithLifecycle()

    LaunchedEffect(id) {
        id ?: return@LaunchedEffect
        PartyRepository.controls.collect { control -> applyGuestControl(clock, control) }
    }
    // The room pauses while someone loads, and plays on once they have, if it was playing.
    LaunchedEffect(id, waitingFor.isEmpty()) {
        id ?: return@LaunchedEffect
        if (waitingFor.isNotEmpty()) {
            if (meansToPlay()) {
                clock.waitPaused = true
                shouldPlay = false
                playerController?.pause()
            }
        } else if (clock.waitPaused) {
            clock.waitPaused = false
            shouldPlay = true
            playerController?.play()
        }
    }
    LaunchedEffect(id) {
        id ?: return@LaunchedEffect
        while (true) {
            hostTick(id, clock)
            delay(PartySyncTickMs)
        }
    }
    DisposableEffect(Unit) {
        onDispose { sharedId?.let(PartyRepository::stopPlayback) }
    }
}

private fun PlayerScreenRuntime.applyGuestControl(clock: HostPartyClock, control: PartyControl) {
    val controller = playerController ?: return
    when (control.action) {
        PartyControlAction.PLAY, PartyControlAction.PAUSE -> {
            val play = control.action == PartyControlAction.PLAY
            if (meansToPlay() == play) return
            // A guest who plays while the room waits plays it on for everyone.
            if (play && PartyRepository.waitingFor.value.isNotEmpty()) PartyRepository.stopWaiting()
            clock.waitPaused = false
            clock.pendingPlay = play to control.memberName
            shouldPlay = play
            if (play) controller.play() else controller.pause()
        }
        PartyControlAction.SEEK -> {
            clock.pendingSeek = control.positionMs to control.memberName
            controller.seekTo(control.positionMs)
        }
    }
    announceRoomChange(control.action, control.memberName, control.positionMs)
}

// Says where the host is: straight away when it plays, pauses, loads or jumps, and every couple of seconds besides.
private fun PlayerScreenRuntime.hostTick(playbackId: Long, clock: HostPartyClock) {
    val snapshot = playbackSnapshot
    val now = partyClockMs()
    val playing = meansToPlay()
    val position = snapshot.positionMs

    // The host played while the room waited: it plays on without whoever is loading.
    if (clock.waitPaused && shouldPlay) {
        clock.waitPaused = false
        PartyRepository.stopWaiting()
    }

    if (clock.lastPositionMs >= 0L) {
        val elapsed = if (clock.lastPlaying && !snapshot.isLoading) ((now - clock.lastAtMs) * snapshot.playbackSpeed).toLong() else 0L
        if (abs(position - (clock.lastPositionMs + elapsed)) > PartySeekJumpMs) {
            clock.seekCount += 1
            val pending = clock.pendingSeek
            clock.changedBy = pending?.takeIf { abs(position - it.first) < PartySeekJumpMs }?.second
            clock.pendingSeek = null
        }
    }
    if (playing != clock.lastPlaying && clock.lastPositionMs >= 0L) {
        clock.changedBy = clock.pendingPlay?.takeIf { it.first == playing }?.second
        clock.pendingPlay = null
    }
    clock.lastPositionMs = position
    clock.lastAtMs = now
    clock.lastPlaying = playing

    val state = PartyPlaybackState(
        playbackId = playbackId,
        isPlaying = playing,
        positionMs = position,
        isLoading = snapshot.isLoading,
        seekCount = clock.seekCount,
        waitingFor = PartyRepository.waitingFor.value.map { it.name },
        changedBy = clock.changedBy,
    )
    val last = clock.lastSent
    val changed = last == null || last.copy(positionMs = 0L) != state.copy(positionMs = 0L)
    if (changed || now - clock.lastSentAtMs >= PartyStateIntervalMs) {
        clock.lastSent = state
        clock.lastSentAtMs = now
        PartyRepository.updatePlaybackState(state)
    }
}

// What the play button shows: while loading, the request to play; otherwise whether it plays.
private fun PlayerScreenRuntime.meansToPlay(): Boolean =
    if (playbackSnapshot.isLoading) shouldPlay else playbackSnapshot.isPlaying

// ---- A guest ----

/** How a guest's player keeps to the host's, which the player's status line reads. */
internal class PartyGuestSync(val playbackId: Long) {
    /** Keeps to the host's time; false while the guest watches on their own. */
    var following by mutableStateOf(true)

    /** Paused or playing apart from the room, by the guest's own choice, until the host next plays or pauses. */
    var ownPace by mutableStateOf(false)

    /** Paused by the sync, for the host to catch up. */
    var holding by mutableStateOf(false)

    var state: PartyPlaybackState? = null
    var stateAtMs = 0L
    var followedPlaying: Boolean? = null
    var seenSeekCount: Int? = null

    // What the guest last asked for, so a change the sync did not make is theirs.
    var lastShouldPlay: Boolean? = null
    var lastPositionMs = -1L
    var lastAtMs = 0L
    var lastPlaying = false
    var settleUntilMs = 0L

    var loadingSinceMs: Long? = null
    var keepUpLeadMs = PartyKeepUpLeadMs
    var keepUpTargetMs: Long? = null

    /** Where the host is now, from what it last said and the time since. */
    fun hostPositionMs(nowMs: Long = partyClockMs()): Long? {
        val state = state ?: return null
        val moving = state.isPlaying && !state.isLoading && state.waitingFor.isEmpty()
        return state.positionMs + if (moving) nowMs - stateAtMs else 0L
    }
}

/**
 * A guest's player keeps to the host's: it plays and pauses with the host, jumps when the host
 * does, and is put back in step when it drifts. What the guest does themselves, and what happens
 * while their player loads, follow the room's settings.
 */
@Composable
internal fun PlayerScreenRuntime.GuestPartySync(room: PartyRoom) {
    val shared by PartyRepository.playback.collectAsStateWithLifecycle()
    val playback = shared?.takeIf { showsSource(it) }
    val sync = remember(playback?.id) { playback?.let { PartyGuestSync(it.id) } }
    partyGuestSync = sync
    val settings by rememberUpdatedState(room.settings)
    DisposableEffect(sync) {
        onDispose {
            if (partyGuestSync === sync) partyGuestSync = null
            sync?.let { PartyRepository.reportLoading(it.playbackId, false) }
        }
    }
    sync ?: return
    LaunchedEffect(sync) {
        PartyRepository.playbackState.collect { state ->
            if (state == null || state.playbackId != sync.playbackId) return@collect
            val previous = sync.state
            sync.state = state
            sync.stateAtMs = partyClockMs()
            onHostState(sync, previous, state, settings)
        }
    }
    LaunchedEffect(sync) {
        while (true) {
            guestTick(sync, settings)
            delay(PartySyncTickMs)
        }
    }
}

/** Back in step with the room: at the host's time, playing or paused as it is. */
internal fun PlayerScreenRuntime.catchUpWithParty(sync: PartyGuestSync) {
    val state = sync.state ?: return
    sync.following = true
    sync.ownPace = false
    sync.holding = false
    sync.followedPlaying = state.isPlaying
    setPartyPlay(sync, state.isPlaying)
    sync.hostPositionMs()?.let { partySeek(sync, it) }
}

private fun PlayerScreenRuntime.onHostState(
    sync: PartyGuestSync,
    previous: PartyPlaybackState?,
    state: PartyPlaybackState,
    settings: PartySettings,
) {
    val myName = partyRoom?.let { room -> room.members.firstOrNull { it.id == room.myMemberId }?.name }
    val byOther = state.changedBy?.takeIf { it != myName }
    // The host jumped: everyone keeping to the room jumps with it.
    val seen = sync.seenSeekCount
    sync.seenSeekCount = state.seekCount
    if (seen != null && seen != state.seekCount && sync.following) {
        sync.holding = false
        // The guest who moved the room is there already.
        if (abs(playbackSnapshot.positionMs - state.positionMs) > settings.syncTolerance.driftMs) partySeek(sync, state.positionMs)
        byOther?.let { announceRoomChange(PartyControlAction.SEEK, it, state.positionMs) }
    }
    // The host played or paused: everyone keeping to the room does too, whatever they chose before.
    // A room that waits for whoever loads waits for the host as well.
    val waits = settings.loading == PartyLoading.PAUSE_EVERYONE && (state.isLoading || state.waitingFor.isNotEmpty())
    val hostPlaying = state.isPlaying && !waits
    if (sync.followedPlaying != hostPlaying) {
        sync.followedPlaying = hostPlaying
        if (sync.following) {
            sync.ownPace = false
            sync.holding = false
            setPartyPlay(sync, hostPlaying)
        }
    }
    if (previous != null && previous.isPlaying != state.isPlaying && sync.following) {
        byOther?.let { announceRoomChange(if (state.isPlaying) PartyControlAction.PLAY else PartyControlAction.PAUSE, it, state.positionMs) }
    }
}

private fun PlayerScreenRuntime.guestTick(sync: PartyGuestSync, settings: PartySettings) {
    playerController ?: return
    val snapshot = playbackSnapshot
    val now = partyClockMs()
    val position = snapshot.positionMs
    // The player's own first seek, to where the host was when it opened, is not the guest's.
    if (!initialSeekApplied) sync.settleUntilMs = now + PartySettleMs
    val settled = initialLoadCompleted && now >= sync.settleUntilMs

    // What the guest did themselves: played or paused, or moved to another moment.
    val lastShouldPlay = sync.lastShouldPlay
    sync.lastShouldPlay = shouldPlay
    if (lastShouldPlay != null && lastShouldPlay != shouldPlay) {
        sync.holding = false
        onOwnPlayPause(sync, settings, shouldPlay)
    }
    if (settled && sync.lastPositionMs >= 0L) {
        val elapsed = if (sync.lastPlaying) ((now - sync.lastAtMs) * snapshot.playbackSpeed).toLong() else 0L
        if (abs(position - (sync.lastPositionMs + elapsed)) > PartySeekJumpMs) onOwnSeek(sync, settings, position)
    }
    sync.lastPositionMs = playbackSnapshot.positionMs
    sync.lastAtMs = now
    sync.lastPlaying = playbackSnapshot.isPlaying && !playbackSnapshot.isLoading

    val state = sync.state ?: return
    val hostNow = sync.hostPositionMs(now) ?: return
    val keeping = sync.following && !sync.ownPace

    // Loading: the room waits, this player keeps up with the host's time, or it carries on behind.
    val loading = snapshot.isLoading && keeping
    if (loading) {
        val since = sync.loadingSinceMs ?: now.also { sync.loadingSinceMs = it }
        if (now - since >= PartyLoadingGraceMs) {
            when (settings.loading) {
                PartyLoading.PAUSE_EVERYONE -> PartyRepository.reportLoading(sync.playbackId, true)
                PartyLoading.KEEP_UP -> if (initialSeekApplied) keepUp(sync, settings, state, hostNow, position)
                PartyLoading.CONTINUE -> Unit
            }
        }
        return
    }
    sync.loadingSinceMs = null
    sync.keepUpTargetMs = null
    sync.keepUpLeadMs = PartyKeepUpLeadMs
    // Paused by the room while it loaded, it is ready once enough is loaded ahead.
    val ready = snapshot.isPlaying || snapshot.bufferedPositionMs - position >= PartyReadyBufferMs || snapshot.isEnded
    if (ready || !keeping) PartyRepository.reportLoading(sync.playbackId, false)

    if (!keeping || !settled) return
    // Waiting for the host to reach where this player paused.
    if (sync.holding) {
        if (!state.isPlaying || hostNow >= position) {
            sync.holding = false
            setPartyPlay(sync, state.isPlaying)
        }
        return
    }
    // Under Continue, a guest who fell behind while loading stays behind.
    if (settings.loading == PartyLoading.CONTINUE) return
    val playing = snapshot.isPlaying
    if (playing != (sync.followedPlaying == true)) return
    val drift = position - hostNow
    if (abs(drift) <= settings.syncTolerance.driftMs) return
    if (playing && drift in 0..PartyMaxHoldMs) {
        // Ahead: pause until the host gets here, rather than load the same part again.
        sync.holding = true
        setPartyPlay(sync, false)
    } else {
        partySeek(sync, hostNow)
    }
}

// While this player loads, its place moves on with the host's, a little ahead, so it is ready when the host gets there.
private fun PlayerScreenRuntime.keepUp(
    sync: PartyGuestSync,
    settings: PartySettings,
    state: PartyPlaybackState,
    hostNow: Long,
    position: Long,
) {
    if (!state.isPlaying) return
    val target = sync.keepUpTargetMs
    if (target != null && hostNow < target) return
    if (target == null && hostNow - position <= settings.syncTolerance.driftMs) return
    if (target != null) sync.keepUpLeadMs = (sync.keepUpLeadMs * 2).coerceAtMost(PartyKeepUpMaxLeadMs)
    val next = hostNow + sync.keepUpLeadMs
    sync.keepUpTargetMs = next
    partySeek(sync, next)
}

private fun PlayerScreenRuntime.onOwnPlayPause(sync: PartyGuestSync, settings: PartySettings, play: Boolean) {
    val state = sync.state ?: return
    val position = playbackSnapshot.positionMs
    if (!sync.following) return
    when (settings.guestPause) {
        PartyGuestPause.PAUSE_EVERYONE -> {
            PartyRepository.requestControl(sync.playbackId, if (play) PartyControlAction.PLAY else PartyControlAction.PAUSE, position)
            awaitRoom(sync)
            // Playing again while the room is paused puts them back where the host is.
            if (play && !state.isPlaying) sync.hostPositionMs()?.let { partySeek(sync, it) }
        }
        PartyGuestPause.CONTINUE_WHERE_LEFT -> {
            if (play == state.isPlaying && !sync.ownPace) return
            sync.following = false
            sync.ownPace = false
        }
        PartyGuestPause.SNAP_TO_HOST -> {
            if (play && state.isPlaying) {
                sync.ownPace = false
                sync.hostPositionMs()?.let { partySeek(sync, it) }
            } else {
                sync.ownPace = play != state.isPlaying
            }
        }
    }
}

private fun PlayerScreenRuntime.onOwnSeek(sync: PartyGuestSync, settings: PartySettings, position: Long) {
    if (!sync.following) return
    sync.holding = false
    when (settings.guestSeek) {
        PartyGuestSeek.SEEK_EVERYONE -> {
            PartyRepository.requestControl(sync.playbackId, PartyControlAction.SEEK, position)
            awaitRoom(sync)
        }
        PartyGuestSeek.ON_THEIR_OWN -> sync.following = false
        PartyGuestSeek.SNAP_BACK -> {
            sync.hostPositionMs()?.let { partySeek(sync, it) }
            scope.launch { ToastController.show(getString(Res.string.watch_party_seek_host_only)) }
        }
    }
}

// Gives the host time to carry out what the guest asked before this player is judged against it.
private fun awaitRoom(sync: PartyGuestSync) {
    sync.settleUntilMs = partyClockMs() + PartySettleMs
}

// Plays or pauses for the room, so the change is not taken for the guest's own.
private fun PlayerScreenRuntime.setPartyPlay(sync: PartyGuestSync, play: Boolean) {
    shouldPlay = play
    sync.lastShouldPlay = play
    if (play) playerController?.play() else playerController?.pause()
}

// Seeks for the room, so the jump is not taken for the guest's own.
private fun PlayerScreenRuntime.partySeek(sync: PartyGuestSync, positionMs: Long) {
    val duration = playbackSnapshot.durationMs
    val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
    playerController?.seekTo(target)
    sync.lastPositionMs = target
    sync.lastAtMs = partyClockMs()
    sync.settleUntilMs = sync.lastAtMs + PartySettleMs
}

// Tells everyone in the player who played, paused or moved the room, when it was a guest.
private fun PlayerScreenRuntime.announceRoomChange(action: PartyControlAction, name: String, positionMs: Long) {
    scope.launch {
        ToastController.show(
            when (action) {
                PartyControlAction.PLAY -> getString(Res.string.watch_party_played_by, name)
                PartyControlAction.PAUSE -> getString(Res.string.watch_party_paused_by, name)
                PartyControlAction.SEEK -> getString(Res.string.watch_party_moved_by, name, formatPlaybackTime(positionMs))
            },
        )
    }
}

// This player shows the host's [playback], rather than something the guest opened alone.
internal fun PlayerScreenRuntime.showsSource(playback: PartyPlayback): Boolean {
    val hostHash = playback.torrentInfoHash?.lowercase()
    return if (hostHash != null) {
        activeTorrentInfoHash?.lowercase() == hostHash && activeTorrentFileIdx == playback.torrentFileIdx
    } else {
        activeSourceUrl == playback.sourceUrl
    }
}
