package io.github.dimitrysaf.provenio.core.tracking

import io.github.dimitrysaf.provenio.core.library.LibrarySourceMode
import kotlinx.serialization.Serializable

@Serializable
enum class WatchProgressSource {
    TRAKT,
    SIMKL,
    ACCOUNT_SYNC;

    val providerId: TrackingProviderId?
        get() = when (this) {
            TRAKT -> TrackingProviderId.TRAKT
            SIMKL -> TrackingProviderId.SIMKL
            ACCOUNT_SYNC -> null
        }

    companion object {
        fun fromStorage(value: String?): WatchProgressSource =
            entries.firstOrNull { it.name == value } ?: DEFAULT_WATCH_PROGRESS_SOURCE
    }
}

val DEFAULT_WATCH_PROGRESS_SOURCE: WatchProgressSource = WatchProgressSource.TRAKT
val DEFAULT_LIBRARY_SOURCE_MODE: LibrarySourceMode = LibrarySourceMode.TRAKT

fun librarySourceModeFromStorage(value: String?): LibrarySourceMode =
    LibrarySourceMode.entries.firstOrNull { it.name == value } ?: DEFAULT_LIBRARY_SOURCE_MODE

val LibrarySourceMode.providerId: TrackingProviderId?
    get() = when (this) {
        LibrarySourceMode.LOCAL -> null
        LibrarySourceMode.TRAKT -> TrackingProviderId.TRAKT
        LibrarySourceMode.SIMKL -> TrackingProviderId.SIMKL
    }

fun effectiveWatchProgressSource(
    requestedSource: WatchProgressSource,
    isProviderAuthenticated: (TrackingProviderId) -> Boolean,
): WatchProgressSource {
    val providerId = requestedSource.providerId ?: return WatchProgressSource.ACCOUNT_SYNC
    return requestedSource.takeIf { isProviderAuthenticated(providerId) }
        ?: WatchProgressSource.ACCOUNT_SYNC
}

fun effectiveLibrarySourceMode(
    requestedSource: LibrarySourceMode,
    isProviderAuthenticated: (TrackingProviderId) -> Boolean,
): LibrarySourceMode {
    val providerId = requestedSource.providerId ?: return LibrarySourceMode.LOCAL
    return requestedSource.takeIf { isProviderAuthenticated(providerId) }
        ?: LibrarySourceMode.LOCAL
}
