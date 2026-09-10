package io.github.dimitrysaf.provenio.feature.settings.panes

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
import io.github.dimitrysaf.provenio.p2p.P2pConsentDialog
import io.github.dimitrysaf.provenio.p2p.P2pRepository
import io.github.dimitrysaf.provenio.p2p.VpnExplainerUrl
import io.github.dimitrysaf.provenio.p2p.P2pServiceState
import io.github.dimitrysaf.provenio.p2p.TorrentProfile
import io.github.dimitrysaf.provenio.designsystem.components.SettingsMainSwitch
import io.github.dimitrysaf.provenio.designsystem.components.SettingsTile
import io.github.dimitrysaf.provenio.designsystem.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.designsystem.components.TilePosition
import io.github.dimitrysaf.provenio.designsystem.components.tilePositionOf
import io.github.dimitrysaf.provenio.core.platform.rememberUrlOpener
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.cancel
import io.github.dimitrysaf.provenio.resources.p2p_cache_size
import io.github.dimitrysaf.provenio.resources.p2p_clear_cache
import io.github.dimitrysaf.provenio.resources.p2p_connection_profile
import io.github.dimitrysaf.provenio.resources.p2p_hide_stats
import io.github.dimitrysaf.provenio.resources.p2p_hide_stats_body
import io.github.dimitrysaf.provenio.resources.p2p_listening_port
import io.github.dimitrysaf.provenio.resources.p2p_not_assigned
import io.github.dimitrysaf.provenio.resources.p2p_not_found
import io.github.dimitrysaf.provenio.resources.p2p_port_in_use
import io.github.dimitrysaf.provenio.resources.p2p_seeding
import io.github.dimitrysaf.provenio.resources.p2p_seeding_body
import io.github.dimitrysaf.provenio.resources.p2p_status
import io.github.dimitrysaf.provenio.resources.p2p_this_device
import io.github.dimitrysaf.provenio.resources.p2p_title
import io.github.dimitrysaf.provenio.resources.p2p_vpn_info
import io.github.dimitrysaf.provenio.resources.p2p_vpn_info_summary
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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
        title = stringResource(Res.string.p2p_title),
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
            title = { Text(stringResource(Res.string.p2p_status)) },
            subtitle = {
                Text(
                    stringResource(
                        if (settings.enabled) status.state.label
                        else P2pServiceState.Disabled.label,
                    ),
                )
            },
            position = TilePosition.First,
            onClick = {},
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_listening_port)) },
            subtitle = {
                Text(
                    when {
                        status.portInUse -> stringResource(Res.string.p2p_port_in_use)
                        status.listenPort != null -> status.listenPort.toString()
                        else -> stringResource(Res.string.p2p_not_assigned)
                    },
                )
            },
            position = TilePosition.Middle,
            onClick = {},
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_this_device)) },
            subtitle = {
                Text(
                    status.localAddresses.takeIf { it.isNotEmpty() }?.joinToString(", ")
                        ?: stringResource(Res.string.p2p_not_found),
                )
            },
            position = TilePosition.Last,
            onClick = {},
        )
    }

    Spacer(Modifier.height(16.dp))

    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_seeding)) },
            subtitle = { Text(stringResource(Res.string.p2p_seeding_body)) },
            position = TilePosition.First,
            action = { Switch(checked = settings.uploadEnabled, onCheckedChange = null) },
            onClick = { P2pRepository.setUploadEnabled(!settings.uploadEnabled) },
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_connection_profile)) },
            subtitle = { Text(stringResource(settings.profile.label)) },
            position = TilePosition.Middle,
            onClick = { showProfileDialog = true },
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_cache_size)) },
            subtitle = { Text(stringResource(settings.cacheSize.label)) },
            position = TilePosition.Middle,
            onClick = { showCacheDialog = true },
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_clear_cache)) },
            position = TilePosition.Middle,
            enabled = status.cacheUsedBytes > 0,
            onClick = { P2pRepository.clearCache() },
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_hide_stats)) },
            subtitle = { Text(stringResource(Res.string.p2p_hide_stats_body)) },
            position = TilePosition.Middle,
            action = { Switch(checked = settings.hideStats, onCheckedChange = null) },
            onClick = { P2pRepository.setHideStats(!settings.hideStats) },
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.p2p_vpn_info)) },
            subtitle = { Text(stringResource(Res.string.p2p_vpn_info_summary)) },
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
            title = stringResource(Res.string.p2p_connection_profile),
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
            title = stringResource(Res.string.p2p_cache_size),
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
    label: (T) -> StringResource,
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
                        Text(
                            text = stringResource(label(option)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
