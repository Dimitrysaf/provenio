package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.party.PartyChatEvent
import io.github.dimitrysaf.provenio.core.party.PartyGuestPause
import io.github.dimitrysaf.provenio.core.party.PartyGuestSeek
import io.github.dimitrysaf.provenio.core.party.PartyChatMessage
import io.github.dimitrysaf.provenio.core.party.PartyControlAction
import io.github.dimitrysaf.provenio.core.party.PartyRequest
import io.github.dimitrysaf.provenio.core.party.PartyPhase
import io.github.dimitrysaf.provenio.core.party.PartyPlayback
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.PartySuggestion
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.ToastController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

// Messages that arrive while the chat is closed show for this long.
private const val PartyChatPeekMs = 8_000L

// A request the host hasn't answered stays this long, so there's time to say Yes.
private const val PartyRequestPeekMs = 20_000L
private const val PartyChatPeekCount = 2

private val ChatTextShadow = Shadow(color = Color.Black.copy(alpha = 0.85f), offset = Offset(0f, 1f), blurRadius = 4f)

/** The watch party inside the player: the host shares what it plays, and guests keep to it. */
@Composable
internal fun PlayerScreenRuntime.BindWatchPartyEffects() {
    val partyState by PartyRepository.uiState.collectAsStateWithLifecycle()
    val room = (partyState.phase as? PartyPhase.InRoom)?.room
    partyRoom = room
    if (room == null) {
        showPartyChat = false
        partyGuestSync = null
        return
    }
    if (room.isHost) HostPartySync() else GuestPartySync(room)
}

internal fun PlayerScreenRuntime.currentPartyPlayback(): PartyPlayback = PartyPlayback(
    id = 0,
    title = title,
    sourceUrl = activeSourceUrl,
    sourceAudioUrl = activeSourceAudioUrl,
    sourceHeaders = activeSourceHeaders,
    sourceResponseHeaders = activeSourceResponseHeaders,
    streamType = activeStreamType,
    logo = logo,
    poster = poster,
    background = background,
    seasonNumber = activeSeasonNumber,
    episodeNumber = activeEpisodeNumber,
    episodeTitle = activeEpisodeTitle,
    episodeThumbnail = activeEpisodeThumbnail,
    streamTitle = activeStreamTitle,
    streamSubtitle = activeStreamSubtitle,
    providerName = activeProviderName,
    providerAddonId = activeProviderAddonId,
    contentType = contentType,
    videoId = activeVideoId,
    parentMetaId = parentMetaId,
    parentMetaType = parentMetaType,
    torrentInfoHash = activeTorrentInfoHash,
    torrentFileIdx = activeTorrentFileIdx,
    torrentFilename = activeTorrentFilename,
    torrentTrackers = activeTorrentTrackers,
)

/** A guest whose room only lets the host move its time: seeking is off, and the seek bar is gray. */
internal val PlayerScreenRuntime.partySeekLocked: Boolean
    get() = partyRoom?.let { !it.isHost && it.settings.guestSeek == PartyGuestSeek.SNAP_BACK } == true

/** A guest whose room only lets the host pause: the pause button is gone. */
internal val PlayerScreenRuntime.partyPauseLocked: Boolean
    get() = partyRoom?.let { !it.isHost && it.settings.guestPause == PartyGuestPause.HOST_ONLY } == true

/** Guests play at the host's speed, so theirs can't change. */
internal val PlayerScreenRuntime.partySpeedLocked: Boolean
    get() = partyGuest

/** What a guest asks the host for, said without their name, which goes beside it. */
@Composable
internal fun partyRequestText(request: PartyRequest): String {
    request.suggestion?.let { suggestion ->
        val episode = if (suggestion.seasonNumber != null && suggestion.episodeNumber != null) {
            " · S${suggestion.seasonNumber}E${suggestion.episodeNumber}"
        } else {
            ""
        }
        return stringResource(Res.string.watch_party_request_title, suggestion.title + episode)
    }
    return when (request.action) {
        PartyControlAction.PLAY -> stringResource(Res.string.watch_party_request_play)
        PartyControlAction.PAUSE -> stringResource(Res.string.watch_party_request_pause)
        PartyControlAction.SEEK -> stringResource(Res.string.watch_party_request_seek, formatPlaybackTime(request.positionMs))
        null -> ""
    }
}

/** A guest whose room has them ask the host to pause or play. */
internal val PlayerScreenRuntime.partyPauseSuggests: Boolean
    get() = partyRoom?.let { !it.isHost && it.settings.guestPause == PartyGuestPause.SUGGEST } == true

/** A guest whose room has them ask the host to seek. */
internal val PlayerScreenRuntime.partySeekSuggests: Boolean
    get() = partyRoom?.let { !it.isHost && it.settings.guestSeek == PartyGuestSeek.SUGGEST } == true

/** Asks the host to play, pause or seek the room, instead of doing it here. */
internal fun PlayerScreenRuntime.askPartyHost(action: PartyControlAction, positionMs: Long) {
    val playbackId = partyGuestSync?.playbackId ?: return
    PartyRepository.requestControl(playbackId, action, positionMs)
    controlsVisible = true
    scope.launch { ToastController.show(getString(Res.string.watch_party_requested)) }
}

/** The room's note about [name], such as them joining. */
@Composable
internal fun partyChatEventText(event: PartyChatEvent, name: String): String = when (event) {
    PartyChatEvent.JOINED -> stringResource(Res.string.watch_party_chat_joined, name)
    PartyChatEvent.LEFT -> stringResource(Res.string.watch_party_chat_left, name)
    PartyChatEvent.REMOVED -> stringResource(Res.string.watch_party_chat_removed, name)
}

/** What the room's line at the top of the player says, and what tapping it does. */
internal class PartyPlayerLine(val text: String, val action: String?, val onClick: () -> Unit)

/** The room's line while the player is out of step with it: someone is loading, or this guest watches on their own. */
@Composable
internal fun PlayerScreenRuntime.partyPlayerLine(): PartyPlayerLine? {
    val room = partyRoom ?: return null
    val hostState by PartyRepository.playbackState.collectAsStateWithLifecycle()
    val hostWaiting by PartyRepository.waitingFor.collectAsStateWithLifecycle()
    val sync = partyGuestSync
    // Ticks, so the time behind or ahead keeps up.
    val tick by produceState(0L, sync) {
        while (true) {
            delay(1_000L)
            value += 1
        }
    }
    tick
    val waiting = if (room.isHost) {
        hostWaiting.map { it.name }
    } else {
        hostState?.takeIf { it.playbackId == sync?.playbackId }?.waitingFor.orEmpty()
    }
    val myName = room.members.firstOrNull { it.id == room.myMemberId }?.name
    return when {
        waiting.isNotEmpty() && !room.isHost && myName in waiting && waiting.size == 1 ->
            PartyPlayerLine(stringResource(Res.string.watch_party_status_waiting_you), null) {}
        waiting.isNotEmpty() -> PartyPlayerLine(
            text = stringResource(Res.string.watch_party_status_waiting, waiting.filter { it != myName }.joinToString(", ")),
            action = if (room.isHost) stringResource(Res.string.watch_party_status_dont_wait) else null,
            onClick = { if (room.isHost) PartyRepository.stopWaiting() },
        )
        sync != null && (!sync.following || sync.ownPace) -> {
            val offset = sync.hostPositionMs()?.let { playbackSnapshot.positionMs - it } ?: 0L
            PartyPlayerLine(
                text = when {
                    offset <= -1_000L -> stringResource(Res.string.watch_party_status_behind, formatPlaybackTime(-offset))
                    offset >= 1_000L -> stringResource(Res.string.watch_party_status_ahead, formatPlaybackTime(offset))
                    else -> stringResource(Res.string.watch_party_status_own)
                },
                action = stringResource(Res.string.watch_party_status_catch_up),
                onClick = { catchUpWithParty(sync) },
            )
        }
        sync?.holding == true -> PartyPlayerLine(stringResource(Res.string.watch_party_status_holding), null) {}
        else -> null
    }
}

/** A guest's next episode goes to the host as a suggestion, and the card goes away. */
internal fun PlayerScreenRuntime.suggestNextEpisodeToHost() {
    val next = nextEpisodeInfo ?: return
    val sent = PartyRepository.suggest(
        PartySuggestion(
            type = contentType ?: parentMetaType,
            videoId = next.videoId,
            parentMetaId = parentMetaId,
            parentMetaType = parentMetaType,
            title = title,
            logo = logo,
            poster = poster,
            background = background,
            seasonNumber = next.season,
            episodeNumber = next.episode,
            episodeTitle = next.title,
            episodeThumbnail = next.thumbnail,
        ),
    )
    nextEpisodeCardDismissed = true
    showNextEpisodeCard = false
    scope.launch {
        ToastController.show(getString(if (sent) Res.string.watch_party_suggested else Res.string.watch_party_suggestions_off))
    }
}

/**
 * The room's chat over the video, at its right edge: no box behind it, a round field to write in, and above it the
 * last two messages, fading out at the top, which scroll back to older ones. Opening it puts the
 * controls away; while it is closed, new messages show for a few seconds where it would be.
 */
@Composable
internal fun BoxScope.PlayerWatchPartyChat(runtime: PlayerScreenRuntime) {
    val room = runtime.partyRoom ?: return
    val messages by PartyRepository.chat.collectAsStateWithLifecycle()
    val nextChatAt by PartyRepository.nextChatAtEpochMs.collectAsStateWithLifecycle()
    val now by produceState(EpisodeReleaseDatePlatform.nowEpochMs(), runtime.showPartyChat, messages.size) {
        while (true) {
            value = EpisodeReleaseDatePlatform.nowEpochMs()
            delay(1_000L)
        }
    }
    val chatOpen = runtime.showPartyChat && room.settings.chatEnabled
    PlatformBackHandler(enabled = chatOpen) { runtime.showPartyChat = false }
    val answered by PartyRepository.answeredRequests.collectAsStateWithLifecycle()
    val canAnswer = { message: PartyChatMessage -> room.isHost && message.request != null && message.id !in answered }
    val peeking = messages.takeLast(PartyChatPeekCount).filter { message ->
        now - message.sentAtEpochMs < if (canAnswer(message)) PartyRequestPeekMs else PartyChatPeekMs
    }
    val modifier = Modifier
        .align(Alignment.BottomEnd)
        .playerFrameInsets(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        .widthIn(max = 420.dp)
        .fillMaxWidth(0.9f)
    // Open, the chat runs the full height, over a shadow that comes in from the right edge.
    AnimatedVisibility(
        visible = chatOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.6f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
        )
    }
    AnimatedVisibility(
        visible = chatOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .fillMaxHeight()
            // Android slides the screen up for the keyboard itself, so no padding for it here.
            .playerFrameInsets(WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal)
            .padding(16.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth(0.9f),
    ) {
        PartyChatBox(
            messages = messages,
            myMemberId = room.myMemberId,
            canAnswer = canAnswer,
            waitSeconds = if (room.isHost) 0 else ((nextChatAt - now + 999) / 1_000).toInt().coerceAtLeast(0),
            onSend = PartyRepository::sendChat,
            onClose = { runtime.showPartyChat = false },
        )
    }
    AnimatedVisibility(
        visible = !chatOpen && peeking.isNotEmpty() && !runtime.controlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            peeking.forEach { message ->
                PartyChatLine(message, mine = message.memberId == room.myMemberId, canAnswer = canAnswer(message))
            }
        }
    }
}

@Composable
private fun PartyChatBox(
    messages: List<PartyChatMessage>,
    myMemberId: String,
    canAnswer: (PartyChatMessage) -> Boolean,
    waitSeconds: Int,
    onSend: (String) -> Boolean,
    onClose: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    // Sent, the field goes away with the controls' chat, and the message shows over the video.
    val send = {
        if (waitSeconds == 0 && onSend(draft)) {
            draft = ""
            onClose()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // The top fades out, so older messages look like they slip away behind it.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(0f to Color.Transparent, 0.15f to Color.Black),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            items(messages, key = { it.id }) { message ->
                PartyChatLine(message, mine = message.memberId == myMemberId, canAnswer = canAnswer(message))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = draft,
                onValueChange = { draft = it.take(PartyRepository.MAX_CHAT_LENGTH) },
                singleLine = true,
                placeholder = {
                    Text(
                        if (waitSeconds > 0) {
                            stringResource(Res.string.watch_party_chat_wait, waitSeconds)
                        } else {
                            stringResource(Res.string.watch_party_chat_hint)
                        },
                    )
                },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Black.copy(alpha = 0.45f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.45f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                trailingIcon = {
                    IconButton(onClick = send, enabled = waitSeconds == 0 && draft.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(Res.string.watch_party_chat_send),
                            tint = Color.White,
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PartyChatLine(message: PartyChatMessage, mine: Boolean, canAnswer: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        message.event?.let { event ->
            Text(
                text = partyChatEventText(event, message.name),
                style = MaterialTheme.typography.bodyMedium.copy(shadow = ChatTextShadow, fontStyle = FontStyle.Italic),
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.End,
            )
            return@Row
        }
        Text(
            textAlign = TextAlign.End,
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        color = if (mine) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.8f),
                    ),
                ) {
                    append(message.name)
                }
                append("  ")
                append(message.request?.let { partyRequestText(it) } ?: message.text)
            },
            style = MaterialTheme.typography.bodyMedium.copy(shadow = ChatTextShadow),
            color = Color.White,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (canAnswer) {
            FilledTonalButton(onClick = { PartyRepository.acceptRequest(message) }) {
                Text(stringResource(Res.string.watch_party_request_yes))
            }
        }
    }
}
