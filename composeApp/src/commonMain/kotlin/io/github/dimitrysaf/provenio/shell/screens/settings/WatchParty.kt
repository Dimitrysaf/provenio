package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.dimitrysaf.provenio.core.party.PartyError
import io.github.dimitrysaf.provenio.core.party.PartyPhase
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.PartyRoom
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.TextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import io.github.dimitrysaf.provenio.shell.components.arrowKeysLeaveField
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.rememberTextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.textActionsMenu
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private val MemberLimitOptions = listOf<Int?>(null, 2, 3, 4, 6, 8, 10, 15, 20)
private val SlowModeOptions = listOf(0, 5, 10, 30, 60)

/**
 * The Watch party page: create a room, or join one by scanning or typing its link or code. In a
 * room it shows the code and invite, who is in it, and its settings, which only the host edits.
 */
internal fun LazyListScope.watchPartySettingsContent(isTablet: Boolean) {
    item(key = "watch-party") {
        WatchPartyPage(isTablet = isTablet)
    }
}

@Composable
private fun WatchPartyPage(isTablet: Boolean) {
    val state by PartyRepository.uiState.collectAsStateWithLifecycle()
    WatchPartyNoticeEffect(state.notice)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        when (val phase = state.phase) {
            PartyPhase.Idle -> WatchPartyStart(isTablet = isTablet)
            is PartyPhase.InRoom -> WatchPartyRoomContent(room = phase.room, isTablet = isTablet)
            else -> WatchPartyProgress(phase = phase, isTablet = isTablet)
        }
    }
}

// Why a join failed or a room ended is said once, as a snackbar.
@Composable
private fun WatchPartyNoticeEffect(notice: PartyError?) {
    LaunchedEffect(notice) {
        notice ?: return@LaunchedEffect
        ToastController.show(getString(notice.messageRes()))
        PartyRepository.clearNotice()
    }
}

@Composable
private fun WatchPartyStart(isTablet: Boolean) {
    var showEntry by rememberSaveable { mutableStateOf(false) }
    val scan = rememberLocalSyncScanner { result -> result?.let(PartyRepository::join) }
    SettingsSection(
        title = stringResource(Res.string.watch_party_section_start),
        isTablet = isTablet,
    ) {
        SettingsList {
            navigationRow(
                title = stringResource(Res.string.watch_party_create),
                description = stringResource(Res.string.watch_party_create_description),
                icon = Icons.Rounded.PersonAdd,
                onClick = PartyRepository::createRoom,
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
            infoRow(
                title = stringResource(Res.string.watch_party_how_title),
                description = stringResource(Res.string.watch_party_how_description),
            )
        }
    }
    if (showEntry) {
        WatchPartyJoinDialog(
            onJoin = { input ->
                showEntry = false
                PartyRepository.join(input)
            },
            onDismiss = { showEntry = false },
        )
    }
}

// Opening, finding, connecting or waiting at the door, with a way to stop.
@Composable
private fun WatchPartyProgress(phase: PartyPhase, isTablet: Boolean) {
    val status = stringResource(
        when (phase) {
            PartyPhase.Creating -> Res.string.watch_party_status_creating
            PartyPhase.Finding -> Res.string.watch_party_status_finding
            PartyPhase.AwaitingApproval -> Res.string.watch_party_status_waiting
            else -> Res.string.watch_party_status_connecting
        },
    )
    SettingsSection(
        title = stringResource(Res.string.watch_party_title),
        isTablet = isTablet,
    ) {
        SettingsList {
            shapedRow { shape -> WatchPartyStatusRow(shape = shape, text = status) }
            navigationRow(
                title = stringResource(Res.string.action_cancel),
                icon = Icons.Rounded.Close,
                onClick = PartyRepository::leave,
            )
        }
    }
}

@Composable
private fun WatchPartyStatusRow(shape: RoundedCornerShape, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocalSyncProgress()
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WatchPartyRoomContent(room: PartyRoom, isTablet: Boolean) {
    var showInvite by rememberSaveable { mutableStateOf(false) }
    var showLeave by rememberSaveable { mutableStateOf(false) }
    var showMemberLimit by rememberSaveable { mutableStateOf(false) }
    var showSlowMode by rememberSaveable { mutableStateOf(false) }
    var memberToRemoveId by rememberSaveable { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val linkCopied = stringResource(Res.string.watch_party_link_copied)
    val guestName = stringResource(Res.string.watch_party_guest_name)
    val hostLabel = stringResource(Res.string.watch_party_member_host)
    val youLabel = stringResource(Res.string.watch_party_member_you)
    val settings = room.settings

    SettingsSection(
        title = stringResource(Res.string.watch_party_section_room),
        isTablet = isTablet,
    ) {
        SettingsList {
            infoRow(
                title = stringResource(Res.string.watch_party_code),
                description = stringResource(Res.string.watch_party_code_description),
                value = room.code.chunked(3).joinToString(" "),
            )
            room.inviteLink?.let { link ->
                navigationRow(
                    title = stringResource(Res.string.watch_party_show_invite),
                    description = stringResource(Res.string.watch_party_show_invite_description),
                    icon = Icons.Rounded.QrCode,
                    onClick = { showInvite = true },
                )
                navigationRow(
                    title = stringResource(Res.string.watch_party_copy_link),
                    icon = Icons.Rounded.ContentCopy,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(link))
                        ToastController.show(linkCopied)
                    },
                )
            }
        }
    }

    if (room.isHost && room.joinRequests.isNotEmpty()) {
        val approveLabel = stringResource(Res.string.watch_party_approve)
        val denyLabel = stringResource(Res.string.watch_party_deny)
        val requestDescription = stringResource(Res.string.watch_party_request_description)
        SettingsSection(
            title = stringResource(Res.string.watch_party_section_requests),
            isTablet = isTablet,
        ) {
            SettingsList {
                room.joinRequests.forEach { request ->
                    navigationRow(
                        title = request.name.ifBlank { guestName },
                        description = requestDescription,
                        icon = Icons.Rounded.Person,
                        trailingContent = {
                            Row {
                                WithTooltip(denyLabel) {
                                    IconButton(onClick = { PartyRepository.deny(request.memberId) }) {
                                        Icon(imageVector = Icons.Rounded.Close, contentDescription = denyLabel)
                                    }
                                }
                                WithTooltip(approveLabel) {
                                    IconButton(
                                        onClick = { PartyRepository.approve(request.memberId) },
                                        enabled = !room.isFull,
                                    ) {
                                        Icon(imageVector = Icons.Rounded.Check, contentDescription = approveLabel)
                                    }
                                }
                            }
                        },
                        onClick = { if (!room.isFull) PartyRepository.approve(request.memberId) },
                    )
                }
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

    WatchPartySettingsSection(
        room = room,
        isTablet = isTablet,
        onMemberLimitClick = { showMemberLimit = true },
        onSlowModeClick = { showSlowMode = true },
    )

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

    SettingsList {
        navigationRow(
            title = stringResource(if (room.isHost) Res.string.watch_party_close else Res.string.watch_party_leave),
            icon = Icons.Rounded.Close,
            onClick = { showLeave = true },
        )
    }

    if (showInvite) {
        room.inviteLink?.let { link ->
            WatchPartyInviteSheet(
                link = link,
                code = room.code,
                qr = room.inviteQr,
                onDismiss = { showInvite = false },
            )
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

// The same rows for everyone; only the host's can be changed.
@Composable
private fun WatchPartySettingsSection(
    room: PartyRoom,
    isTablet: Boolean,
    onMemberLimitClick: () -> Unit,
    onSlowModeClick: () -> Unit,
) {
    val settings = room.settings
    val editable = room.isHost
    SettingsSection(
        title = stringResource(Res.string.watch_party_section_settings),
        isTablet = isTablet,
    ) {
        SettingsList {
            navigationRow(
                title = stringResource(Res.string.watch_party_member_limit),
                description = memberLimitLabel(settings.memberLimit),
                enabled = editable,
                onClick = onMemberLimitClick,
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
                onClick = onSlowModeClick,
            )
            switchRow(
                title = stringResource(Res.string.watch_party_suggestions),
                description = stringResource(Res.string.watch_party_suggestions_description),
                checked = { settings.suggestionsEnabled },
                enabled = editable,
                onCheckedChange = { enabled -> PartyRepository.updateSettings { it.copy(suggestionsEnabled = enabled) } },
            )
            if (!editable) {
                infoRow(title = stringResource(Res.string.watch_party_settings_read_only))
            }
        }
    }
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

/** The QR code and link for guests, and the code for those on the same Wi-Fi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchPartyInviteSheet(
    link: String,
    code: String,
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
                text = code.chunked(3).joinToString(" "),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface,
            )
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

private fun PartyError.messageRes(): StringResource = when (this) {
    PartyError.NOT_SUPPORTED -> Res.string.watch_party_error_not_supported
    PartyError.NO_NETWORK -> Res.string.watch_party_error_no_network
    PartyError.INVALID_LINK -> Res.string.watch_party_error_invalid_link
    PartyError.NO_ROOM_FOR_CODE -> Res.string.watch_party_error_no_room
    PartyError.UNREACHABLE -> Res.string.watch_party_error_unreachable
    PartyError.REJECTED -> Res.string.watch_party_error_rejected
    PartyError.ROOM_FULL -> Res.string.watch_party_error_full
    PartyError.BANNED -> Res.string.watch_party_error_banned
    PartyError.DENIED -> Res.string.watch_party_error_denied
    PartyError.KICKED -> Res.string.watch_party_error_kicked
    PartyError.CLOSED -> Res.string.watch_party_error_closed
    PartyError.CONNECTION_LOST -> Res.string.watch_party_error_lost
    PartyError.VERSION_MISMATCH -> Res.string.watch_party_error_version
    PartyError.FAILED -> Res.string.watch_party_error_failed
}
