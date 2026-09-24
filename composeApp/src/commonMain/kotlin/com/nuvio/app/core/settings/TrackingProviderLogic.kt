package com.nuvio.app.core.settings

import com.nuvio.app.core.tracking.simkl.SimklConnectionMode
import com.nuvio.app.core.tracking.trakt.TraktConnectionMode

internal enum class TrackingBrand(val displayName: String) {
    NUVIO("Nuvio"),
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
    TrackingBrand.NUVIO,
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
