package io.github.dimitrysaf.provenio.p2p

/**
 * Non-loopback addresses this device is reachable on.
 *
 * Shown so the user can see what a swarm would see. The public address is a separate
 * matter — that comes from the engine, since only it talks to the outside world.
 */
expect fun localNetworkAddresses(): List<String>
