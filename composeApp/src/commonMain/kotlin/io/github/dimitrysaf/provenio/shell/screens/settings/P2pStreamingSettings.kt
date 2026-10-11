package io.github.dimitrysaf.provenio.shell.screens.settings

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.screens.p2p.P2pConsentDialog
import io.github.dimitrysaf.provenio.core.p2p.P2pCacheClearResult
import io.github.dimitrysaf.provenio.core.p2p.P2pCacheSize
import io.github.dimitrysaf.provenio.core.p2p.P2pSettingsRepository
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingEngine
import io.github.dimitrysaf.provenio.core.p2p.P2pStreams
import io.github.dimitrysaf.provenio.core.p2p.P2pStreamingState
import io.github.dimitrysaf.provenio.core.p2p.P2pTorrentProfile
import kotlinx.coroutines.launch
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

/** P2P streaming's options and its cache. */
@Composable
private fun P2pGroup(
    isTablet: Boolean,
) {
    var showP2pProfileDialog by remember { mutableStateOf(false) }
    var showP2pCacheSizeDialog by remember { mutableStateOf(false) }
    var showExternalServerDialog by remember { mutableStateOf(false) }
    var p2pCacheClearResult by remember { mutableStateOf<P2pCacheClearResult?>(null) }
    var p2pCacheClearFailed by remember { mutableStateOf(false) }
    // Read as values here, not through delegates inside the list's builder: a state read in the
    // builder re-runs only the builder, so the rows' labels would keep their old values.
    val p2pSettings = remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle().value
    val p2pCacheState = P2pStreamingEngine.cacheState.collectAsStateWithLifecycle().value
    val p2pStreamingState = P2pStreams.state.collectAsStateWithLifecycle().value
    val p2pDownloadStreams = P2pStreamingEngine.downloadStreamCount.collectAsStateWithLifecycle().value
    val coroutineScope = rememberCoroutineScope()
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
                description = p2pCacheSizeLabel(p2pSettings.cacheSize),
                enabled = p2pSettings.p2pEnabled,
                onClick = { showP2pCacheSizeDialog = true },
            )
            val cacheClearAvailable = p2pStreamingState !is P2pStreamingState.Connecting &&
                p2pStreamingState !is P2pStreamingState.Streaming &&
                p2pDownloadStreams == 0 &&
                !p2pCacheState.isClearing
            navigationRow(
                title = stringResource(Res.string.settings_p2p_clear_cache_title),
                description = when {
                    p2pCacheState.isClearing ->
                        stringResource(Res.string.settings_p2p_clear_cache_clearing)
                    !cacheClearAvailable ->
                        stringResource(Res.string.settings_p2p_clear_cache_playback_active)
                    p2pCacheClearFailed ->
                        stringResource(Res.string.settings_p2p_clear_cache_failed)
                    p2pCacheClearResult != null -> stringResource(
                        Res.string.settings_p2p_clear_cache_done,
                        formatP2pCacheBytes(p2pCacheClearResult!!.reclaimedBytes),
                    )
                    !p2pCacheState.hasMeasurement ->
                        stringResource(Res.string.settings_p2p_clear_cache_usage_pending)
                    else -> stringResource(
                        Res.string.settings_p2p_clear_cache_usage,
                        formatP2pCacheBytes(p2pCacheState.usedBytes),
                    )
                },
                enabled = cacheClearAvailable,
                onClick = {
                    p2pCacheClearResult = null
                    p2pCacheClearFailed = false
                    coroutineScope.launch {
                        runCatching { P2pStreamingEngine.clearCache() }
                            .onSuccess { p2pCacheClearResult = it }
                            .onFailure { p2pCacheClearFailed = true }
                    }
                },
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
                p2pCacheClearResult = null
                showP2pCacheSizeDialog = false
            },
            onDismiss = { showP2pCacheSizeDialog = false },
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
private fun p2pCacheSizeLabel(size: P2pCacheSize): String = when (size) {
    P2pCacheSize.NONE -> stringResource(Res.string.settings_p2p_cache_none)
    P2pCacheSize.GB_2 -> stringResource(Res.string.settings_p2p_cache_2_gb)
    P2pCacheSize.GB_5 -> stringResource(Res.string.settings_p2p_cache_5_gb)
    P2pCacheSize.GB_10 -> stringResource(Res.string.settings_p2p_cache_10_gb)
}

private fun formatP2pCacheBytes(bytes: Long): String {
    val gibibyte = 1024.0 * 1024.0 * 1024.0
    val mebibyte = 1024.0 * 1024.0
    return if (bytes >= gibibyte) {
        "${kotlin.math.round(bytes / gibibyte * 10.0) / 10.0} GB"
    } else {
        "${kotlin.math.round(bytes / mebibyte * 10.0) / 10.0} MB"
    }
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
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    invalid = false
                },
                modifier = Modifier.fillMaxWidth(),
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
