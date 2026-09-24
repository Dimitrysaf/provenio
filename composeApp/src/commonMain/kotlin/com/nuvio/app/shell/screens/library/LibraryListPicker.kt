package com.nuvio.app.shell.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nuvio.app.core.library.LibraryItem
import com.nuvio.app.core.library.LibraryRepository
import com.nuvio.app.core.tracking.TrackingLibraryTab
import com.nuvio.app.core.tracking.TrackingMembershipApplyResult
import com.nuvio.app.core.tracking.TrackingProviderId
import com.nuvio.app.core.tracking.toggleTrackingLibraryMembership
import com.nuvio.app.shell.components.TrackingListPickerSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tracking_lists_update_failed
import nuvio.composeapp.generated.resources.trakt_lists_load_failed
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// The list picker sheet for one library item, and the membership changes made from it.
@Stable
internal class LibraryListPickerState(private val scope: CoroutineScope) {
    var visible by mutableStateOf(false)
        private set
    var title by mutableStateOf("")
        private set
    var tabs by mutableStateOf<List<TrackingLibraryTab>>(emptyList())
        private set
    var membership by mutableStateOf<Map<String, Boolean>>(emptyMap())
        private set
    var isPending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var item: LibraryItem? = null

    fun open(item: LibraryItem, title: String) {
        this.item = item
        this.title = title
        tabs = LibraryRepository.libraryListTabs(item)
        membership = tabs.associate { it.key to false }
        isPending = true
        error = null
        visible = true
        scope.launch {
            runCatching {
                val snapshot = LibraryRepository.getMembershipSnapshot(item)
                val loadedTabs = LibraryRepository.libraryListTabs(item)
                tabs = loadedTabs
                membership = loadedTabs.associate { tab -> tab.key to (snapshot[tab.key] == true) }
            }.onFailure { failure ->
                error = failure.message ?: getString(Res.string.trakt_lists_load_failed)
            }
            isPending = false
        }
    }

    fun toggle(
        key: String,
        failedMessage: String,
        onRemovalNeedsConfirmation: (PendingTrackingMembershipRemoval) -> Unit,
    ) {
        val item = item ?: return
        val previousMembership = membership
        membership = toggleTrackingLibraryMembership(tabs = tabs, membership = membership, key = key)
        scope.launch {
            isPending = true
            error = null
            val desiredMembership = membership.toMap()
            val applyMembership: suspend (Set<TrackingProviderId>) -> TrackingMembershipApplyResult =
                { confirmedProviders ->
                    LibraryRepository.applyMembershipChanges(
                        item = item,
                        desiredMembership = desiredMembership,
                        confirmedRemovalProviders = confirmedProviders,
                    )
                }
            val revertMembership: suspend (Throwable) -> Unit = { failure ->
                membership = previousMembership
                error = failure.message ?: failedMessage
            }
            executeTrackingMembershipOperation(
                operation = { applyMembership(emptySet()) },
                onSuccess = { result ->
                    if (result.requiresRemovalConfirmation) {
                        onRemovalNeedsConfirmation(
                            PendingTrackingMembershipRemoval(
                                itemTitle = item.name,
                                confirmations = result.requiredRemovalConfirmations,
                                retry = applyMembership,
                                onApplied = { applied -> showTrackingMembershipRewriteFeedback(applied) },
                                onFailure = revertMembership,
                                onCancelled = { membership = previousMembership },
                            ),
                        )
                    } else {
                        showTrackingMembershipRewriteFeedback(result)
                    }
                },
                onFailure = revertMembership,
            )
            isPending = false
        }
    }

    fun dismiss() {
        visible = false
        item = null
        error = null
    }
}

@Composable
internal fun rememberLibraryListPickerState(): LibraryListPickerState {
    val scope = rememberCoroutineScope()
    return remember(scope) { LibraryListPickerState(scope) }
}

// Shows the picker sheet for whatever item the state was last opened with.
@Composable
internal fun LibraryListPickerHost(
    state: LibraryListPickerState,
    onRemovalNeedsConfirmation: (PendingTrackingMembershipRemoval) -> Unit,
) {
    val failedMessage = stringResource(Res.string.tracking_lists_update_failed)
    TrackingListPickerSheet(
        visible = state.visible,
        title = state.title,
        tabs = state.tabs,
        membership = state.membership,
        isPending = state.isPending,
        errorMessage = state.error,
        onToggle = { key -> state.toggle(key, failedMessage, onRemovalNeedsConfirmation) },
        onDismiss = state::dismiss,
    )
}
