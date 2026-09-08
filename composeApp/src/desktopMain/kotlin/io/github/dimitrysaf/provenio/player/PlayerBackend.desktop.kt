package io.github.dimitrysaf.provenio.player

// media3 is Android only and there is no desktop backend yet, so nothing is offered
// rather than listing a choice that cannot play anything.
actual fun availablePlayerBackends(): List<PlayerBackend> = emptyList()
