package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_retry
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.details_check_connection
import provenio.composeapp.generated.resources.details_servers_unreachable
import provenio.composeapp.generated.resources.network_cannot_reach_servers
import provenio.composeapp.generated.resources.network_connection_issue
import provenio.composeapp.generated.resources.network_no_internet_connection
import provenio.composeapp.generated.resources.network_please_check_connection

/**
 * The page has nothing to show because the network is down.
 *
 * That is an empty state like any other, so it is the app's empty state: the same icon, title,
 * message and single action every other one has, rather than a card of its own.
 */
@Composable
fun NetworkOfflineCard(
    condition: NetworkCondition,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    EmptyState(
        icon = Icons.Rounded.CloudOff,
        title = condition.titleForEmptyState(),
        message = condition.messageForEmptyState(),
        modifier = modifier,
        actionLabel = onRetry?.let { stringResource(Res.string.action_retry) },
        onActionClick = onRetry,
    )
}

@Composable
fun NetworkCondition.titleForEmptyState(): String =
    when (this) {
        NetworkCondition.ServersUnreachable -> stringResource(Res.string.network_cannot_reach_servers)
        NetworkCondition.NoInternet -> stringResource(Res.string.network_no_internet_connection)
        else -> stringResource(Res.string.network_connection_issue)
    }

@Composable
fun NetworkCondition.messageForEmptyState(): String =
    when (this) {
        NetworkCondition.ServersUnreachable -> stringResource(Res.string.details_servers_unreachable)
        NetworkCondition.NoInternet -> stringResource(Res.string.details_check_connection)
        else -> stringResource(Res.string.network_please_check_connection)
    }
