package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.dimitrysaf.provenio.core.network.LoadFailure
import io.github.dimitrysaf.provenio.core.network.LoadFailureKind
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.mostActionable
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * The icon for why a page could not load, for the error dialog and for the empty artwork slot of
 * the page behind it. Offline wins over whatever each source reported.
 */
fun loadErrorIcon(failures: List<LoadFailure>, networkCondition: NetworkCondition): ImageVector =
    if (networkCondition.isOffline()) Icons.Rounded.CloudOff else failures.mostActionable()?.kind.icon()

/**
 * The page could not load, and this dialog says why: which source failed and what its answer
 * means. It sits over the page's skeleton, so the page keeps its shape and whatever is known
 * locally. Nothing here is remembered, so Retry always asks the sources again.
 *
 * When the device itself is offline that is the real reason, whatever each source reported.
 * With several sources, the headline is the failure most worth acting on (see [mostActionable]),
 * and each source's own result is listed under it.
 */
@Composable
fun LoadErrorDialog(
    failures: List<LoadFailure>,
    networkCondition: NetworkCondition,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val primary = failures.mostActionable()
    val allNotFound = failures.size > 1 && failures.all { it.kind == LoadFailureKind.NotFound }
    val source = primary?.sourceName.orEmpty()
    ErrorDialog(
        icon = loadErrorIcon(failures, networkCondition),
        title = when {
            networkCondition.isOffline() -> networkCondition.titleForEmptyState()
            primary == null -> stringResource(Res.string.load_error_unknown_title)
            else -> primary.kind.title(source)
        },
        message = when {
            networkCondition.isOffline() -> networkCondition.messageForEmptyState()
            allNotFound -> stringResource(Res.string.load_error_not_found_all_message)
            primary == null -> stringResource(Res.string.load_error_unknown_message)
            else -> primary.kind.message(source)
        },
        failures = failures,
        onRetry = onRetry,
        onDismiss = onDismiss,
    )
}

/**
 * The error dialog itself: what went wrong, one line per failed source, and Show verbose, which
 * grows the dialog with a scrollable record of every failure, stack traces included, for debugging
 * and bug reports. Dismissing it leaves the page, since the page has nothing to show without it.
 */
@Composable
fun ErrorDialog(
    icon: ImageVector,
    title: String,
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    failures: List<LoadFailure> = emptyList(),
) {
    val verboseText = remember(failures) {
        failures.mapNotNull { it.details }.joinToString(separator = "\n\n────────\n\n")
    }
    var showVerbose by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(text = title, textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(message)
                if (failures.size > 1) {
                    LoadFailureList(failures)
                }
                if (verboseText.isNotEmpty()) {
                    TextButton(
                        onClick = { showVerbose = !showVerbose },
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Text(
                            stringResource(
                                if (showVerbose) Res.string.load_error_hide_verbose else Res.string.load_error_show_verbose,
                            ),
                        )
                    }
                    AnimatedVisibility(visible = showVerbose) {
                        VerboseDetails(text = verboseText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(Res.string.action_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_back))
            }
        },
    )
}

private fun NetworkCondition.isOffline(): Boolean =
    this == NetworkCondition.NoInternet || this == NetworkCondition.ServersUnreachable

@Composable
private fun VerboseDetails(text: String) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                )
            }
        }
        TextButton(onClick = { clipboardManager.setText(AnnotatedString(text)) }) {
            Text(stringResource(Res.string.load_error_copy_details))
        }
    }
}

@Composable
private fun LoadFailureList(failures: List<LoadFailure>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        failures.forEach { failure ->
            val reason = failure.kind.shortReason()
            Text(
                text = failure.statusCode
                    ?.let { stringResource(Res.string.load_error_source_line_http, failure.sourceName, reason, it) }
                    ?: stringResource(Res.string.load_error_source_line, failure.sourceName, reason),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun LoadFailureKind?.icon(): ImageVector =
    when (this) {
        LoadFailureKind.NotFound,
        LoadFailureKind.EmptyResponse,
        -> Icons.Rounded.SearchOff
        LoadFailureKind.RateLimited -> Icons.Rounded.HourglassTop
        LoadFailureKind.Unauthorized,
        LoadFailureKind.Forbidden,
        LoadFailureKind.LegalBlock,
        LoadFailureKind.SecureConnectionFailed,
        -> Icons.Rounded.Lock
        LoadFailureKind.ServerError,
        LoadFailureKind.Unavailable,
        LoadFailureKind.GatewayError,
        -> Icons.Rounded.Dns
        LoadFailureKind.Timeout -> Icons.Rounded.Timer
        LoadFailureKind.DnsFailure,
        LoadFailureKind.Network,
        -> Icons.Rounded.CloudOff
        LoadFailureKind.BadRequest -> Icons.Rounded.Warning
        LoadFailureKind.InvalidResponse -> Icons.Rounded.SyncProblem
        LoadFailureKind.Unknown, null -> Icons.Rounded.ErrorOutline
    }

@Composable
private fun LoadFailureKind.title(source: String): String =
    when (this) {
        LoadFailureKind.NotFound -> stringResource(Res.string.load_error_not_found_title)
        LoadFailureKind.EmptyResponse -> stringResource(Res.string.load_error_empty_title, source)
        LoadFailureKind.RateLimited -> stringResource(Res.string.load_error_rate_limited_title, source)
        LoadFailureKind.Unauthorized -> stringResource(Res.string.load_error_unauthorized_title, source)
        LoadFailureKind.Forbidden -> stringResource(Res.string.load_error_forbidden_title, source)
        LoadFailureKind.LegalBlock -> stringResource(Res.string.load_error_legal_title, source)
        LoadFailureKind.BadRequest -> stringResource(Res.string.load_error_bad_request_title, source)
        LoadFailureKind.ServerError -> stringResource(Res.string.load_error_server_title, source)
        LoadFailureKind.Unavailable -> stringResource(Res.string.load_error_unavailable_title, source)
        LoadFailureKind.GatewayError -> stringResource(Res.string.load_error_gateway_title, source)
        LoadFailureKind.Timeout -> stringResource(Res.string.load_error_timeout_title, source)
        LoadFailureKind.DnsFailure -> stringResource(Res.string.load_error_dns_title, source)
        LoadFailureKind.SecureConnectionFailed -> stringResource(Res.string.load_error_tls_title, source)
        LoadFailureKind.Network -> stringResource(Res.string.load_error_network_title, source)
        LoadFailureKind.InvalidResponse -> stringResource(Res.string.load_error_invalid_title, source)
        LoadFailureKind.Unknown -> stringResource(Res.string.load_error_unknown_title)
    }

@Composable
private fun LoadFailureKind.message(source: String): String =
    when (this) {
        LoadFailureKind.NotFound -> stringResource(Res.string.load_error_not_found_message, source)
        LoadFailureKind.EmptyResponse -> stringResource(Res.string.load_error_empty_message)
        LoadFailureKind.RateLimited -> stringResource(Res.string.load_error_rate_limited_message)
        LoadFailureKind.Unauthorized -> stringResource(Res.string.load_error_unauthorized_message)
        LoadFailureKind.Forbidden -> stringResource(Res.string.load_error_forbidden_message)
        LoadFailureKind.LegalBlock -> stringResource(Res.string.load_error_legal_message)
        LoadFailureKind.BadRequest -> stringResource(Res.string.load_error_bad_request_message)
        LoadFailureKind.ServerError -> stringResource(Res.string.load_error_server_message)
        LoadFailureKind.Unavailable -> stringResource(Res.string.load_error_unavailable_message)
        LoadFailureKind.GatewayError -> stringResource(Res.string.load_error_gateway_message)
        LoadFailureKind.Timeout -> stringResource(Res.string.load_error_timeout_message)
        LoadFailureKind.DnsFailure -> stringResource(Res.string.load_error_dns_message)
        LoadFailureKind.SecureConnectionFailed -> stringResource(Res.string.load_error_tls_message)
        LoadFailureKind.Network -> stringResource(Res.string.load_error_network_message)
        LoadFailureKind.InvalidResponse -> stringResource(Res.string.load_error_invalid_message)
        LoadFailureKind.Unknown -> stringResource(Res.string.load_error_unknown_message)
    }

@Composable
private fun LoadFailureKind.shortReason(): String =
    when (this) {
        LoadFailureKind.NotFound -> stringResource(Res.string.load_error_reason_not_found)
        LoadFailureKind.EmptyResponse -> stringResource(Res.string.load_error_reason_empty)
        LoadFailureKind.RateLimited -> stringResource(Res.string.load_error_reason_rate_limited)
        LoadFailureKind.Unauthorized -> stringResource(Res.string.load_error_reason_unauthorized)
        LoadFailureKind.Forbidden -> stringResource(Res.string.load_error_reason_forbidden)
        LoadFailureKind.LegalBlock -> stringResource(Res.string.load_error_reason_legal)
        LoadFailureKind.BadRequest -> stringResource(Res.string.load_error_reason_bad_request)
        LoadFailureKind.ServerError -> stringResource(Res.string.load_error_reason_server)
        LoadFailureKind.Unavailable -> stringResource(Res.string.load_error_reason_unavailable)
        LoadFailureKind.GatewayError -> stringResource(Res.string.load_error_reason_gateway)
        LoadFailureKind.Timeout -> stringResource(Res.string.load_error_reason_timeout)
        LoadFailureKind.DnsFailure -> stringResource(Res.string.load_error_reason_dns)
        LoadFailureKind.SecureConnectionFailed -> stringResource(Res.string.load_error_reason_tls)
        LoadFailureKind.Network -> stringResource(Res.string.load_error_reason_network)
        LoadFailureKind.InvalidResponse -> stringResource(Res.string.load_error_reason_invalid)
        LoadFailureKind.Unknown -> stringResource(Res.string.load_error_reason_unknown)
    }
