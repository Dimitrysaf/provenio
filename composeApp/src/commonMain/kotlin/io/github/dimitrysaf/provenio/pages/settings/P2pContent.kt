package io.github.dimitrysaf.provenio.pages.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import io.github.dimitrysaf.provenio.p2p.TorrentProfile
import io.github.dimitrysaf.provenio.ui.P2pConsentDialog
import io.github.dimitrysaf.provenio.ui.VpnExplainerUrl
import io.github.dimitrysaf.provenio.ui.components.SettingsMainSwitch
import io.github.dimitrysaf.provenio.ui.components.SettingsTile
import io.github.dimitrysaf.provenio.ui.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.ui.components.TilePosition
import io.github.dimitrysaf.provenio.ui.components.tilePositionOf
import io.github.dimitrysaf.provenio.core.platform.rememberUrlOpener
import kotlinx.coroutines.delay



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
        checked = settings.enabled,
        onCheckedChange = { wanted ->
            if (wanted && !settings.consentAccepted) {
                showConsent = true
            } else {
                P2pRepository.setEnabled(wanted)
            }
        },
    )

    Spacer(Modifier.height(16.dp))

    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text("Status") },
            subtitle = {
                Text(if (settings.enabled) status.state.label else P2pServiceState.Disabled.label)
            },
            position = TilePosition.First,
            onClick = {},
        )
        SettingsTile(
            title = { Text("Listening port") },
            subtitle = {
                Text(
                    when {
                        status.portInUse -> "Port already in use"
                        status.listenPort != null -> status.listenPort.toString()
                        else -> "Not assigned"
                    },
                )
            },
            position = TilePosition.Middle,
            onClick = {},
        )
        SettingsTile(
            title = { Text("This device") },
            subtitle = {
                Text(
                    status.localAddresses.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ?: "Not found",
                )
            },
            position = TilePosition.Last,
            onClick = {},
        )
    }

    Spacer(Modifier.height(16.dp))

    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text("Seeding") },
            subtitle = {
                Text(
                    "Seeding means uploading what you already have downloaded from " +
                        "torrents. Check with local laws.",
                )
            },
            position = TilePosition.First,
            action = { Switch(checked = settings.uploadEnabled, onCheckedChange = null) },
            onClick = { P2pRepository.setUploadEnabled(!settings.uploadEnabled) },
        )
        SettingsTile(
            title = { Text("Connection profile") },
            subtitle = { Text(settings.profile.label) },
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
            position = TilePosition.Middle,
            enabled = status.cacheUsedBytes > 0,
            onClick = { P2pRepository.clearCache() },
        )
        SettingsTile(
            title = { Text("Hide torrent statistics") },
            subtitle = {
                Text("Hide information on speed, peer and seed counts from the video player")
            },
            position = TilePosition.Middle,
            action = { Switch(checked = settings.hideStats, onCheckedChange = null) },
            onClick = { P2pRepository.setHideStats(!settings.hideStats) },
        )
        SettingsTile(
            title = { Text("Information about VPNs") },
            subtitle = { Text("Virtual Private Networks") },
            position = TilePosition.Last,
            action = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                )
            },
            onClick = { openUrl(VpnExplainerUrl) },
        )
    }

    if (showProfileDialog) {
        ChoiceDialog(
            title = "Connection profile",
            options = TorrentProfile.entries,
            selected = settings.profile,
            label = { it.label },
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
