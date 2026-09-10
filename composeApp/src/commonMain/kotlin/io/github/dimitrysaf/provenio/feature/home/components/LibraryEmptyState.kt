package io.github.dimitrysaf.provenio.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.designsystem.components.EmptyState
import io.github.dimitrysaf.provenio.simkl.SyncState

/**
 * Says why the shelves are empty.
 *
 * A failed sync and a genuinely empty library produced the same blank screen before this,
 * which made a broken sync indistinguishable from having watched nothing.
 */
@Composable
fun LibraryEmptyState(
    signedIn: Boolean,
    syncState: SyncState,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        !signedIn -> EmptyState(
            modifier = modifier,
            icon = Icons.Outlined.CloudSync,
            title = "Your library lives on Simkl",
            description = "Sign in to bring what you are watching and planning to watch " +
                "into Provenio.",
            actionLabel = "Sign in to Simkl",
            onAction = onOpenSettings,
        )
        // A spinner with a line under it, not an empty state: there is nothing to act on
        // while a sync is still running.
        syncState is SyncState.Running -> Column(
            modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = "Syncing your library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        syncState is SyncState.Failed -> EmptyState(
            modifier = modifier,
            icon = Icons.Outlined.CloudOff,
            iconTint = MaterialTheme.colorScheme.error,
            title = "Sync did not finish",
            description = syncState.message,
            actionLabel = "Try again",
            onAction = onRetry,
        )
        else -> EmptyState(
            modifier = modifier,
            icon = Icons.Outlined.Bookmarks,
            title = "Nothing tracked yet",
            description = "Anything you mark as watching or plan to watch on Simkl shows " +
                "up here.",
            actionLabel = "Refresh",
            onAction = onRetry,
        )
    }
}
