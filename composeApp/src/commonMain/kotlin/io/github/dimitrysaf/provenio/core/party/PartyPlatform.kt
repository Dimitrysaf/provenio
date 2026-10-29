package io.github.dimitrysaf.provenio.core.party

/** How the router was asked to forward the room's port. */
internal enum class PartyPortMappingMethod { UPNP, NAT_PMP }

/** A port the router forwards to this device, and what is needed to renew or remove it. */
internal data class PartyPortMapping(
    val method: PartyPortMappingMethod,
    val externalAddress: String?,
    val externalPort: Int,
    val internalPort: Int,
    val internalClient: String,
    /** How long the router keeps it, in seconds; 0 until it is removed. */
    val leaseSeconds: Int,
    // UPnP: the router's control address and the service it answers on.
    val controlUrl: String? = null,
    val serviceType: String? = null,
    // NAT-PMP: the router's address.
    val gateway: String? = null,
)

/**
 * Asks the router to forward a port from the internet to this device, with UPnP or NAT-PMP,
 * whichever it answers. Neither needs a server of our own; a router with both turned off
 * leaves the room reachable only over IPv6 or on its own network.
 */
internal expect object PartyPortMapper {
    suspend fun map(internalPort: Int): PartyPortMapping?

    /** Asks again before the lease runs out; returns the mapping as the router now has it. */
    suspend fun renew(mapping: PartyPortMapping): PartyPortMapping?

    suspend fun unmap(mapping: PartyPortMapping)
}

/** This device's IPv6 addresses that other networks can reach, if the router lets them through. */
internal expect fun partyGlobalIpv6Addresses(): List<String>
