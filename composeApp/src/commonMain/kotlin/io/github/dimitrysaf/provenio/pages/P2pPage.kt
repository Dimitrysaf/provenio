package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import io.github.dimitrysaf.provenio.p2p.CacheSize
import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.p2p.P2pServiceState
import io.github.dimitrysaf.provenio.p2p.P2pStatus
import io.github.dimitrysaf.provenio.p2p.TorrentProfile
import io.github.dimitrysaf.provenio.p2p.formatBytes
import io.github.dimitrysaf.provenio.p2p.formatSpeed
import io.github.dimitrysaf.provenio.ui.components.SettingsMainSwitch
import io.github.dimitrysaf.provenio.ui.components.SettingsTile
import io.github.dimitrysaf.provenio.ui.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.ui.components.TilePosition
import io.github.dimitrysaf.provenio.ui.components.tilePositionOf
import io.github.dimitrysaf.provenio.ui.rememberUrlOpener
import kotlinx.coroutines.delay

private const val VpnExplainerUrl = "https://en.wikipedia.org/wiki/Virtual_private_network"

/** Seconds the consent notice stays un-acceptable, so it is read rather than dismissed. */
private const val ConsentCountdownSeconds = 10

@Composable
fun P2pContent() {
    val settings by P2pRepository.settings.collectAsState()
    val status by P2pRepository.status.collectAsState()
    var showConsent by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    val openUrl = rememberUrlOpener()

    SettingsMainSwitch(
        title = "Peer-to-peer",
        subtitle = if (settings.enabled) status.state.label else "Off",
        checked = settings.enabled,
        onCheckedChange = { wanted ->
            // Turning it on for the first time asks first; turning it off never does.
            if (wanted && !settings.consentAccepted) {
                showConsent = true
            } else {
                P2pRepository.setEnabled(wanted)
            }
        },
    )

    Spacer(Modifier.height(16.dp))

    if (settings.enabled) {
        SectionLabel("Status")
        StatusGroup(status)
        Spacer(Modifier.height(16.dp))
    }

    SectionLabel("Transfer")
    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text("Share while watching") },
            subtitle = {
                Text(
                    "Off by default. Distributing copyrighted material is treated far " +
                        "more seriously than downloading it in most jurisdictions.",
                )
            },
            position = TilePosition.First,
            action = { Switch(checked = settings.uploadEnabled, onCheckedChange = null) },
            onClick = { P2pRepository.setUploadEnabled(!settings.uploadEnabled) },
        )
        SettingsTile(
            title = { Text("Connection profile") },
            subtitle = { Text("${settings.profile.label} — ${settings.profile.summary}") },
            position = TilePosition.Middle,
            onClick = { showProfileDialog = true },
        )
        SettingsTile(
            title = { Text("Cache size") },
            subtitle = { Text(settings.cacheSize.label) },
            position = TilePosition.Middle,
            onClick = { showCacheDialog = true },
        )
        SettingsTile(
            title = { Text("Clear cache") },
            subtitle = { Text("Currently using ${formatBytes(status.cacheUsedBytes)}") },
            position = TilePosition.Last,
            onClick = { P2pRepository.clearCache() },
        )
    }

    Spacer(Modifier.height(16.dp))

    SectionLabel("Privacy")
    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text("Hide transfer statistics") },
            subtitle = { Text("Keep speeds and peer counts off the player") },
            position = TilePosition.First,
            action = { Switch(checked = settings.hideStats, onCheckedChange = null) },
            onClick = { P2pRepository.setHideStats(!settings.hideStats) },
        )
        SettingsTile(
            title = { Text("What is a VPN?") },
            subtitle = { Text("Why it matters for peer-to-peer") },
            position = TilePosition.Middle,
            onClick = { openUrl(VpnExplainerUrl) },
        )
        SettingsTile(
            title = { Text("Withdraw consent") },
            subtitle = { Text("Turns peer-to-peer off and asks again next time") },
            position = TilePosition.Last,
            enabled = settings.consentAccepted,
            onClick = { P2pRepository.revokeConsent() },
        )
    }

    if (showProfileDialog) {
        ChoiceDialog(
            title = "Connection profile",
            options = TorrentProfile.entries,
            selected = settings.profile,
            label = { "${it.label} — ${it.summary}" },
            onSelect = {
                P2pRepository.setProfile(it)
                showProfileDialog = false
            },
            onDismiss = { showProfileDialog = false },
        )
    }

    if (showCacheDialog) {
        ChoiceDialog(
            title = "Cache size",
            options = CacheSize.entries,
            selected = settings.cacheSize,
            label = { it.label },
            onSelect = {
                P2pRepository.setCacheSize(it)
                showCacheDialog = false
            },
            onDismiss = { showCacheDialog = false },
        )
    }

    if (showConsent) {
        P2pConsentDialog(
            onAccept = {
                P2pRepository.acceptConsent()
                P2pRepository.setEnabled(true)
                showConsent = false
            },
            onDismiss = { showConsent = false },
        )
    }
}

/** One enumerated choice, the same shape the theme picker uses. */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(text = label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun StatusGroup(status: P2pStatus) {
    val rows = buildList {
        add("State" to (status.detail ?: status.state.label))
        if (status.state == P2pServiceState.Running) {
            add("Download" to formatSpeed(status.downloadBytesPerSecond))
            add("Upload" to formatSpeed(status.uploadBytesPerSecond))
            add("Peers" to "${status.peers} connected, ${status.seeds} seeding")
            add("Active torrents" to status.activeTorrents.toString())
            add(
                "This session" to
                    "${formatBytes(status.sessionDownloadedBytes)} down, " +
                    "${formatBytes(status.sessionUploadedBytes)} up",
            )
            add("Cache used" to formatBytes(status.cacheUsedBytes))
        }
        add("Listening port" to (status.listenPort?.toString() ?: "Not assigned"))
        add(
            "This device" to
                status.localAddresses.takeIf { it.isNotEmpty() }?.joinToString(", ")
                .orEmpty().ifEmpty { "Unknown" },
        )
        add("Visible address" to (status.publicAddress ?: "Unknown until connected"))
    }

    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        rows.forEachIndexed { index, (label, value) ->
            SettingsTile(
                title = { Text(label) },
                subtitle = { Text(value) },
                position = tilePositionOf(index, rows.size),
                onClick = {},
            )
        }
    }
}

/**
 * Shown once, before peer-to-peer is ever switched on.
 *
 * The primary action is held back for [ConsentCountdownSeconds] because the point of the
 * notice is the address disclosure, and a button that is immediately tappable gets tapped
 * without being read.
 */
@Composable
private fun P2pConsentDialog(
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
            // The notice is long enough to overflow a short window, so it scrolls rather
            // than pushing the buttons off screen.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "This stream uses peer-to-peer technology. By enabling it, " +
                        "you acknowledge and agree that:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                ConsentPoint(
                    "Your IP address is visible to other peers on the network and to " +
                        "your Internet Service Provider.",
                )
                ConsentPoint(
                    "You are solely responsible for your use of peer-to-peer connections " +
                        "and for any content you access through them.",
                )
                ConsentPoint(
                    "This app does not host, distribute, or control any content. It " +
                        "connects to networks operated by third parties.",
                )
                ConsentPoint(
                    "The developers accept no liability for any consequences arising " +
                        "from your use of this feature.",
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Uploading and seeding are disabled by default. Most " +
                        "jurisdictions treat distributing copyrighted material far more " +
                        "seriously than downloading it, and enabling upload may expose " +
                        "you to significantly greater legal risk. Check your local law " +
                        "before changing this.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Using a VPN prevents other peers from seeing your address.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "This can be turned off at any time in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = remaining == 0, onClick = onAccept) {
                Text(if (remaining == 0) "Enable P2P" else "Enable P2P ($remaining)")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { openUrl(VpnExplainerUrl) }) { Text("What is a VPN?") }
            }
        },
    )
}

@Composable
private fun ConsentPoint(text: String) {
    Row(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(text = "\u2022", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
