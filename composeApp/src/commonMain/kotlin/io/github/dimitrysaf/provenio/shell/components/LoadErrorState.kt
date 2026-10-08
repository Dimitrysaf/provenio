package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.network.LoadFailure
import io.github.dimitrysaf.provenio.core.network.LoadFailureKind
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.mostActionable
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * The page could not load, and says why: which source failed, in what way, and whether waiting or
 * retrying will help. Nothing here is remembered, so Retry always asks the sources again.
 *
 * When the device itself is offline that is the real reason, whatever each source reported, so the
 * offline state is shown instead.
 *
 * With several sources, the headline is the failure most worth acting on (see [mostActionable]),
 * and each source's own result is listed under it.
 */
@Composable
fun LoadErrorState(
    failures: List<LoadFailure>,
    networkCondition: NetworkCondition,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable) {
        NetworkOfflineCard(condition = networkCondition, modifier = modifier, onRetry = onRetry)
        return
    }

    val primary = failures.mostActionable()
    val allNotFound = failures.size > 1 && failures.all { it.kind == LoadFailureKind.NotFound }
    val source = primary?.sourceName.orEmpty()

    EmptyState(
        icon = primary?.kind.icon(),
        title = when {
            primary == null -> stringResource(Res.string.load_error_unknown_title)
            else -> primary.kind.title(source)
        },
        message = when {
            allNotFound -> stringResource(Res.string.load_error_not_found_all_message)
            primary == null -> stringResource(Res.string.load_error_unknown_message)
            else -> primary.kind.message(source)
        },
        modifier = modifier,
        actionLabel = stringResource(Res.string.action_retry),
        onActionClick = onRetry,
        supportingContent = if (failures.size > 1) {
            { LoadFailureList(failures) }
        } else {
            null
        },
    )
}

@Composable
private fun LoadFailureList(failures: List<LoadFailure>) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        failures.forEach { failure ->
            val reason = failure.kind.shortReason()
            Text(
                text = failure.statusCode
                    ?.let { stringResource(Res.string.load_error_source_line_http, failure.sourceName, reason, it) }
                    ?: stringResource(Res.string.load_error_source_line, failure.sourceName, reason),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun LoadFailureKind?.icon(): ImageVector =
    when (this) {
        LoadFailureKind.NotFound -> Icons.Rounded.SearchOff
        LoadFailureKind.RateLimited -> Icons.Rounded.HourglassTop
        LoadFailureKind.Refused -> Icons.Rounded.Lock
        LoadFailureKind.ServerError -> Icons.Rounded.Dns
        LoadFailureKind.Timeout -> Icons.Rounded.Timer
        LoadFailureKind.Network -> Icons.Rounded.CloudOff
        LoadFailureKind.InvalidResponse -> Icons.Rounded.SyncProblem
        LoadFailureKind.Unknown, null -> Icons.Rounded.ErrorOutline
    }

@Composable
private fun LoadFailureKind.title(source: String): String =
    when (this) {
        LoadFailureKind.NotFound -> stringResource(Res.string.load_error_not_found_title)
        LoadFailureKind.RateLimited -> stringResource(Res.string.load_error_rate_limited_title, source)
        LoadFailureKind.Refused -> stringResource(Res.string.load_error_refused_title, source)
        LoadFailureKind.ServerError -> stringResource(Res.string.load_error_server_title, source)
        LoadFailureKind.Timeout -> stringResource(Res.string.load_error_timeout_title, source)
        LoadFailureKind.Network -> stringResource(Res.string.load_error_network_title, source)
        LoadFailureKind.InvalidResponse -> stringResource(Res.string.load_error_invalid_title, source)
        LoadFailureKind.Unknown -> stringResource(Res.string.load_error_unknown_title)
    }

@Composable
private fun LoadFailureKind.message(source: String): String =
    when (this) {
        LoadFailureKind.NotFound -> stringResource(Res.string.load_error_not_found_message, source)
        LoadFailureKind.RateLimited -> stringResource(Res.string.load_error_rate_limited_message)
        LoadFailureKind.Refused -> stringResource(Res.string.load_error_refused_message)
        LoadFailureKind.ServerError -> stringResource(Res.string.load_error_server_message)
        LoadFailureKind.Timeout -> stringResource(Res.string.load_error_timeout_message)
        LoadFailureKind.Network -> stringResource(Res.string.load_error_network_message)
        LoadFailureKind.InvalidResponse -> stringResource(Res.string.load_error_invalid_message)
        LoadFailureKind.Unknown -> stringResource(Res.string.load_error_unknown_message)
    }

@Composable
private fun LoadFailureKind.shortReason(): String =
    when (this) {
        LoadFailureKind.NotFound -> stringResource(Res.string.load_error_reason_not_found)
        LoadFailureKind.RateLimited -> stringResource(Res.string.load_error_reason_rate_limited)
        LoadFailureKind.Refused -> stringResource(Res.string.load_error_reason_refused)
        LoadFailureKind.ServerError -> stringResource(Res.string.load_error_reason_server)
        LoadFailureKind.Timeout -> stringResource(Res.string.load_error_reason_timeout)
        LoadFailureKind.Network -> stringResource(Res.string.load_error_reason_network)
        LoadFailureKind.InvalidResponse -> stringResource(Res.string.load_error_reason_invalid)
        LoadFailureKind.Unknown -> stringResource(Res.string.load_error_reason_unknown)
    }
