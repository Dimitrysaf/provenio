package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.simkl.SimklStatus

/**
 * The lists a title can be moved to from here, in the order they are offered.
 *
 * Watching is deliberately absent: starting something is what the watch button above does,
 * and Simkl moves a title there itself once an episode is scrobbled.
 */
private val WatchlistOptions = listOf(
    SimklStatus.PlanToWatch to "Plan to watch",
    SimklStatus.Completed to "Completed",
    SimklStatus.Hold to "On hold",
    SimklStatus.Dropped to "Dropped",
)

/** What one of Simkl's status strings reads as. */
fun watchlistLabel(status: String): String =
    WatchlistOptions.firstOrNull { it.first == status }?.second
        ?: if (status == SimklStatus.Watching) "Watching" else status

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
            text = currentStatus?.let { "In ${watchlistLabel(it)}" } ?: "Add to Watchlist",
        )
    }

    if (open) {
        WatchlistSheet(
            onSelect = {
                open = false
                onSelect(it)
            },
            onDismiss = { open = false },
        )
    }
}

/**
 * No cancel button: the sheet is dismissed by dragging it away or tapping outside, which
 * is what a bottom sheet already does, and a button repeating that only adds a row to miss
 * the real ones with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistSheet(
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Add to Watchlist",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            WatchlistOptions.forEach { (status, label) ->
                FilledTonalButton(
                    onClick = { onSelect(status) },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Text(label)
                }
            }

            FilledTonalButton(
                onClick = { onSelect(null) },
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Text("Remove from list")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
