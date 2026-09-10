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
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.library_lives_on_simkl
import io.github.dimitrysaf.provenio.resources.library_lives_on_simkl_body
import io.github.dimitrysaf.provenio.resources.library_nothing_tracked
import io.github.dimitrysaf.provenio.resources.library_nothing_tracked_body
import io.github.dimitrysaf.provenio.resources.library_sign_in
import io.github.dimitrysaf.provenio.resources.library_sync_failed
import io.github.dimitrysaf.provenio.resources.library_syncing
import io.github.dimitrysaf.provenio.resources.refresh
import io.github.dimitrysaf.provenio.resources.try_again
import org.jetbrains.compose.resources.stringResource

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
            title = stringResource(Res.string.library_lives_on_simkl),
            description = stringResource(Res.string.library_lives_on_simkl_body),
            actionLabel = stringResource(Res.string.library_sign_in),
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
                text = stringResource(Res.string.library_syncing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        syncState is SyncState.Failed -> EmptyState(
            modifier = modifier,
            icon = Icons.Outlined.CloudOff,
            iconTint = MaterialTheme.colorScheme.error,
            title = stringResource(Res.string.library_sync_failed),
            description = syncState.message,
            actionLabel = stringResource(Res.string.try_again),
            onAction = onRetry,
        )
        else -> EmptyState(
            modifier = modifier,
            icon = Icons.Outlined.Bookmarks,
            title = stringResource(Res.string.library_nothing_tracked),
            description = stringResource(Res.string.library_nothing_tracked_body),
            actionLabel = stringResource(Res.string.refresh),
            onAction = onRetry,
        )
    }
}
