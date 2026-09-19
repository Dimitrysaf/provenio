package com.nuvio.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.runtime.Composable
import com.nuvio.app.features.cloud.cloudLibraryDisplayArtworkUrl
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cw_action_go_to_details
import nuvio.composeapp.generated.resources.cw_action_remove
import nuvio.composeapp.generated.resources.cw_action_start_from_beginning
import nuvio.composeapp.generated.resources.play_manually
import org.jetbrains.compose.resources.stringResource

/**
 * What can be done with something already being watched.
 *
 * The same sheet a poster's long press opens, given this card's actions: one long press, one
 * presentation. It used to draw its own header and rows, which is why it had accent-tinted icons
 * and dividers nothing else in the app has.
 */
@Composable
fun NuvioContinueWatchingActionSheet(
    item: ContinueWatchingItem,
    showManualPlayOption: Boolean,
    showDetailsOption: Boolean = true,
    blurThumbnail: Boolean = false,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    onStartFromBeginning: (() -> Unit)? = null,
    onPlayManually: (() -> Unit)? = null,
    onRemove: () -> Unit,
) {
    val actions = buildList {
        if (showDetailsOption) {
            add(
                MediaSheetAction(
                    icon = Icons.Rounded.Info,
                    label = stringResource(Res.string.cw_action_go_to_details),
                    onSelected = onOpenDetails,
                ),
            )
        }
        if (showManualPlayOption && onPlayManually != null) {
            add(
                MediaSheetAction(
                    icon = Icons.Rounded.PlayArrow,
                    label = stringResource(Res.string.play_manually),
                    onSelected = onPlayManually,
                ),
            )
        }
        if (!item.isNextUp && onStartFromBeginning != null) {
            add(
                MediaSheetAction(
                    icon = Icons.Rounded.Replay,
                    label = stringResource(Res.string.cw_action_start_from_beginning),
                    onSelected = onStartFromBeginning,
                ),
            )
        }
        add(
            MediaSheetAction(
                icon = Icons.Rounded.DeleteOutline,
                label = stringResource(Res.string.cw_action_remove),
                isDestructive = true,
                onSelected = onRemove,
            ),
        )
    }

    MediaActionsSheet(
        imageUrl = (item.poster ?: item.imageUrl)?.let { cloudLibraryDisplayArtworkUrl(it) },
        title = item.title,
        subtitle = localizedContinueWatchingSubtitle(item),
        actions = actions,
        onDismiss = onDismiss,
        landscapeThumbnail = item.poster == null && item.imageUrl != null,
        blurThumbnail = blurThumbnail,
    )
}
