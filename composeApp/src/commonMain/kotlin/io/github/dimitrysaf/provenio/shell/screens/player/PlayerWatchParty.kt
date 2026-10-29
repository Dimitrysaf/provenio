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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.party.PartyChatMessage
import io.github.dimitrysaf.provenio.core.party.PartyPhase
import io.github.dimitrysaf.provenio.core.party.PartyPlayback
import io.github.dimitrysaf.provenio.core.party.PartyPlaybackState
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.PartySuggestion
import io.github.dimitrysaf.provenio.core.party.isShareablePartySource
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
import io.github.dimitrysaf.provenio.shell.components.PlatformBackHandler
import io.github.dimitrysaf.provenio.shell.components.ToastController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

// How far a guest may drift from the host before it seeks to catch up.
private const val PartySyncDriftMs = 3_000L
private const val PartyStateIntervalMs = 2_000L

// Messages that arrive while the chat is closed show for this long.
private const val PartyChatPeekMs = 8_000L
private const val PartyChatPeekCount = 2

private val ChatTextShadow = Shadow(color = Color.Black.copy(alpha = 0.85f), offset = Offset(0f, 1f), blurRadius = 4f)

/**
 * The watch party inside the player. The host shares the source it plays and where it is in
 * it; a guest's player keeps to the host's: it plays and pauses with the host, and seeks when it
 * drifts too far behind or ahead.
 */
@Composable
internal fun PlayerScreenRuntime.BindWatchPartyEffects() {
    val partyState by PartyRepository.uiState.collectAsStateWithLifecycle()
    val room = (partyState.phase as? PartyPhase.InRoom)?.room
    partyRoom = room
    if (room == null) {
        showPartyChat = false
        return
    }
    if (room.isHost) HostPartySync() else GuestPartySync()
}

@Composable
private fun PlayerScreenRuntime.HostPartySync() {
    var sharedId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(activeSourceIdentityKey, activeVideoId) {
        sharedId = if (isShareablePartySource(activeSourceUrl, activeTorrentInfoHash)) {
            PartyRepository.startPlayback(currentPartyPlayback())
        } else {
            null
        }
    }
    val id = sharedId
    // Straight away when the host plays or pauses, and every couple of seconds besides.
    LaunchedEffect(id, playbackSnapshot.isPlaying) {
        id ?: return@LaunchedEffect
        while (true) {
            PartyRepository.updatePlaybackState(
                PartyPlaybackState(
                    playbackId = id,
                    isPlaying = playbackSnapshot.isPlaying,
                    positionMs = playbackSnapshot.positionMs,
                ),
            )
            delay(PartyStateIntervalMs)
        }
    }
    DisposableEffect(Unit) {
        onDispose { sharedId?.let(PartyRepository::stopPlayback) }
    }
}

@Composable
private fun PlayerScreenRuntime.GuestPartySync() {
    val shared by PartyRepository.playback.collectAsStateWithLifecycle()
    val hostState by PartyRepository.playbackState.collectAsStateWithLifecycle()
    var followedPlaying by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(hostState) {
        val state = hostState ?: return@LaunchedEffect
        shared?.takeIf { it.id == state.playbackId && showsSource(it) } ?: return@LaunchedEffect
        val controller = playerController ?: return@LaunchedEffect
        // Play and pause follow the host when the host changes them; a guest who paused on their
        // own stays paused until then.
        if (followedPlaying != state.isPlaying) {
            followedPlaying = state.isPlaying
            shouldPlay = state.isPlaying
            if (state.isPlaying) controller.play() else controller.pause()
        }
        if (playbackSnapshot.isLoading || playbackSnapshot.isPlaying != state.isPlaying) return@LaunchedEffect
        if (abs(playbackSnapshot.positionMs - state.positionMs) > PartySyncDriftMs) {
            controller.seekTo(state.positionMs)
        }
    }
}

// This player shows the host's [playback], rather than something the guest opened alone.
private fun PlayerScreenRuntime.showsSource(playback: PartyPlayback): Boolean {
    val hostHash = playback.torrentInfoHash?.lowercase()
    return if (hostHash != null) {
        activeTorrentInfoHash?.lowercase() == hostHash && activeTorrentFileIdx == playback.torrentFileIdx
    } else {
        activeSourceUrl == playback.sourceUrl
    }
}

private fun PlayerScreenRuntime.currentPartyPlayback(): PartyPlayback = PartyPlayback(
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
 * The room's chat over the video: no box behind it, a round field to write in, and above it the
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
    val peeking = messages.takeLast(PartyChatPeekCount).filter { now - it.sentAtEpochMs < PartyChatPeekMs }
    val modifier = Modifier
        .align(Alignment.BottomStart)
        .playerFrameInsets(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
        .imePadding()
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        .widthIn(max = 420.dp)
        .fillMaxWidth(0.9f)
    AnimatedVisibility(visible = chatOpen, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        PartyChatBox(
            messages = messages,
            myMemberId = room.myMemberId,
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
            peeking.forEach { message -> PartyChatLine(message, mine = message.memberId == room.myMemberId) }
        }
    }
}

@Composable
private fun PartyChatBox(
    messages: List<PartyChatMessage>,
    myMemberId: String,
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
    val send = {
        if (waitSeconds == 0 && onSend(draft)) draft = ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 56.dp)
                // The top fades out, so older messages look like they slip away behind it.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(0f to Color.Transparent, 0.6f to Color.Black),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            items(messages, key = { it.id }) { message ->
                PartyChatLine(message, mine = message.memberId == myMemberId)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
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
private fun PartyChatLine(message: PartyChatMessage, mine: Boolean) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
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
                append(message.text)
            },
            style = MaterialTheme.typography.bodyMedium.copy(shadow = ChatTextShadow),
            color = Color.White,
        )
    }
}
