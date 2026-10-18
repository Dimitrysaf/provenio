package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * The P2P Streaming page's Network section: whether the internet is reachable, each connection
 * and its addresses, the public address on request, and the speed test, which opens in a dialog.
 */
@Composable
internal fun P2pNetworkGroup(isTablet: Boolean) {
    var showSpeedTest by remember { mutableStateOf(false) }
    val speedTest = NetworkDiagnosticsRepository.speedTest.collectAsStateWithLifecycle().value
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
    val speedTestTitle = stringResource(Res.string.settings_network_section_speed)
    val speedTestDescription = speedTestSummary(speedTest)

    SettingsSection(
        title = stringResource(Res.string.settings_network_title),
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
                navigationRow(
                    title = speedTestTitle,
                    description = speedTestDescription,
                    progress = (speedTest as? SpeedTestState.Running)?.progress,
                    onClick = { showSpeedTest = true },
                )
            }
        }
    }
    if (showSpeedTest) {
        SpeedTestDialog(onDismiss = { showSpeedTest = false })
    }
}

/** The speed test row's line: the last results once there are some, otherwise what it does. */
@Composable
private fun speedTestSummary(state: SpeedTestState): String {
    val result = state.result
    val download = result.downloadMbps
    val upload = result.uploadMbps
    return when {
        state is SpeedTestState.Running -> stringResource(
            when (state.phase) {
                SpeedTestPhase.Latency -> Res.string.settings_network_speed_running_latency
                SpeedTestPhase.Download -> Res.string.settings_network_speed_running_download
                SpeedTestPhase.Upload -> Res.string.settings_network_speed_running_upload
            },
        )
        download != null || upload != null -> listOfNotNull(
            download?.let { stringResource(Res.string.settings_network_speed_summary_download, formatMbps(it)) },
            upload?.let { stringResource(Res.string.settings_network_speed_summary_upload, formatMbps(it)) },
        ).joinToString(" · ")
        state is SpeedTestState.Failed -> state.reason?.let {
            stringResource(Res.string.settings_network_speed_failed_reason, it)
        } ?: stringResource(Res.string.settings_network_speed_failed)
        else -> stringResource(Res.string.settings_network_speed_hint)
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
