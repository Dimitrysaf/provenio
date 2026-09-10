package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.simkl.SimklStatus
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.watchlist_add
import io.github.dimitrysaf.provenio.resources.watchlist_completed
import io.github.dimitrysaf.provenio.resources.watchlist_dropped
import io.github.dimitrysaf.provenio.resources.watchlist_in
import io.github.dimitrysaf.provenio.resources.watchlist_on_hold
import io.github.dimitrysaf.provenio.resources.watchlist_plan_to_watch
import io.github.dimitrysaf.provenio.resources.watchlist_remove
import io.github.dimitrysaf.provenio.resources.watchlist_watching
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The lists a title can be moved to from here, in the order they are offered.
 *
 * Watching is deliberately absent: starting something is what the watch button above does,
 * and Simkl moves a title there itself once an episode is scrobbled.
 */
private val WatchlistOptions = listOf(
    Triple(
        SimklStatus.PlanToWatch,
        Res.string.watchlist_plan_to_watch,
        Icons.Outlined.WatchLater,
    ),
    Triple(SimklStatus.Completed, Res.string.watchlist_completed, Icons.Outlined.CheckCircle),
    Triple(SimklStatus.Hold, Res.string.watchlist_on_hold, Icons.Outlined.PauseCircle),
    Triple(SimklStatus.Dropped, Res.string.watchlist_dropped, Icons.Outlined.Cancel),
)

/** What one of Simkl's status strings reads as. */
@Composable
fun watchlistLabel(status: String): String {
    val offered = WatchlistOptions.firstOrNull { it.first == status }?.second
    return when {
        offered != null -> stringResource(offered)
        status == SimklStatus.Watching -> stringResource(Res.string.watchlist_watching)
        // Simkl could name a list this build does not offer; its own name is all there is.
        else -> status
    }
}

/**
 * Which of the user's Simkl lists this title is on, and a way to change it.
 *
 * Disabled while a change is in flight: the write and the sync that follows it are two
 * round trips, and a second tap in between would race the first.
 */
@Composable
fun WatchlistAction(
    currentStatus: String?,
    working: Boolean,
    onSelect: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    FilledTonalButton(
        onClick = { open = true },
        enabled = !working,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (working) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            Icon(
                imageVector = if (currentStatus == null) {
                    Icons.Outlined.BookmarkAdd
                } else {
                    Icons.Outlined.BookmarkAdded
                },
                contentDescription = null,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = currentStatus
                ?.let { stringResource(Res.string.watchlist_in, watchlistLabel(it)) }
                ?: stringResource(Res.string.watchlist_add),
        )
    }

    if (open) {
        WatchlistSheet(
            currentStatus = currentStatus,
            onSelect = {
                open = false
                onSelect(it)
            },
            onDismiss = { open = false },
        )
    }
}

/**
 * A plain list sheet: one row per list, nothing else.
 *
 * No cancel row — dragging the sheet away or tapping outside already does that, and a row
 * repeating it is one more thing to hit by mistake on the way to the real ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistSheet(
    currentStatus: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            WatchlistOptions.forEach { (status, label, icon) ->
                WatchlistRow(
                    label = stringResource(label),
                    icon = icon,
                    selected = status == currentStatus,
                    onClick = { onSelect(status) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            WatchlistRow(
                label = stringResource(Res.string.watchlist_remove),
                icon = Icons.Outlined.Delete,
                destructive = true,
                onClick = { onSelect(null) },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * The sheet paints its own container, which is not the colour [ListItem] assumes, so the
 * rows are left transparent to sit on it rather than on a second, slightly different one.
 */
@Composable
private fun WatchlistRow(
    label: String,
    icon: ImageVector,
    selected: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = if (!selected) null else {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        colors = if (destructive) {
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            )
        } else {
            ListItemDefaults.colors(containerColor = Color.Transparent)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
