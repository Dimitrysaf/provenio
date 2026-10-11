package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.network.NetworkCondition
import io.github.dimitrysaf.provenio.core.network.NetworkConnectionInfo
import io.github.dimitrysaf.provenio.core.network.NetworkDiagnosticsRepository
import io.github.dimitrysaf.provenio.core.network.NetworkKind
import io.github.dimitrysaf.provenio.core.network.NetworkStatusRepository
import io.github.dimitrysaf.provenio.core.network.PublicIpState
import io.github.dimitrysaf.provenio.core.network.SpeedTestPhase
import io.github.dimitrysaf.provenio.core.network.SpeedTestState
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * The Network page: whether the internet is reachable, each connection and its addresses, the
 * public address on request, and a speed test.
 */
internal fun LazyListScope.networkSettingsContent(isTablet: Boolean) {
    item {
        NetworkStatusGroup(isTablet = isTablet)
    }
    if (NetworkDiagnosticsRepository.isSupported) {
        item {
            SpeedTestGroup(isTablet = isTablet)
        }
    }
}

@Composable
private fun NetworkStatusGroup(isTablet: Boolean) {
    val status = remember {
        NetworkStatusRepository.ensureStarted()
        NetworkStatusRepository.uiState
    }.collectAsStateWithLifecycle().value
    val connections = NetworkDiagnosticsRepository.connections.collectAsStateWithLifecycle().value
    val publicIp = NetworkDiagnosticsRepository.publicIp.collectAsStateWithLifecycle().value
    // Addresses change as networks come and go, so they are read again while the page is open.
    LaunchedEffect(Unit) {
        NetworkStatusRepository.requestRefresh(force = true)
        while (true) {
            NetworkDiagnosticsRepository.refreshConnections()
            delay(ConnectionsRefreshMillis)
        }
    }
    val internetText = stringResource(
        when (status.condition) {
            NetworkCondition.Online -> Res.string.settings_network_internet_online
            NetworkCondition.NoInternet, NetworkCondition.ServersUnreachable -> Res.string.settings_network_internet_offline
            NetworkCondition.Unknown, NetworkCondition.Checking -> Res.string.settings_network_internet_checking
        },
    )
    val connectionRows = connections.map { connection -> connectionTitle(connection) to connection }
    val publicIpTitle = stringResource(Res.string.settings_network_public_ip)
    val publicIpDescription = when (publicIp) {
        PublicIpState.Idle -> stringResource(Res.string.settings_network_public_ip_hint)
        PublicIpState.Loading -> stringResource(Res.string.settings_network_public_ip_loading)
        PublicIpState.Failed -> stringResource(Res.string.settings_network_public_ip_failed)
        is PublicIpState.Loaded -> listOfNotNull(
            publicIp.info.ip,
            publicIp.info.country?.takeIf(String::isNotBlank),
            publicIp.info.dataCenter?.takeIf(String::isNotBlank)?.let {
                stringResource(Res.string.settings_network_public_ip_data_center, it)
            },
        ).joinToString(" · ")
    }
    val noConnection = stringResource(Res.string.settings_network_no_connection)

    SettingsSection(
        title = stringResource(Res.string.settings_network_section_status),
        isTablet = isTablet,
    ) {
        SettingsList {
            infoRow(
                title = stringResource(Res.string.settings_network_internet),
                value = internetText,
            )
            if (NetworkDiagnosticsRepository.isSupported) {
                if (connectionRows.isEmpty()) {
                    infoRow(title = noConnection)
                }
                connectionRows.forEach { (title, connection) ->
                    infoRow(
                        title = title,
                        description = connection.ipv6.takeIf { it.isNotEmpty() }?.joinToString("\n"),
                        value = connection.ipv4.joinToString("\n").ifBlank { null },
                    )
                }
                navigationRow(
                    title = publicIpTitle,
                    description = publicIpDescription,
                    enabled = publicIp != PublicIpState.Loading,
                    onClick = NetworkDiagnosticsRepository::lookUpPublicIp,
                )
            }
        }
    }
}

@Composable
private fun connectionTitle(connection: NetworkConnectionInfo): String {
    val kind = stringResource(
        when (connection.kind) {
            NetworkKind.WiFi -> Res.string.settings_network_kind_wifi
            NetworkKind.Ethernet -> Res.string.settings_network_kind_ethernet
            NetworkKind.Cellular -> Res.string.settings_network_kind_cellular
            NetworkKind.Vpn -> Res.string.settings_network_kind_vpn
            NetworkKind.Other -> Res.string.settings_network_kind_other
        },
    )
    return "$kind · ${connection.name}"
}

@Composable
private fun SpeedTestGroup(isTablet: Boolean) {
    val state = NetworkDiagnosticsRepository.speedTest.collectAsStateWithLifecycle().value
    val running = state as? SpeedTestState.Running
    val result = state.result
    val actionTitle = stringResource(
        when (state) {
            is SpeedTestState.Running -> Res.string.settings_network_speed_stop
            SpeedTestState.Idle -> Res.string.settings_network_speed_run
            is SpeedTestState.Done, is SpeedTestState.Failed -> Res.string.settings_network_speed_again
        },
    )
    val actionDescription = when {
        running != null -> stringResource(
            when (running.phase) {
                SpeedTestPhase.Latency -> Res.string.settings_network_speed_running_latency
                SpeedTestPhase.Download -> Res.string.settings_network_speed_running_download
                SpeedTestPhase.Upload -> Res.string.settings_network_speed_running_upload
            },
        )
        state is SpeedTestState.Failed -> stringResource(Res.string.settings_network_speed_failed)
        else -> stringResource(Res.string.settings_network_speed_hint)
    }
    val pending = "—"
    val latency = result.latencyMs?.let { stringResource(Res.string.settings_network_ms_value, it) } ?: pending
    val jitter = result.jitterMs?.let { stringResource(Res.string.settings_network_ms_value, it) } ?: pending
    val download = result.downloadMbps?.let { stringResource(Res.string.settings_network_mbps_value, formatMbps(it)) } ?: pending
    val upload = result.uploadMbps?.let { stringResource(Res.string.settings_network_mbps_value, formatMbps(it)) } ?: pending
    val showResults = state !is SpeedTestState.Idle

    SettingsSection(
        title = stringResource(Res.string.settings_network_section_speed),
        isTablet = isTablet,
    ) {
        SettingsList {
            navigationRow(
                title = actionTitle,
                description = actionDescription,
                progress = running?.progress,
                onClick = {
                    if (running != null) {
                        NetworkDiagnosticsRepository.cancelSpeedTest()
                    } else {
                        NetworkDiagnosticsRepository.runSpeedTest()
                    }
                },
            )
            if (showResults) {
                infoRow(title = stringResource(Res.string.settings_network_latency), value = latency)
                infoRow(title = stringResource(Res.string.settings_network_jitter), value = jitter)
                infoRow(title = stringResource(Res.string.settings_network_download), value = download)
                infoRow(title = stringResource(Res.string.settings_network_upload), value = upload)
            }
        }
    }
}

/** A speed in megabits per second, with one decimal below 100. */
internal fun formatMbps(mbps: Double): String =
    if (mbps >= 100) mbps.roundToLong().toString() else ((mbps * 10).roundToLong() / 10.0).toString()

/** A transfer rate in bytes per second, as kB/s or MB/s. */
internal fun formatTransferRate(bytesPerSecond: Long): String {
    val megabyte = 1024.0 * 1024.0
    return if (bytesPerSecond >= megabyte) {
        "${(bytesPerSecond / megabyte * 10).roundToLong() / 10.0} MB/s"
    } else {
        "${(bytesPerSecond / 1024.0).roundToLong()} kB/s"
    }
}

private const val ConnectionsRefreshMillis = 5_000L
