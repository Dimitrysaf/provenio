package com.nuvio.app.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.runtime.Composable
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.library.LibraryItem
import com.nuvio.app.core.library.LibraryRepository
import com.nuvio.app.core.library.librarySectionItemKey
import com.nuvio.app.core.library.toLibraryItem
import com.nuvio.app.core.tracking.TrackingMembershipApplyResult
import com.nuvio.app.core.tracking.TrackingProviderId
import com.nuvio.app.core.watch.watching.application.WatchingActions
import com.nuvio.app.core.watch.watching.application.WatchingState
import com.nuvio.app.shell.components.DisintegrationRequestController
import com.nuvio.app.shell.components.MediaActionsSheet
import com.nuvio.app.shell.components.MediaSheetAction
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.screens.library.LibraryListPickerState
import com.nuvio.app.shell.screens.library.PendingTrackingMembershipRemoval
import com.nuvio.app.shell.screens.library.executeTrackingMembershipOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// The long press sheet on a poster: add or remove it from the library, and mark it watched.
@Composable
internal fun PosterActionsSheet(
    target: PosterActionTarget,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    isRemoteLibrarySource: Boolean,
    scope: CoroutineScope,
    libraryDisintegrationRequests: DisintegrationRequestController<String>,
    libraryListPicker: LibraryListPickerState,
    onRemovalNeedsConfirmation: (PendingTrackingMembershipRemoval) -> Unit,
    onDismiss: () -> Unit,
) {
    val preview = target.preview
    val failedMessage = stringResource(Res.string.tracking_lists_update_failed)
    val isSaved = LibraryRepository.isSaved(preview.id, preview.type)
    val isWatched = WatchingState.isPosterWatched(
        watchedKeys = watchedKeys,
        item = preview,
        fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
    )
    val removesFromLibrary = isSaved && (target.libraryItem != null || !isRemoteLibrarySource)
    MediaActionsSheet(
        imageUrl = preview.poster,
        title = preview.name,
        subtitle = preview.releaseInfo
            ?.takeIf { it.isNotBlank() }
            ?.let { formatReleaseDateForDisplay(it) }
            ?: preview.type.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            },
        actions = listOf(
            MediaSheetAction(
                icon = if (isSaved) Icons.Default.DeleteOutline else Icons.Default.Add,
                label = if (isSaved) {
                    stringResource(Res.string.hero_remove_from_library)
                } else {
                    stringResource(Res.string.hero_add_to_library)
                },
                isDestructive = removesFromLibrary,
                onSelected = {
                    val libraryItem = target.libraryItem ?: preview.toLibraryItem(savedAtEpochMs = 0L)
                    when {
                        target.libraryItem == null && !isRemoteLibrarySource ->
                            LibraryRepository.toggleLocalSaved(libraryItem)
                        target.libraryItem == null ->
                            libraryListPicker.open(libraryItem, preview.name)
                        isRemoteLibrarySource -> scope.launch {
                            removeFromRemoteLibrary(
                                libraryItem = libraryItem,
                                listKey = target.libraryListKey,
                                animate = removesFromLibrary,
                                libraryDisintegrationRequests = libraryDisintegrationRequests,
                                failedMessage = failedMessage,
                                onRemovalNeedsConfirmation = onRemovalNeedsConfirmation,
                            )
                        }
                        else -> {
                            if (removesFromLibrary) {
                                target.libraryListKey
                                    ?.let { listKey -> librarySectionItemKey(listKey, libraryItem) }
                                    ?.let(libraryDisintegrationRequests::arm)
                            }
                            LibraryRepository.remove(libraryItem.id)
                        }
                    }
                },
            ),
            MediaSheetAction(
                icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                label = if (isWatched) {
                    stringResource(Res.string.hero_mark_unwatched)
                } else {
                    stringResource(Res.string.hero_mark_watched)
                },
                onSelected = {
                    scope.launch {
                        WatchingActions.togglePosterWatched(preview)
                    }
                },
            ),
        ),
        onDismiss = onDismiss,
    )
}

// Removes an item from one remote list, or from all of them, playing the disintegration while it goes.
private suspend fun removeFromRemoteLibrary(
    libraryItem: LibraryItem,
    listKey: String?,
    animate: Boolean,
    libraryDisintegrationRequests: DisintegrationRequestController<String>,
    failedMessage: String,
    onRemovalNeedsConfirmation: (PendingTrackingMembershipRemoval) -> Unit,
) {
    val animationKey = listKey?.let { key -> librarySectionItemKey(key, libraryItem) }
    val removeMembership: suspend (Set<TrackingProviderId>) -> TrackingMembershipApplyResult =
        { confirmedProviders ->
            if (listKey.isNullOrBlank()) {
                val currentMembership = LibraryRepository.getMembershipSnapshot(libraryItem)
                LibraryRepository.applyMembershipChanges(
                    item = libraryItem,
                    desiredMembership = currentMembership.mapValues { false },
                    confirmedRemovalProviders = confirmedProviders,
                )
            } else {
                LibraryRepository.removeFromList(
                    item = libraryItem,
                    listKey = listKey,
                    confirmedRemovalProviders = confirmedProviders,
                )
            }
        }
    val removeMembershipWithAnimation: suspend (Set<TrackingProviderId>) -> TrackingMembershipApplyResult =
        { confirmedProviders ->
            val request = if (animate) animationKey?.let(libraryDisintegrationRequests::arm) else null
            try {
                removeMembership(confirmedProviders).also { result ->
                    if (result.requiresRemovalConfirmation && request != null) {
                        libraryDisintegrationRequests.cancel(request)
                    }
                }
            } catch (error: Throwable) {
                request?.let(libraryDisintegrationRequests::cancel)
                throw error
            }
        }
    executeTrackingMembershipOperation(
        operation = { removeMembershipWithAnimation(emptySet()) },
        onSuccess = { result ->
            if (result.requiresRemovalConfirmation) {
                onRemovalNeedsConfirmation(
                    PendingTrackingMembershipRemoval(
                        itemTitle = libraryItem.name,
                        confirmations = result.requiredRemovalConfirmations,
                        retry = removeMembershipWithAnimation,
                        onApplied = {},
                        onFailure = { error -> NuvioToastController.show(error.message ?: failedMessage) },
                    ),
                )
            }
        },
        onFailure = { error -> NuvioToastController.show(error.message ?: failedMessage) },
    )
}
