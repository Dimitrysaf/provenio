package io.github.dimitrysaf.provenio.core.localsync

internal actual object LocalSyncFirewall {
    actual suspend fun state(tcpPort: Int, udpPort: Int): LocalSyncFirewallState = LocalSyncFirewallState.OPEN

    actual suspend fun allow(tcpPort: Int, udpPort: Int): Boolean = true
}
