package io.github.dimitrysaf.provenio.core.settings

import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklConnectionMode
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktConnectionMode

internal enum class TrackingBrand(val displayName: String) {
    ACCOUNT("Provenio"),
    TRAKT("Trakt"),
    SIMKL("Simkl"),
    TMDB("TMDB"),
}

internal enum class TrackingConnectionCardMode {
    DISCONNECTED,
    AWAITING_APPROVAL,
    CONNECTED,
}

internal fun isTrackingBrandAvailable(
    brand: TrackingBrand,
    traktConnected: Boolean,
    simklConnected: Boolean,
): Boolean = when (brand) {
    TrackingBrand.ACCOUNT,
    TrackingBrand.TMDB,
    -> true
    TrackingBrand.TRAKT -> traktConnected
    TrackingBrand.SIMKL -> simklConnected
}

internal fun TraktConnectionMode.toTrackingConnectionCardMode(): TrackingConnectionCardMode = when (this) {
    TraktConnectionMode.DISCONNECTED -> TrackingConnectionCardMode.DISCONNECTED
    TraktConnectionMode.AWAITING_APPROVAL -> TrackingConnectionCardMode.AWAITING_APPROVAL
    TraktConnectionMode.CONNECTED -> TrackingConnectionCardMode.CONNECTED
}

internal fun SimklConnectionMode.toTrackingConnectionCardMode(): TrackingConnectionCardMode = when (this) {
    SimklConnectionMode.DISCONNECTED -> TrackingConnectionCardMode.DISCONNECTED
    SimklConnectionMode.AWAITING_APPROVAL -> TrackingConnectionCardMode.AWAITING_APPROVAL
    SimklConnectionMode.CONNECTED -> TrackingConnectionCardMode.CONNECTED
}
