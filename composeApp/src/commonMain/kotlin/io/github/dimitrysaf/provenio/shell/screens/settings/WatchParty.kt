package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.localsync.LocalSyncFirewallPrompt
import io.github.dimitrysaf.provenio.core.party.PartyPhase
import io.github.dimitrysaf.provenio.core.party.PartyReachability
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.PartyRoom
import io.github.dimitrysaf.provenio.shell.WatchPartyPrompts
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.TextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.arrowKeysLeaveField
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.rememberTextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.textActionsMenu
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private val MemberLimitOptions = listOf<Int?>(null, 2, 3, 4, 6, 8, 10, 15, 20)
private val SlowModeOptions = listOf(0, 5, 10, 30, 60)

/**
 * The Watch party page: create a room, or join one by scanning or pasting its invite. In a room
 * it shows how guests reach it, its invite, who is in it, and its settings, which only the host
 * edits. Opening and connecting are dialogs, shown app-wide with the room's other messages.
 */
internal fun LazyListScope.watchPartySettingsContent(
    isTablet: Boolean,
    onBack: (() -> Unit)?,
    onOpenRoomSettings: () -> Unit,
) {
    item(key = "watch-party") {
        val state by PartyRepository.uiState.collectAsStateWithLifecycle()
        when (val phase = state.phase) {
            // In a room the page is its conversation, filling the screen as in Google Messages.
            is PartyPhase.InRoom -> WatchPartyConversation(
                room = phase.room,
                onBack = onBack,
                onOpenRoomSettings = onOpenRoomSettings,
                modifier = Modifier.fillParentMaxSize(),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                WatchPartyStart(isTablet = isTablet)
            }
        }
    }
}

/** The room's settings, opened from the top of its conversation: how guests reach it, who is in it, and its rules. */
internal fun LazyListScope.watchPartyRoomSettingsContent(isTablet: Boolean, onRoomClosed: () -> Unit) {
    item(key = "watch-party-room") {
        val state by PartyRepository.uiState.collectAsStateWithLifecycle()
        val room = (state.phase as? PartyPhase.InRoom)?.room
        // Out of the room there is nothing left to set; back to the start.
        LaunchedEffect(room == null) {
            if (room == null) onRoomClosed()
        }
        if (room != null) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                WatchPartyRoomContent(room = room, isTablet = isTablet)
            }
        }
    }
}

@Composable
private fun WatchPartyStart(isTablet: Boolean) {
    var showEntry by rememberSaveable { mutableStateOf(false) }
    var showHostWarning by rememberSaveable { mutableStateOf(false) }
    val scan = rememberLocalSyncScanner { result -> result?.let(WatchPartyPrompts::requestJoin) }
    SettingsSection(
        title = stringResource(Res.string.watch_party_title),
        isTablet = isTablet,
    ) {
        SettingsList {
            navigationRow(
                title = stringResource(Res.string.watch_party_create),
                description = stringResource(Res.string.watch_party_create_description),
                icon = Icons.Rounded.PersonAdd,
                onClick = { showHostWarning = true },
            )
            if (scan != null) {
                navigationRow(
                    title = stringResource(Res.string.watch_party_scan),
                    description = stringResource(Res.string.watch_party_scan_description),
                    icon = Icons.Rounded.QrCodeScanner,
                    onClick = scan,
                )
            }
            navigationRow(
                title = stringResource(Res.string.watch_party_enter),
                description = stringResource(Res.string.watch_party_enter_description),
                icon = Icons.Rounded.Keyboard,
                onClick = { showEntry = true },
            )
        }
    }
    if (showEntry) {
        WatchPartyJoinDialog(
            onJoin = { input ->
                showEntry = false
                WatchPartyPrompts.requestJoin(input)
            },
            onDismiss = { showEntry = false },
        )
    }
    if (showHostWarning) {
        WatchPartyRiskDialog(
            title = stringResource(Res.string.watch_party_host_warning_title),
            text = stringResource(Res.string.watch_party_host_warning),
            confirm = stringResource(Res.string.watch_party_create),
            onConfirm = {
                showHostWarning = false
                PartyRepository.createRoom()
            },
            onDismiss = { showHostWarning = false },
        )
    }
}

@Composable
private fun WatchPartyRoomContent(room: PartyRoom, isTablet: Boolean) {
    var showInvite by rememberSaveable { mutableStateOf(false) }
    var showLeave by rememberSaveable { mutableStateOf(false) }
    var showMemberLimit by rememberSaveable { mutableStateOf(false) }
    var showSlowMode by rememberSaveable { mutableStateOf(false) }
    var memberToRemoveId by rememberSaveable { mutableStateOf<String?>(null) }
    val guestName = stringResource(Res.string.watch_party_guest_name)
    val hostLabel = stringResource(Res.string.watch_party_member_host)
    val youLabel = stringResource(Res.string.watch_party_member_you)
    val settings = room.settings

    SettingsSection(
        title = stringResource(Res.string.watch_party_section_room),
        isTablet = isTablet,
    ) {
        SettingsList {
            if (room.isHost) {
                infoRow(
                    title = stringResource(room.reachability.titleRes()),
                    description = stringResource(room.reachability.descriptionRes()),
                )
                navigationRow(
                    title = stringResource(Res.string.watch_party_show_invite),
                    description = stringResource(Res.string.watch_party_show_invite_description),
                    icon = Icons.Rounded.QrCode,
                    enabled = room.inviteLink != null,
                    onClick = { showInvite = true },
                )
            } else {
                infoRow(
                    title = stringResource(Res.string.watch_party_hosted_by, room.hostName.orEmpty().ifBlank { guestName }),
                    description = stringResource(Res.string.watch_party_guest_description),
                )
            }
        }
    }

    SettingsSection(
        title = settings.memberLimit?.let { limit ->
            stringResource(Res.string.watch_party_section_members_limited, room.members.size, limit)
        } ?: stringResource(Res.string.watch_party_section_members, room.members.size),
        isTablet = isTablet,
    ) {
        SettingsList {
            room.members.forEach { member ->
                val name = member.name.ifBlank { guestName }
                val description = listOfNotNull(
                    youLabel.takeIf { member.id == room.myMemberId },
                    hostLabel.takeIf { member.isHost },
                ).joinToString(" · ").ifBlank { null }
                if (room.isHost && !member.isHost) {
                    navigationRow(
                        title = name,
                        description = description,
                        icon = Icons.Rounded.Person,
                        onClick = { memberToRemoveId = member.id },
                    )
                } else {
                    infoRow(title = name, description = description)
                }
            }
        }
    }

    SettingsSection(
        title = stringResource(Res.string.watch_party_section_settings),
        isTablet = isTablet,
    ) {
        val editable = room.isHost
        SettingsList {
            navigationRow(
                title = stringResource(Res.string.watch_party_member_limit),
                description = memberLimitLabel(settings.memberLimit),
                enabled = editable,
                onClick = { showMemberLimit = true },
            )
            switchRow(
                title = stringResource(Res.string.watch_party_chat),
                description = stringResource(Res.string.watch_party_chat_description),
                checked = { settings.chatEnabled },
                enabled = editable,
                onCheckedChange = { enabled -> PartyRepository.updateSettings { it.copy(chatEnabled = enabled) } },
            )
            navigationRow(
                title = stringResource(Res.string.watch_party_slow_mode),
                description = slowModeLabel(settings.slowModeSeconds),
                enabled = editable && settings.chatEnabled,
                onClick = { showSlowMode = true },
            )
            switchRow(
                title = stringResource(Res.string.watch_party_suggestions),
                description = stringResource(Res.string.watch_party_suggestions_description),
                checked = { settings.suggestionsEnabled },
                enabled = editable,
                onCheckedChange = { enabled -> PartyRepository.updateSettings { it.copy(suggestionsEnabled = enabled) } },
            )
        }
    }

    if (room.isHost && room.banned.isNotEmpty()) {
        val unbanDescription = stringResource(Res.string.watch_party_unban_description)
        SettingsSection(
            title = stringResource(Res.string.watch_party_section_banned),
            isTablet = isTablet,
        ) {
            SettingsList {
                room.banned.forEach { member ->
                    navigationRow(
                        title = member.name.ifBlank { guestName },
                        description = unbanDescription,
                        icon = Icons.Rounded.PersonOff,
                        onClick = { PartyRepository.unban(member.id) },
                    )
                }
            }
        }
    }

    Button(
        onClick = { showLeave = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
        Text(
            text = stringResource(if (room.isHost) Res.string.watch_party_close else Res.string.watch_party_leave),
            modifier = Modifier.padding(start = 8.dp),
        )
    }

    if (showInvite) {
        room.inviteLink?.let { link ->
            WatchPartyInviteSheet(link = link, qr = room.inviteQr, onDismiss = { showInvite = false })
        }
    }

    if (showLeave) {
        WatchPartyLeaveDialog(
            isHost = room.isHost,
            onConfirm = {
                showLeave = false
                PartyRepository.leave()
            },
            onDismiss = { showLeave = false },
        )
    }

    room.members.firstOrNull { it.id == memberToRemoveId && room.isHost }?.let { member ->
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

    room.firewall?.let { prompt -> WatchPartyFirewallDialog(prompt) }

    if (showMemberLimit && room.isHost) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.watch_party_member_limit),
            options = MemberLimitOptions,
            selected = settings.memberLimit,
            label = { memberLimitLabel(it) },
            onSelect = { limit ->
                showMemberLimit = false
                PartyRepository.updateSettings { it.copy(memberLimit = limit) }
            },
            onDismiss = { showMemberLimit = false },
        )
    }

    if (showSlowMode && room.isHost) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.watch_party_slow_mode),
            options = SlowModeOptions,
            selected = settings.slowModeSeconds,
            label = { slowModeLabel(it) },
            onSelect = { seconds ->
                showSlowMode = false
                PartyRepository.updateSettings { it.copy(slowModeSeconds = seconds) }
            },
            onDismiss = { showSlowMode = false },
        )
    }
}

private fun PartyReachability.titleRes(): StringResource = when (this) {
    PartyReachability.CHECKING -> Res.string.watch_party_reach_checking
    PartyReachability.OPEN -> Res.string.watch_party_reach_open
    PartyReachability.SHARED_ADDRESS -> Res.string.watch_party_reach_shared
    PartyReachability.IPV6_ONLY -> Res.string.watch_party_reach_ipv6
    PartyReachability.LOCAL_ONLY -> Res.string.watch_party_reach_local
}

private fun PartyReachability.descriptionRes(): StringResource = when (this) {
    PartyReachability.CHECKING -> Res.string.watch_party_reach_checking_description
    PartyReachability.OPEN -> Res.string.watch_party_reach_open_description
    PartyReachability.SHARED_ADDRESS -> Res.string.watch_party_reach_shared_description
    PartyReachability.IPV6_ONLY -> Res.string.watch_party_reach_ipv6_description
    PartyReachability.LOCAL_ONLY -> Res.string.watch_party_reach_local_description
}

@Composable
private fun memberLimitLabel(limit: Int?): String =
    limit?.let { stringResource(Res.string.watch_party_member_limit_value, it) }
        ?: stringResource(Res.string.watch_party_member_limit_unlimited)

@Composable
private fun slowModeLabel(seconds: Int): String =
    if (seconds > 0) {
        stringResource(Res.string.watch_party_slow_mode_value, seconds)
    } else {
        stringResource(Res.string.watch_party_slow_mode_off)
    }

/** The QR code and the link, with the one way to copy it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchPartyInviteSheet(
    link: String,
    qr: List<BooleanArray>?,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val linkCopied = stringResource(Res.string.watch_party_link_copied)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss) } }
    ModalSheet(onDismissRequest = close, sheetState = sheetState) {
        SheetHeader(
            title = stringResource(Res.string.watch_party_invite_title),
            navigation = SheetNavigation.Close,
            onNavigate = close,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (qr != null) LocalSyncQrCode(matrix = qr)
            Text(
                text = link,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(link))
                    ToastController.show(linkCopied)
                },
            ) {
                Text(stringResource(Res.string.watch_party_copy_link))
            }
        }
    }
}

@Composable
private fun WatchPartyJoinDialog(
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.watch_party_enter)) },
        text = {
            val textMenu = rememberTextActionsMenu()
            TextActionsMenu(textMenu, text = input) { pasted -> input += pasted }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(Res.string.watch_party_input_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().arrowKeysLeaveField { input.isEmpty() }.textActionsMenu(textMenu),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(input) },
                enabled = input.isNotBlank(),
            ) {
                Text(stringResource(Res.string.watch_party_join))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

// What hosting or joining means, acknowledged before it happens.
@Composable
internal fun WatchPartyRiskDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Rounded.Public, contentDescription = null) },
        title = { Text(title) },
        text = {
            Text(
                text = text,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun WatchPartyLeaveDialog(
    isHost: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isHost) Res.string.watch_party_close_confirm_title else Res.string.watch_party_leave_confirm_title))
        },
        text = {
            Text(
                stringResource(
                    if (isHost) Res.string.watch_party_close_confirm_description else Res.string.watch_party_leave_confirm_description,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(if (isHost) Res.string.watch_party_close else Res.string.watch_party_leave))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun WatchPartyMemberDialog(
    name: String,
    onKick: () -> Unit,
    onBan: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = { Text(stringResource(Res.string.watch_party_member_dialog_description)) },
        confirmButton = {
            Row {
                TextButton(
                    onClick = onBan,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(Res.string.watch_party_ban))
                }
                TextButton(onClick = onKick) {
                    Text(stringResource(Res.string.watch_party_kick))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

// A Linux firewall that blocks the room's port, with a way to allow it or the command to do so.
@Composable
private fun WatchPartyFirewallDialog(prompt: LocalSyncFirewallPrompt) {
    val clipboardManager = LocalClipboardManager.current
    val command = "sudo firewall-cmd --permanent --add-port=${prompt.tcpPort}/tcp && sudo firewall-cmd --reload"
    AlertDialog(
        onDismissRequest = PartyRepository::dismissFirewallPrompt,
        title = { Text(stringResource(Res.string.watch_party_firewall_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.watch_party_firewall_text))
                Surface(
                    onClick = { clipboardManager.setText(AnnotatedString(command)) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = command,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(12.dp),
                    )
                }
                if (prompt.allowFailed) {
                    Text(
                        text = stringResource(Res.string.local_sync_firewall_allow_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = PartyRepository::allowThroughFirewall,
                enabled = !prompt.allowing,
            ) {
                Text(stringResource(Res.string.local_sync_firewall_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = PartyRepository::dismissFirewallPrompt) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}
