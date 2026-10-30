package io.github.dimitrysaf.provenio.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.party.PartyActivity
import io.github.dimitrysaf.provenio.core.party.PartyActivityKind
import io.github.dimitrysaf.provenio.core.party.PartyError
import io.github.dimitrysaf.provenio.core.party.PartyPhase
import io.github.dimitrysaf.provenio.core.party.PartyPlayback
import io.github.dimitrysaf.provenio.core.party.PartyRepository
import io.github.dimitrysaf.provenio.core.party.toPlayerLaunch
import io.github.dimitrysaf.provenio.core.playback.PlayerLaunchStore
import io.github.dimitrysaf.provenio.shell.components.LoadingSpinner
import io.github.dimitrysaf.provenio.shell.nav.AppRoute
import io.github.dimitrysaf.provenio.shell.nav.DetailRoute
import io.github.dimitrysaf.provenio.shell.nav.Navigator
import io.github.dimitrysaf.provenio.shell.nav.PlayerRoute
import io.github.dimitrysaf.provenio.shell.nav.StreamRoute
import io.github.dimitrysaf.provenio.shell.screens.settings.WatchPartyRiskDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** Questions the room needs answered wherever the app is, such as leaving it to pick a source alone. */
internal object WatchPartyPrompts {
    private val _leaveRequest = MutableStateFlow<(() -> Unit)?>(null)
    val leaveRequest: StateFlow<(() -> Unit)?> = _leaveRequest.asStateFlow()

    /** Asks the guest to leave the room first; [then] runs once they have. */
    fun askToLeave(then: () -> Unit) {
        _leaveRequest.value = then
    }

    fun dismiss() {
        _leaveRequest.value = null
    }

    private val _joinRequest = MutableStateFlow<String?>(null)
    val joinRequest: StateFlow<String?> = _joinRequest.asStateFlow()

    /** Shows what joining means before joining with [link], wherever the link came from. */
    fun requestJoin(link: String) {
        _joinRequest.value = link
    }

    fun dismissJoin() {
        _joinRequest.value = null
    }

    private val _conversationShown = MutableStateFlow(false)
    val conversationShown: StateFlow<Boolean> = _conversationShown.asStateFlow()

    /** The room's conversation is on screen, with the room at its top. */
    fun setConversationShown(shown: Boolean) {
        _conversationShown.value = shown
    }
}

/** The height of the room's line at the top, under the status bar; the page below moves down by it. */
internal val WatchPartyLineHeight = 40.dp

/** What the room's line at the top of the app says, set by [WatchPartyAppHost]; null text hides it. */
@Stable
internal class WatchPartyLine {
    var text by mutableStateOf<String?>(null)
    var action by mutableStateOf<String?>(null)
    var onClick by mutableStateOf<() -> Unit>({})
}

/** The watch party wherever the app is: the line at the top, its dialogs, suggestions, and a guest following the host. */
@Composable
internal fun WatchPartyAppHost(
    line: WatchPartyLine,
    navController: Navigator,
    playback: AppPlayback,
    profileId: Int,
    currentRoute: AppRoute?,
    enabled: Boolean,
    onOpenWatchParty: () -> Unit,
) {
    if (!PartyRepository.isSupported) return
    val state by PartyRepository.uiState.collectAsStateWithLifecycle()
    val room = (state.phase as? PartyPhase.InRoom)?.room
    val activity by PartyRepository.activity.collectAsStateWithLifecycle()
    val sharedPlayback by PartyRepository.playback.collectAsStateWithLifecycle()
    val leaveRequest by WatchPartyPrompts.leaveRequest.collectAsStateWithLifecycle()
    val joinRequest by WatchPartyPrompts.joinRequest.collectAsStateWithLifecycle()
    val conversationShown by WatchPartyPrompts.conversationShown.collectAsStateWithLifecycle()
    val isHost = room?.isHost == true
    val isGuest = room?.isHost == false
    var openedPlaybackId by remember { mutableStateOf<Long?>(null) }
    // What the guest wanted to do alone, done once they are out of the room.
    var afterLeaving by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(state.phase, afterLeaving) {
        val then = afterLeaving ?: return@LaunchedEffect
        if (state.phase == PartyPhase.Idle) {
            afterLeaving = null
            then()
        }
    }
    var partyPlayerRoute by remember { mutableStateOf<PlayerRoute?>(null) }

    fun openShared(shared: PartyPlayback) {
        val positionMs = PartyRepository.playbackState.value?.takeIf { it.playbackId == shared.id }?.positionMs ?: 0L
        val route = PlayerRoute(
            launchId = PlayerLaunchStore.put(shared.toPlayerLaunch(profileId, positionMs)),
            title = shared.title,
        )
        DetachedPlayer.closeAny()
        navController.navigate(route) { popUpTo<PlayerRoute> { inclusive = true } }
        openedPlaybackId = shared.id
        partyPlayerRoute = route
    }

    // The host tells the room what it is looking at.
    LaunchedEffect(enabled, isHost, currentRoute) {
        if (enabled && isHost) PartyRepository.setActivity(currentRoute.partyActivity())
    }

    // A guest opens what the host opens, and leaves the player when the host does.
    LaunchedEffect(enabled, isGuest, sharedPlayback?.id) {
        if (!enabled || !isGuest) return@LaunchedEffect
        val shared = sharedPlayback
        if (shared == null) {
            partyPlayerRoute?.let { route -> navController.popBackStack(expectedRoute = route) }
            partyPlayerRoute = null
            openedPlaybackId = null
            return@LaunchedEffect
        }
        if (openedPlaybackId != shared.id) openShared(shared)
    }

    if (room != null && currentRoute !is PlayerRoute && !conversationShown) {
        val hostName = room.hostName.orEmpty()
        val shared = sharedPlayback
        val (text, action) = when {
            // Everyone streams the source the host picks, so the best seeded one serves them best.
            isHost && currentRoute is StreamRoute -> stringResource(Res.string.watch_party_line_source_advice) to null
            isHost -> stringResource(Res.string.watch_party_line_hosting, room.members.size) to null
            activity.kind == PartyActivityKind.WATCHING && shared != null -> stringResource(
                Res.string.watch_party_line_watching,
                hostName,
                activity.title?.takeIf { it.isNotBlank() } ?: shared.title,
            ) to stringResource(Res.string.watch_party_line_join)
            activity.kind == PartyActivityKind.CHOOSING -> stringResource(
                Res.string.watch_party_line_choosing,
                hostName,
                activity.title.orEmpty(),
            ) to null
            activity.kind == PartyActivityKind.BROWSING -> stringResource(
                Res.string.watch_party_line_browsing,
                hostName,
                activity.title.orEmpty(),
            ) to null
            else -> stringResource(Res.string.watch_party_line_connected, hostName) to null
        }
        SideEffect {
            line.text = text
            line.action = action
            line.onClick = { if (action != null && shared != null) openShared(shared) else onOpenWatchParty() }
        }
    } else {
        SideEffect { line.text = null }
    }

    when (state.phase) {
        PartyPhase.Creating -> WatchPartyProgressDialog(Res.string.watch_party_status_creating)
        PartyPhase.Connecting -> WatchPartyProgressDialog(Res.string.watch_party_status_connecting)
        else -> Unit
    }

    state.notice?.let { notice -> WatchPartyNoticeDialog(notice) }

    // A title the host said Yes to in the chat plays as anything does: by the remembered source, the auto-play rules, or Sources.
    LaunchedEffect(enabled, isHost) {
        if (!enabled || !isHost) return@LaunchedEffect
        PartyRepository.acceptedSuggestions.collect { suggestion ->
            playback.play(
                type = suggestion.type,
                videoId = suggestion.videoId,
                parentMetaId = suggestion.parentMetaId,
                parentMetaType = suggestion.parentMetaType,
                title = suggestion.title,
                logo = suggestion.logo,
                poster = suggestion.poster,
                background = suggestion.background,
                seasonNumber = suggestion.seasonNumber,
                episodeNumber = suggestion.episodeNumber,
                episodeTitle = suggestion.episodeTitle,
                episodeThumbnail = suggestion.episodeThumbnail,
                pauseDescription = null,
                resumePositionMs = null,
                resumeProgressFraction = null,
                manualSelection = false,
                startFromBeginning = false,
            )
        }
    }

    joinRequest?.let { link ->
        WatchPartyRiskDialog(
            title = stringResource(Res.string.watch_party_join_warning_title),
            text = stringResource(Res.string.watch_party_join_warning),
            confirm = stringResource(Res.string.watch_party_join),
            onConfirm = {
                WatchPartyPrompts.dismissJoin()
                PartyRepository.join(link)
            },
            onDismiss = WatchPartyPrompts::dismissJoin,
        )
    }

    leaveRequest?.let { then ->
        AlertDialog(
            onDismissRequest = WatchPartyPrompts::dismiss,
            title = { Text(stringResource(Res.string.watch_party_leave_to_choose_title)) },
            text = { Text(stringResource(Res.string.watch_party_leave_to_choose)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        WatchPartyPrompts.dismiss()
                        afterLeaving = then
                        PartyRepository.leave()
                    },
                ) {
                    Text(stringResource(Res.string.watch_party_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = WatchPartyPrompts::dismiss) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

private fun AppRoute?.partyActivity(): PartyActivity = when (this) {
    is PlayerRoute -> PartyActivity(PartyActivityKind.WATCHING, title)
    is StreamRoute -> PartyActivity(PartyActivityKind.CHOOSING, title)
    is DetailRoute -> PartyActivity(PartyActivityKind.BROWSING, title)
    else -> PartyActivity()
}

/** A line from edge to edge at the top, under the status bar, with no icons; tapping it does its action. */
@Composable
internal fun WatchPartyLineBar(
    text: String?,
    action: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The last words stay while the line folds away.
    var shownText by remember { mutableStateOf(text.orEmpty()) }
    var shownAction by remember { mutableStateOf(action) }
    if (text != null) {
        shownText = text
        shownAction = action
    }
    AnimatedVisibility(
        visible = text != null,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier.fillMaxWidth(),
    ) {
        // Clickable on its own, so the line keeps its height instead of the 48dp a clickable Surface takes.
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .height(WatchPartyLineHeight)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = shownText,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                shownAction?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Opening a room or reaching one takes a moment, said in a dialog with a way to stop.
@Composable
private fun WatchPartyProgressDialog(status: StringResource) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LoadingSpinner(size = 72.dp, active = true)
                Text(
                    text = stringResource(status),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = PartyRepository::leave) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Composable
private fun WatchPartyNoticeDialog(notice: PartyError) {
    AlertDialog(
        onDismissRequest = PartyRepository::clearNotice,
        title = { Text(stringResource(notice.titleRes())) },
        text = { Text(stringResource(notice.messageRes())) },
        confirmButton = {
            TextButton(onClick = PartyRepository::clearNotice) {
                Text(stringResource(Res.string.action_close))
            }
        },
        // A room that couldn't open points to help with forwarding the port; the wiki page isn't written yet.
        dismissButton = if (notice == PartyError.PORT_CLOSED || notice == PartyError.SHARED_ADDRESS) {
            {
                TextButton(onClick = {}) {
                    Text(stringResource(Res.string.watch_party_error_help))
                }
            }
        } else {
            null
        },
    )
}

private fun PartyError.titleRes(): StringResource = when (this) {
    PartyError.CONNECTION_LOST -> Res.string.watch_party_ended_lost_title
    PartyError.CLOSED -> Res.string.watch_party_ended_closed_title
    PartyError.KICKED, PartyError.BANNED -> Res.string.watch_party_ended_removed_title
    PartyError.PORT_CLOSED, PartyError.SHARED_ADDRESS -> Res.string.watch_party_error_open_title
    else -> Res.string.watch_party_title
}

internal fun PartyError.messageRes(): StringResource = when (this) {
    PartyError.NOT_SUPPORTED -> Res.string.watch_party_error_not_supported
    PartyError.NO_NETWORK -> Res.string.watch_party_error_no_network
    PartyError.PORT_CLOSED -> Res.string.watch_party_error_port_closed
    PartyError.SHARED_ADDRESS -> Res.string.watch_party_error_shared_address
    PartyError.INVALID_LINK -> Res.string.watch_party_error_invalid_link
    PartyError.UNREACHABLE -> Res.string.watch_party_error_unreachable
    PartyError.REJECTED -> Res.string.watch_party_error_rejected
    PartyError.ROOM_FULL -> Res.string.watch_party_error_full
    PartyError.BANNED -> Res.string.watch_party_error_banned
    PartyError.KICKED -> Res.string.watch_party_error_kicked
    PartyError.CLOSED -> Res.string.watch_party_error_closed
    PartyError.CONNECTION_LOST -> Res.string.watch_party_error_lost
    PartyError.VERSION_MISMATCH -> Res.string.watch_party_error_version
    PartyError.FAILED -> Res.string.watch_party_error_failed
}
