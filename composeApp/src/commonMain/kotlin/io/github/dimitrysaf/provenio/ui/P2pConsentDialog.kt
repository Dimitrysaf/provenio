package io.github.dimitrysaf.provenio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.platform.rememberUrlOpener
import kotlinx.coroutines.delay

internal const val VpnExplainerUrl = "https://en.wikipedia.org/wiki/Virtual_private_network"

/** Seconds the consent notice stays un-acceptable, so it is read rather than dismissed. */
private const val ConsentCountdownSeconds = 10

/**
 * Shown once, before peer-to-peer is ever switched on.
 *
 * The primary action is held back for [ConsentCountdownSeconds] because the point of the
 * notice is the address disclosure, and a button that is immediately tappable gets tapped
 * without being read.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun P2pConsentDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val openUrl = rememberUrlOpener()
    var remaining by remember { mutableIntStateOf(ConsentCountdownSeconds) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
        title = { Text("Enable P2P Streaming?") },
        text = {
            // Still long enough to overflow a short window, so it scrolls rather than
            // pushing the buttons off screen.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "This stream uses peer-to-peer technology. By enabling it " +
                        "you agree that:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                ConsentPoint("Your IP address is visible to other peers and to your ISP.")
                ConsentPoint(
                    "You are responsible for your use of peer-to-peer connections and " +
                        "any content accessed through them.",
                )
                ConsentPoint(
                    "This app does not host or control content. It connects to " +
                        "third-party networks.",
                )
                ConsentPoint(
                    "The developers accept no liability for your use of this feature.",
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Uploading is off by default. Distributing copyrighted " +
                        "material is treated far more seriously than downloading it. " +
                        "Check your local law before enabling it.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A VPN prevents peers from seeing your address. You can turn " +
                        "this off any time in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        // All three actions live in one slot so they can be centred. A flow row keeps
        // them on one line where they fit and wraps only when they do not, which costs
        // far less height than stacking them.
        confirmButton = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.Center,
            ) {
                // Cancel carries the emphasis. Declining is the safe outcome here, so it
                // should be the easiest thing to hit and the obvious default.
                Button(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { openUrl(VpnExplainerUrl) }) {
                    Text("What is a VPN?")
                }
                TextButton(enabled = remaining == 0, onClick = onAccept) {
                    Text(if (remaining == 0) "Enable P2P" else "Enable P2P ($remaining)")
                }
            }
        },
    )
}

@Composable
internal fun ConsentPoint(text: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = "\u2022", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
