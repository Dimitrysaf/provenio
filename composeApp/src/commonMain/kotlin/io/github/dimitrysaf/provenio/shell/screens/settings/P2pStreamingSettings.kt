package io.github.dimitrysaf.provenio.shell.screens.settings

import io.github.dimitrysaf.provenio.shell.components.textActionsMenu
import io.github.dimitrysaf.provenio.shell.components.rememberTextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.TextActionsMenu
import io.github.dimitrysaf.provenio.shell.components.arrowKeysLeaveField
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.screens.p2p.P2pConsentDialog
import io.github.dimitrysaf.provenio.core.p2p.P2pCacheSize
import io.github.dimitrysaf.provenio.core.p2p.P2pDetailsRefresh
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pTorrentProfile
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The P2P Streaming page: the torrent server's status, the switch that turns it on or off, its
 * options and its cache, then the network it streams over and a speed test.
 */
internal fun LazyListScope.p2pStreamingSettingsContent(isTablet: Boolean) {
    item {
        P2pStatusPreview()
    }
    item {
        P2pMainSwitch()
    }
    item {
        P2pGroup(isTablet = isTablet)
    }
    item {
        P2pNetworkGroup(isTablet = isTablet)
    }
}

/** Turns P2P streaming on, after the consent dialog the first time, or off. */
@Composable
private fun P2pMainSwitch() {
    var showConsentDialog by remember { mutableStateOf(false) }
    val settings = remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle().value
    SettingsMainSwitch(
        title = stringResource(Res.string.settings_p2p_title),
        checked = settings.p2pEnabled,
        onCheckedChange = { enabled ->
            if (enabled && !settings.p2pEnabled) {
                showConsentDialog = true
            } else {
                P2pSettingsRepository.setP2pEnabled(enabled)
            }
        },
    )
    if (showConsentDialog) {
        P2pConsentDialog(
            onEnableP2p = {
                P2pSettingsRepository.setP2pEnabled(true)
                showConsentDialog = false
            },
            onDismiss = { showConsentDialog = false },
        )
    }
}

/** P2P streaming's options. */
@Composable
private fun P2pGroup(
    isTablet: Boolean,
) {
    var showP2pProfileDialog by remember { mutableStateOf(false) }
    var showP2pCacheSizeDialog by remember { mutableStateOf(false) }
    var showDetailsRefreshDialog by remember { mutableStateOf(false) }
    var showExternalServerDialog by remember { mutableStateOf(false) }
    // Read as values here, not through delegates inside the list's builder: a state read in the
    // builder re-runs only the builder, so the rows' labels would keep their old values.
    val p2pSettings = remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle().value
    SettingsSection(
        title = stringResource(Res.string.settings_p2p_section_options),
        isTablet = isTablet,
    ) {
        SettingsList {
            switchRow(
                title = stringResource(Res.string.settings_p2p_seeding_title),
                enabled = p2pSettings.p2pEnabled,
                description = stringResource(Res.string.settings_p2p_seeding_subtitle),
                checked = { p2pSettings.enableUpload },
                onCheckedChange = P2pSettingsRepository::setEnableUpload,
            )
            switchRow(
                title = stringResource(Res.string.settings_p2p_hide_stats_title),
                enabled = p2pSettings.p2pEnabled,
                description = stringResource(Res.string.settings_p2p_hide_stats_subtitle),
                checked = { p2pSettings.hideTorrentStats },
                onCheckedChange = P2pSettingsRepository::setHideTorrentStats,
            )
            switchRow(
                title = stringResource(Res.string.settings_p2p_prefetch_title),
                enabled = p2pSettings.p2pEnabled,
                description = stringResource(Res.string.settings_p2p_prefetch_subtitle),
                checked = { p2pSettings.prefetchTorrentInfo },
                onCheckedChange = P2pSettingsRepository::setPrefetchTorrentInfo,
            )
            navigationRow(
                title = stringResource(Res.string.settings_p2p_external_server_title),
                description = p2pSettings.externalServer
                    ?: stringResource(Res.string.settings_p2p_external_server_not_set),
                enabled = p2pSettings.p2pEnabled,
                onClick = { showExternalServerDialog = true },
            )
            navigationRow(
                title = stringResource(Res.string.settings_p2p_profile_title),
                description = p2pProfileLabel(p2pSettings.torrentProfile),
                enabled = p2pSettings.p2pEnabled,
                onClick = { showP2pProfileDialog = true },
            )
            navigationRow(
                title = stringResource(Res.string.settings_p2p_cache_size_title),
                description = if (p2pSettings.cacheSize == P2pCacheSize.NONE) {
                    p2pCacheSizeLabel(p2pSettings.cacheSize)
                } else {
                    stringResource(Res.string.settings_p2p_cache_size_description, p2pCacheSizeLabel(p2pSettings.cacheSize))
                },
                enabled = p2pSettings.p2pEnabled,
                onClick = { showP2pCacheSizeDialog = true },
            )
            navigationRow(
                title = stringResource(Res.string.settings_p2p_details_refresh_title),
                description = stringResource(
                    Res.string.settings_p2p_details_refresh_description,
                    p2pDetailsRefreshLabel(p2pSettings.detailsRefresh),
                ),
                enabled = p2pSettings.p2pEnabled,
                onClick = { showDetailsRefreshDialog = true },
            )
        }
    }

    if (showExternalServerDialog) {
        ExternalServerDialog(
            current = p2pSettings.externalServer,
            onDismiss = { showExternalServerDialog = false },
        )
    }

    if (showP2pProfileDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_p2p_profile_title),
            options = P2pTorrentProfile.entries,
            selected = p2pSettings.torrentProfile,
            label = { p2pProfileLabel(it) },
            description = { profile ->
                when (profile) {
                    P2pTorrentProfile.SOFT ->
                        stringResource(Res.string.settings_p2p_profile_soft_description)
                    P2pTorrentProfile.BALANCED ->
                        stringResource(Res.string.settings_p2p_profile_balanced_description)
                    P2pTorrentProfile.FAST ->
                        stringResource(Res.string.settings_p2p_profile_fast_description)
                }
            },
            onSelect = { profile ->
                P2pSettingsRepository.setTorrentProfile(profile)
                showP2pProfileDialog = false
            },
            onDismiss = { showP2pProfileDialog = false },
        )
    }

    if (showP2pCacheSizeDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_p2p_cache_size_title),
            options = P2pCacheSize.entries,
            selected = p2pSettings.cacheSize,
            label = { p2pCacheSizeLabel(it) },
            onSelect = { size ->
                P2pSettingsRepository.setCacheSize(size)
                showP2pCacheSizeDialog = false
            },
            onDismiss = { showP2pCacheSizeDialog = false },
        )
    }

    if (showDetailsRefreshDialog) {
        IosEnumSelectionDialog(
            title = stringResource(Res.string.settings_p2p_details_refresh_title),
            options = P2pDetailsRefresh.entries,
            selected = p2pSettings.detailsRefresh,
            label = { p2pDetailsRefreshLabel(it) },
            onSelect = { refresh ->
                P2pSettingsRepository.setDetailsRefresh(refresh)
                showDetailsRefreshDialog = false
            },
            onDismiss = { showDetailsRefreshDialog = false },
        )
    }
}

@Composable
private fun p2pProfileLabel(profile: P2pTorrentProfile): String = when (profile) {
    P2pTorrentProfile.SOFT -> stringResource(Res.string.settings_p2p_profile_soft)
    P2pTorrentProfile.BALANCED -> stringResource(Res.string.settings_p2p_profile_balanced)
    P2pTorrentProfile.FAST -> stringResource(Res.string.settings_p2p_profile_fast)
}

@Composable
private fun p2pDetailsRefreshLabel(refresh: P2pDetailsRefresh): String = when (refresh) {
    P2pDetailsRefresh.MS_100 -> stringResource(Res.string.settings_p2p_details_refresh_100_ms)
    P2pDetailsRefresh.MS_250 -> stringResource(Res.string.settings_p2p_details_refresh_250_ms)
    P2pDetailsRefresh.MS_500 -> stringResource(Res.string.settings_p2p_details_refresh_500_ms)
    P2pDetailsRefresh.S_1 -> stringResource(Res.string.settings_p2p_details_refresh_1_s)
    P2pDetailsRefresh.S_2 -> stringResource(Res.string.settings_p2p_details_refresh_2_s)
    P2pDetailsRefresh.S_5 -> stringResource(Res.string.settings_p2p_details_refresh_5_s)
}

@Composable
private fun p2pCacheSizeLabel(size: P2pCacheSize): String = when (size) {
    P2pCacheSize.NONE -> stringResource(Res.string.settings_p2p_cache_none)
    P2pCacheSize.GB_2 -> stringResource(Res.string.settings_p2p_cache_2_gb)
    P2pCacheSize.GB_5 -> stringResource(Res.string.settings_p2p_cache_5_gb)
    P2pCacheSize.GB_10 -> stringResource(Res.string.settings_p2p_cache_10_gb)
}

/**
 * Asks for the address of a Stremio-compatible streaming server. Saving an address sends torrent
 * playback there; clearing it goes back to Provenio's own engine.
 */
@Composable
private fun ExternalServerDialog(
    current: String?,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current.orEmpty()) }
    var invalid by remember { mutableStateOf(false) }
    val save: () -> Unit = {
        if (P2pSettingsRepository.setExternalServer(draft)) {
            onDismiss()
        } else {
            invalid = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_p2p_external_server_title)) },
        text = {
            val textMenu = rememberTextActionsMenu()
            TextActionsMenu(textMenu, text = draft) { pasted ->
                draft += pasted
                invalid = false
            }
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    invalid = false
                },
                modifier = Modifier.fillMaxWidth().arrowKeysLeaveField { draft.isEmpty() }.textActionsMenu(textMenu),
                singleLine = true,
                label = { Text(stringResource(Res.string.settings_p2p_external_server_label)) },
                placeholder = { Text("192.168.1.20") },
                isError = invalid,
                supportingText = {
                    Text(
                        stringResource(
                            if (invalid) {
                                Res.string.settings_p2p_external_server_invalid
                            } else {
                                Res.string.settings_p2p_external_server_hint
                            },
                        ),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )
        },
        confirmButton = {
            TextButton(onClick = save) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(
                        onClick = {
                            P2pSettingsRepository.setExternalServer(null)
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(Res.string.action_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        },
    )
}
