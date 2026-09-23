package com.nuvio.app.core.tracking

import com.nuvio.app.core.tracking.simkl.SimklAuthRepository
import com.nuvio.app.core.tracking.simkl.SimklMutationRepository
import com.nuvio.app.core.tracking.simkl.SimklLibraryRepository
import com.nuvio.app.core.tracking.simkl.SimklProgressRepository
import com.nuvio.app.core.tracking.simkl.SimklTrackingLibraryProvider
import com.nuvio.app.core.tracking.simkl.SimklTrackingProgressProvider
import com.nuvio.app.core.tracking.simkl.SimklWatchedSyncAdapter
import com.nuvio.app.core.tracking.simkl.SimklSyncRepository
import com.nuvio.app.core.tracking.trakt.TraktAuthRepository
import com.nuvio.app.core.tracking.trakt.TraktScrobbleRepository
import com.nuvio.app.core.tracking.trakt.TraktTrackingLibraryProvider
import com.nuvio.app.core.tracking.trakt.TraktTrackingProgressProvider
import com.nuvio.app.core.watch.watching.sync.TraktWatchedSyncAdapter

fun ensureTrackingProvidersRegistered() {
    TraktAuthRepository.descriptor
    TraktScrobbleRepository.ensureRegistered()
    SimklAuthRepository.descriptor
    SimklSyncRepository.state
    SimklLibraryRepository.uiState
    SimklProgressRepository.uiState
    SimklMutationRepository.ensureRegistered()
    TrackingProviderRegistry.registerLibraryProvider(TraktTrackingLibraryProvider)
    TrackingProviderRegistry.registerLibraryProvider(SimklTrackingLibraryProvider)
    TrackingProviderRegistry.registerWatchedProvider(TraktWatchedSyncAdapter)
    TrackingProviderRegistry.registerWatchedProvider(SimklWatchedSyncAdapter)
    TrackingProviderRegistry.registerProgressProvider(TraktTrackingProgressProvider)
    TrackingProviderRegistry.registerProgressProvider(SimklTrackingProgressProvider)
}
