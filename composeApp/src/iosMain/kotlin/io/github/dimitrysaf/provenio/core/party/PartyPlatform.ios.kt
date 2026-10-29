package io.github.dimitrysaf.provenio.core.party

// Watch parties are not built for iOS yet, as with Local sync, whose sockets they use.
internal actual object PartyPortMapper {
    actual suspend fun map(internalPort: Int): PartyPortMapping? = null

    actual suspend fun renew(mapping: PartyPortMapping): PartyPortMapping? = null

    actual suspend fun unmap(mapping: PartyPortMapping) = Unit
}

internal actual fun partyGlobalIpv6Addresses(): List<String> = emptyList()
