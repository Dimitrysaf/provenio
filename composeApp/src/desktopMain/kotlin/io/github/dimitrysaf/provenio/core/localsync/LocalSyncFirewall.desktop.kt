package io.github.dimitrysaf.provenio.core.localsync

import io.github.dimitrysaf.provenio.desktop.DesktopDbus
import io.github.dimitrysaf.provenio.desktop.GBusTypeSystem
import io.github.dimitrysaf.provenio.desktop.GDBusCallAllowInteractiveAuthorization
import io.github.dimitrysaf.provenio.desktop.toGVariantString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val FirewallDName = "org.fedoraproject.FirewallD1"
private const val FirewallDPath = "/org/fedoraproject/FirewallD1"
private const val FirewallDConfigPath = "/org/fedoraproject/FirewallD1/config"
private const val AuthorizationTimeoutMs = 120_000

private val PortRangePattern = Regex("""'(\d+)(?:-(\d+))?', '(tcp|udp)'""")
private val ObjectPathPattern = Regex("""'(/[^']+)'""")

internal actual object LocalSyncFirewall {
    private val isLinux = System.getProperty("os.name").orEmpty().lowercase().contains("linux")

    actual suspend fun state(tcpPort: Int, udpPort: Int): LocalSyncFirewallState = withContext(Dispatchers.IO) {
        if (!isLinux) return@withContext LocalSyncFirewallState.OPEN
        if (!firewallDRunning()) return@withContext LocalSyncFirewallState.UNKNOWN
        val zone = defaultZone() ?: return@withContext LocalSyncFirewallState.UNKNOWN
        if (zone == "trusted") return@withContext LocalSyncFirewallState.OPEN
        val ports = firewallD(
            interfaceName = "$FirewallDName.zone",
            method = "getPorts",
            arguments = "(${zone.toGVariantString()},)",
        ) ?: return@withContext LocalSyncFirewallState.UNKNOWN
        val ranges = PortRangePattern.findAll(ports).map { match ->
            val start = match.groupValues[1].toInt()
            val end = match.groupValues[2].toIntOrNull() ?: start
            Triple(start, end, match.groupValues[3])
        }.toList()
        fun covered(port: Int, protocol: String) = ranges.any { (start, end, rangeProtocol) ->
            rangeProtocol == protocol && port in start..end
        }
        if (covered(tcpPort, "tcp") && covered(udpPort, "udp")) {
            LocalSyncFirewallState.OPEN
        } else {
            LocalSyncFirewallState.BLOCKED
        }
    }

    actual suspend fun allow(tcpPort: Int, udpPort: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isLinux || !firewallDRunning()) return@withContext false
        val zone = defaultZone() ?: return@withContext false
        val zoneArgument = zone.toGVariantString()
        val openings = listOf(tcpPort.toString() to "tcp", udpPort.toString() to "udp")
        openings.forEach { (port, protocol) ->
            firewallD(
                interfaceName = "$FirewallDName.zone",
                method = "addPort",
                arguments = "($zoneArgument, ${port.toGVariantString()}, ${protocol.toGVariantString()}, 0)",
                interactive = true,
            )
        }
        val zonePath = firewallD(
            path = FirewallDConfigPath,
            interfaceName = "$FirewallDName.config",
            method = "getZoneByName",
            arguments = "($zoneArgument,)",
            interactive = true,
        )?.let { ObjectPathPattern.find(it)?.groupValues?.get(1) }
        if (zonePath != null) {
            openings.forEach { (port, protocol) ->
                firewallD(
                    path = zonePath,
                    interfaceName = "$FirewallDName.config.zone",
                    method = "addPort",
                    arguments = "(${port.toGVariantString()}, ${protocol.toGVariantString()})",
                    interactive = true,
                )
            }
        }
        state(tcpPort, udpPort) == LocalSyncFirewallState.OPEN
    }

    private fun firewallDRunning(): Boolean = DesktopDbus.call(
        busName = "org.freedesktop.DBus",
        objectPath = "/org/freedesktop/DBus",
        interfaceName = "org.freedesktop.DBus",
        method = "NameHasOwner",
        arguments = "(${FirewallDName.toGVariantString()},)",
        busType = GBusTypeSystem,
    )?.contains("true") == true

    private fun defaultZone(): String? = firewallD(
        interfaceName = FirewallDName,
        method = "getDefaultZone",
        arguments = "()",
    )?.let { Regex("""'([^']+)'""").find(it)?.groupValues?.get(1) }

    private fun firewallD(
        interfaceName: String,
        method: String,
        arguments: String,
        path: String = FirewallDPath,
        interactive: Boolean = false,
    ): String? = DesktopDbus.call(
        busName = FirewallDName,
        objectPath = path,
        interfaceName = interfaceName,
        method = method,
        arguments = arguments,
        timeoutMilliseconds = if (interactive) AuthorizationTimeoutMs else 5_000,
        busType = GBusTypeSystem,
        flags = if (interactive) GDBusCallAllowInteractiveAuthorization else 0,
    )
}
