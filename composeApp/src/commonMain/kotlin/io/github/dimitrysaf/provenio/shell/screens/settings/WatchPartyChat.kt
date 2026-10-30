package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.party.PartyChatMessage
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.PartyRoom
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
import io.github.dimitrysaf.provenio.shell.WatchPartyPrompts
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.screens.player.partyChatEventText
import io.github.dimitrysaf.provenio.shell.screens.player.partyRequestText
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private val BubbleCorner = 20.dp
private val BubbleJoinCorner = 6.dp
private val AvatarSize = 32.dp
private val InputHeight = 56.dp

// A gap this long between two messages starts a new stretch, with the time over it.
private const val TimeSeparatorGapMs = 15 * 60_000L

/**
 * The room, as a conversation in Google Messages: the room at the top, which opens its
 * settings, the messages filling the page in round bubbles, this device's on the right and
 * everyone else's on the left, and the field to write in at the bottom.
 */
@Composable
internal fun WatchPartyConversation(
    room: PartyRoom,
    onBack: (() -> Unit)?,
    onOpenRoomSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages by PartyRepository.chat.collectAsStateWithLifecycle()
    val nextChatAt by PartyRepository.nextChatAtEpochMs.collectAsStateWithLifecycle()
    val now by produceState(EpisodeReleaseDatePlatform.nowEpochMs(), nextChatAt) {
        while (true) {
            value = EpisodeReleaseDatePlatform.nowEpochMs()
            if (value >= nextChatAt) break
            delay(1_000L)
        }
    }
    val waitSeconds = if (room.isHost) 0 else ((nextChatAt - now + 999) / 1_000).toInt().coerceAtLeast(0)
    // The line at the top of the app would only repeat the room's header here.
    DisposableEffect(Unit) {
        WatchPartyPrompts.setConversationShown(true)
        onDispose { WatchPartyPrompts.setConversationShown(false) }
    }
    // Android already slides the screen up for the keyboard; padding for it here as well lifted the field twice.
    Column(modifier = modifier) {
        ConversationHeader(room = room, onBack = onBack, onClick = onOpenRoomSettings)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ConversationMessages(messages = messages, room = room)
        }
        ConversationInput(enabled = room.settings.chatEnabled, waitSeconds = waitSeconds)
    }
}

@Composable
private fun ConversationHeader(room: PartyRoom, onBack: (() -> Unit)?, onClick: () -> Unit) {
    // Only the settings button at the end opens the room's settings, not the whole header.
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(Res.string.action_back),
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(start = if (onBack != null) 4.dp else 0.dp).size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Rounded.Groups, contentDescription = null)
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                text = if (room.isHost) {
                    stringResource(Res.string.watch_party_title)
                } else {
                    stringResource(Res.string.watch_party_hosted_by, room.hostName.orEmpty())
                },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.watch_party_section_members, room.members.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(Res.string.watch_party_section_settings),
            )
        }
    }
}

@Composable
private fun ConversationMessages(messages: List<PartyChatMessage>, room: PartyRoom) {
    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Forum,
                title = stringResource(Res.string.watch_party_chat_empty_title),
                message = stringResource(Res.string.watch_party_chat_empty_message),
            )
        }
        return
    }
    val listState = rememberLazyListState()
    // Opens on the newest message and follows new ones as they come.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    val guestName = stringResource(Res.string.watch_party_guest_name)
    var memberToRemoveId by rememberSaveable { mutableStateOf<String?>(null) }
    val answered by PartyRepository.answeredRequests.collectAsStateWithLifecycle()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
            val previous = messages.getOrNull(index - 1)
            val next = messages.getOrNull(index + 1)
            val newStretch = previous == null || message.sentAtEpochMs - previous.sentAtEpochMs >= TimeSeparatorGapMs
            if (newStretch) {
                localClockTime(message.sentAtEpochMs)?.let { time ->
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (previous == null) 4.dp else 20.dp, bottom = 12.dp),
                    )
                }
            }
            val name = message.name.ifBlank { guestName }
            message.event?.let { event ->
                Text(
                    text = partyChatEventText(event, name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                return@itemsIndexed
            }
            val firstOfRun = newStretch || previous.memberId != message.memberId || previous.event != null
            val lastOfRun = next == null || next.memberId != message.memberId || next.event != null ||
                next.sentAtEpochMs - message.sentAtEpochMs >= TimeSeparatorGapMs
            // The host taps a guest's name or picture to remove or ban them.
            val manageable = room.isHost && room.members.any { it.id == message.memberId && !it.isHost }
            MessageBubble(
                message = message,
                name = name,
                mine = message.memberId == room.myMemberId,
                firstOfRun = firstOfRun,
                lastOfRun = lastOfRun,
                onSenderClick = if (manageable) ({ memberToRemoveId = message.memberId }) else null,
                onYes = if (room.isHost && message.request != null && message.id !in answered) {
                    { PartyRepository.acceptRequest(message) }
                } else {
                    null
                },
            )
        }
    }
    room.members.firstOrNull { it.id == memberToRemoveId && room.isHost && !it.isHost }?.let { member ->
        WatchPartyMemberDialog(
            name = member.name.ifBlank { guestName },
            onKick = {
                memberToRemoveId = null
                PartyRepository.kick(member.id)
            },
            onBan = {
                memberToRemoveId = null
                PartyRepository.ban(member.id)
            },
            onDismiss = { memberToRemoveId = null },
        )
    }
}

// Messages from one person in a row sit close together, their joined corners tighter; others' carry their name.
@Composable
private fun MessageBubble(
    message: PartyChatMessage,
    name: String,
    mine: Boolean,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    onSenderClick: (() -> Unit)?,
    onYes: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val senderModifier = if (onSenderClick != null) Modifier.clickable(onClick = onSenderClick) else Modifier
    val top = if (firstOfRun) BubbleCorner else BubbleJoinCorner
    val bottom = if (lastOfRun) BubbleCorner else BubbleJoinCorner
    val shape = if (mine) {
        RoundedCornerShape(topStart = BubbleCorner, topEnd = top, bottomEnd = bottom, bottomStart = BubbleCorner)
    } else {
        RoundedCornerShape(topStart = top, topEnd = BubbleCorner, bottomEnd = BubbleCorner, bottomStart = bottom)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (firstOfRun) 10.dp else 2.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (!mine && firstOfRun) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = AvatarSize + 8.dp + 12.dp, bottom = 4.dp)
                    .then(senderModifier),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            if (!mine) {
                if (lastOfRun) {
                    Box(modifier = senderModifier) { Avatar(name) }
                } else {
                    Spacer(modifier = Modifier.width(AvatarSize))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Surface(
                shape = shape,
                color = if (mine) colors.primaryContainer else colors.surfaceContainerHigh,
                contentColor = if (mine) colors.onPrimaryContainer else colors.onSurface,
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Text(
                    text = message.request?.let { partyRequestText(it) } ?: message.text,
                    style = if (message.request != null) {
                        MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        // The host answers a guest's request here; Yes plays the title, or plays, pauses or seeks the room.
        if (onYes != null) {
            FilledTonalButton(
                onClick = onYes,
                modifier = Modifier.padding(start = AvatarSize + 8.dp, top = 6.dp),
            ) {
                Text(stringResource(Res.string.watch_party_request_yes))
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.size(AvatarSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ConversationInput(enabled: Boolean, waitSeconds: Int) {
    var draft by rememberSaveable { mutableStateOf("") }
    val canSend = enabled && waitSeconds == 0 && draft.isNotBlank()
    val send = {
        if (canSend && PartyRepository.sendChat(draft)) draft = ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextField(
            value = draft,
            onValueChange = { draft = it.take(PartyRepository.MAX_CHAT_LENGTH) },
            enabled = enabled,
            singleLine = true,
            placeholder = {
                Text(
                    when {
                        !enabled -> stringResource(Res.string.watch_party_chat_off)
                        waitSeconds > 0 -> stringResource(Res.string.watch_party_chat_wait, waitSeconds)
                        else -> stringResource(Res.string.watch_party_chat_hint)
                    },
                )
            },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier
                .weight(1f)
                .height(InputHeight),
        )
        FilledIconButton(
            onClick = send,
            enabled = canSend,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.size(InputHeight),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(Res.string.watch_party_chat_send),
            )
        }
    }
}

// The hour and minute [epochMs] falls on here, as 11:10.
private fun localClockTime(epochMs: Long): String? {
    val date = EpisodeReleaseDatePlatform.localIsoDateAtEpochMs(epochMs) ?: return null
    val dayStart = EpisodeReleaseDatePlatform.localDateTimeToEpochMs("${date}T00:00:00") ?: return null
    val minutes = ((epochMs - dayStart) / 60_000L).toInt().coerceIn(0, 24 * 60 - 1)
    return "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"
}
