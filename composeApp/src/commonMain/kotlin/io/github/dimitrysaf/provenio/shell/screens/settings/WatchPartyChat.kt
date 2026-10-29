package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.FilledIconButton
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
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private val BubbleShape = RoundedCornerShape(26.dp)
private val AvatarSize = 32.dp

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
    Column(modifier = modifier.imePadding()) {
        ConversationHeader(room = room, onBack = onBack, onClick = onOpenRoomSettings)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ConversationMessages(messages = messages, room = room)
        }
        ConversationInput(enabled = room.settings.chatEnabled, waitSeconds = waitSeconds)
    }
}

@Composable
private fun ConversationHeader(room: PartyRoom, onBack: (() -> Unit)?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
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
        }
    }
}

@Composable
private fun ConversationMessages(messages: List<PartyChatMessage>, room: PartyRoom) {
    val listState = rememberLazyListState()
    // Opens on the newest message and follows new ones as they come.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    val guestName = stringResource(Res.string.watch_party_guest_name)
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
            val mine = message.memberId == room.myMemberId
            val firstOfRun = newStretch || previous?.memberId != message.memberId
            val lastOfRun = next == null || next.memberId != message.memberId ||
                next.sentAtEpochMs - message.sentAtEpochMs >= TimeSeparatorGapMs
            MessageBubble(
                message = message,
                name = message.name.ifBlank { guestName },
                mine = mine,
                firstOfRun = firstOfRun,
                lastOfRun = lastOfRun,
                // Two people need no names; a room of more does.
                showSender = room.members.size > 2 || room.members.none { it.id == message.memberId },
            )
        }
    }
}

// Messages from one person in a row sit close together; a new person starts further down.
@Composable
private fun MessageBubble(
    message: PartyChatMessage,
    name: String,
    mine: Boolean,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    showSender: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (firstOfRun) 10.dp else 3.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (!mine && showSender && firstOfRun) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = AvatarSize + 8.dp + 14.dp, bottom = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            if (!mine && showSender) {
                if (lastOfRun) Avatar(name) else Spacer(modifier = Modifier.width(AvatarSize))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Surface(
                shape = BubbleShape,
                color = if (mine) colors.primaryContainer else colors.surfaceContainerHigh,
                contentColor = if (mine) colors.onPrimaryContainer else colors.onSurface,
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
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
            onValueChange = { draft = it },
            enabled = enabled,
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
            maxLines = 5,
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
                .heightIn(min = 56.dp),
        )
        FilledIconButton(
            onClick = send,
            enabled = canSend,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = Modifier.size(56.dp),
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
