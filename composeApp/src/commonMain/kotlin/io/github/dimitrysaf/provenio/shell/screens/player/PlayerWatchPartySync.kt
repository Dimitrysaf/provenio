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
import io.github.dimitrysaf.provenio.core.party.PartyChatEvent
import io.github.dimitrysaf.provenio.core.playback.languageMatchesPreference
import io.github.dimitrysaf.provenio.core.playback.selectionKey
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

// How far ahead of the host a player keeping up loads, doubled each time the host passes it.
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

    var pendingSpeed: Pair<Float, String>? = null

    // Paused for someone loading, so it plays on when they have.
    var waitPaused = false

    // A play or pause the room made itself, for a wait, which the chat doesn't note.
    var quietPlay: Boolean? = null
    var lastSpeed = 1f

    var lastPositionMs = -1L
    var lastAtMs = 0L
    var lastPlaying = false
    var lastSent: PartyPlaybackState? = null
    var lastSentAtMs = 0L
}

/** The host shares its source and place, carries out guests' allowed controls, and waits for loaders. */
@Composable
internal fun PlayerScreenRuntime.HostPartySync() {
    var sharedId by remember { mutableStateOf<Long?>(null) }
    // Guests stream the torrent themselves, even the one behind a debrid link; a web link goes out only when shared.
    val shareLinks = partyRoom?.settings?.shareStreamLinks == true
    val sourceTorrent = activeSourceTorrent
    LaunchedEffect(activeSourceIdentityKey, activeVideoId, shareLinks, sourceTorrent) {
        val playback = currentPartyPlayback()
        sharedId = PartyRepository.startPlayback(
            when {
                !activeTorrentInfoHash.isNullOrBlank() -> playback
                shareLinks && isShareablePartySource(activeSourceUrl, null) -> playback
                sourceTorrent != null -> playback.copy(
                    sourceUrl = "torrent://${sourceTorrent.infoHash}${sourceTorrent.fileIdx?.let { "?index=$it" }.orEmpty()}",
                    sourceAudioUrl = null,
                    sourceHeaders = emptyMap(),
                    sourceResponseHeaders = emptyMap(),
                    torrentInfoHash = sourceTorrent.infoHash,
                    torrentFileIdx = sourceTorrent.fileIdx,
                    torrentFilename = sourceTorrent.filename,
                    torrentTrackers = sourceTorrent.trackers,
                )
                else -> playback.copy(
                    sourceUrl = "",
                    sourceAudioUrl = null,
                    sourceHeaders = emptyMap(),
                    sourceResponseHeaders = emptyMap(),
                    ownSource = true,
                )
            },
        )
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
                clock.quietPlay = false
                shouldPlay = false
                playerController?.pause()
            }
        } else if (clock.waitPaused) {
            clock.waitPaused = false
            clock.quietPlay = true
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
        PartyControlAction.SPEED -> {
            clock.pendingSpeed = control.speed to control.memberName
            controller.setPlaybackSpeed(control.speed)
        }
    }
}

// Says where the host is on every change, and every couple of seconds besides.
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

    // Each jump, play, pause and change of speed goes in the chat, as the host's or the guest's who asked for it.
    if (clock.lastPositionMs >= 0L) {
        val elapsed = if (clock.lastPlaying && !snapshot.isLoading) ((now - clock.lastAtMs) * snapshot.playbackSpeed).toLong() else 0L
        if (abs(position - (clock.lastPositionMs + elapsed)) > PartySeekJumpMs) {
            clock.seekCount += 1
            val pending = clock.pendingSeek
            clock.changedBy = pending?.takeIf { abs(position - it.first) < PartySeekJumpMs }?.second
            clock.pendingSeek = null
            PartyRepository.postRoomAction(PartyChatEvent.SEEKED, clock.changedBy, positionMs = position)
        }
        if (playing != clock.lastPlaying) {
            clock.changedBy = clock.pendingPlay?.takeIf { it.first == playing }?.second
            clock.pendingPlay = null
            if (clock.quietPlay != playing) {
                PartyRepository.postRoomAction(if (playing) PartyChatEvent.PLAYED else PartyChatEvent.PAUSED, clock.changedBy)
            }
            clock.quietPlay = null
        }
        // Holding for speed is a moment's boost, not a change the room needs told about.
        if (abs(snapshot.playbackSpeed - clock.lastSpeed) > 0.01f && speedBoostRestoreSpeed == null) {
            val by = clock.pendingSpeed?.takeIf { abs(it.first - snapshot.playbackSpeed) < 0.01f }?.second
            clock.pendingSpeed = null
            PartyRepository.postRoomAction(PartyChatEvent.SPEED, by, speed = snapshot.playbackSpeed)
        }
    }
    clock.lastSpeed = snapshot.playbackSpeed
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
        speed = snapshot.playbackSpeed,
        audioLanguage = audioTracks.firstOrNull { it.index == selectedAudioIndex }?.language,
        subtitleLanguage = when {
            useCustomSubtitles -> selectedAddonSubtitle?.language
            selectedSubtitleIndex < 0 -> ""
            else -> subtitleTracks.firstOrNull { it.index == selectedSubtitleIndex }?.language
        },
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

    /** Paused or playing apart from the room by choice, until the host next plays or pauses. */
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

    // The host's subtitle and audio languages this player last matched, so each is matched once.
    var matchedAudio: String? = null
    var matchedSubtitle: String? = null

    /** Where the host is now, from what it last said and the time since, at the host's speed. */
    fun hostPositionMs(nowMs: Long = partyClockMs()): Long? {
        val state = state ?: return null
        val moving = state.isPlaying && !state.isLoading && state.waitingFor.isEmpty()
        return state.positionMs + if (moving) ((nowMs - stateAtMs) * state.speed).toLong() else 0L
    }
}

/** A guest's player keeps to the host's, as the room's settings say. */
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
            sync.state = state
            sync.stateAtMs = partyClockMs()
            onHostState(sync, state, settings)
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
    playerController?.setPlaybackSpeed(state.speed)
    sync.hostPositionMs()?.let { partySeek(sync, it) }
}

private fun PlayerScreenRuntime.onHostState(
    sync: PartyGuestSync,
    state: PartyPlaybackState,
    settings: PartySettings,
) {
    // The host jumped: everyone keeping to the room jumps with it.
    val seen = sync.seenSeekCount
    sync.seenSeekCount = state.seekCount
    if (seen != null && seen != state.seekCount && sync.following) {
        sync.holding = false
        // The guest who moved the room is there already.
        if (abs(playbackSnapshot.positionMs - state.positionMs) > settings.syncTolerance.driftMs) partySeek(sync, state.positionMs)
    }
    // Everyone keeping to the room plays at the host's speed.
    if (sync.following && abs(playbackSnapshot.playbackSpeed - state.speed) > 0.01f) playerController?.setPlaybackSpeed(state.speed)
    // Everyone keeping to the room plays and pauses with the host, and waits while it loads if the room waits.
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
    if (settings.matchTracks && sync.following) matchPartyTracks(sync, state)

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

// While loading, moves this player a little ahead of the host, so it is ready when the host gets there.
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
        // A pause from the keyboard or a headset is undone, since only the host pauses.
        PartyGuestPause.HOST_ONLY -> setPartyPlay(sync, sync.followedPlaying == true)
        // One that got past the controls, from a headset say, asks the host and is undone here.
        PartyGuestPause.SUGGEST -> {
            PartyRepository.requestControl(sync.playbackId, if (play) PartyControlAction.PLAY else PartyControlAction.PAUSE, position)
            setPartyPlay(sync, sync.followedPlaying == true)
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
        PartyGuestSeek.SUGGEST -> {
            PartyRepository.requestControl(sync.playbackId, PartyControlAction.SEEK, position)
            sync.hostPositionMs()?.let { partySeek(sync, it) }
        }
        PartyGuestSeek.SNAP_BACK -> {
            sync.hostPositionMs()?.let { partySeek(sync, it) }
            scope.launch { ToastController.show(getString(Res.string.watch_party_seek_host_only)) }
        }
    }
}

// Picks the subtitle and audio tracks in the host's languages, each once it changes and this player has a track for it.
private fun PlayerScreenRuntime.matchPartyTracks(sync: PartyGuestSync, state: PartyPlaybackState) {
    val controller = playerController ?: return
    state.audioLanguage?.takeIf { it != sync.matchedAudio }?.let { language ->
        val track = audioTracks.firstOrNull { languageMatchesPreference(it.language, language) } ?: return@let
        sync.matchedAudio = language
        if (track.index != selectedAudioIndex) {
            selectedAudioIndex = track.index
            controller.selectAudioTrack(track.index)
        }
    }
    val subtitle = state.subtitleLanguage?.takeIf { it != sync.matchedSubtitle } ?: return
    if (subtitle.isEmpty()) {
        sync.matchedSubtitle = subtitle
        if (useCustomSubtitles) controller.clearExternalSubtitleAndSelect(-1) else controller.selectSubtitleTrack(-1)
        useCustomSubtitles = false
        selectedAddonSubtitleId = null
        selectedSubtitleIndex = -1
        return
    }
    val builtIn = subtitleTracks.firstOrNull { languageMatchesPreference(it.language, subtitle) }
    if (builtIn != null) {
        sync.matchedSubtitle = subtitle
        if (useCustomSubtitles) controller.clearExternalSubtitleAndSelect(builtIn.index) else controller.selectSubtitleTrack(builtIn.index)
        useCustomSubtitles = false
        selectedAddonSubtitleId = null
        selectedSubtitleIndex = builtIn.index
        return
    }
    val addon = addonSubtitles.firstOrNull { languageMatchesPreference(it.language, subtitle) } ?: return
    sync.matchedSubtitle = subtitle
    useCustomSubtitles = true
    selectedAddonSubtitleId = addon.selectionKey
    selectedSubtitleIndex = -1
    controller.setSubtitleUri(addon.url)
}

// Gives the host time to carry out the guest's request before this player is judged.
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

// This player shows the host's [playback], rather than something the guest opened alone.
internal fun PlayerScreenRuntime.showsSource(playback: PartyPlayback): Boolean {
    // From a source of the guest's own, the same title and episode is the host's.
    if (playback.ownSource) {
        return parentMetaId == playback.parentMetaId && (playback.videoId == null || activeVideoId == playback.videoId)
    }
    val hostHash = playback.torrentInfoHash?.lowercase()
    return if (hostHash != null) {
        activeTorrentInfoHash?.lowercase() == hostHash && activeTorrentFileIdx == playback.torrentFileIdx
    } else {
        activeSourceUrl == playback.sourceUrl
    }
}
