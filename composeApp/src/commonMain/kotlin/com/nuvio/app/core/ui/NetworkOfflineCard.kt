package com.nuvio.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.messageForEmptyState
import com.nuvio.app.core.network.titleForEmptyState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_retry
import org.jetbrains.compose.resources.stringResource

/**
 * The page has nothing to show because the network is down.
 *
 * That is an empty state like any other, so it is the app's empty state: the same icon, title,
 * message and single action every other one has, rather than a card of its own.
 */
@Composable
fun NuvioNetworkOfflineCard(
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
