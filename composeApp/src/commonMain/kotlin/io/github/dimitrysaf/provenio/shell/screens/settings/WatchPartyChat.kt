package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.party.PartyChatMessage
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.PartyRoom
import io.github.dimitrysaf.provenio.core.time.EpisodeReleaseDatePlatform
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private val ChatHeight = 380.dp
private val BubbleCorner = 20.dp
private val BubbleTail = 4.dp

/**
 * The room's chat on the Watch party page, as a messaging app has it: this device's messages in
 * bubbles on the right, everyone else's on the left under their name, and the field to write in
 * at the bottom. It is the same chat the player shows over the video.
 */
@Composable
internal fun WatchPartyChatSection(room: PartyRoom, isTablet: Boolean) {
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
    SettingsSection(
        title = stringResource(Res.string.watch_party_chat),
        isTablet = isTablet,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(ChatHeight),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (messages.isEmpty()) {
                        Text(
                            text = stringResource(
                                if (room.settings.chatEnabled) Res.string.watch_party_chat_empty else Res.string.watch_party_chat_off,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    } else {
                        ChatMessages(messages = messages, myMemberId = room.myMemberId)
                    }
                }
                if (room.settings.chatEnabled) {
                    ChatInput(waitSeconds = waitSeconds)
                } else if (messages.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.watch_party_chat_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessages(messages: List<PartyChatMessage>, myMemberId: String) {
    val listState = rememberLazyListState()
    // Opens on the newest message and follows new ones as they come.
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
            val previous = messages.getOrNull(index - 1)
            val next = messages.getOrNull(index + 1)
            ChatBubble(
                message = message,
                mine = message.memberId == myMemberId,
                firstOfRun = previous?.memberId != message.memberId,
                lastOfRun = next?.memberId != message.memberId,
            )
        }
    }
}

// A run of messages from one person reads as one block: the name over its first bubble, the
// tail on its last, and a wider gap before the next person's.
@Composable
private fun ChatBubble(
    message: PartyChatMessage,
    mine: Boolean,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val shape = if (mine) {
        RoundedCornerShape(
            topStart = BubbleCorner,
            topEnd = if (firstOfRun) BubbleCorner else BubbleTail,
            bottomEnd = if (lastOfRun) BubbleCorner else BubbleTail,
            bottomStart = BubbleCorner,
        )
    } else {
        RoundedCornerShape(
            topStart = if (firstOfRun) BubbleCorner else BubbleTail,
            topEnd = BubbleCorner,
            bottomEnd = BubbleCorner,
            bottomStart = if (lastOfRun) BubbleCorner else BubbleTail,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (firstOfRun) 8.dp else 0.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        if (firstOfRun && !mine) {
            Text(
                text = message.name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
            )
        }
        Surface(
            shape = shape,
            color = if (mine) colors.primary else colors.surfaceContainerHighest,
            contentColor = if (mine) colors.onPrimary else colors.onSurface,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun ChatInput(waitSeconds: Int) {
    var draft by rememberSaveable { mutableStateOf("") }
    val canSend = waitSeconds == 0 && draft.isNotBlank()
    val send = {
        if (canSend && PartyRepository.sendChat(draft)) draft = ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, bottom = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
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
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            modifier = Modifier.weight(1f),
        )
        FilledIconButton(onClick = send, enabled = canSend) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(Res.string.watch_party_chat_send),
            )
        }
    }
}
